package com.ckkeith.monitor;

import java.util.Date;

import nl.infcomtec.jparticle.Event;

public class ParticleEvent {
	Event	event;
	
	ParticleEvent(Event event) {
		this.event = event;
	}
	
	public String toTabbedString(ParticleDevice device) {
		StringBuilder sb = new StringBuilder();
		sb.append(event.name).append("\t")
			.append(event.data).append("\t")
			.append(device.getName());
		return sb.toString();
	}

	public Date getPublishedAt() {
		return event.publishedAt;
	}

	public String getName() {
		return event.name;
	}

	public String getData() {
		return event.data;
	}

	public String getCoreId() {
		return event.coreId;
	}	
}
