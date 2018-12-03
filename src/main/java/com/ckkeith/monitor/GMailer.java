package com.ckkeith.monitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;

public class GMailer {
	private static Gmail service;
	/**
	 * If modifying these scopes, delete your previously saved credentials/ folder.
	 */
	private static final List<String> SCOPES = Arrays.asList(GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_COMPOSE,
			GmailScopes.GMAIL_INSERT, GmailScopes.GMAIL_MODIFY);

	private static final String CLIENT_SECRET_DIR = "client_secret.json";

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
		return service.users().messages().send(userId, message).execute();
	}

	private static Gmail getService() throws Exception {
		if (service == null) {
			InputStream in = GMailer.class.getResourceAsStream(CLIENT_SECRET_DIR);
			service = GServicesProvider.getGmailService(SCOPES, in);
		}
		return service;
	}

	public static String printOneLabel() throws Exception {
		ListLabelsResponse listResponse = getService().users().labels().list("me").execute();
		List<Label> labels = listResponse.getLabels();
		if (labels.isEmpty()) {
			return "No labels found.";
		}
		return "first label : " + labels.get(0).getName();
	}

	public static String sendMessageX(String from, String to, String subject, String body) {
		String ret;
		try {
			ret = sendMessage(getService(), "me", createEmail(from, to, subject, body)).toPrettyString();
		} catch (Exception e) {
			System.out.println("Error sending email : " + subject + "\t" + e.toString());
			e.printStackTrace();
			ret = e.getMessage();
		}
		return ret;
	}

	public static String sendSubjectLine(String subject) {
		return sendMessageX("chris.keith@gmail.com", "chris.keith@gmail.com", subject, subject);
	}
}
