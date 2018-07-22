package com.ckkeith.monitor;

import java.io.FileWriter;
import java.io.PrintStream;
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
			deviceTime = LocalDateTime.parse(fields[1], logDateFormat);
			degreesInF = Integer.parseInt(fields[2]);
		}
	}

	private final DateTimeFormatter googleSheetsDateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

	private final int temperatureLimit = 80; // degrees F
	private final int timeLimit = 30; // minutes before alert is logged (or, eventually, emailed).

	ThermistorData lastDataSeen = null;
	ThermistorData lastDataOverLimit = null;

	public StoveThermistorEvent(String accountName, Device device, DeviceMonitor deviceMonitor) throws Exception {
		super(accountName, device, deviceMonitor);
	}

	private String writeEmailFile(String subject, String body) {
		String emailFilePath = null;
		try {
			emailFilePath = Utils.getLogFileName(accountName, "warning.eml");
			FileWriter fstream = new FileWriter(emailFilePath, false);

			fstream.write("From : chris.keith@gmail.com" + System.getProperty("line.separator"));
			fstream.write("Subject : " + subject + System.getProperty("line.separator"));
			fstream.write("To : chris.keith@gmail.com" + System.getProperty("line.separator"));
			fstream.write(System.getProperty("line.separator"));
			fstream.write(body);
			fstream.flush();
			fstream.close();
			Utils.logToConsole("Finished writing : " + emailFilePath);
		} catch (Exception e) {
			System.out.println("Error writing email file : " + emailFilePath + "\t" + e.toString());
			e.printStackTrace(new PrintStream(System.out));
		}
		return emailFilePath;
	}

	private final int SEND_INTERVAL_IN_MINUTES = 30;
	private LocalDateTime lastSent = null;

	private void sendEmail(String warn, Event e) {
		String fn = writeEmailFile(warn, e.data);
		if (fn != null) {
			if (lastSent == null
					|| Duration.between(lastSent, LocalDateTime.now()).toMinutes() > SEND_INTERVAL_IN_MINUTES) {
				// For the future : Is there any case to be made for turning off the email at some point?
				// E.g., a sensor going bad?

				GMailer.sendMail(fn);
				lastSent = LocalDateTime.now();
			}
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
