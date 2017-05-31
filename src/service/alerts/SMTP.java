package service.alerts;

import logs.TLog;
import com.html.core.*;
import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import com.mlogger.LogKind;
import service.*;
import service.alerts.Alerts.AlertGroup;

/**
 * Miłosz Ziernik 2013/01/08
 */
public class SMTP extends AlertService {

    @Override
    public void disconnect() {
    }

    private MimeMessage prepare() {
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", "mercury.kolporter.com.pl");
        props.setProperty("mail.port", "25");
        props.setProperty("mail.smtp.auth", "true");

        javax.mail.Session mailSession = javax.mail.Session.getDefaultInstance(
                props, new Authenticator() {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("alerts.eclicto", "Innowacja6");
                    }
                });

        return new MimeMessage(mailSession);
    }

    @Override
    public void sendPlainText(String recipient, String msg) throws Exception {
        MimeMessage message = prepare();
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        message.setContent(message, "text/plain; charset=UTF-8");
    }

    @Override
    public void send(List<AlertGroup> groups, TLog log, List<String> recipients) throws Exception {

        MimeMessage message = prepare();

        message.setSubject(getGroups(groups, log)
                + (log.kind == LogKind.error ? ": " + log.value : ""));

        HtmlBuilder html = new HtmlBuilder();
        html.head.styles("body, table").font("10pt 'Courier New'");

        html.body.h3().textToDivs(log.value.toString());

        //  Console.printDetails(log, html.body);
        StringWriter wr = new StringWriter();
        try {
            html.returnHTML(wr, html);
            message.setContent(wr.toString(), "text/html; charset=UTF-8");
        } finally {
            wr.close();
        }

        for (String rec : recipients)
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(rec));

        message.setFrom(new InternetAddress("alerts.eclicto@kolporter.com.pl"));
        try {
            Transport.send(message);
        } catch (SendFailedException e) {
            String addr = "";
            for (Address a : e.getInvalidAddresses()) {
                if (!addr.isEmpty())
                    addr += ", ";
                addr += a.toString();
            }

            throw new SendFailedException("Nieprawidłowy adres: " + addr);
        }

        /*   transport.connect();
         try {
         transport.sendMessage(message,
         message.getRecipients(Message.RecipientType.TO));
         } finally {
         transport.close();
         }
         */
    }

    @Override
    public String getProtocolName() {
        return "email";
    }
}
