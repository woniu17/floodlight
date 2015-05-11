package net.floodlightcontroller.loadbalancer;

import java.io.IOException;
import java.util.HashMap;

import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class LBFlowResource extends ServerResource  {
	protected static Logger log = LoggerFactory.getLogger(LBFlowResource.class);
	@Put
    @Post
    public void updateLBFlowList(String postData) {
		HashMap<String, LBFlow> flow_list = null;
        try {
            flow_list=jsonToFlow(postData);
        } catch (IOException e) {
            log.error("Could not parse JSON {}", e.getMessage());
        }
        
        ILoadBalancerService lbs =
                (ILoadBalancerService)getContext().getAttributes().
                    get(ILoadBalancerService.class.getCanonicalName());
         lbs.updateFlowList(flow_list);
         for (LBFlow flow : flow_list.values()){
//        	 System.out.println("flow " + flow.network_id);
         }
    }
    

    protected HashMap<String, LBFlow> jsonToFlow(String json) throws IOException {
        
        if (json==null) return null;
        
        MappingJsonFactory f = new MappingJsonFactory();
        JsonParser jp;
        HashMap<String, LBFlow> flow_list = new HashMap<String, LBFlow>();
        
        try {
            jp = f.createJsonParser(json);
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
        
        jp.nextToken();
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected START_OBJECT");
        }
        LBFlow flow = new LBFlow();
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
                throw new IOException("Expected FIELD_NAME");
            }
            
            String n = jp.getCurrentName();
            jp.nextToken();
//            System.out.println("n: "+ n);
//            System.out.println("jp.getText(): "+ jp.getText());
            if (jp.getText().equals("")) 
                continue;
 
            if (n.equals("network_id")) {
            	  flow = new LBFlow();
            	  flow.network_id = Integer.parseInt(jp.getText());
                continue;
            } 
            if (n.equals("weight")) {
            	  flow.weight =Double.parseDouble(jp.getText());
                continue;
            } 
            if (n.equals("member")) {
            	ILoadBalancerService lbs =
                        (ILoadBalancerService)getContext().getAttributes().
                            get(ILoadBalancerService.class.getCanonicalName());
            	for(LBMember member : lbs.listMember(jp.getText())){
            		flow.member = member;
            	}
            	flow_list.put(flow.network_id + "", flow);
                continue;
            }
        
            
            log.warn("Unrecognized field {} in " +
                    "parsing Vips", 
                    jp.getText());
        }
        jp.close();
        
        return flow_list;
    }
}
