package com.ckkeith.monitor;

import java.util.List;

import junit.framework.TestCase;

public class SheetsTest extends TestCase {

	public SheetsTest(String testName) {
		super(testName);
	}

	public void testGetLabel() {
		try {
			List<List<Object>> ret = GSheeter.getRange("1vo9OiDWyDvUGhjaXLUrnr9FkzO2NdUgN-AVe_CONU6U",
					"Sheet 1!A1:C10");
			for (List<Object> list : ret) {
				StringBuilder sb = new StringBuilder();
				for (Object o : list) {
					sb.append(o.toString()).append("\t");
				}
				Utils.logToConsole(sb.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
//			fail();
		}
	}
}
