package net.floodlightcontroller.proxycache;

import net.floodlightcontroller.devicemanager.internal.Device;

public class TProxyServer {

	private Device device;
	public TProxyServer(Device device){
		this.device = device;
	}
	public Device getDevice() {
		return device;
	}
	public void setDevice(Device device) {
		this.device = device;
	}
	
	public Integer getIP(){
		return this.device.getIPv4Addresses()[0];
	}
	public String getMAC(){
		return this.device.getMACAddressString();
	}
}
