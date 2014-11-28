package net.floodlightcontroller.proxycache.web;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;
import net.floodlightcontroller.staticflowentry.web.ClearStaticFlowEntriesResource;
import net.floodlightcontroller.staticflowentry.web.ListStaticFlowEntriesResource;
import net.floodlightcontroller.staticflowentry.web.StaticFlowEntryDeleteResource;
import net.floodlightcontroller.staticflowentry.web.StaticFlowEntryPusherResource;

public class ProxyCacheWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		//System.out.println("ProxyCacheWebRoutable.getRestlet!!!!!!!!!!!!!!!!!");
		// TODO Auto-generated method stub
		Router router = new Router(context);
        router.attach("/addproxy/{mac}/json", ProxyCacheAddProxyResource.class);
        return router;
	}

	@Override
	public String basePath() {
		// TODO Auto-generated method stub
		return "/wm/proxycache";
	}

}
