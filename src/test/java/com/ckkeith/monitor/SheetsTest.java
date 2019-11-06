package com.ckkeith.monitor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import junit.framework.TestCase;

public class SheetsTest extends TestCase {

	public SheetsTest(String testName) {
		super(testName);
	}

    private void printData(List<List<Object>> values) {
        System.out.println(Utils.getDataSb(values));
    }
	
	@Test
	public void testGetData() {
		try {
			List<List<Object>> values = GSheetsUtility.getRange("1vo9OiDWyDvUGhjaXLUrnr9FkzO2NdUgN-AVe_CONU6U",
					"Sheet1!A1:C10");
			printData(values);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	String doTestCreate() throws Exception {
		String t = LocalDateTime.now().toString();
		String name = "test sheet " + t;
		String spreadSheetId = GSheetsUtility.create(name);
		System.out.println("created sheet named: " + name + " with spreadSheetId: " + spreadSheetId);
		return spreadSheetId;
	}

	Integer doTestAppend(String spreadSheetId, int nRows) throws Exception {
		Integer i;
		for (i = 1; i <= nRows; i++) {
			List<List<Object>> values = Arrays.asList(Arrays.asList((Object) ("value A " + i), (Object) ("value B " + i)));
			String targetCell = "A" + i;
			GSheetsUtility.appendData(spreadSheetId, targetCell, values);
			System.out.println("Appended row at " + targetCell);
		}
		List<List<Object>> values = GSheetsUtility.getRange(spreadSheetId, "Sheet1!A1:B" + i);
		printData(values);
		return i;
	}

	void doTestDelete(String spreadSheetId, Integer rowToStart) throws Exception {
		for (Integer i = rowToStart; i > 0; i--) {
			GSheetsUtility.deleteRows(spreadSheetId, i - 1, 1);
			List<List<Object>>  values = GSheetsUtility.getRange(spreadSheetId, "Sheet1!A1:B10");
			System.out.println("Deleted (I hope) row " + i);
			printData(values);
		}
	}

	void doTestClear(String spreadSheetId) throws Exception {
		GSheetsUtility.clear(spreadSheetId, "Sheet1!A1:B10");
		List<List<Object>>  values = GSheetsUtility.getRange(spreadSheetId, "Sheet1!A1:B10");
		printData(values);
	}

	private void doSleep(int seconds) throws Exception {
		System.out.println("Sleeping...");
		Thread.sleep(seconds * 1000);
	}

	@Test
	public void testAll() throws Exception {
		String spreadSheetId = doTestCreate();
		doSleep(10);
		Integer i = doTestAppend(spreadSheetId, 2);
		doTestDelete(spreadSheetId, i);
		doTestClear(spreadSheetId);
		doSleep(10);
		GSheetsUtility.clear(spreadSheetId);
	}
}
