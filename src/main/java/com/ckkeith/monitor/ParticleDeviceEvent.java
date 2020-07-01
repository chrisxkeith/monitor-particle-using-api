// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.ZoneId;

import nl.infcomtec.jparticle.AnyDeviceEvent;
import nl.infcomtec.jparticle.Event;

public class ParticleDeviceEvent extends AnyDeviceEvent {

	protected ParticleDevice device;
	protected AccountMonitor accountMonitor;
	protected String logFileName;
	private ParticleEvent mostRecentEvent;

	public ParticleDeviceEvent(AccountMonitor accountMonitor, ParticleDevice device2) throws Exception {
		this.device = device2;
		this.accountMonitor = accountMonitor;
		this.logFileName = Utils.getLogFileName(accountMonitor.accountName, device2.getName() + "_particle_log.txt");
	}
	
	private void handleServerEvent(ParticleEvent e) {
		try {
			if (!accountMonitor.handleServerEvent(e.getData())) {
				String subject = "Unknown server event : " + e.getData();
				String body = "photon coreId: " + e.getCoreId() + ", publishedAt: "
						+ Utils.logDateFormat.format(e.getPublishedAt()) + ", server: " + Utils.getHostName()
						+ ", booted: " + Utils.googleSheetsDateFormat.format(Utils.getBootTime());
				if ("send test email".equalsIgnoreCase(e.getData())) {
					subject = "Test email requested.";
				}
				GMailer.sendMessageX(this.accountMonitor.runParams.emailTo, this.accountMonitor.runParams.emailTo,
					subject, body);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public void handleEvent(ParticleEvent e) {
		if (e.getName().equals("server")) {
			handleServerEvent(e);
		} else {
			LocalDateTime ldt = LocalDateTime.ofInstant(e.getPublishedAt().toInstant(), ZoneId.systemDefault());
			String s = Utils.logWithGSheetsDate(ldt, e.toTabbedString(device), logFileName);
			if (Utils.isDebug) {
				Utils.logToConsole(s);
			}
			mostRecentEvent = e;
			accountMonitor.addDataPoint(ldt, device.getName(), e.getName(), e.getData());
		}
	}

	@Override
	public void event(Event e) {
		ParticleEvent event = new ParticleEvent(e);
		try {
			handleEvent(event);
		} catch (Exception ex) {
			Utils.logToConsole("run()\t" + ex.getClass().getName() + "\t" + ex.getMessage());
			ex.printStackTrace(new PrintStream(System.out));
		}
	}
	
    @Override
    public String forDeviceId() {
        return device.getId();
    }

    @Override
    public String forDeviceName() {
        return device.getName();
    }

    @Override
    public void finalize() throws Throwable {
		super.finalize();
		Utils.logToConsole("ParticleDeviceEvent.finalize() called!");
    }

	public String getMostRecentEvent() {
		if (mostRecentEvent == null) {
			return "No mostRecentEvent\tn/a";
		}
		return mostRecentEvent.toTabbedString(device);
	}
	public String getMostRecentEventDateTime() {
		if (mostRecentEvent == null) {
			return "No mostRecentEventDateTime";
		}
		LocalDateTime ldt = LocalDateTime.ofInstant(mostRecentEvent.getPublishedAt().toInstant(), ZoneId.systemDefault());
		return Utils.googleSheetsDateFormat.format(ldt);
	}
}
