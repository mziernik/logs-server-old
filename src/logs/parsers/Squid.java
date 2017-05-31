package logs.parsers;

import logs.TLog;
import com.utils.date.TDate;
import com.*;
import java.net.*;
import java.util.*;
import com.mlogger.LogKind;
import com.utils.collections.Strings;
import logs.Logs.RawPacket;

/**
 * Miłosz Ziernik 2013/07/01
 */
public class Squid {

    public static boolean read(RawPacket packet) throws Exception {
        String val = new String(packet.data, "UTF-8").trim();

        if (!val.startsWith("<squid>") || !val.endsWith("</squid>"))
            return false;

        for (String line : val.split("\\n")) {
            TLog log = new TLog(line, 1, null);
            log.createTime = packet.date;
            log.protocol = "SQUID";
            log.source = "Squid";
            log.loggerName = "Squid";

            log.kind = LogKind.event;
            log.device = "";
            String userAgent = "";
            String rm = "";
            int status = 0;
            int time = 0;
            String par = "";
            final Set<String> outAddr = new HashSet<>();
            String mimeType = "";
            int size = 0;

            final Set<String> user = new HashSet<>();

            /*
             un	User name
             ul	User name from authentication
             ui	User name from ident
             us	User name from SSL
             ue	User name from external acl helper
             */
            String referer = "";

            for (String ll : line.split("\\<")) {
                if (ll == null || ll.indexOf(">") <= 0)
                    continue;
                String t = ll.substring(0, ll.indexOf(">")).trim();
                String s = ll.substring(ll.indexOf(">") + 1).trim();
                if (t.isEmpty() || s.isEmpty() || s.equals("-"))
                    continue;
                switch (t) {
                    case "n":
                        log.source = s;
                        break;
                    case "a":
                        log.address(s);
                        break;
                    case "tu":
                        log.date = new TDate(s);
                        break;
                    case "Hs":
                        status = Utils.strInt(s, 0);
                        break;
                    case "tr":
                        time = Utils.strInt(s, 0);
                        break;
                    case "st":
                        size = Utils.strInt(s, 0);
                        break;
                    case "rm":
                        rm = s;
                        break;
                    case "rp":
                        par = s;
                        break;
                    case "ru":
                        log.value(s);
                        break;
                    case "A":
                    case "oa":
                        if (!s.equals("0.0.0.0"))
                            outAddr.add(s);
                        break;

                    case "mt":
                        mimeType = s;
                        break;
                    case "un":
                    case "ul":
                    case "ui":
                    case "us":
                    case "ue":
                        user.add(s);
                        break;
                    case "ua":
                        userAgent = s;
                        break;
                    case "r":
                        referer = s;
                        log.attribute("Referer", s);
                        break;
                    default:
                        log.attribute(t, s);
                        break;
                }
            }

            log.address(packet.address);
            log.tag(rm + (status == 0 ? "" : ", " + status));

            if (status >= 400)
                log.kind = LogKind.warning;
            if (status >= 500)
                log.kind = LogKind.error;
            if (status == 0)
                log.kind = LogKind.debug;

            if (!referer.isEmpty())
                referer = new URI(referer).getHost();

            log.comment = Utils.formatSize(size)
                    + (time == 0 ? "" : ", " + Utils.formatValue(time) + " ms")
                    + (mimeType.isEmpty() ? "" : ", " + mimeType)
                    + ", " + new Strings(outAddr).toString(", ")
                    + (!referer.isEmpty()
                    && !log.value.toString().contains(referer) ? " << " + referer : "");

            if (!par.isEmpty() && par.contains("?"))
                log.value((log.value != null && log.value.value != null
                        ? log.value.value.toString() : "")
                        + par.substring(par.indexOf("?") + 1));

            if (!user.isEmpty())
                log.user = new Strings(user).toString(", ");

            if (!userAgent.isEmpty()) {
                userAgent = StrUtils.decodeURIComponent(userAgent);
                log.setDevice(userAgent);
            }

            packet.logs.add(log);
        }

        return true;
    }
}
/*

 logformat Logger <squid> <n>Squid Milosz <tu>%{%Y-%m-%d %H:%M:%S}tl.%tu <tr>%6tr <a>%>a <Hs>%Hs <st>%st <rm>%rm <ru>%ru <rp>%rp <oa>%oa <A>%<A <mt>%mt <un>%un <ul>%ul <ui>%ui <ue>%ue <ua>%{User-Agent}>h <r>%{Referer}>h  </squid>


 #access_log udp://10.25.4.166:5140 FORMAT
 #access_log udp://10.1.0.254:5140 FORMAT
 access_log udp://192.168.1.100:5140 Logger
 access_log udp://192.168.1.100:514 Logger


 */
