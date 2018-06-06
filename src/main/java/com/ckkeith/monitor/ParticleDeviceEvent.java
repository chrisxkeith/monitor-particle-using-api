package com.ckkeith.monitor;

import nl.infcomtec.jparticle.AnyDeviceEvent;
import nl.infcomtec.jparticle.Event;

public class ParticleDeviceEvent extends AnyDeviceEvent {

	private String accountName;
	private String deviceName;

	public ParticleDeviceEvent(String accountName, String deviceName) {
		this.deviceName = deviceName;
		this.accountName = accountName;
	}
	
	private String getLogFileName() {
		return accountName + "_" + deviceName + ".txt";
	}
	
	private String toCsvString(Event e) {
		return e.toString();
	}
	
	@Override
	public void event(Event e) {
		Utils.log(toCsvString(e), getLogFileName());
	}
	
    @Override
    public String forDeviceName() {
        return deviceName;
    }
}
