package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;

import nl.infcomtec.jparticle.AnyDeviceEvent;
import nl.infcomtec.jparticle.Device;
import nl.infcomtec.jparticle.Event;

public class ParticleDeviceEvent extends AnyDeviceEvent {

	private String accountName;
	private Device device;

	public ParticleDeviceEvent(String accountName, Device device) {
		this.device = device;
		this.accountName = accountName;
	}
	
	private String toCsvString(Event e) {
		StringBuilder sb = new StringBuilder();
		sb.append(e.name).append("\t").append(e.data).append("\t").append(device.name);
		return sb.toString();
	}
	
	@Override
	public void event(Event e) {
		try {
			Utils.log(toCsvString(e), Utils.getLogFileName(accountName + "_" + device.name + ".txt"));
		} catch (Exception ex) {
			System.out.println(
					"run() : " + LocalDateTime.now().toString() + "\t" + ex.getClass().getName() + " " + ex.getMessage());
			ex.printStackTrace(new PrintStream(System.out));
		}
	}
	
    @Override
    public String forDeviceId() {
        return device.id;
    }

    @Override
    public String forDeviceName() {
        return device.name;
    }
}
