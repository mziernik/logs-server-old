package service.alerts;

import logs.TLog;
import com.*;
import java.util.*;
import java.util.Map.Entry;
import com.mlogger.LogAttr;
import com.utils.Str;
import service.*;
import service.alerts.AlertService.AlertObject;

/**
 * Miłosz Ziernik 2013/01/04
 */
public class Alerts extends Thread implements Runnable {

    public final static List<AlertGroup> groups = new LinkedList<>();
    public final static List<GroupRcipient> recipients = new LinkedList<>();
    public final static SMTP smtp = new SMTP();
    public final static XMPP xmpp = new XMPP();
    public final static GG gg = new GG();

    public static class Alert {

        public LogAttr type;
        public final AlertGroup group;
        public boolean exclude;
        public String value;
        public int id;
        public boolean enabled;

        public Alert(LogAttr type, AlertGroup group, boolean exclude) {
            this.type = type;
            this.group = group;
            this.exclude = exclude;
            group.alerts.add(this);

            if (!exclude) {
                List<Alert> lst = group.orGroups.get(type);
                if (lst == null) {
                    lst = new LinkedList<>();
                    group.orGroups.put(type, lst);
                }
                lst.add(this);
            }
        }

        @Override
        public String toString() {
            return type.title + (exclude ? " <> " : " = ") + value + "\n";
        }
    }

    public static class GroupRcipient {

        public int groupRecipientId;
        public int groupId;
        public int recipientId;
        public boolean groupEnabled;
        public boolean recipientEnabled;
        public String uri;
        public String username;
    }

    public static class AlertGroup {

        public final Date date = new Date();
        public int id;
        public String username;
        public boolean enabled;
        public String name;
        public final List<Alert> alerts = new LinkedList<>();
        public final List<String> recipients = new LinkedList<>();
        public final HashMap<LogAttr, List<Alert>> orGroups = new HashMap<>();

        public AlertGroup() {
            groups.add(this);
        }

        @Override
        public String toString() {
            String res = name + ":\n";
            for (Alert alert : alerts)
                res += alert + "\n";
            return res;
        }
    }

    public static void addLog(TLog log) {

        if (true)
            return;

        List<AlertGroup> passed = new LinkedList<>();

        synchronized (groups) {

            for (AlertGroup group : groups) {
                if (!group.enabled || group.alerts.isEmpty())
                    continue;

                boolean add = true;

                for (Entry<LogAttr, List<Alert>> entry : group.orGroups.entrySet()) {
                    List<Alert> list = entry.getValue();
                    if (list.isEmpty())
                        continue;
                    boolean ok = false;
                    for (Alert alert : list) {
                        Object val = alert.type.value;
                        String value = val == null ? null : val.toString();

                        if ((!alert.exclude && Str.matchesMask(value, alert.value))
                                || (alert.exclude && !Str.matchesMask(value, alert.value)))
                            ok = true;
                    }
                    if (!ok)
                        add = false;
                }

                if (add)
                    passed.add(group);
            }
        }

        if (passed.isEmpty())
            return;

        List<String> users = new LinkedList<>();
        for (AlertGroup ag : passed)
            Utils.addToStringListUnique(ag.username, users);

        List<String> lst = new LinkedList<>();

        for (String user : users) {

            lst.clear();
            for (AlertGroup ag : passed)
                if (ag.username.equals(user))
                    for (String uri : ag.recipients)
                        if (uri.startsWith("email://"))
                            Utils.addToStringListUnique(uri.substring("email://".length()), lst);
            if (!lst.isEmpty())
                smtp.add(new AlertObject(passed, log, lst));

            //------------------------------------------------------------------
            lst.clear();
            for (AlertGroup ag : passed)
                if (ag.username.equals(user))
                    for (String uri : ag.recipients)
                        if (uri.startsWith("xmpp://"))
                            Utils.addToStringListUnique(uri.substring("xmpp://".length()), lst);
            if (!lst.isEmpty())
                xmpp.add(new AlertObject(passed, log, lst));

            //------------------------------------------------------------------
            lst.clear();
            for (AlertGroup ag : passed)
                if (ag.username.equals(user))
                    for (String uri : ag.recipients)
                        if (uri.startsWith("gg://"))
                            Utils.addToStringListUnique(uri.substring("gg://".length()), lst);
            if (!lst.isEmpty())
                gg.add(new AlertObject(passed, log, lst));
        }
    }

}
