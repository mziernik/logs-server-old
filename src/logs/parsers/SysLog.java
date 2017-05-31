package logs.parsers;

import logs.TLog;
import com.utils.date.TDate;
import com.*;
import java.text.*;
import java.util.*;
import com.mlogger.LogKind;
import logs.Logs.RawPacket;

/**
 * Miłosz Ziernik 2013/07/02
 */
public class SysLog {

    public static boolean read(RawPacket packet) throws Exception {
        Integer syslogId = null;
        String value = new String(packet.data);
        if (value.startsWith("<") && value.indexOf(">") > 0 && value.indexOf(">") <= 4)
            syslogId = Utils.strInt(value.substring(1, value.indexOf(">")), null);
        if (syslogId == null)
            return false;

        TLog log = new TLog(value, 1, null);
        log.createTime = packet.date;
        log.kind = LogKind.debug;
        log.source = "SysLog " + packet.address;
        log.loggerName = "SysLog";
        log.date = new TDate();
        log.address(packet.address);
        log.protocol = "SysLog";

        int sev = syslogId % 8;
        int fac = syslogId / 8;

        String severity = "";
        String facility = "";

        switch (sev) {
            case 0:
                log.kind = LogKind.error;
                severity = "Emergency";
                break;
            case 1:
                log.kind = LogKind.event;
                severity = "Alert";
                break;
            case 2:
                log.kind = LogKind.error;
                severity = "Critical";
                break;
            case 3:
                log.kind = LogKind.error;
                severity = "Error";
                break;
            case 4:
                log.kind = LogKind.warning;
                severity = "Warning";
                break;
            case 5:
                log.kind = LogKind.query;
                severity = "Notice";
                break;
            case 6:
                log.kind = LogKind.log;
                severity = "Information";
                break;
            case 7:
                log.kind = LogKind.debug;
                severity = "Debug";
                break;
        }


        /*
         * 0	Emergency: system is unusable
         1	Alert: action must be taken immediately
         2	Critical: critical conditions
         3	Error: error conditions
         4	Warning: warning conditions
         5	Notice: normal but significant condition
         6	Informational: informational messages
         7	Debug: debug-level messages
         */
        switch (fac) {
            case 0:
                facility = "Kernel";
                break;
            case 1:
                facility = "User";
                break;
            case 2:
                facility = "Mail";
                break;
            case 3:
                facility = "Daemons";
                break;
            case 4:
                facility = "Security";
                break;
            case 5:
                facility = "Syslogd";
                break;
            case 6:
                facility = "Printer";
                break;
            case 7:
                facility = "Network";
                break;
            case 8:
                facility = "UUCP";
                break;
            case 9:
                facility = "Clock";
                break;
            case 10:
                facility = "Security";
                break;
            case 11:
                facility = "FTP";
                break;
            case 12:
                facility = "NTP";
                break;
            case 13:
                facility = "Audit";
                break;
            case 14:
                facility = "Alert";
                break;
            case 15:
                facility = "Clock";
                break;

            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                facility = "Local";
                break;
        }
        log.tag(facility);

        log.attribute("Facility", facility);
        log.attribute("Severity", severity);

        String str = value.substring(value.indexOf(">") + 1);

        if (str.length() >= 16 && str.charAt(3) == ' ' && str.charAt(6) == ' ')
            try {
                log.date = new TDate(new SimpleDateFormat("MMM dd HH:mm:ss yyyy",
                        Locale.ENGLISH).parse(str.substring(0, 15) + " "
                                + Calendar.getInstance().get(Calendar.YEAR)));
                log.includeMilliseconds = false;
                str = str.substring(16);
            } catch (Exception pe) {
            }

        str = str.trim();

        if (str.contains(":")) {
            // rsyslog
            String[] split = str.substring(0, str.indexOf(":")).split(" ");

            if (split.length == 2) {
                log.device(split[0]);

                String s = split[1];
                if (s.contains("[") && s.endsWith("]")) {
                    s = s.substring(s.indexOf("[") + 1, s.indexOf("]"));
                    Integer pid = Utils.strInt(s, null);
                    if (pid != null) {
                        log.processId = (long) pid;
                        s = split[1].substring(0, split[1].indexOf("["));
                    }
                }
                log.tag(s);
                str = str.substring(str.indexOf(":") + 1);
            }
        }

        log.value(str.trim());

        /*
         * Numerical Code	Facility
         0	kernel messages
         1	user-level messages
         2	mail system
         3	system daemons
         4	security/authorization messages
         5	messages generated internally by syslogd
         6	line printer subsystem
         7	network news subsystem
         8	UUCP subsystem
         9	clock daemon
         10	security/authorization messages
         11	FTP daemon
         12	NTP subsystem
         13	log audit
         14	log alert
         15	clock daemon
         16	local use 0
         17	local use 1
         18	local use 2
         19	local use 3
         20	local use 4
         21	local use 5
         22	local use 6
         23	local use 7
         */
        packet.logs.add(log);

        return true;
    }
}
