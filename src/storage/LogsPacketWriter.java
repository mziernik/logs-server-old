package storage;

import com.Utils;
import com.io.TOutputStream;
import com.mlogger.*;
import java.io.IOException;
import java.util.*;
import logs.TLog;

import static storage.common.Module.UTF8;

class LogsPacketWriter {

    final TOutputStream attrs = new TOutputStream(true);
    final TLog log;
    final LogsPacket packet;

    public LogsPacketWriter(LogsPacket packet, TLog log) {
        this.packet = packet;
        this.log = log;
    }

    public byte[] build(int id) throws IOException {
        TOutputStream out = new TOutputStream(true);
        buildAttributes();
        byte[] battrs = attrs.memory();
        out.writeDyn(id);
        out.writeLong(log.createTime.getTime());
        out.writeLong(log.date.getTime());
        out.writeDyn(log.expireDatabase != null ? log.expireDatabase : 0);
        out.write(log.level != null ? log.level : 0);
        out.writeDyn(battrs.length);
        out.write(battrs);

       // System.out.println("Write LOG " + id + " " + log.createTime.toString(TDate.TIME_MS));

        byte xor = 0;
        for (byte i : out.memory())
            xor ^= i;

        out.writeByte(xor);
        out.flush();
        return out.memory();

    }

    private void buildAttributes() throws IOException {

        addVal(LogAttr.source, log.source);
        addVal(LogAttr.kind, log.kind.name());
        addVal(LogAttr.device, log.deviceFull);
        addVal(LogAttr.user, log.user);
        addVal(LogAttr.address, log.addresses);
        addVal(LogAttr.tags, log.tags);
        addVal(LogAttr.background, log.background);
        addVal(LogAttr.color, log.color);
        addVal(LogAttr.callStack, log.callStack);
        addVal(LogAttr.errorStack, log.errorStack);
        addVal(LogAttr.comment, log.comment);
        addVal(LogAttr.flags, log.flags);
        addVal(LogAttr.clazz, log.className);
        addVal(LogAttr.method, log.method);
        addVal(LogAttr.logger, log.loggerName);
        addVal(LogAttr.processId, log.processId);
        addVal(LogAttr.threadId, log.threadId);
        addVal(LogAttr.threadName, log.threadName);
        addVal(LogAttr.threadPriority, log.threadPriority);
        addVal(LogAttr.request, log.request);
        addVal(LogAttr.session, log.session);
        addVal(LogAttr.url, log.urls);
        addVal(LogAttr.version, log.version);
        addDataVal(LogAttr.value, log.value, false);
        addDataVal(LogAttr.details, log.details, false);
        for (LogElement.DataObj data : log.data)
            addDataVal(LogAttr.data, data, true);

        for (Map.Entry<String, LogElement.DataPairs> en : log.attributes.entrySet())
            for (LogElement.DataPair dp : en.getValue()) {
                List<String> vals = new LinkedList<>();
                vals.add(en.getKey());
                vals.add(dp.name);

                if (dp.value instanceof Collection)
                    for (Object o : (Collection) dp.value)
                        vals.add(Utils.toString(o));
                else
                    vals.add(Utils.toString(dp.value));

                addVal(LogAttr.attributes, vals);
            }
    }

    private void addVal(LogAttr attr, DataType type, Object value) throws IOException {
        if (value == null || value.toString().isEmpty())
            return;

        if (value instanceof Collection)
            if (((Collection) value).isEmpty())
                return;

        if (type == null)
            type = DataType.text;

        Collection<Object> lst = null;
        if (value instanceof Collection)
            lst = (Collection<Object>) value;

        if (lst == null) {
            lst = new LinkedList<>();
            lst.add(value);
        }

        attrs.writeDyn(attr.getId());
        attrs.write(type.getId());
        attrs.writeDyn(lst.size());

        for (Object o : lst) {
            byte[] buff = Utils.coalesce(Utils.toString(o), "").getBytes(UTF8);
            ++packet.attributesCount;

            attrs.writeDyn(buff.length);
            attrs.write(buff);
        }
    }

    private void addVal(LogAttr attr, Object value) throws IOException {
        addVal(attr, null, value);
    }

    private void addDataVal(LogAttr attr, LogElement.DataObj value, boolean includeName)
            throws IOException {
        if (value == null || value.isEmpty())
            return;

        List<String> vals = new LinkedList<>();
        if (includeName)
            vals.add(Utils.coalesce(value.name, ""));
        if (value.value instanceof Collection)
            for (Object o : (Collection) value.value)
                vals.add(Utils.toString(o));
        else
            vals.add(Utils.toString(value.value));

        addVal(attr, value.type, vals);
    }
}
