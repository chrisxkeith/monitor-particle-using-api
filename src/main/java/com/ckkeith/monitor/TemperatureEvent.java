// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TemperatureEvent extends ParticleDeviceEvent {

	class ThermistorData {
		final private DateTimeFormatter logDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");

		LocalDateTime deviceTime = null;
		double degreesInF = Double.MIN_VALUE;

		ThermistorData(String fields[]) {
			deviceTime = LocalDateTime.parse(fields[1], logDateFormat);
			degreesInF = Double.parseDouble(fields[2]);
		}
		ThermistorData(ParticleEvent e) {
			this.deviceTime = e.getPublishedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			this.degreesInF = Double.parseDouble(e.getData());
		}
	}

	ThermistorData lastDataSeen = null;
	ThermistorData lastDataOverLimit = null;

	public TemperatureEvent(AccountMonitor accountMonitor, ParticleDevice device) throws Exception {
		super(accountMonitor, device);
	}

	private LocalDateTime lastSent = null;

	private void sendEmail(String warn, String body) {
		if (lastSent == null
				|| Duration.between(lastSent, LocalDateTime.now()).toMinutes() > this.accountMonitor.runParams.resendIntervalInMinutes) {
			GMailer.sendMessageX(this.accountMonitor.runParams.emailTo, this.accountMonitor.runParams.emailTo, warn, body);
			lastSent = LocalDateTime.now();
		}
	}

	final static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("h:mm a, MMMM dd, yyyy");

	// public for automated testing only.
	public String checkStoveLeftOn(ParticleEvent e) {
		String subjectLine = "subject line : no message yet";
		try {
			ThermistorData t;
			String fields[] = e.getData().split("\\|");
			if (fields.length < 3) {
				t = new ThermistorData(e);
			} else {
				t = new ThermistorData(fields);
			}
			if (t.degreesInF > this.accountMonitor.runParams.temperatureLimit) {
				if (lastDataOverLimit == null) {
					lastDataOverLimit = t;
				} else {
					long minutes = Duration.between(lastDataOverLimit.deviceTime, t.deviceTime).toMinutes();
					if (minutes > this.accountMonitor.runParams.timeLimit) {
						subjectLine = "Warning: stove top temperature has been over " + this.accountMonitor.runParams.temperatureLimit + " degrees F for "
								+ minutes + " minutes starting at " + dateFormatter.format(lastDataOverLimit.deviceTime);
						String body = "Sent from sensor '" + e.getName() + "' on Photon '" + e.getCoreId() +  "' by server '" + Utils.getHostName()
								+ "'. Raw data: " + e.getData();
						Utils.logWithGSheetsDate(LocalDateTime.now(), subjectLine, logFileName);
						sendEmail(subjectLine, body);
					}
				}
			} else {
				lastDataOverLimit = null;
				lastSent = null;
			}
			lastDataSeen = t;
		} catch (Exception ex) {
			subjectLine = ex.getClass().getCanonicalName() + " " + ex.getMessage();
		}
		return subjectLine;
	}

	public void handleEvent(ParticleEvent e) {
		super.handleEvent(e);
		checkStoveLeftOn(e);
	}
}
