package com.ckkeith.monitor;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

// https://developers.google.com/sheets/api/quickstart/java
// https://github.com/gsuitedevs/java-samples/blob/master/sheets/quickstart/src/main/java/SheetsQuickstart.java

public class GSheeter {
	private static Sheets sheetService;

//	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	private static final List<String> SCOPES = Collections.singletonList(
			SheetsScopes.SPREADSHEETS_READONLY);
	private static final String CREDENTIALS_FILE_PATH = "sheets-credentials.json";

	private static Sheets getService() throws Exception {
		if (sheetService == null) {
			InputStream in = GMailer.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
			sheetService = GServicesProvider.getSheetsService(SCOPES, in);
		}
		return sheetService;
	}

	public static List<List<Object>> getRange(final String spreadsheetId, final String range) throws Exception {
		Get get;
		try {
			get = getService().spreadsheets().values().get(spreadsheetId, range);
		} catch (Exception e) {
			Utils.logToConsole(
					"getService().spreadsheets().values().get(spreadsheetId, range) failed: " + e.getMessage());
			throw e;
		}
		ValueRange vr;
		try {
			vr = get.execute();
		} catch (Exception e) {
			Utils.logToConsole("get.execute(): " + e.getMessage());
			throw e;
		}
		return vr.getValues();
	}
}
