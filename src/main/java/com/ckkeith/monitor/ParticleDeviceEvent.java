package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.ZoneId;

import nl.infcomtec.jparticle.AnyDeviceEvent;
import nl.infcomtec.jparticle.Device;
import nl.infcomtec.jparticle.Event;

public class ParticleDeviceEvent extends AnyDeviceEvent {

	protected Device device;
	protected AccountMonitor accountMonitor;
	protected String logFileName;
	private Event mostRecentEvent;

	public ParticleDeviceEvent(AccountMonitor accountMonitor, Device device) throws Exception {
		this.device = device;
		this.accountMonitor = accountMonitor;
		this.logFileName = Utils.getLogFileName(accountMonitor.accountName, device.name + "_particle_log.txt");
	}
	
	private String toTabbedString(Event e) {
		StringBuilder sb = new StringBuilder();
		sb.append(e.name).append("\t")
			.append(e.data).append("\t")
			.append(device.name).append("\t")
			.append(Utils.getHostName());
		return sb.toString();
	}
	
	public void handleEvent(Event e) {
		LocalDateTime ldt = LocalDateTime.ofInstant(e.publishedAt.toInstant(), ZoneId.systemDefault());
		String s = Utils.logWithGSheetsDate(ldt, toTabbedString(e), logFileName);
		if (Utils.isDebug) {
			Utils.logToConsole(s);
		}
		mostRecentEvent = e;
		accountMonitor.addDataPoint(ldt, e.name, e.data);
	}

	@Override
	public void event(Event e) {
		try {
			handleEvent(e);
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
    public void finalize() throws Throwable {
		super.finalize();
		Utils.logToConsole("ParticleDeviceEvent.finalize() called!");
    }

	public String getMostRecentEvent() {
		if (mostRecentEvent == null) {
			return "No mostRecentEvent";
		}
		return toTabbedString(mostRecentEvent);
	}
	public String getMostRecentEventDateTime() {
		if (mostRecentEvent == null) {
			return "No mostRecentEventDateTime";
		}
		LocalDateTime ldt = LocalDateTime.ofInstant(mostRecentEvent.publishedAt.toInstant(), ZoneId.systemDefault());
		return Utils.googleSheetsDateFormat.format(ldt);
	}
}
