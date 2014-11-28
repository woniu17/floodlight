package net.floodlightcontroller.proxycache.web;

import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.proxycache.IProxyCacheService;
import net.floodlightcontroller.proxycache.ProxyCache;
import net.floodlightcontroller.proxycache.TProxyRule;
import net.floodlightcontroller.proxycache.TProxyServer;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class ProxyCacheGetProxyRuleListResource extends ServerResource {
	@Get("json")
	public List<TProxyRule> getProxyRuleList(){
		
		ProxyCache proxycache = (ProxyCache) getContext().getAttributes().get(
				IProxyCacheService.class.getCanonicalName());
		
		List<TProxyRule> rule_list = new ArrayList<TProxyRule>();
		
		String client_ip = "10.0.0.";
		Short client_port = 1234;
		String server_ip = "20.0.0.";
		Short server_port = 80;
		for(int i=1;i<=5;i++){
			TProxyRule rule = new TProxyRule();
			rule.setClient_ip(client_ip+i);
			rule.setClient_port((short)(client_port*10+i));
			rule.setServer_ip(server_ip+i);
			rule.setServer_port(server_port);
			if(proxycache != null && proxycache.getProxyList()!=null){
				if(proxycache.getProxyList().size() > 0)
					rule.setProxy(proxycache.getProxyList().get(0));
			}
			rule_list.add(rule);
		}
		return rule_list;
	}
}
