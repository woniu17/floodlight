package net.floodlightcontroller.proxycache;

public class TProxyRule {
	//match field
	private String src_ip;
	private Short src_port;
	private Integer dst_ip;
	private Short dst_prot;
	private Integer priority;
	
	//proxy
	private TProxyServer proxy;
	
	public String getSrc_ip() {
		return src_ip;
	}
	public void setSrc_ip(String src_ip) {
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
