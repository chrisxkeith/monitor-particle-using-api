package com.ckkeith.monitor;

import java.util.List;

public class GoogleSheetsReader {

	static public String readData(String sheetId, String sheetName, String range) throws Exception {
		List<List<Object>> values = GSheetsUtility.getRange(sheetId, sheetName + "!" + range);
		return Utils.getDataSb(values).toString();
    }
}
