package com.ckkeith.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import com.google.api.services.gmail.Gmail;
import com.google.api.services.sheets.v4.Sheets;

public class GServicesProvider {
	private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String CREDENTIALS_FOLDER = "credentials"; // Directory to store user credentials.
	private static NetHttpTransport http_transport;
	private static Gmail gmailService;
	private static Sheets sheetsService;

	/**
	 * Creates an authorized Credential object.
	 * 
	 * @param http_transport The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If there is no client_secret.
	 */
	private static Credential getCredentials(final List<String> scopes, final InputStream in) throws Exception {
		// Load client secrets.
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				http_transport, JSON_FACTORY,
				clientSecrets, scopes)
						.setDataStoreFactory(new FileDataStoreFactory(
								new java.io.File(CREDENTIALS_FOLDER)))
						.setAccessType("offline").build();
		return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
	}

	// Gmail API access must be enabled for your account
	public static Gmail getGmailService(List<String> scopes, final InputStream in) throws Exception {
		if (gmailService == null) {
			// Build a new authorized API client service.
			http_transport = GoogleNetHttpTransport.newTrustedTransport();
			gmailService = new Gmail.Builder(http_transport, JSON_FACTORY, 
					getCredentials(scopes, in))
					.setApplicationName(APPLICATION_NAME).build();

		}
		return gmailService;
	}
	
	// Gsheets API access must be enabled for your account
	public static Sheets getSheetsService(List<String> scopes, final InputStream in) throws Exception {
		if (sheetsService == null) {
			http_transport = GoogleNetHttpTransport.newTrustedTransport();
			sheetsService = new Sheets.Builder(http_transport, JSON_FACTORY,
					getCredentials(scopes, in))
					.setApplicationName(APPLICATION_NAME).build();
		}
		return sheetsService;
	}
}
