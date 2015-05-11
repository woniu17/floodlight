package net.floodlightcontroller.loadbalancer;

public class LBFlow {
	protected int network_id;
	protected double weight;
	protected LBMember member;
	public LBFlow(){
		
	}
	public LBFlow(int network_id, double weight, LBMember member){
		this.network_id = network_id;
		this.weight = weight;
		this.member = member;
	}
	public int getNetwork_id() {
		return network_id;
	}
	public void setNetwork_id(int network_id) {
		this.network_id = network_id;
	}
	public double getWeight() {
		return weight;
	}
	public void setWeight(double weight) {
		this.weight = weight;
	}
	public LBMember getMember() {
		return member;
	}
	public void setMember(LBMember member) {
		this.member = member;
	}

}
