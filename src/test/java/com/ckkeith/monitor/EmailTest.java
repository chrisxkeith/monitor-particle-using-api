package com.ckkeith.monitor;

import java.time.LocalDateTime;

import junit.framework.TestCase;

public class EmailTest extends TestCase {

	public EmailTest(String testName) {
		super(testName);
	}

	public void testGetLabel() {
		try {
			String ret = GMailer.printOneLabel();
			System.out.println(ret);
			assertTrue(ret.contains("label"));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	// Only run manually when necessary.
	public void xtestSendEmail() {
		try {
			String response = GMailer.sendMessageX("chris.keith@gmail.com", "chris.keith@gmail.com", "Test running at "
					+ LocalDateTime.now() + " : " + getClass().getCanonicalName() + " from " + Utils.getHostName(),
					"... body text ...");
			System.out.println(response);
			assertTrue(response.contains("id"));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
