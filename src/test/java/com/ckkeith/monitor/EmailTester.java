package com.ckkeith.monitor;

import java.time.LocalDateTime;

import junit.framework.TestCase;

public class EmailTester extends TestCase {

	public EmailTester(String testName) {
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

	public void testSendEmail() {
		try {
			String response = GMailer.sendMessageX("chris.keith@gmail.com", "chris.keith@gmail.com", "Test running at "
					+ LocalDateTime.now() + " from " + Utils.getHostName() + " : " + getClass().getCanonicalName(), "... body text ...");
			System.out.println(response);
			assertTrue(response.contains("id"));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
