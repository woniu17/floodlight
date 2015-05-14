/**
 *    Copyright 2013, Big Switch Networks, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.floodlightcontroller.loadbalancer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Data structure for Load Balancer based on
 * Quantum proposal http://wiki.openstack.org/LBaaS/CoreResourceModel/proposal 
 * 
 * @author KC Wang
 */

@JsonSerialize(using=LBMemberSerializer.class)
public class LBMember {
	static final short RUNSTATUS_ACTIVE = 0;
	static final short RUNSTATUS_STANDBY = 1;
	static final short RUNSTATUS_TOSTOP = 2;
	static final short RUNSTATUS_FAIL = 3;
    protected String id;
    protected int address;
    protected short port;
    protected String macString;
    
    protected int connectionLimit;
    protected short adminState;
    protected short status;

    protected String poolId;
    protected String vipId;
    
    protected double weight;
    protected short runStatus;
    
    public LBMember() {
        id = String.valueOf((int) (Math.random()*10000));
        address = 0;
        macString = null;
        port = 0;
        
        connectionLimit = 0;
        adminState = 0;
        status = 0;
        poolId = null;
        vipId = null;
        
        weight = 0;
        runStatus = LBMember.RUNSTATUS_ACTIVE;
    }
    public boolean isActive(){
    	return this.runStatus == LBMember.RUNSTATUS_ACTIVE;
    }
}
