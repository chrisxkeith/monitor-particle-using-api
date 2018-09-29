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
		double degreesInF = Double.MIN_VALUE;

		ThermistorData(String fields[]) {
			deviceTime = LocalDateTime.parse(fields[1], logDateFormat);
			degreesInF = Double.parseDouble(fields[2]);
		}
	}

	private final int temperatureLimit = 80; // degrees F
	private final int timeLimit = 60; // minutes before alert is logged.
	private final int SEND_INTERVAL_IN_MINUTES = 30;

	ThermistorData lastDataSeen = null;
	ThermistorData lastDataOverLimit = null;

	public StoveThermistorEvent(AccountMonitor accountMonitor, Device device) throws Exception {
		super(accountMonitor, device);
	}

	private LocalDateTime lastSent = null;

	private void sendEmail(String warn, String body) {
		if (lastSent == null
				|| Duration.between(lastSent, LocalDateTime.now()).toMinutes() > SEND_INTERVAL_IN_MINUTES) {
			// For the future : Is there any case to be made for turning off the email at some point?
			// E.g., a sensor going bad?

			GMailer.sendMessageX("chris.keith@gmail.com", "chris.keith@gmail.com", warn, body);
			lastSent = LocalDateTime.now();
		}
	}

	final static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("h:mm a, MMMM dd, yyyy");

	// public for automated testing only.
	public String checkStoveLeftOn(Event e) {
		String subjectLine = "subject line : no message yet";
		try {
			String fields[] = e.data.split("\\|");
			if (fields.length < 3) {
				subjectLine = "No sensor data : " + e.data;
			} else {
				ThermistorData t = new ThermistorData(fields);
				if (t.degreesInF > this.temperatureLimit) {
					if (lastDataOverLimit == null) {
						lastDataOverLimit = t;
					} else {
						long minutes = Duration.between(lastDataOverLimit.deviceTime, t.deviceTime).toMinutes();
						if (minutes > timeLimit) {
							subjectLine = "Warning: stove top temperature has been over " + temperatureLimit + " degrees F for "
									+ minutes + " minutes starting at " + dateFormatter.format(lastDataOverLimit.deviceTime);
							String body = "Sent from sensor '" + e.name + "' on Photon '" + e.coreId +  "' by server '" + Utils.getHostName()
									+ "'. Raw data: " + e.data;
							Utils.logWithGSheetsDate(LocalDateTime.now(), subjectLine, logFileName);
							sendEmail(subjectLine, body);
						}
					}
				} else {
					lastDataOverLimit = null;
					lastSent = null;
				}
				lastDataSeen = t;
			}
		} catch (Exception ex) {
			subjectLine = ex.getMessage();
		}
		return subjectLine;
	}

	public void handleEvent(Event e) {
		super.handleEvent(e);
		checkStoveLeftOn(e);
	}
}
