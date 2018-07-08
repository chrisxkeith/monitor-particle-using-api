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
			String fields[] = data.split("\t");
			deviceTime = LocalDateTime.parse(fields[1], logDateFormat);
			degreesInF = Integer.parseInt(fields[2]);
		}
	}
	
	private final int temperatureLimit = 75; // degrees F
	private final int timeLimit = 30; // minutes before alert is logged (or, eventually, emailed).

	ThermistorData lastDataSeen = null;
	ThermistorData lastDataOverLimit = null;
	
	public StoveThermistorEvent(String accountName, Device device) throws Exception {
		super(accountName, device);
	}

	public void handleEvent(Event e) {
		super.handleEvent(e);
		if (e.name.equals("Thermistor 01 sensor:")) {
			ThermistorData t = new ThermistorData(e.data);
			if (t.degreesInF > this.temperatureLimit) {
				if (lastDataOverLimit == null) {
					lastDataOverLimit = t;
				} else {
					if (Duration.between(lastDataOverLimit.deviceTime,
							lastDataOverLimit.deviceTime).toMinutes() > timeLimit) {
						Utils.logWithGSheetsDate(LocalDateTime.now(),
								"Temperature has been over " + temperatureLimit + " from " +
								lastDataOverLimit.deviceTime + " to " + t.deviceTime,
								logFileName);						
					}
				}
			} else {
				lastDataOverLimit = null;
			}
			lastDataSeen = t;
		}
	}
}
