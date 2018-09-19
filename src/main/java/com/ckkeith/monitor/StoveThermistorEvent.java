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
				deviceTime = LocalDateTime.now(); // TODO : Figure out a better value for this, or is this just bad data?
				degreesInF = Integer.parseInt(fields[0]);
			} else if (fields.length >= 3) {
				deviceTime = LocalDateTime.parse(fields[1], logDateFormat);
				degreesInF = Integer.parseInt(fields[2]);
			}
		}
	}

	private final int temperatureLimit = 80; // degrees F
	private final int timeLimit = 60; // minutes before alert is logged.
	private final int SEND_INTERVAL_IN_MINUTES = 30;

	ThermistorData lastDataSeen = null;
	ThermistorData lastDataOverLimit = null;

	public StoveThermistorEvent(String accountName, Device device) throws Exception {
		super(accountName, device);
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
		String subjectLine = "";
		ThermistorData t = new ThermistorData(e.data);
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
		return subjectLine;
	}

	public void handleEvent(Event e) {
		super.handleEvent(e);
		checkStoveLeftOn(e);
	}
}
