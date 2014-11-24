package net.floodlightcontroller.proxycache.web;

import net.floodlightcontroller.devicemanager.IDeviceService;

import org.restlet.data.Form;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class ProxyCacheResource extends ServerResource {
	
	@Get("json")
	public String addProxy(){
		IDeviceService deviceManager = 
                (IDeviceService)getContext().getAttributes().
                    get(IDeviceService.class.getCanonicalName());
		Form form = getQuery();
        String ip = form.getFirstValue("ip", true);
        ip = (String) getRequestAttributes().get("ip");
		return "add proxy " + ip;
	}

}
