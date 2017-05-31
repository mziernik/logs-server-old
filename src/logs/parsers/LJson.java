package logs.parsers;

import com.exceptions.EError;
import com.utils.date.TDate;
import com.mlogger.LogElement;
import com.mlogger.LogAttr;
import com.mlogger.LogKind;
import com.google.gson.*;
import com.json.*;
import com.json.exceptions.JException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import logs.Logs.RawPacket;
import pages.*;
import pages.Common.InternalLogObject;
import service.*;
import logs.TLog;

/**
 * Miłosz Ziernik 2013/07/31
 */
public class LJson {

    public static boolean read(RawPacket packet) throws Exception {

        boolean result = false;
        if (packet.data.length < 10
                || packet.data[0] != '{'
                || packet.data[packet.data.length - 1] != '}')
            // to nie jest JSON
            return false;

        JObject json = JObject.parse(new ByteArrayInputStream(packet.data));
        json.options.quotaNames(false);

        if (json.isEmpty())
            return false;

        if (!Api.forwarding.isEmpty())
            forward(json, packet);

        try {
            int ver = json.getInt("ver");

            if (ver > 3)
                throw new Error("Nieprawidłowa wersja protokołu (" + ver + ")");

            JElement jLog = json.element("log");
            result |= jLog != null;

            if (jLog != null && jLog.isObject())
                JLog.read(jLog.asObject(), packet, ver);
            else
                if (jLog != null && jLog.isArray())
                    for (JElement je : jLog.asArray()) {
                        if (!je.isObject())
                            throw new JsonParseException("Element nie jest obiektem");
                        JLog.read(je.asObject(), packet, ver);
                    }

            JElement jStat = json.element("sts");
            result |= jStat != null;

            if (jStat != null && jStat.isObject())
                JStatus.read(jStat.asObject(), packet, ver);

            /*
             JElement mlog = json.element("jlog");
             if (mlog != null && mlog.isObject())
             readJLog(mlog.asObject(), packet, request);
             else
             if (mlog != null && mlog.isArray())

             for (JElement je : mlog.asArray()) {
             if (!je.isObject())
             throw new JsonParseException("Element nie jest obiektem");
             readJLog(je.asObject(), packet, request);
             }
             */
        } catch (Throwable e) {
            InternalLogObject log = new InternalLogObject(LogKind.warning, EError.toString(e));
            log.setExceptionDetails(e);
            log.data("JSON", json.toString());
            log.address(packet.address);
            log.setErrorStackTrace(e);
            log.add();
        }
        return result;
    }

    static void readLogElement(JObject json, TLog log, RawPacket packet)
            throws JException {

        log.attributes.clear();
        log.data.clear();
        log.properties.clear();

        log.source = json.getStr("src");
        if (log.source.isEmpty())
            throw new Error("Nazwa źródła nie może być pusta");

        try {
            JValue val = json.getValue(LogAttr.date.key);
            if (val.isNumber())
                log.date = new TDate(val.asNumber().longValue());
            else
                log.date = new TDate(val.asString());

        } catch (Exception e) {
            throw new Error("Błąd parsowania daty: " + e.getMessage()
                    + "\nFormat: yyyy-MM-dd HH:mm:ss.SSS");
        }

        log.user = json.getStr(LogAttr.user.key, "");
        JLog.readCollection(log.urls, json.element(LogAttr.url.key));
        log.session = json.getStr(LogAttr.session.key, "");
        log.instance = json.getStr(LogAttr.instance.key, "");
        log.deviceFull = json.getStr(LogAttr.device.key, "");

        if (log.deviceFull.isEmpty() && packet.request != null)
            log.deviceFull = packet.request.getHeader("User-Agent");

        log.setDevice(log.deviceFull);

        log.processId = json.getLong(LogAttr.processId.key, 0l);
        log.threadId = json.getLong(LogAttr.threadId.key, 0l);
        log.threadName = json.getStr(LogAttr.threadName.key, "");
        log.threadPriority = json.getInt(LogAttr.threadPriority.key, null);
        log.expireConsole = json.getInt(LogAttr.expireConsole.key, null);
        log.expireDatabase = json.getInt(LogAttr.expireDatabase.key, null);
        log.color = json.getStr(LogAttr.color.key, "");
        log.background = json.getStr(LogAttr.background.key, "");
        log.request = json.getStr(LogAttr.request.key, "");

        for (String adr : JLog.readCollection(new LinkedHashSet<String>(),
                json.element(LogAttr.address.key)))
            log.addAddress(adr);

        log.version = json.getStr(LogAttr.version.key, "");
        log.addAddress(packet.address);

        log.className = json.getStr(LogAttr.clazz.key, "");
        log.level = json.getInt(LogAttr.level.key, null);
        log.method = json.getStr(LogAttr.method.key, "");
        log.loggerName = json.getStr(LogAttr.logger.key, "");

        for (JValue jv : json.arrayD(LogAttr.flags.key).getValues())
            log.flags.add(jv.asString());

        JElement elValue = json.element(LogAttr.value.key, new JValue(null));

        if (elValue.isArray()) {
            log.value = JLog.readDataObj(json.arrayD(LogAttr.value.key));
            log.details = JLog.readDataObj(json.arrayD(LogAttr.details.key));
        }
        if (elValue.isValue()) {
            log.value(json.getStr(LogAttr.value.key, ""));
            log.details(json.getStr(LogAttr.details.key, ""));
        }
        JLog.readCollection(log.tags, json.element(LogAttr.tags.key));

        log.comment = json.getStr(LogAttr.comment.key, "");

        if (json.element(LogAttr.errorStack.key) instanceof JValue) {
            String[] est = json.getStr(LogAttr.errorStack.key, "").split("\n");
            for (String s : est)
                if (!s.trim().isEmpty())
                    log.errorStack.add(s.trim());
        }
        else
            for (JValue jv : json.arrayD(LogAttr.errorStack.key).getValues())
                log.errorStack.add(jv.asString());

        if (json.element(LogAttr.callStack.key) instanceof JValue) {
            String[] est = json.getStr(LogAttr.callStack.key, "").split("\n");
            for (String s : est)
                if (!s.trim().isEmpty())
                    log.callStack.add(s.trim());
        }
        else
            for (JValue jv : json.arrayD(LogAttr.callStack.key).getValues())
                log.callStack.add(jv.asString());

        log.protocol = packet.request != null ? "HTTP" : "UDP";

//-------------------------------------------------------------------------------
/*
         if (obj.has(LogAttr.properties.key))
         for (Map.Entry<String, JElement> entry
         : obj.objectC(LogAttr.properties.key).items.entrySet())
         try {
         LogElement.LogProperty prop = LogProperty.valueOf(entry.getKey());
         JElement value = entry.getValue();
         if (value.isArray()) {
         List<Object> lst = new LinkedList<>();
         for (JValue val : value.asArray().getValues())
         lst.add(val.value);

         log.properties.value.put(prop, lst);
         } else
         if (value.isValue())
         log.properties.put(prop, value.asValue().value);

         } catch (Exception e) {
         Common.addInternalWarning(e);
         }
         */
        if (log.ver >= 3)
            for (JArray ja : json.arrayD(LogAttr.data.key).getArrays()) {
                LogElement.DataObj data = JLog.readDataObj(ja);
                if (data != null)
                    log.data.add(data);
            }

        if (log.ver < 3)
            for (JArray ja : json.arrayD(LogAttr.data.key).getArrays())
                if (ja.size() == 2) {
                    JElement k = ja.element(0);
                    JElement v = ja.element(1);
                    if (k.isValue() && v.isValue())
                        log.data(k.asValue().asString(), v.asValue().asString());
                }

        // wersja 1
        for (JArray ja : json.arrayD(LogAttr.attributes.key).getArrays()) {
            if (ja.size() == 1) {
                JElement v = ja.elementF(0);
                if (v.isValue())
                    log.attribute(null, null, v.asValue().asString());
            }

            if (ja.size() == 2) {
                JElement k = ja.element(0);
                JElement v = ja.element(1);
                if (k.isValue() && v.isValue())
                    log.attribute(k.asValue().asString(), v.asValue().asString());
            }

            if (ja.size() == 3) {
                JElement g = ja.element(0);
                JElement k = ja.element(1);
                JElement v = ja.element(2);
                if (g.isValue() && k.isValue() && v.isValue())
                    log.attribute(g.asValue().asString(),
                            k.asValue().asString(), v.asValue().asString());
            }
        }

        // wersja 2
        for (JArray jo : json.objectD(LogAttr.attributes.key).getArrays()) {

            for (JArray ja : jo.getArrays()) {
                if (ja.size() == 1)
                    log.attribute(
                            jo.getName(),
                            null,
                            ja.element(0).asValue().asString());

                if (ja.size() == 2)
                    log.attribute(
                            jo.getName(),
                            ja.element(0).asValue().asString(),
                            ja.element(1).asValue().asString());

            }
        }

        for (JValue jv : json.objectD(LogAttr.properties.key).getValues()) {
            log.properties.put(jv.getName(), jv.asString());
        }

        if (log.properties.containsKey("lvl")
                && log.properties.containsKey("nbr")
                && log.properties.containsKey("lvn")) {
            // standardowy log javy

            Object lvn = log.properties.get("lvn");

            Level level = Level.ALL;
            try {
                level = Level.parse(lvn.toString());
            } catch (Exception e) {
            }

            log.kind = LogKind.debug;

            if (level == Level.SEVERE)
                log.kind = LogKind.error;

            if (level == Level.WARNING)
                log.kind = LogKind.warning;

            if (level == Level.CONFIG || level == Level.INFO)
                log.kind = LogKind.log;

            log.attribute("Level Name", lvn);
            log.attribute("Level", log.properties.get("lvl"));
            log.attribute("Sequence Number", log.properties.get("nbr"));
            log.attribute("Class", log.properties.get("src"));

        }

    }

    private static void forward(JObject json, final RawPacket packet) {
        try {
            JObject fjson = JObject.parse(json.toString());
            //     fjson.compactMode = true;
/*
             fjson.visit(new JElements.JsonElementVisitor() {

             @Override
             public void visitElement(JElement element) {
             if (element.isValue()) {
             JValue val = element.asValue();
             if (val.getName().equals(LogAttr.address.key)) {
             String ss = val.asString();
             if (ss == null || ss.trim().isEmpty())
             val.value = packet.address;
             }
             }
             }
             });
             */
            byte[] buff = json.toString().getBytes("UTF-8");
            try (DatagramSocket udpSocket = new DatagramSocket()) {
                for (SocketAddress addr : Api.forwarding)
                    udpSocket.send(new DatagramPacket(buff, buff.length, addr));
            }

        } catch (Throwable e) {
            Common.addInternalError(e);
        }
    }

    private static void JSON(ByteArrayInputStream byteArrayInputStream) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
