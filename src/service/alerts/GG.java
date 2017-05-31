package service.alerts;

import com.Utils;
import java.util.List;
import pages.Common;
import pl.mn.communicator.*;
import logs.TLog;
import service.alerts.Alerts.AlertGroup;

/**
 * Miłosz Ziernik 2013/01/08
 */
public class GG extends AlertService {

    private ISession session;
    private IConnectionService connectionService;

    @Override
    public void disconnect() {
        if (connectionService != null) {
            try {
                connectionService.disconnect();
            } catch (GGException ex) {
                Common.addInternalError(ex);
            }
            connectionService = null;
        }
    }

    private IMessageService prepare() throws Exception {
        if (session == null)
            try {
                session = SessionFactory.createSession();
                connectionService = session.getConnectionService();

                connectionService.connect(new IServer() {
                    @Override
                    public String getAddress() {
                        return "91.214.237.10";
                    }

                    @Override
                    public int getPort() {
                        return 8074;
                    }
                });

                ILoginService loginService = session.getLoginService();
                LoginContext loginContext = new LoginContext(45888765, "Innowacja6");
                loginService.login(loginContext);
            } catch (Exception e) {
                connectionService = null;
                session = null;
                throw e;
            }

        return session.getMessageService();
    }

    @Override
    public void sendPlainText(String recipient, String message) throws Exception {
        IMessageService messageService = prepare();
        int nr = Utils.strInt(recipient, -1);
        if (nr > 0)
            messageService.sendMessage(OutgoingMessage.createNewMessage(nr, message));
    }

    @Override
    public void send(List<AlertGroup> groups, TLog log, List<String> recipients) throws Exception {
        IMessageService messageService = prepare();
        String s = getGroups(groups, log);

      /*  if (log.tag != null && !log.tag.value.trim().isEmpty())
            s += log.tag + ": ";
        */
        s += log.value + "\n";
        if (log.user != null && !log.user.trim().isEmpty())
            s += "Użytkownik: " + log.user;

        for (String recipient : recipients) {
            int nr = Utils.strInt(recipient, -1);
            if (nr > 0)
                messageService.sendMessage(OutgoingMessage.createNewMessage(nr, s));
        }
    }

    @Override
    public String getProtocolName() {
        return "gg";
    }
}
