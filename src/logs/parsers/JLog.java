package logs.parsers;

import logs.TLog;
import com.utils.hashes.Hashes;
import com.mlogger.LogElement;
import com.mlogger.LogKind;
import com.mlogger.LogAttr;
import com.mlogger.DataType;
import com.*;
import com.json.*;
import com.json.exceptions.JException;
import java.util.*;
import com.mlogger.LogElement.DataObj;
import com.utils.collections.Strings;
import logs.Logs.RawPacket;
import pages.*;

public class JLog {

    static boolean read(JObject obj, RawPacket packet, int ver) throws Exception {
        if ((ver < 1 || ver > 3))
            throw new Error("Nieprawidłowa wersja obiektu");

        String sUid = obj.getStr(LogAttr.uid.key, "");
        UUID uid;
        try {
            uid = sUid.isEmpty() ? null : UUID.fromString(sUid);
        } catch (IllegalArgumentException e) {
            uid = UUID.nameUUIDFromBytes(Hashes.md5B(sUid));
        }

        TLog log = new TLog(obj.toString(), ver, uid);

        String sKind = obj.getStr(LogAttr.kind.key);

        log.createTime = packet.date;
        try {
            log.kind = LogKind.valueOf(sKind);

        } catch (Exception e) {
            List<String> lst = new LinkedList<>();
            for (LogKind lk : LogKind.values())
                lst.add(lk.name());

            throw new Error("Nieprawidłowa wartość "
                    + LogAttr.kind + "\nDozwolone wartości: "
                    + new Strings(lst).toString(", "));
        }

        LJson.readLogElement(obj, log, packet);
        packet.logs.add(log);
        return true;
    }

    static DataObj readDataObj(JArray arr) throws JException {

        if (arr == null || arr.isEmpty())
            return null;

        JElement el = arr.elementF(0);
        DataType type = DataType.text;
        if (el.isValue())
            try {
                String s = el.asValue().asString().trim().toLowerCase();
                if (!s.isEmpty())
                    type = DataType.valueOf(s);
            } catch (Exception ex) {
                Common.addInternalWarning(ex);
            }
        String name = "";

        if (arr.size() > 1 && arr.elementF(1).isValue())
            name = arr.elementF(1).asValue().asString();

        String value = "";
        if (arr.size() > 2 && arr.elementF(2).isValue())
            value = arr.elementF(2).asValue().asString();

        return new LogElement.DataObj(name, value, type);
    }

    static Set<String> readCollection(Set<String> set, JElement el) {
        if (el == null)
            return set;
        if (el.isValue()) {
            String s = el.asValue().asString();
            if (s != null && !s.isEmpty())
                set.add(s);
        }

        if (el.isArray())
            for (JValue jv : el.asArray().getValues()) {
                String s = jv.asString();
                if (s != null && !s.isEmpty())
                    set.add(s);
            }
        return set;
    }

}
