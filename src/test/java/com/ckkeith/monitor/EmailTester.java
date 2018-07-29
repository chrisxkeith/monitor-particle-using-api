package com.ckkeith.monitor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

	private String writeTestFile() throws Exception {
	    File temp = File.createTempFile("tempfile", ".tmp");
		String[] emlFileLines = {
				"To : chris.keith@gmail.com",
				"From : chris.keith@gmail.com",
				"Subject : Test running at " + LocalDateTime.now() + " from " + Utils.getHostName(),
				"",
				temp.getName(),
		};
		BufferedWriter br = new BufferedWriter(new FileWriter(temp));
		for (int i = 0; i < emlFileLines.length; i++) {
			br.write(emlFileLines[i] + System.getProperty("line.separator"));
		}
		br.close();
		return temp.getCanonicalPath();
	}

	public void testSendEmail() {
		try {
			String tempFileName = writeTestFile();
			System.out.println("About to send : " + tempFileName);
			String response = GMailer.sendMail(tempFileName);
			System.out.println(response);
			assertTrue(response.contains("id"));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
