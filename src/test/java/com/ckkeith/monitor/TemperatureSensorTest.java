package com.ckkeith.monitor;

import java.util.TreeMap;

import org.json.JSONObject;

import junit.framework.TestCase;
import nl.infcomtec.jparticle.Device;
import nl.infcomtec.jparticle.Event;

public class TemperatureSensorTest extends TestCase {

	public TemperatureSensorTest(String testName) {
		super(testName);
	}

	private void doTest(AccountMonitor accountMonitor, ParticleDevice d, JSONObject deviceJson,
					String firstEvent, String secondEvent) throws Exception {
		TemperatureEvent TemperatureEvent = new TemperatureEvent(accountMonitor, d);
		JSONObject eventJson = new JSONObject(firstEvent);
		TreeMap<String, Device> devices = new TreeMap<String, Device>();
		Device device = new Device(deviceJson);
		devices.put("yowza", device);
		ParticleEvent e = new ParticleEvent(new Event(devices, "Thermistor 01 sensor:", eventJson));
		String warn = TemperatureEvent.checkStoveLeftOn(e);
		System.out.println("warn: " + warn);
		assertTrue(warn.equals("subject line : no message yet"));
		eventJson = new JSONObject(secondEvent);
		e = new ParticleEvent(new Event(devices, "This is just a test", eventJson));
		warn = TemperatureEvent.checkStoveLeftOn(e);
		System.out.println("warn: " + warn);
		assertTrue(warn.contains("temperature"));
	}

	public void testDetectStoveLeftOn() {
		try {
			JSONObject deviceJson = new JSONObject(
					"{ cellular : false, id : 'fubar', platform_id : 0, "
					+ "product_id : 0, status : 'junk', "
					+ "connected : true, last_heard : '2018-01-01 00:00:00.000Z', "
					+ "name : 'yowza', last_ip_address : '0.0.0.0' }");
			ParticleDevice d = new ParticleDevice(new Device(deviceJson));
			AccountMonitor accountMonitor = new AccountMonitor("junk	chris.keith@gmail.com");
			doTest(accountMonitor, d, deviceJson, "{ coreid : 'fubar', "
					+ "data : '|2018-07-06T21:30:00Z|91|85|853|85|8|0.022000',"
					+ "published_at : '2018-07-06 21:30:00.000Z', ttl : 1 }", 
				"{ coreid : 'fubar', "
					+ "data : '|2018-07-06T23:35:00Z|91|85|853|85|8|0.022000',"
					+ "published_at : '2018-07-06 23:31:00.000Z', ttl : 1 }");
			doTest(accountMonitor, d, deviceJson, "{ coreid : 'fubar', "
					+ "data : '91',"
					+ "published_at : '2018-07-06 21:30:00.000Z', ttl : 1 }", 
				"{ coreid : 'fubar', "
					+ "data : '91',"
					+ "published_at : '2018-07-06 24:31:00.000Z', ttl : 1 }");
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
