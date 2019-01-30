package com.ckkeith.monitor;

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
}
