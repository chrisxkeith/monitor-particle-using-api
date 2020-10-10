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
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Update;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DeleteSheetRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;

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
    private static Drive driveService;

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

    private static Credential getCredentialsX() {
        try {
            return getCredentials(GoogleNetHttpTransport.newTrustedTransport());
        } catch (Exception e) {
            Utils.logToConsole("Unable to get Google Cloud credentials. No credentials file?");
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }
    
    private static Sheets getSheetsService() throws Exception {
    	if (sheetsService == null) {
            sheetsService = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, getCredentialsX())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
    	}
    	return sheetsService;
    }

    private static Drive getDriveService() throws Exception {
    	if (driveService == null) {
            driveService = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, getCredentialsX())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
    	}
    	return driveService;
    }
    
    public static List<List<Object>> getRange(String spreadsheetId, String range) throws Exception {
        ValueRange response = getSheetsService().spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        return response.getValues();
    }

    public static void giveAccess(String spreadsheetId, String type, String role) throws Exception {
        JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
            @Override
            public void onFailure(GoogleJsonError e,
                                    HttpHeaders responseHeaders)
                throws IOException {
                System.err.println(e.getMessage());
            }

            @Override
            public void onSuccess(Permission permission,
                                    HttpHeaders responseHeaders)
                throws IOException {
                System.out.println("Permission ID: '" + permission.getId() + "'");
            }
        };
        BatchRequest batch = getDriveService().batch();
        Permission userPermission = new Permission()
            .setType(type)
            .setRole(role);
            getDriveService().permissions().create(spreadsheetId, userPermission)
            .setFields("id")
            .queue(batch, callback);

        batch.execute();
    }
    
	public static String create(String sheetName) throws Exception {
		Spreadsheet spreadSheet = new Spreadsheet().setProperties(new SpreadsheetProperties().setTitle(sheetName));
		Spreadsheet result = getSheetsService().spreadsheets().create(spreadSheet).execute();
		return result.getSpreadsheetId();
    }
    
    public static void delete(String sheetId, int sheetIndex) throws Exception {
        final List<Request> requests = new ArrayList<>(0);
        final DeleteSheetRequest delete = new DeleteSheetRequest().setSheetId(sheetIndex);
        requests.add(new Request().setDeleteSheet(delete));        
        final BatchUpdateSpreadsheetRequest batch = new BatchUpdateSpreadsheetRequest().setRequests(requests);
        getSheetsService().spreadsheets().batchUpdate(sheetId, batch).execute();
    }

    public static void clear(String spreadSheetId) throws Exception {
        UpdateCellsRequest clearAllDataRequest = new UpdateCellsRequest();
        int allSheetsId = 0;
        GridRange gridRange = new GridRange();
        gridRange.setSheetId(allSheetsId);
        clearAllDataRequest.setRange(gridRange);
        String clearAllFieldsSpell = "*";
        clearAllDataRequest.setFields(clearAllFieldsSpell);
        final List<Request> requests = new ArrayList<>(0);
        BatchUpdateSpreadsheetRequest request = new BatchUpdateSpreadsheetRequest().setRequests(requests);
        requests.add(new Request().setUpdateCells(clearAllDataRequest));
        getSheetsService().spreadsheets().batchUpdate(spreadSheetId, request).execute();
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
		getSheetsService().spreadsheets().batchUpdate(spreadSheetId, content);
	}

	public static void clear(String spreadSheetId, String range) throws Exception {
		getSheetsService().spreadsheets().values().clear(spreadSheetId, range, new ClearValuesRequest()).execute();
	}
}
