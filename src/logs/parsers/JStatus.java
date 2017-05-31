package logs.parsers;

import logs.Statuses;
import com.json.JObject;
import com.json.JValue;
import com.json.exceptions.JException;
import com.mlogger.LogAttr;
import com.utils.collections.Strings;
import java.io.*;
import logs.Logs.RawPacket;
import service.*;
import logs.Statuses.TStatus;
import logs.Statuses.TStatusItem;

public class JStatus {

    static void read(JObject main, RawPacket packet, int ver)
            throws IOException {

        if (ver != 3)
            throw new Error("Nieprawidłowa wersja obiektu");

        String src = main.getStr(LogAttr.source.key, "");
        String dev = main.getStr(LogAttr.device.key, "");
        Integer prc = main.getInt(LogAttr.processId.key, null);
        String comm = main.getStr(LogAttr.comment.key, "");

        String key = src + "_" + dev + "_" + prc + "_" + packet.address;

        TStatus status = new TStatus(null, key, src);
        status.comment(new Strings()
                .nonEmpty(true)
                .add(comm)
                .add(dev)
                .add(prc)
                .add(packet.address)
                .toString(", ")
        );

        status.source = src;
        status.device = dev;
        status.processId = prc;

        new Object() {

            void visitGroup(JObject obj, TStatus status) throws JException {

                status.caption = obj.getStr("cap", status.caption);
                status.comment = obj.getStr(LogAttr.comment.key, status.comment);

                for (JObject o : obj.getObjects()) {

                    String name = o.getName();
                    if (name.isEmpty())
                        continue;

                    char c = name.charAt(0);
                    name = name.substring(1);

                    switch (c) {
                        case 'g':
                            visitGroup(o, name.isEmpty()
                                    ? status : new TStatus(status, name, null));
                            break;

                        case 'i':
                            TStatusItem log = new TStatusItem(status, name);
                            for (JValue jv : o.arrayD("tag").getValues())
                                log.tags.add(jv.asString());
                            log.value = o.getStr("val", "").trim();
                            log.comment = o.getStr("com", "").trim();
                            log.color = o.getStr("fcl", "").trim();
                            log.background = o.getStr("bcl", "").trim();

                            for (JValue jv : o.arrayD("tag").getValues())
                                log.tags.add(jv.asString());

                            JLog.readCollection(log.addresses, o.element("adr"));
                            
                            log.expire = o.getInt(LogAttr.expireConsole.key, null);
                            log.progress = o.getDouble("prg", null);
                            if (log.progress != null)
                                if (log.progress < 0)
                                    log.progress = 0d;
                                else
                                    if (log.progress > 1)
                                        log.progress = 1d;
                            break;
                    }

                }
            }
        }.visitGroup(main, status);

        //  if (status.getItems(true).isEmpty())
        //      return;
        //   status.source = values.get(0).asString();
        Statuses.add(status);

    }

}
