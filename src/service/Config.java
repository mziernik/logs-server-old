package service;

import com.config.engine.interfaces.Cfg;
import com.config.engine.ConfigItem;
import com.config.engine.items.CBoolStringList;
import com.config.engine.items.CInt;
import com.config.engine.items.CBool;
import servers.UDP;
import com.*;
import com.config.engine.interfaces.*;
import com.utils.collections.Pair;
import java.util.*;

/**
 * Miłosz Ziernik 2013/06/10
 */
public abstract class Config {

    @Cfg(name = "Konsola")
    public static class CConsole {

        @Cfg(name = "Interfejsy nasłuchujące")
        public final static CBoolStringList interfaces
                = new CBoolStringList("udp_server.interfaces",
                        "Aktywny", "URI");

        @Cfg(name = "Tolerancja różnicy czasu", nulls = true, unit = "sekundy")
        public final static CInt timeTolerance = new CInt("console.time_tolerance", 300);

        @Cfg(name = "Ogólny limit logów")
        public final static CInt totalLogsLimit = new CInt("console.total_logs_limit", 50000);

        @Cfg(name = "Limit logów dla jednego źródła")
        public final static CInt sourceLogsLimit = new CInt("console.source_logs_limit", 10000);

        @Cfg(name = "Domyślny czas życia logu", unit = "sekundy")
        public final static CInt logAliveTime = new CInt("console.log_alive_time", 60 * 60 * 72);

        @Cfg(name = "Domyślny czas życia statusu", unit = "sekundy")
        public final static CInt statusAliveTime = new CInt("console.status_alive_time", 60);

        @Cfg(name = "Pomiń powtarzające się logi (na posdstawie UID)")
        public final static CBool omitNonUnique = new CBool("console.omit_non_unique", true);

        @Cfg(name = "Wysyłaj statusy konsoli")
        public final static CBool sendStatusses = new CBool("console.send_statusses", true);

        /* @Cfg(name = "Przekazywanie logów")
         public final CStrings forwarding = new CStrings();

         @Override
         protected void beforeValueChange(CFieldData cfd, Collection<String> sValues,
         Collection<Object> values, ValueType type) throws Exception {
         if (cfd.item.isField(forwarding)) {
         Api.forwarding.clear();
         for (String s : sValues) {
         InetSocketAddress addr = Utils.pareseSocketAddress(s, 514);
         if (addr != null)
         Api.forwarding.add(addr);
         }
         }
         }
         */
        static {
            interfaces.addChangeListener(new CfgItemChangeEvent() {

                @Override
                public void onItemChange(ConfigItem item, Collection<? extends Object> values,
                        CfgItemChangeSource source) throws Exception {

                    for (Object o : values) {
                        Pair<Boolean, String> p = (Pair<Boolean, String>) o;
                        if (p.first != null && p.first && Utils.parseSocketAddress(p.second, 514) == null)
                            throw new Exception("Nieprawidłowy adres IP: " + p.second);
                    }

                    if (source != CfgItemChangeSource.afterChange)
                        return;

                    for (Object o : values) {
                        Pair<Boolean, String> p = (Pair<Boolean, String>) o;
                        if (p.first != null && p.first)
                            UDP.interfaces.add(Utils.parseSocketAddress(p.second, 514));
                    }

                    UDP.stopServer();
                    UDP.startServer();

                }
            });
        }

    }

}
