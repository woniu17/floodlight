package cn.edu.fzu.cmcs.emil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.firewall.FirewallRule;
import net.floodlightcontroller.forwarding.Forwarding;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.Route;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;

public class ProxyCache extends Forwarding implements IProxyCacheServer {

	@Override
	public Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi,
			IRoutingDecision decision, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		System.out.println("ProxyCache.processPacketInMessage!!!!!!!");
		// return super.processPacketInMessage(sw, pi, decision, cntx);
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

		// If a decision has been made we obey it
		// otherwise we just forward
		if (decision != null) {
			if (log.isTraceEnabled()) {
				log.trace("Forwaring decision={} was made for PacketIn={}",
						decision.getRoutingAction().toString(), pi);
			}

			switch (decision.getRoutingAction()) {
			case NONE:
				// don't do anything
				return Command.CONTINUE;
			case FORWARD_OR_FLOOD:
			case FORWARD:
				doForwardFlow(sw, pi, cntx, false);
				return Command.CONTINUE;
			case MULTICAST:
				// treat as broadcast
				doFlood(sw, pi, cntx);
				return Command.CONTINUE;
			case DROP:
				doDropFlow(sw, pi, decision, cntx);
				return Command.CONTINUE;
			default:
				log.error("Unexpected decision made for this packet-in={}", pi,
						decision.getRoutingAction());
				return Command.CONTINUE;
			}
		} else {
			if (log.isTraceEnabled()) {
				log.trace("No decision was made for PacketIn={}, forwarding",
						pi);
			}

			if (eth.isBroadcast() || eth.isMulticast()) {
				// For now we treat multicast as broadcast
				doFlood(sw, pi, cntx);
			} else {
				// doForwardFlow(sw, pi, cntx, false);
				checkAndDoForwardFlow(sw, pi, cntx, false);
			}
		}

		return Command.CONTINUE;
	}

	private void checkAndDoForwardFlow(IOFSwitch sw, OFPacketIn pi,
			FloodlightContext cntx, boolean requestFlowRemovedNotifn) {
		IDevice srcDevice = IDeviceService.fcStore.get(cntx,
				IDeviceService.CONTEXT_SRC_DEVICE);
		System.out.println("src:" + srcDevice.getDeviceKey());
		IDevice dstDevice = IDeviceService.fcStore.get(cntx,
				IDeviceService.CONTEXT_DST_DEVICE);
		System.out.println("dst:" + dstDevice.getDeviceKey());
		Collection<IDevice> devices = (Collection<IDevice>) this.deviceManager
				.getAllDevices();
		for (IDevice d : devices) {
			System.out.println("key:" + d.getDeviceKey());
			System.out.println("mac:" + d.getMACAddressString());
			Integer[] ip = d.getIPv4Addresses();
			System.out.print("ip:");
			boolean first = true;
			for(Integer i:ip){
				if(first){
					System.out.print(i);
					first = false;
				}
				System.out.print("."+i);
			}
			System.out.println();
		}
		doForwardFlow(sw, pi, cntx, false);
	}

	@Override
	protected void doForwardFlow(IOFSwitch sw, OFPacketIn pi,
			FloodlightContext cntx, boolean requestFlowRemovedNotifn) {
		// TODO Auto-generated method stub
		// super.doForwardFlow(sw, pi, cntx, requestFlowRemovedNotifn);
		OFMatch match = new OFMatch();
		match.loadFromPacket(pi.getPacketData(), pi.getInPort());

		// Check if we have the location of the destination
		IDevice dstDevice = IDeviceService.fcStore.get(cntx,
				IDeviceService.CONTEXT_DST_DEVICE);

		if (dstDevice != null) {
			IDevice srcDevice = IDeviceService.fcStore.get(cntx,
					IDeviceService.CONTEXT_SRC_DEVICE);
			Long srcIsland = topology.getL2DomainId(sw.getId());

			if (srcDevice == null) {
				log.debug("No device entry found for source device");
				return;
			}
			if (srcIsland == null) {
				log.debug("No openflow island found for source {}/{}",
						sw.getStringId(), pi.getInPort());
				return;
			}

			// Validate that we have a destination known on the same island
			// Validate that the source and destination are not on the same
			// switchport
			boolean on_same_island = false;
			boolean on_same_if = false;
			for (SwitchPort dstDap : dstDevice.getAttachmentPoints()) {
				long dstSwDpid = dstDap.getSwitchDPID();
				Long dstIsland = topology.getL2DomainId(dstSwDpid);
				if ((dstIsland != null) && dstIsland.equals(srcIsland)) {
					on_same_island = true;
					if ((sw.getId() == dstSwDpid)
							&& (pi.getInPort() == dstDap.getPort())) {
						on_same_if = true;
					}
					break;
				}
			}

			if (!on_same_island) {
				// Flood since we don't know the dst device
				if (log.isTraceEnabled()) {
					log.trace("No first hop island found for destination "
							+ "device {}, Action = flooding", dstDevice);
				}
				doFlood(sw, pi, cntx);
				return;
			}

			if (on_same_if) {
				if (log.isTraceEnabled()) {
					log.trace("Both source and destination are on the same "
							+ "switch/port {}/{}, Action = NOP", sw.toString(),
							pi.getInPort());
				}
				return;
			}

			// Install all the routes where both src and dst have attachment
			// points. Since the lists are stored in sorted order we can
			// traverse the attachment points in O(m+n) time
			SwitchPort[] srcDaps = srcDevice.getAttachmentPoints();
			Arrays.sort(srcDaps, clusterIdComparator);
			SwitchPort[] dstDaps = dstDevice.getAttachmentPoints();
			Arrays.sort(dstDaps, clusterIdComparator);

			int iSrcDaps = 0, iDstDaps = 0;

			while ((iSrcDaps < srcDaps.length) && (iDstDaps < dstDaps.length)) {
				SwitchPort srcDap = srcDaps[iSrcDaps];
				SwitchPort dstDap = dstDaps[iDstDaps];

				// srcCluster and dstCluster here cannot be null as
				// every switch will be at least in its own L2 domain.
				Long srcCluster = topology
						.getL2DomainId(srcDap.getSwitchDPID());
				Long dstCluster = topology
						.getL2DomainId(dstDap.getSwitchDPID());

				int srcVsDest = srcCluster.compareTo(dstCluster);
				if (srcVsDest == 0) {
					if (!srcDap.equals(dstDap)) {
						Route route = routingEngine.getRoute(
								srcDap.getSwitchDPID(),
								(short) srcDap.getPort(),
								dstDap.getSwitchDPID(),
								(short) dstDap.getPort(), 0); // cookie = 0,
																// i.e., default
																// route
						if (route != null) {
							if (log.isTraceEnabled()) {
								log.trace("pushRoute match={} route={} "
										+ "destination={}:{}", new Object[] {
										match, route, dstDap.getSwitchDPID(),
										dstDap.getPort() });
							}
							long cookie = AppCookie.makeCookie(
									FORWARDING_APP_ID, 0);

							// if there is prior routing decision use wildcard
							Integer wildcard_hints = null;
							IRoutingDecision decision = null;
							if (cntx != null) {
								decision = IRoutingDecision.rtStore.get(cntx,
										IRoutingDecision.CONTEXT_DECISION);
							}
							if (decision != null) {
								wildcard_hints = decision.getWildcards();
							} else {
								// L2 only wildcard if there is no prior route
								// decision
								wildcard_hints = ((Integer) sw
										.getAttribute(IOFSwitch.PROP_FASTWILDCARDS))
										.intValue()
										& ~OFMatch.OFPFW_IN_PORT
										& ~OFMatch.OFPFW_DL_VLAN
										& ~OFMatch.OFPFW_DL_SRC
										& ~OFMatch.OFPFW_DL_DST
										& ~OFMatch.OFPFW_NW_SRC_MASK
										& ~OFMatch.OFPFW_NW_DST_MASK;
							}

							pushRoute(route, match, wildcard_hints, pi,
									sw.getId(), cookie, cntx,
									requestFlowRemovedNotifn, false,
									OFFlowMod.OFPFC_ADD);
						}
					}
					iSrcDaps++;
					iDstDaps++;
				} else if (srcVsDest < 0) {
					iSrcDaps++;
				} else {
					iDstDaps++;
				}
			}
		} else {
			// Flood since we don't know the dst device
			doFlood(sw, pi, cntx);
		}
	}

	// IproxyCacheServer
	@Override
	public void addCache(int cache) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteCache(int cache) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<Integer> listCache() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addProxy(int host, int cache) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteProxy(int host, int cache) {
		// TODO Auto-generated method stub

	}

	@Override
	public ConcurrentHashMap<Integer, Integer> listProxyPair() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer listProxy(int host) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IpWithType getHostLink(int host) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FirewallRule> getRuleList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addRule(FirewallRule rule) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteRule(FirewallRule rule) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean checkCache(int ip) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IpWithType getHostLinkWithRule(String src, short srcport,
			String dst, short dstport) {
		// TODO Auto-generated method stub
		return null;
	}

}
