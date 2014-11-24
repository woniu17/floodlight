package net.floodlightcontroller.proxycache;

public class TProxyCache {
	
	private Integer src_ip;
	private Short src_port;
	private Integer dst_ip;
	private Short dst_prot;
	
	private Integer proxy_ip;
	private Short proxy_port;
	private String proxy_mac;
	
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
	public Short getDst_prot() {
		return dst_prot;
	}
	public void setDst_prot(Short dst_prot) {
		this.dst_prot = dst_prot;
	}
	public Integer getProxy_ip() {
		return proxy_ip;
	}
	public void setProxy_ip(Integer proxy_ip) {
		this.proxy_ip = proxy_ip;
	}
	public Short getProxy_port() {
		return proxy_port;
	}
	public void setProxy_port(Short proxy_port) {
		this.proxy_port = proxy_port;
	}
	public String getProxy_mac() {
		return proxy_mac;
	}
	public void setProxy_mac(String proxy_mac) {
		this.proxy_mac = proxy_mac;
	}
	

}
