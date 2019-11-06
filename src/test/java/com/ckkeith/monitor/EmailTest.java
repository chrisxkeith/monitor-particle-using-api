package com.ckkeith.monitor;

import org.junit.Test;

import junit.framework.TestCase;

public class EmailTest extends TestCase {

	public EmailTest(String testName) {
		super(testName);
	}

	@Test
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
