package com.example.bankingapplication.Utils;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtils {

    private static final String TAG = "EmailUtils";

    // Replace with your email and app password (App-specific password from Gmail, not your real password)
    private static final String SENDER_EMAIL = "tatriet16@gmail.com";
    private static final String SENDER_PASSWORD = "qoaputtijsolpzxv";

    public static void sendEmail(String recipientEmail, String subject, String body) {
        new SendEmailTask(recipientEmail, subject, body).execute();
    }

    private static class SendEmailTask extends AsyncTask<Void, Void, Boolean> {

        private final String toEmail;
        private final String subject;
        private final String body;

        SendEmailTask(String toEmail, String subject, String body) {
            this.toEmail = toEmail;
            this.subject = subject;
            this.body = body;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Send email failed", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            Log.d(TAG, success ? "Email sent" : "Email send failed");
        }
    }
}
