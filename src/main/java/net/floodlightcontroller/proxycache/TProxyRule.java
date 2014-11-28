package net.floodlightcontroller.proxycache;

public class TProxyRule {
	//match field
	private String client_ip;
	private Short client_port;
	private String server_ip;
	private Short server_port;
	private Integer priority;
	
	//proxy
	private TProxyServer proxy;
	
	
	public String getClient_ip() {
		return client_ip;
	}
	public void setClient_ip(String client_ip) {
		this.client_ip = client_ip;
	}
	public Short getClient_port() {
		return client_port;
	}
	public void setClient_port(Short client_port) {
		this.client_port = client_port;
	}
	public String getServer_ip() {
		return server_ip;
	}
	public void setServer_ip(String server_ip) {
		this.server_ip = server_ip;
	}
	public Short getServer_port() {
		return server_port;
	}
	public void setServer_port(Short server_port) {
		this.server_port = server_port;
	}
	public Integer getPriority() {
		return priority;
	}
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	public TProxyServer getProxy() {
		return proxy;
	}
	public void setProxy(TProxyServer proxy) {
		this.proxy = proxy;
	}
	

}
