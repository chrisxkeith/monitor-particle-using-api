package com.ckkeith.monitor;

import java.time.LocalDateTime;

public class SensorDataPoint {
	LocalDateTime	timestamp;
	String			sensorName;
	String			sensorData;

	public SensorDataPoint(LocalDateTime timestamp, String sensorName, String sensorData) {
		super();
		this.timestamp = timestamp;
		this.sensorName = sensorName;
		this.sensorData = sensorData;
	}
}
