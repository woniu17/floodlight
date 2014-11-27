package net.floodlightcontroller.proxycache;

import net.floodlightcontroller.devicemanager.IDevice;

public class TProxyServer {

	private IDevice device;
	public TProxyServer(IDevice device){
		this.device = device;
	}
	
	
	public IDevice getDevice() {
		return device;
	}


	public void setDevice(IDevice device) {
		this.device = device;
	}


	public Integer getIP(){
		return this.device.getIPv4Addresses()[0];
	}
	public String getMAC(){
		return this.device.getMACAddressString();
	}
}
