package service.alerts;

import logs.TLog;
import com.context.AppContext;
import java.util.*;
import org.jivesoftware.smack.*;
import service.*;
import service.alerts.Alerts.AlertGroup;

/**
 * Miłosz Ziernik 2013/01/08
 */
public class XMPP extends AlertService {

    private Connection connection;

    @Override
    public void disconnect() {
        if (connection != null)
            connection.disconnect();
    }

    private ChatManager prepare() throws XMPPException {
        if (connection == null) {
            ConnectionConfiguration config = new ConnectionConfiguration("kolporter.com.pl", 5222);
            config.setCompressionEnabled(false);
            config.setSASLAuthenticationEnabled(true);
            connection = new XMPPConnection(config);
            connection.connect();
            connection.login("alerts.eclicto", "Innowacja6", "Usługa LOGI "
                    + AppContext.hostname);
        }
        return connection.getChatManager();
    }

    @Override
    public void sendPlainText(String recipient, String message) throws Exception {
        ChatManager chatmanager = prepare();
        chatmanager.createChat(recipient, null).sendMessage(message);
    }

    @Override
    public void send(List<AlertGroup> groups, TLog log, List<String> recipients) throws Exception {

        String s = getGroups(groups, log);

        s += "\nźródło: " + log.source;
/*
        if (log.tag != null && !log.tag.isEmpty())
            s += ", tag: " + log.tag;
  */      
        if (log.user != null && !log.user.isEmpty())
            s += ", użytkownik: " + log.user;

        s += "\n" + log.value;

        ChatManager chatmanager = prepare();

        for (String recipient : recipients)
            chatmanager.createChat(recipient, null).sendMessage(s);

    }

    @Override
    public String getProtocolName() {
        return "xmpp";
    }
}
