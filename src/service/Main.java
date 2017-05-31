package service;

import com.config.*;
import com.config.CAdministration.CRegistration;
import com.config.CAdministration.CSMTP;
import com.config.CAuthorization.CLDAP;
import com.context.*;
import com.context.intf.OnContextDestroyed;
import com.events.ServiceEvent;
import com.extensions.Email.EmailType;
import com.mlogger.*;
import com.servers.tomcat.Juli;
import com.servers.tomcat.Tomcat;
import com.servlet.Handlers;
import com.utils.collections.Pair;
import console.Console;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import javax.servlet.annotation.WebListener;
import org.apache.catalina.LifecycleException;
import servers.*;
import service.handlers.*;

@WebListener
public class Main extends WebAppContext {

    static void test() throws InterruptedException {

    }

    public Main() {
        super();
    }

    @Override
    protected void onInitialize(Handlers handlers) throws Exception {
        serviceName = "logi";
        serviceTitle = "Serwer logów";

        PreConfig.httpsPort = 8766;
        PreConfig.httpsCertFile = "appdata/keystore.jks";
        PreConfig.httpsCertPass = "871d170c-ec5f-43a7-8143-1b75ac95b1bb";
        PreConfig.autoRunBrowserUrl = "http://localhost:8765";
        PreConfig.lockFile = "appdata/instance.lock";

        MLogger mlogger = MLogger.instance();
        mlogger.setSourceName("Logi Embedded");
        mlogger.addHandler("udp://10.1.0.254:514");
        //    mlogger.addHandler("udp://10.25.4.166:5140");

        handlers.setUserData(UserData.class);
        handlers.setUserConfig(UserConfig.class);
        handlers.setSession(Session.class);
        handlers.setLdap(Ldap.class);

        Juli.setLevel(Juli.TomcatLogLevel.info);

        Config.CConsole.interfaces.setDefaults(new Pair<>(true, "0.0.0.0:514"));

        switch (AppContext.hostname) {
            case "ZMLAP":
                CLogs.protocols.setDefaults(new Pair<>(true, "udp://192.168.1.101:514"));
                Config.CConsole.interfaces.setDefaults(new Pair<>(true, "192.168.1.101:5140"));
                //   Juli.setLevel(Juli.TomcatLogLevel.debug);
                break;

            case "milosz":

                CSMTP.host.setDefaults("poczta.kolporter.com.pl");
                CSMTP.username.setDefaults("alerts.eclicto");
                CSMTP.password.setDefaults("Innowacja6");
                CSMTP.senderName.setDefaults("Serwer logów");
                CSMTP.senderEmail.setDefaults("alerts.eclicto@kolporter.com.pl");
                CAdministration.emailList.setDefaults(
                        new Pair<>(EmailType.to, "milosz.ziernik@infover.pl")
                );

                CLDAP.enabled.setDefaults(true);
                CLDAP.domain.setDefaults("o=kolporter,c=pl");
                CLDAP.listUsersFilter.setDefaults("(&(KolAccountDisabled=FALSE),(mail=*infover.pl))");
                CLDAP.url.setDefaults("ldap://10.1.1.120:389");
                CLDAP.adminUsername.setDefaults(CSMTP.username.value());
                CLDAP.adminPassword.setDefaults(CSMTP.password.value());

                CRegistration.enabled.setDefaults(true);
                CRegistration.emailVerification.setDefaults(false);
                CRegistration.passwordRequired.setDefaults(false);
                break;
        }

    }

    public static void main(String[] args) {
        try {
            test();
            new Main(args);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public Main(String[] args) throws LifecycleException {
        super(args);
        new Tomcat(8765, null).start();
    }

    public static Main context() {
        return (Main) (AppContext) getInstance();
    }

    @Override
    public void onServiceEvent(ServiceEvent event) {

    }

    @OnContextDestroyed
    public static void onServletContextDestroy() {
        List<Console> sockets = new LinkedList<>();
        sockets.addAll(Console.allSockets);
        for (Console socket : sockets)
            try {
                socket.close();
            } catch (IOException ex) {
            }
        UDP.stopServer();
        Console.allSockets.clear();
    }

}
