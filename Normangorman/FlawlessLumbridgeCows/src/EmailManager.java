import org.osbot.rs07.script.Script;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.function.Supplier;

/**
 * Created by Ben on 06/07/2015.
 */
public class EmailManager {
    private Script script;

    private String emailSmtpHost = "smtp.gmail.com";
    private String emailSmtpPort = "587";

    private String emailAddress = "";
    private String emailPassword = "";
    private int emailFrequency = 60; // in minutes
    private long lastEmailTime = System.currentTimeMillis();

    private Supplier<String> getEmailContent;

    public EmailManager(
            Script s,
            String address,
            String password,
            String smtpHost,
            String smtpPort,
            int frequency,
            Supplier<String> getEmailContent) {
        script = s;
        emailAddress = address;
        emailPassword = password;
        emailSmtpHost = smtpHost;
        emailSmtpPort = smtpPort;
        emailFrequency = frequency;
        this.getEmailContent = getEmailContent;
    }

    public void loop() {
        long timeSinceLastEmail = System.currentTimeMillis() - lastEmailTime; // milliseconds

        // email_frequency is in minutes.
        if (timeSinceLastEmail > emailFrequency * 60 * 1000) {
            sendEmail(getEmailContent.get());
            lastEmailTime = System.currentTimeMillis();
        }
    }

    public void sendEmail(String msg) {
        script.log("About to send an email update.");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", emailSmtpHost);
        props.put("mail.smtp.port", emailSmtpPort);

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(emailAddress, emailPassword);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress));
            message.setSubject("OSBot Progress Update - " + script.myPlayer().getName());
            message.setText(msg);

            Transport.send(message);
            script.log("Sent email successfully.");

        } catch (MessagingException e) {
            script.log("ERROR - could not send email successfully");
            throw new RuntimeException(e);
        }
    }

    public int getTimeTilNextEmail() {
        long nextEmailTime = lastEmailTime + emailFrequency * 60 * 1000;
        long timeLeftMillis = nextEmailTime - System.currentTimeMillis();
        int timeLeftSecs = (int)Math.floor(timeLeftMillis / 1000);

        return timeLeftSecs;
    }
}
