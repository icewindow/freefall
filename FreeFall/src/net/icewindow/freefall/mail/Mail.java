package net.icewindow.freefall.mail;

import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.icewindow.freefall.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Mail sender Based on <a
 * href="http://www.jondev.net/articles/Sending_Emails_without_User_Intervention_(no_Intents)_in_Android"
 * >http://www.jondev.net/articles/Sending_Emails_without_User_Intervention_(no_Intents)_in_Android</a>
 * 
 * @author icewindow
 *
 */
public class Mail extends javax.mail.Authenticator {
	
	public static final String TAG = "FreefallMail";

	private String host;
	private int port;
	private int sport;

	private String user;
	private String pass;
	private boolean useAuth;

	private String from;
	private String[] to;
	private String subject;
	private StringBuilder body;

	private Multipart multipart;

	private SharedPreferences sharedPreferences;

	public Mail(Context context) {
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		host = "smtp.gmail.com";
		port = 465;
		sport = 465;
		useAuth = true;

		String _user;
		if (sharedPreferences.getBoolean(context.getString(R.string.MAIL_AUTH_SENDERISUSER), true)) {
			_user = sharedPreferences.getString(context.getString(R.string.MAIL_ADDRESS_FROM), "");
		} else {
			_user = sharedPreferences.getString(context.getString(R.string.MAIL_AUTH_USER), "");
		}
		user = _user;
		pass = sharedPreferences.getString(context.getString(R.string.MAIL_AUTH_PASSWORD), "");

		subject = "";
		body = new StringBuilder();

		multipart = new MimeMultipart();

		MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
		mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
		mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
		mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
		mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
		mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
		CommandMap.setDefaultCommandMap(mc);
	}

	public Mail(Context context, String user, String password) {
		this(context);
		this.user = user;
		this.pass = password;
	}

	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(user, pass);
	}

	public boolean send() {
		Properties properties = getProperties();

		if (!user.equals("") && !pass.equals("") && to.length > 0 && !from.equals("") && !subject.equals("")
				&& body.length() > 0) {
			Session session = Session.getInstance(properties, this);
			try {
				MimeMessage message = new MimeMessage(session);
				message.setFrom(new InternetAddress(from));
				InternetAddress[] addressesTo = new InternetAddress[to.length];
				for (int i = 0; i < to.length; i++) {
					addressesTo[i] = new InternetAddress(to[i]);
				}
				message.setRecipients(RecipientType.TO, addressesTo);
				message.setSubject(subject);
				BodyPart messageBody = new MimeBodyPart();
				messageBody.setHeader("content-type", "text/plain");
				messageBody.setText(body.toString());
				multipart.addBodyPart(messageBody);

				message.setContent(multipart);

				Transport.send(message);

				return true;
			} catch (AddressException e) {
				Log.e(TAG, "Address exception");
			} catch (MessagingException e) {
				Log.e(TAG, "Messaging Exception", e);
			}
		}
		return false;
	}

	private Properties getProperties() {
		Properties props = new Properties();

		props.put("mail.smtp.host", host);

		if (useAuth) {
			props.put("mail.smtp.auth", "true");
		}

		props.put("mail.smtp.port", port);
		props.put("mail.smtp.socketFactory.port", sport);
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.socketFactory.fallback", "false");

		return props;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String[] getTo() {
		return to;
	}

	public void setTo(String[] to) {
		this.to = to;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public void addBodyText(String data) {
		body.append(data);
	}
}
