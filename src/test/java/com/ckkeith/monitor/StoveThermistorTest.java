package com.ckkeith.monitor;

import org.json.JSONObject;

import junit.framework.TestCase;
import nl.infcomtec.jparticle.Device;
import nl.infcomtec.jparticle.Event;

public class StoveThermistorTest extends TestCase {

	public StoveThermistorTest(String testName) {
		super(testName);
	}

	public void testDetectStoveLeftOn() {
		try {
			JSONObject deviceJson = new JSONObject(
					"{ cellular : false, id : 'fubar', platform_id : 0, "
					+ "product_id : 0, status : 'junk', "
					+ "connected : true, last_heard : '2018-01-01 00:00:00.000Z', "
					+ "name : 'yowza', last_ip_address : '0.0.0.0' }");
			Device d = new Device(deviceJson);
			AccountMonitor accountMonitor = new AccountMonitor("junk	chris.keith@gmail.com");
			StoveThermistorEvent stoveThermistorEvent = new StoveThermistorEvent(accountMonitor, d);
			JSONObject eventJson = new JSONObject("{ coreid : 'fubar', "
					+ "data : '|2018-07-06T21:30:00Z|85|85|853|85|8|0.022000',"
					+ "published_at : '2018-07-06 21:30:00.000Z', ttl : 1 }");
			Event e = new Event("Thermistor 01 sensor:", eventJson);
			String warn = stoveThermistorEvent.checkStoveLeftOn(e);
			assertTrue(warn.equals("subject line : no message yet"));
			eventJson = new JSONObject("{ coreid : 'fubar', "
					+ "data : '|2018-07-06T22:35:00Z|85|85|853|85|8|0.022000',"
					+ "published_at : '2018-07-06 22:35:00.000Z', ttl : 1 }");
			e = new Event("This is just a test", eventJson);
			warn = stoveThermistorEvent.checkStoveLeftOn(e);
			assertTrue(warn.contains("temperature"));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
