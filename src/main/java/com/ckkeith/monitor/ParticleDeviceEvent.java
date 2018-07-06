package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.ZoneId;

import nl.infcomtec.jparticle.AnyDeviceEvent;
import nl.infcomtec.jparticle.Device;
import nl.infcomtec.jparticle.Event;

public class ParticleDeviceEvent extends AnyDeviceEvent {

	private Device device;
	private String logFileName;

	public ParticleDeviceEvent(String accountName, Device device) throws Exception {
		this.device = device;
		logFileName = Utils.getLogFileName(accountName, device.name + "_particle_log.txt");
	}
	
	private String toTabbedString(Event e) {
		StringBuilder sb = new StringBuilder();
		sb.append(e.name).append("\t")
			.append(e.data).append("\t")
			.append(device.name);
		return sb.toString();
	}
	
	@Override
	public void event(Event e) {
		try {
			LocalDateTime ldt = LocalDateTime.ofInstant(e.publishedAt.toInstant(), ZoneId.systemDefault());
			Utils.logWithGSheetsDate(ldt, toTabbedString(e), logFileName);
		} catch (Exception ex) {
			Utils.logToConsole("run()\t" + ex.getClass().getName() + "\t" + ex.getMessage());
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

    @Override
    public void finalize() {
		Utils.logToConsole("ParticleDeviceEvent.finalize() called!");
    }
}
