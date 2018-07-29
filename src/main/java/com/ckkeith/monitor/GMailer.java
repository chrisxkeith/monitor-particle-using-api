package com.ckkeith.monitor;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.mail.internet.*;
import javax.mail.*;
import java.util.Properties;

public class GMailer {
	private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String CREDENTIALS_FOLDER = "credentials"; // Directory to store user credentials.
	private static NetHttpTransport HTTP_TRANSPORT;
	private static Gmail service;

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved credentials/ folder.
	 */
	private static final List<String> SCOPES = Arrays.asList(GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_COMPOSE,
			GmailScopes.GMAIL_INSERT, GmailScopes.GMAIL_MODIFY);

	private static final String CLIENT_SECRET_DIR = "client_secret.json";

	/**
	 * Creates an authorized Credential object.
	 * 
	 * @param HTTP_TRANSPORT
	 *            The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException
	 *             If there is no client_secret.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		InputStream in = GMailer.class.getResourceAsStream(CLIENT_SECRET_DIR);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(CREDENTIALS_FOLDER)))
						.setAccessType("offline").build();
		return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
	}

	/**
	 * Create a MimeMessage using the parameters provided.
	 *
	 * @param to
	 *            email address of the receiver
	 * @param from
	 *            email address of the sender, the mailbox account
	 * @param subject
	 *            subject of the email
	 * @param bodyText
	 *            body text of the email
	 * @return the MimeMessage to be used to send email
	 * @throws MessagingException
	 */
	public static MimeMessage createEmail(String to, String from, String subject, String bodyText)
			throws MessagingException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		MimeMessage email = new MimeMessage(session);
		Multipart multiPart = new MimeMultipart("alternative");

		email.setFrom(new InternetAddress(from));
		email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
		email.setSubject(subject);

		MimeBodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(bodyText, "text/html; charset=utf-8");
		multiPart.addBodyPart(htmlPart);
		email.setContent(multiPart);

		return email;
	}

	/**
	 * Create a message from an email.
	 *
	 * @param emailContent
	 *            Email to be set to raw of message
	 * @return a message containing a base64url encoded email
	 * @throws IOException
	 * @throws MessagingException
	 */
	public static Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		emailContent.writeTo(buffer);
		byte[] bytes = buffer.toByteArray();
		String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
		Message message = new Message();
		message.setRaw(encodedEmail);
		return message;
	}

	/**
	 * Send an email from the user's mailbox to its recipient.
	 *
	 * @param service
	 *            Authorized Gmail API instance.
	 * @param userId
	 *            User's email address. The special value "me" can be used to
	 *            indicate the authenticated user.
	 * @param emailContent
	 *            Email to be sent.
	 * @return The sent message
	 * @throws MessagingException
	 * @throws IOException
	 */
	public static Message sendMessage(Gmail service, String userId, MimeMessage emailContent)
			throws MessagingException, IOException {
		Message message = createMessageWithEmail(emailContent);
		message = service.users().messages().send(userId, message).execute();
		System.out.println(message.toPrettyString());
		return message;
	}

	private static ArrayList<String> readFile(String fn) throws Exception {
		ArrayList<String> lines = new ArrayList<String>();
		String st;

		BufferedReader br = new BufferedReader(new FileReader(new File(fn)));
		while ((st = br.readLine()) != null) {
			lines.add(st);
		}
		br.close();
		return lines;
	}

	private static Gmail getService() throws Exception {
		if (service == null) {
			// Build a new authorized API client service.
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
					.setApplicationName(APPLICATION_NAME).build();

		}
		return service;
	}

	public static String printOneLabel() throws Exception {
		return "Not implemented yet.";
//		ListLabelsResponse listResponse = getService().users().labels().list("me").execute();
//		List<Label> labels = listResponse.getLabels();
//		if (labels.isEmpty()) {
//			return "No labels found.";
//		}
//		return "first label : " + labels.get(0).getName();
	}

	private static String rebuild(String[] strings) {
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < strings.length; i++) {
			if (i > 1) {
				sb.append(":");
			}
			sb.append(strings[i]);
		}
		return sb.toString();
	}

	public static String sendMail(String fn) {
		String subject = "No input mail file specified.";
		String body = "No body";
		String from = "chris.keith@gmail.com";
		String to = "chris.keith@gmail.com";
		if (fn != null && (!fn.isEmpty())) {
			try {
				subject = "No subject line specified.";
				body = "No body specified.";
				ArrayList<String> lines = readFile(fn);
				for (String s : lines) {
					String[] fields = s.split(":");
					if (fields.length > 1) {
						String headerType = fields[0].trim();
						if (headerType.equalsIgnoreCase("To")) {
							to = rebuild(fields);
						} else if (headerType.equalsIgnoreCase("From")) {
							from = rebuild(fields);
						} else if (headerType.equalsIgnoreCase("Subject")) {
							subject = rebuild(fields);
						} else {
							body += (" " + s);
						}
					} else {
						body += (" " + s);
					}
				}
			} catch (Throwable e) {
				subject = "Error reading : " + fn + " : " + e.getMessage();
			}
		}
		return sendMessageX(from, to, subject, body);
	}

	public static String sendMessageX(String from, String to, String subject, String body) {
		return "Not implemented yet.";
//		String ret;
//		try {
//			ret = sendMessage(getService(), "me", createEmail(from, to, subject, body)).toPrettyString();
//		} catch (Exception e) {
//			System.out.println("Error sending email : " + subject + "\t" + e.toString());
//			e.printStackTrace();
//			ret = e.getMessage();
//		}
//		return ret;
	}

	public static String sendSubjectLine(String subject) {
		return sendMessageX("chris.keith@gmail.com", "chris.keith@gmail.com", subject, subject);
	}
}
