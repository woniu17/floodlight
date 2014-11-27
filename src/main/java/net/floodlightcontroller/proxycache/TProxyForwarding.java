package net.floodlightcontroller.proxycache;

public class TProxyForwarding {

	private Integer client_ip;
	private Short client_port;
	private byte[] client_mac;
	private Integer server_ip;
	private Short server_port;
	private byte[] server_mac;

	private TProxyServer proxy;

	public TProxyForwarding(Integer client_ip, Short client_port, byte[] client_mac, Integer server_ip,
			Short server_port, byte[] server_mac, TProxyServer proxy) {
		this.client_ip = client_ip;
		this.client_port = client_port;
		this.client_mac = client_mac;
		this.server_ip = server_ip;
		this.server_port = server_port;
		this.server_mac = server_mac;
		this.proxy = proxy;
	}

	public Integer getClient_ip() {
		return client_ip;
	}

	public void setClient_ip(Integer client_ip) {
		this.client_ip = client_ip;
	}

	public Short getClient_port() {
		return client_port;
	}

	public void setClient_port(Short client_port) {
		this.client_port = client_port;
	}

	public byte[] getClient_mac() {
		return client_mac;
	}

	public void setClient_mac(byte[] client_mac) {
		this.client_mac = client_mac;
	}

	public Integer getServer_ip() {
		return server_ip;
	}

	public void setServer_ip(Integer server_ip) {
		this.server_ip = server_ip;
	}

	public Short getServer_port() {
		return server_port;
	}

	public void setServer_port(Short server_port) {
		this.server_port = server_port;
	}

	public byte[] getServer_mac() {
		return server_mac;
	}

	public void setServer_mac(byte[] server_mac) {
		this.server_mac = server_mac;
	}

	public TProxyServer getProxy() {
		return proxy;
	}

	public void setProxy(TProxyServer proxy) {
		this.proxy = proxy;
	}

}
