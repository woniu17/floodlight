package cn.edu.fzu.cmcs.emil;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.firewall.FirewallRule;

public interface IProxyCacheServer extends IFloodlightService {
	
	void addCache(int cache);
	
	void deleteCache(int cache);
	
	Set<Integer> listCache();
	
	void addProxy(int host, int cache);
	
	void deleteProxy(int host, int cache);
	
	ConcurrentHashMap<Integer, Integer> listProxyPair();
	
	Integer listProxy(int host);
	
	IpWithType getHostLink(int host);
	
	List<FirewallRule> getRuleList();
	
	void addRule(FirewallRule rule);
	
	void deleteRule(FirewallRule rule);
	
	boolean checkCache(int ip);
	
	IpWithType getHostLinkWithRule(String src, short srcport, String dst, short dstport);

}
