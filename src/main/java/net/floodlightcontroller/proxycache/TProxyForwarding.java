package net.floodlightcontroller.proxycache;

public class TProxyForwarding {

	private Integer src_ip;
	private Short src_port;
	private Integer dst_ip;
	private Short dst_port;

	private TProxyServer proxy;

	public TProxyForwarding(Integer src_ip, Short src_port, Integer dst_ip,
			Short dst_port, TProxyServer proxy) {
		this.src_ip = src_ip;
		this.src_port = dst_port;
		this.dst_ip = dst_ip;
		this.dst_port = dst_port;
		this.proxy = proxy;
	}

	public Integer getSrc_ip() {
		return src_ip;
	}

	public void setSrc_ip(Integer src_ip) {
		this.src_ip = src_ip;
	}

	public Short getSrc_port() {
		return src_port;
	}

	public void setSrc_port(Short src_port) {
		this.src_port = src_port;
	}

	public Integer getDst_ip() {
		return dst_ip;
	}

	public void setDst_ip(Integer dst_ip) {
		this.dst_ip = dst_ip;
	}

	public Short getDst_port() {
		return dst_port;
	}

	public void setDst_port(Short dst_port) {
		this.dst_port = dst_port;
	}

	public TProxyServer getProxy() {
		return proxy;
	}

	public void setProxy(TProxyServer proxy) {
		this.proxy = proxy;
	}

}
