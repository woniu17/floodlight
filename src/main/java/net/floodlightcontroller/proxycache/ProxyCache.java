package net.floodlightcontroller.proxycache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.annotations.LogMessageDoc;
import net.floodlightcontroller.core.annotations.LogMessageDocs;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.counter.ICounterStoreService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.firewall.FirewallRule;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.proxycache.web.ProxyCacheWebRoutable;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.ForwardingBase;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

public class ProxyCache extends ForwardingBase implements IProxyCacheService,
		IFloodlightModule {

	private Command command;
	protected IRestApiService restApi;
	// Map.key = client_ip
	private Map<String, TProxyRule> rule_map;
	// Map.key = client_ip + client_port
	private Map<String, TProxyForwarding> fwd_map;
	//
	private List<TProxyServer> proxy_list;

	// ForwardingBase
	@Override
	public Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi,
			IRoutingDecision decision, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		System.out.println("ProxyCache.processPacketInMessage!!!!!!!");
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

		command = Command.CONTINUE;
		checkAndDoForwardFlow(sw, pi, cntx, false);

		return command;
	}

	@Override
	public boolean pushRoute(Route route, OFMatch match,
			Integer wildcard_hints, OFPacketIn pi, long pinSwitch, long cookie,
			FloodlightContext cntx, boolean reqeustFlowRemovedNotifn,
			boolean doFlush, short flowModCommand) {
		boolean srcSwitchIncluded = false;
		OFFlowMod fm = (OFFlowMod) floodlightProvider.getOFMessageFactory()
				.getMessage(OFType.FLOW_MOD);
		OFActionOutput action = new OFActionOutput();
		action.setMaxLength((short) 0xffff);
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(action);

		fm.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
				.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
				.setBufferId(OFPacketOut.BUFFER_ID_NONE)
				.setCookie(cookie)
				.setCommand(flowModCommand)
				.setMatch(match)
				.setActions(actions)
				.setLengthU(
						OFFlowMod.MINIMUM_LENGTH
								+ OFActionOutput.MINIMUM_LENGTH);

		List<NodePortTuple> switchPortList = route.getPath();
		for (int indx = 1; indx > 0; indx -= 2) {
			// indx and indx-1 will always have the same switch DPID.
			long switchDPID = switchPortList.get(indx).getNodeId();
			IOFSwitch sw = floodlightProvider.getSwitch(switchDPID);
			if (sw == null) {
				if (log.isWarnEnabled()) {
					log.warn("Unable to push route, switch at DPID {} "
							+ "not available", switchDPID);
				}
				return srcSwitchIncluded;
			}

			// set the match.
			fm.setMatch(wildcard(match, sw, wildcard_hints));

			// set buffer id if it is the source switch
			if (1 == indx) {
				// Set the flag to request flow-mod removal notifications only
				// for the
				// source switch. The removal message is used to maintain the
				// flow
				// cache. Don't set the flag for ARP messages - TODO generalize
				// check
				if ((reqeustFlowRemovedNotifn)
						&& (match.getDataLayerType() != Ethernet.TYPE_ARP)) {
					/**
					 * with new flow cache design, we don't need the flow
					 * removal message from switch anymore
					 * fm.setFlags(OFFlowMod.OFPFF_SEND_FLOW_REM);
					 */
					match.setWildcards(fm.getMatch().getWildcards());
				}
			}

			short outPort = switchPortList.get(indx).getPortId();
			short inPort = switchPortList.get(indx - 1).getPortId();
			// set input and output ports on the switch
			fm.getMatch().setInputPort(inPort);
			((OFActionOutput) fm.getActions().get(0)).setPort(outPort);

			try {
				counterStore.updatePktOutFMCounterStoreLocal(sw, fm);
				if (log.isTraceEnabled()) {
					log.trace("Pushing Route flowmod routeIndx={} "
							+ "sw={} inPort={} outPort={}", new Object[] {
							indx, sw, fm.getMatch().getInputPort(), outPort });
				}
				messageDamper.write(sw, fm, cntx);
				if (doFlush) {
					sw.flush();
					counterStore.updateFlush();
				}

				// Push the packet out the source switch
				if (sw.getId() == pinSwitch) {
					// TODO: Instead of doing a packetOut here we could also
					// send a flowMod with bufferId set....
					pushPacket(sw, pi, false, outPort, cntx);
					srcSwitchIncluded = true;
				}
			} catch (IOException e) {
				log.error("Failure writing flow mod", e);
			}

			try {
				fm = fm.clone();
			} catch (CloneNotSupportedException e) {
				log.error("Failure cloning flow mod", e);
			}
		}

		return srcSwitchIncluded;
	}

	@Override
	protected void pushPacket(IOFSwitch sw, OFPacketIn pi, boolean useBufferId,
			short outport, FloodlightContext cntx) {
		if (pi == null) {
			return;
		}

		// The assumption here is (sw) is the switch that generated the
		// packet-in. If the input port is the same as output port, then
		// the packet-out should be ignored.
		if (pi.getInPort() == outport) {
			if (log.isDebugEnabled()) {
				log.debug("Attempting to do packet-out to the same "
						+ "interface as packet-in. Dropping packet. "
						+ " SrcSwitch={}, pi={}", new Object[] { sw, pi });
				return;
			}
		}

		if (log.isTraceEnabled()) {
			log.trace("PacketOut srcSwitch={} pi={}", new Object[] { sw, pi });
		}

		OFPacketOut po = (OFPacketOut) floodlightProvider.getOFMessageFactory()
				.getMessage(OFType.PACKET_OUT);

		// set actions
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(new OFActionOutput(outport, (short) 0xffff));

		po.setActions(actions).setActionsLength(
				(short) OFActionOutput.MINIMUM_LENGTH);
		short poLength = (short) (po.getActionsLength() + OFPacketOut.MINIMUM_LENGTH);

		if (useBufferId) {
			po.setBufferId(pi.getBufferId());
		} else {
			po.setBufferId(OFPacketOut.BUFFER_ID_NONE);
		}

		if (po.getBufferId() == OFPacketOut.BUFFER_ID_NONE) {
			byte[] packetData = pi.getPacketData();
			poLength += packetData.length;
			po.setPacketData(packetData);
		}

		po.setInPort(pi.getInPort());
		po.setLength(poLength);

		try {
			counterStore.updatePktOutFMCounterStoreLocal(sw, po);
			messageDamper.write(sw, po, cntx);

		} catch (IOException e) {
			log.error("Failure writing packet out", e);
		}
	}

	private void checkAndDoForwardFlow(IOFSwitch sw, OFPacketIn pi,
			FloodlightContext cntx, boolean requestFlowRemovedNotifn) {
		IDevice srcDevice = IDeviceService.fcStore.get(cntx,
				IDeviceService.CONTEXT_SRC_DEVICE);
		IDevice dstDevice = IDeviceService.fcStore.get(cntx,
				IDeviceService.CONTEXT_DST_DEVICE);

		if (srcDevice == null || dstDevice == null) {
			return;
		}
		/**
		 * if not HTTP return if HTTP REQUEST if matchs in proxy_rule_table
		 * dstDevice <-- proxyDevice else //HTTP REPLAY if dstDevice !=
		 * proxyDevice ( accroding proxy_forwarding_table ) return dstDevice <--
		 * web server(according proxy_cache_table)
		 */
		// if not HTTP return
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		if (!(eth.getPayload() instanceof IPv4)) {
			return;
		}
		IPv4 ipv4 = (IPv4) eth.getPayload();
		if (!(ipv4.getPayload() instanceof TCP)) {
			return;
		}
		TCP tcp = (TCP) ipv4.getPayload();
		Short src_port = tcp.getSourcePort();
		Short dst_port = tcp.getDestinationPort();
		if ((src_port != 80) && (dst_port != 80)) {
			return;
		}
		Integer src_ip = ipv4.getSourceAddress();
		Integer dst_ip = ipv4.getDestinationAddress();
		// if HTTP request
		if (dst_port == 80) {
			// if match in rule_map
			String key = src_ip + "";
			if (rule_map == null) {
				return;
			}
			TProxyRule rule = rule_map.get(key);
			if (rule == null) {
				return;
			}
			TProxyForwarding fwd = new TProxyForwarding(src_ip, src_port,
					dst_ip, dst_port, rule.getProxy());
			if (fwd_map == null) {
				fwd_map = new HashMap<String, TProxyForwarding>();
			}
			key = src_ip + ":" + src_port + "";
			fwd_map.put(key, fwd);
			// dstDevice <- proxy.device
			IDeviceService.fcStore.put(cntx, IDeviceService.CONTEXT_DST_DEVICE,
					rule.getProxy().getDevice());
			//
		} else {
		// else if HTTP response
			//if match in fwd_map
			if(fwd_map == null){
				return;
			}
			String key = src_ip + ":" + src_port + "";
			TProxyForwarding fwd = fwd_map.get(key);
			if(fwd == null){
				return;
			}
			//
		}

		if (sw.getId() != 1) {
			return;
		}
		// System.out.println("(eth.getPayload() instanceof ARP):"
		// + (eth.getPayload() instanceof ARP));
		// System.out.println("(eth.getPayload() instanceof IPv4):"
		// + (eth.getPayload() instanceof IPv4));
		// System.out.println("src:" + srcDevice.getDeviceKey());
		// System.out.println("dst:" + dstDevice.getDeviceKey());
		// IDeviceService.fcStore.put(cntx, IDeviceService.CONTEXT_SRC_DEVICE,
		// dstDevice);
		// IDeviceService.fcStore.put(cntx, IDeviceService.CONTEXT_DST_DEVICE,
		// srcDevice);

		// Collection<IDevice> devices = (Collection<IDevice>)
		// this.deviceManager
		// .getAllDevices();
		// for (IDevice d : devices) {
		// System.out.println("key:" + d.getDeviceKey());
		// System.out.println("mac:" + d.getMACAddressString());
		// Integer[] ip = d.getIPv4Addresses();
		// System.out.print("ip:");
		// boolean first = true;
		// for (Integer i : ip) {
		// if (first) {
		// System.out.print(i);
		// first = false;
		// }
		// System.out.print("." + i);
		// }
		// System.out.println();
		// }
		doForwardFlow(sw, pi, cntx, false);
		this.command = Command.STOP;
	}

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
			// doFlood(sw, pi, cntx);
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
		System.out.println("addProxy!!!!!!");
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

	// IOFMessageListener
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "proxycache";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return super.isCallbackOrderingPrereq(type, name);
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		// We need to go before forwarding
		return (type.equals(OFType.PACKET_IN) && name.equals("forwarding"));
	}

	// IFloodlightModule method
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IProxyCacheService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IProxyCacheService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IDeviceService.class);
		l.add(IRoutingService.class);
		l.add(ITopologyService.class);
		l.add(ICounterStoreService.class);
		l.add(IRestApiService.class);
		return l;
	}

	@Override
	@LogMessageDocs({
			@LogMessageDoc(level = "WARN", message = "Error parsing flow idle timeout, "
					+ "using default of {number} seconds", explanation = "The properties file contains an invalid "
					+ "flow idle timeout", recommendation = "Correct the idle timeout in the "
					+ "properties file."),
			@LogMessageDoc(level = "WARN", message = "Error parsing flow hard timeout, "
					+ "using default of {number} seconds", explanation = "The properties file contains an invalid "
					+ "flow hard timeout", recommendation = "Correct the hard timeout in the "
					+ "properties file.") })
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		super.init();
		this.floodlightProvider = context
				.getServiceImpl(IFloodlightProviderService.class);
		this.deviceManager = context.getServiceImpl(IDeviceService.class);
		this.routingEngine = context.getServiceImpl(IRoutingService.class);
		this.topology = context.getServiceImpl(ITopologyService.class);
		this.counterStore = context.getServiceImpl(ICounterStoreService.class);
		this.restApi = context.getServiceImpl(IRestApiService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		super.startUp();
		if (restApi != null) {
			restApi.addRestletRoutable(new ProxyCacheWebRoutable());
		} else {
			System.out.println("ProxyCache.startUp: restApi == null");
		}
	}

}
