// Please credit chris.keith@gmail.com
package com.ckkeith.monitor;

import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Map;

public class EventData {
	LocalDateTime timestamp;
	String deviceName;
	private String eventName;
	private String eventData;
	private int currentSensorIndex;

	public EventData(LocalDateTime timestamp, String deviceName, String eventName, String eventData) {
		super();
		this.timestamp = timestamp;
		this.deviceName = deviceName;
		this.eventName = eventName;
		this.eventData = eventData;
		this.currentSensorIndex = -1;
	}

	public Map.Entry<String, String> getNextSensorData() {
		if (!eventName.startsWith("All sensors")) {
			// 'Old style' event (one per sensor)
			if (currentSensorIndex == 0) {
				return null;
			}
			currentSensorIndex++;
			return new AbstractMap.SimpleEntry<String, String>(eventName, eventData);
		}
		String[] sensorDatas = eventData.split(" ");
		currentSensorIndex++;
		if (currentSensorIndex >= sensorDatas.length) {
			return null;
		}
		String[] sensorData = sensorDatas[currentSensorIndex].split(":");
		if (sensorData.length > 1) {
			return new AbstractMap.SimpleEntry<String, String>(sensorData[0], sensorData[1]);
		}
		return null;
	}

	public String getEventName() {
		return eventName;
	}

	public String getEventData() {
		return eventData;
	}
}
