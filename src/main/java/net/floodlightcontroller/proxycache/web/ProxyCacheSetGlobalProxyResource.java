package net.floodlightcontroller.proxycache.web;

import java.util.Iterator;

import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.devicemanager.internal.Device;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.proxycache.IProxyCacheService;
import net.floodlightcontroller.proxycache.ProxyCache;
import net.floodlightcontroller.util.FilterIterator;

import org.openflow.util.HexString;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class ProxyCacheSetGlobalProxyResource extends ServerResource {
	public static final String MAC_ERROR = "Invalid MAC address: must be a 48-bit quantity, "
			+ "expressed in hex as AA:BB:CC:DD:EE:FF";
	public static final String VLAN_ERROR = "Invalid VLAN: must be an integer in the range 0-4095";
	public static final String IPV4_ERROR = "Invalid IPv4 address: must be in dotted decimal format, "
			+ "234.0.59.1";
	public static final String DPID_ERROR = "Invalid Switch DPID: must be a 64-bit quantity, expressed in "
			+ "hex as AA:BB:CC:DD:EE:FF:00:11";
	public static final String PORT_ERROR = "Invalid Port: must be a positive integer";

	@Get("json")
	public String setGlobalProxy() {

		Form form = getQuery();
		String mac = (String) getRequestAttributes().get("mac");
		long macAddress = HexString.toLong(mac);

		ProxyCache proxycache = (ProxyCache) getContext().getAttributes().get(
				IProxyCacheService.class.getCanonicalName());

		IDeviceService deviceManager = (IDeviceService) getContext()
				.getAttributes().get(IDeviceService.class.getCanonicalName());

		// Long macAddress = null;
		Short vlan = null;
		Integer ipv4Address = null;
		Long switchDPID = null;
		Integer switchPort = null;

		// Form form = getQuery();
		String macAddrStr = form.getFirstValue("mac", true);
		String vlanStr = form.getFirstValue("vlan", true);
		String ipv4Str = form.getFirstValue("ipv4", true);
		String dpid = form.getFirstValue("dpid", true);
		String port = form.getFirstValue("port", true);

		if (macAddrStr != null) {
			try {
				macAddress = HexString.toLong(macAddrStr);
			} catch (Exception e) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST, MAC_ERROR);
				return null;
			}
		}
		if (vlanStr != null) {
			try {
				vlan = Short.parseShort(vlanStr);
				if (vlan > 4095 || vlan < 0) {
					setStatus(Status.CLIENT_ERROR_BAD_REQUEST, VLAN_ERROR);
					return null;
				}
			} catch (Exception e) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST, VLAN_ERROR);
				return null;
			}
		}
		if (ipv4Str != null) {
			try {
				ipv4Address = IPv4.toIPv4Address(ipv4Str);
			} catch (Exception e) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST, IPV4_ERROR);
				return null;
			}
		}
		if (dpid != null) {
			try {
				switchDPID = HexString.toLong(dpid);
			} catch (Exception e) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST, DPID_ERROR);
				return null;
			}
		}
		if (port != null) {
			try {
				switchPort = Integer.parseInt(port);
				if (switchPort < 0) {
					setStatus(Status.CLIENT_ERROR_BAD_REQUEST, PORT_ERROR);
					return null;
				}
			} catch (Exception e) {
				setStatus(Status.CLIENT_ERROR_BAD_REQUEST, PORT_ERROR);
				return null;
			}
		}

		@SuppressWarnings("unchecked")
		Iterator<Device> diter = (Iterator<Device>) deviceManager.queryDevices(
				macAddress, vlan, ipv4Address, switchDPID, switchPort);

		final String macStartsWith = form
				.getFirstValue("mac__startswith", true);
		final String vlanStartsWith = form.getFirstValue("vlan__startswith",
				true);
		final String ipv4StartsWith = form.getFirstValue("ipv4__startswith",
				true);
		final String dpidStartsWith = form.getFirstValue("dpid__startswith",
				true);
		final String portStartsWith = form.getFirstValue("port__startswith",
				true);

		Iterator<Device> it = new FilterIterator<Device>(diter) {
			@Override
			protected boolean matches(Device value) {
				if (macStartsWith != null) {
					if (!value.getMACAddressString().startsWith(macStartsWith))
						return false;
				}
				if (vlanStartsWith != null) {
					boolean match = false;
					for (Short v : value.getVlanId()) {
						if (v != null
								&& v.toString().startsWith(vlanStartsWith)) {
							match = true;
							break;
						}
					}
					if (!match)
						return false;
				}
				if (ipv4StartsWith != null) {
					boolean match = false;
					for (Integer v : value.getIPv4Addresses()) {
						String str;
						if (v != null
								&& (str = IPv4.fromIPv4Address(v)) != null
								&& str.startsWith(ipv4StartsWith)) {
							match = true;
							break;
						}
					}
					if (!match)
						return false;
				}
				if (dpidStartsWith != null) {
					boolean match = false;
					for (SwitchPort v : value.getAttachmentPoints(true)) {
						String str;
						if (v != null
								&& (str = HexString.toHexString(
										v.getSwitchDPID(), 8)) != null
								&& str.startsWith(dpidStartsWith)) {
							match = true;
							break;
						}
					}
					if (!match)
						return false;
				}
				if (portStartsWith != null) {
					boolean match = false;
					for (SwitchPort v : value.getAttachmentPoints(true)) {
						String str;
						if (v != null
								&& (str = Integer.toString(v.getPort())) != null
								&& str.startsWith(portStartsWith)) {
							match = true;
							break;
						}
					}
					if (!match)
						return false;
				}
				return true;
			}
		};
		String mac_str = null;
		String ip_str = null;
		while (it.hasNext()) {
			Device d = it.next();
			Integer[] ip = d.getIPv4Addresses();
			mac_str = d.getMACAddressString();
			System.out.println("mac_str:" + mac_str);
			if (ip == null || ip.length < 1)
				continue;
			ip_str = IPv4.fromIPv4Address(ip[0]);
			System.out.println("ip_str:" + ip_str);
			proxycache.setProxy(d);
		}
		if (mac_str == null || ip_str == null)
			return "fail to set web proxy";
		// return "{\"proxy\":\"" + mac+"\"}";
		String res = "set web proxy as: \n";
		res += "MAC: " + mac_str + "\n";
		res += "IP: " + ip_str + "\n";
		return res;
	}
}
