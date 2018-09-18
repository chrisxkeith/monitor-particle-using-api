package com.ckkeith.monitor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import nl.infcomtec.jparticle.Device;
import nl.infcomtec.jparticle.Event;

public class StoveThermistorEvent extends ParticleDeviceEvent {

	class ThermistorData {
		final private DateTimeFormatter logDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");

		LocalDateTime deviceTime = null;
		int degreesInF = Integer.MIN_VALUE;

		ThermistorData(String data) {
			String fields[] = data.split("\\|");
			if (fields.length == 1) {
				deviceTime = LocalDateTime.now(); // Figure out a better value for this, or is this just bad data?
				degreesInF = Integer.parseInt(fields[0]);
			} else if (fields.length >= 3) {
				deviceTime = LocalDateTime.parse(fields[1], logDateFormat);
				degreesInF = Integer.parseInt(fields[2]);
			}
		}
	}

	private final DateTimeFormatter googleSheetsDateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

	private final int temperatureLimit = 80; // degrees F
	private final int timeLimit = 30; // minutes before alert is logged.

	ThermistorData lastDataSeen = null;
	ThermistorData lastDataOverLimit = null;

	public StoveThermistorEvent(String accountName, Device device) throws Exception {
		super(accountName, device);
	}

	private final int SEND_INTERVAL_IN_MINUTES = 30;
	private LocalDateTime lastSent = null;

	private void sendEmail(String warn, Event e) {
		if (lastSent == null
				|| Duration.between(lastSent, LocalDateTime.now()).toMinutes() > SEND_INTERVAL_IN_MINUTES) {
			// For the future : Is there any case to be made for turning off the email at some point?
			// E.g., a sensor going bad?

			GMailer.sendMessageX("chris.keith@gmail.com", "chris.keith@gmail.com", warn, e.data);
			lastSent = LocalDateTime.now();
		}
	}

	// public for automated testing only.
	public String checkStoveLeftOn(Event e) {
		String warn = "";
		if (e.name.equals("Thermistor 01 sensor:")) {
			ThermistorData t = new ThermistorData(e.data);
			if (t.degreesInF > this.temperatureLimit) {
				if (lastDataOverLimit == null) {
					lastDataOverLimit = t;
				} else {
					long minutes = Duration.between(lastDataOverLimit.deviceTime, t.deviceTime).toMinutes();
					if (minutes > timeLimit) {
						warn = "Temperature has been over " + temperatureLimit + " degrees F for " + minutes
								+ " minutes (from " + googleSheetsDateFormat.format(lastDataOverLimit.deviceTime)
								+ " to " + googleSheetsDateFormat.format(t.deviceTime) + ")\t" + Utils.getHostName();
						Utils.logWithGSheetsDate(LocalDateTime.now(), "Warning\t" + warn, logFileName);
						sendEmail(warn, e);
					}
				}
			} else {
				lastDataOverLimit = null;
				lastSent = null;
			}
			lastDataSeen = t;
		}
		return warn;
	}

	public void handleEvent(Event e) {
		super.handleEvent(e);
		checkStoveLeftOn(e);
	}
}
