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

	public ParticleDeviceEvent(Device device) throws Exception {
		this.device = device;
		logFileName = Utils.getLogFileName(device.name + "_particle_log.txt");
	}
	
	private String toTabbedString(Event e) {
		StringBuilder sb = new StringBuilder();
		LocalDateTime ldt = LocalDateTime.ofInstant(e.publishedAt.toInstant(), ZoneId.systemDefault());
		String d = Utils.googleSheetsDateFormat.format(ldt);
		sb.append(d).append("\t")
			.append(e.name).append("\t")
			.append(e.data).append("\t")
			.append(device.name);
		return sb.toString();
	}
	
	@Override
	public void event(Event e) {
		try {
			Utils.log(toTabbedString(e), logFileName);
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
}
