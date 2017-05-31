package service.alerts;

import logs.TLog;
import com.context.AppContext;
import java.util.*;
import pages.*;
import service.*;
import service.alerts.Alerts.AlertGroup;

/**
 * Miłosz Ziernik 2013/01/08
 */
public abstract class AlertService extends Thread implements Runnable {

    public static class AlertObject {

        public final List<AlertGroup> groups;
        public final TLog log;
        public final List<String> recipients = new LinkedList<>();

        public AlertObject(List<AlertGroup> groups, TLog log,
                List<String> recipients) {
            this.groups = groups;
            this.log = log;
            for (String s : recipients)
                this.recipients.add(s);
        }
    }
    private final List<AlertObject> queue = new LinkedList<>();

    public void add(AlertObject alertObj) {
        if (getState() == State.NEW)
            start();

        synchronized (queue) {
            queue.add(alertObj);
        }
    }

    protected String getGroups(List<AlertGroup> groups, TLog log) {
        if (groups == null)
            return "Alert";

        String sGroups = "";
        for (AlertGroup gr : groups) {
            if (!sGroups.isEmpty())
                sGroups += ", ";
            sGroups += gr.name;
        }
        return sGroups;
    }

    @Override
    public void run() {
        try {
            while (AppContext.initialized && !AppContext.terminated)
                try {
                    if (queue.isEmpty()) {
                        Thread.sleep(100);
                        continue;
                    }

                    AlertObject alertObj = null;
                    synchronized (queue) {
                        alertObj = queue.get(0);
                        queue.remove(0);
                    }
                    send(alertObj.groups, alertObj.log, alertObj.recipients);

                } catch (InterruptedException ex) {
                    return;
                } catch (Exception e) {
                    Common.addInternalError(e);
                }

        } finally {
            disconnect();
        }
    }

    public abstract void disconnect();

    public abstract String getProtocolName();

    public abstract void send(List<AlertGroup> groups, TLog log,
            List<String> recipients) throws Exception;

    public abstract void sendPlainText(String recipient, String message) throws Exception;

    public static String getRecipient(String uri) {
        if (uri == null || getService(uri) == null)
            return "";
        uri = uri.trim();
        return uri.substring(uri.indexOf("://") + "://".length());
    }

    public static AlertService getService(String uri) {
        if (uri == null)
            return null;

        uri = uri.trim();

        if (uri.toLowerCase().startsWith(Alerts.smtp.getProtocolName() + "://"))
            return Alerts.smtp;
        if (uri.toLowerCase().startsWith(Alerts.xmpp.getProtocolName() + "://"))
            return Alerts.xmpp;
        if (uri.toLowerCase().startsWith(Alerts.gg.getProtocolName() + "://"))
            return Alerts.gg;
        return null;
    }
}
