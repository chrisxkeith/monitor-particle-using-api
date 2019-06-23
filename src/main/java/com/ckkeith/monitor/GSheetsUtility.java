package com.ckkeith.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.BatchUpdate;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Update;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;

public class GSheetsUtility {
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart. If modifying these
     * scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.DRIVE, SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";

    private static Sheets sheetsService;

    /**
     * Creates an authorized Credential object.
     * 
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GSheetsUtility.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static Sheets getSheetsService() throws Exception {
    	if (sheetsService == null) {
            // Build a new authorized API client service.
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
    	}
    	return sheetsService;
    }
    
    public static List<List<Object>> getRange(String spreadsheetId, String range) throws Exception {
        ValueRange response = getSheetsService().spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        return response.getValues();
    }
    
	public static String create(String sheetName) throws Exception {
		Spreadsheet spreadSheet = new Spreadsheet().setProperties(new SpreadsheetProperties().setTitle(sheetName));
		Spreadsheet result = getSheetsService().spreadsheets().create(spreadSheet).execute();
		return result.getSpreadsheetId();
    }
    
	public static void appendData(String spreadSheetId, String targetCell, List<List<Object>> values)
			throws Exception {
		ValueRange appendBody = new ValueRange().setValues(values);
		getSheetsService().spreadsheets().values().append(spreadSheetId, targetCell, appendBody)
				.setValueInputOption("USER_ENTERED").setInsertDataOption("INSERT_ROWS").setIncludeValuesInResponse(true)
				.execute();
	}

	public static void updateData(String spreadSheetId, String targetCell, List<List<Object>> values) throws Exception {
        String opToDo = "setValues";
        try {
            ValueRange updateBody = new ValueRange().setValues(values);
            opToDo = "update";
            Update update = getSheetsService().spreadsheets().values().update(spreadSheetId, targetCell, updateBody);
            opToDo = "setValueInputOption";
            update = update.setValueInputOption("USER_ENTERED");
            opToDo = "execute";
            update.execute();
        } catch (Exception e) {
			Utils.logToConsole("updateData(): FAILED to : " + opToDo + ", spreadSheetId : " +
                        spreadSheetId + " : " + e.getClass().getCanonicalName() + " " + e.getMessage());
			e.printStackTrace();
			throw e;
        }
	}

	public static void deleteRows(String spreadSheetId, int startRowIndex, int endRowIndex) throws Exception {
		BatchUpdateSpreadsheetRequest content = new BatchUpdateSpreadsheetRequest();
		Request request = new Request();
		request.setDeleteDimension(new DeleteDimensionRequest().setRange(new DimensionRange()
				.setSheetId(0)
				.setDimension("ROWS")
				.setStartIndex(startRowIndex)
				.setEndIndex(endRowIndex)));
		List<Request> requests = new ArrayList<Request>();
		requests.add(request);
		content.setRequests(requests);
		BatchUpdate batchUpdate = getSheetsService().spreadsheets().batchUpdate(spreadSheetId, content);
		System.out.println("batchUpdate: " + batchUpdate.toString());
	}

	public static void clear(String spreadSheetId, String range) throws Exception {
		getSheetsService().spreadsheets().values().clear(spreadSheetId, range, new ClearValuesRequest()).execute();
	}
}
