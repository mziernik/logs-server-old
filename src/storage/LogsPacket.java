package storage;

import com.Utils;
import com.io.*;
import com.mlogger.DataType;
import com.mlogger.LogAttr;
import com.utils.Counter;
import com.utils.date.TDate;
import java.io.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import logs.TLog;
import storage.common.LogsVisitor;

import static storage.common.Module.UTF8;

public class LogsPacket {

    public final LogsFile logs;
    int minId = Integer.MAX_VALUE;
    int maxId = Integer.MIN_VALUE;
    long minDate = Long.MAX_VALUE;
    long maxDate = Long.MIN_VALUE;
    int packetSize;
    int position;
    int logsCount;
    int attributesCount;

    final Counter<String> sources = new Counter<>();
    final Counter<String> addresses = new Counter<>();
    final Counter<String> devices = new Counter<>();
    final Counter<String> users = new Counter<>();
    final Counter<String> tags = new Counter<>();
    final Counter<String> kinds = new Counter<>();

    final List<byte[]> blocks = new LinkedList<>();

    @Override
    public String toString() {
        return "Logów: " + logsCount + ", rozmiar: " + Utils.formatSize(packetSize) + ",\n"
                + Utils.formatValue(minId) + " - " + Utils.formatValue(maxId) + ",\n"
                + new TDate(minDate) + " - " + new TDate(maxDate);
    }

    public byte[] build() throws IOException {
        TOutputStream out = new TOutputStream(true);
        out.writeDyn(position);
        out.writeDyn(packetSize);
        out.writeDyn(logsCount);
        out.writeDyn(attributesCount);
        out.writeDyn(minId);
        out.writeDyn(maxId);
        out.writeLong(minDate);
        out.writeLong(maxDate);
        return out.memory();
    }

    public int getPosition() {
        return position;
    }

    public LogsPacket(LogsFile logs) {
        this.logs = logs;
    }

    LogsPacket(LogsFile logs, TInputStream in) throws IOException {
        this.logs = logs;
        position = (int) in.readDyn();
        packetSize = (int) in.readDyn();
        logsCount = (int) in.readDyn();
        attributesCount = (int) in.readDyn();
        minId = (int) in.readDyn();
        maxId = (int) in.readDyn();
        minDate = in.readLong();
        maxDate = in.readLong();
    }

    /**
    
     @param src
     @param logs
     @param maxId
     @param maxDate
     @param visitor
     @return Ilość zwróconych logów
     @throws IOException 
     */
    boolean readData(final TInputStream src, final LogsFile logs, final Integer maxId,
            final Long maxDate, final LogAttr[] attrs, final LogsVisitor visitor)
            throws IOException {

        src.setLimit(packetSize);

        byte[] rawData = IOUtils.read(new InflaterInputStream(src));

        TInputStream in = new TInputStream(rawData);

        for (int i = 0; i < logsCount; i++) {

            int blockSize = (int) in.readDyn();

            byte[] block = new byte[blockSize];
            in.readFully(block);

            TLog log = readLog(block, attrs);

            if (maxId != null && log.id >= maxId - 1)
                continue;

            if (maxDate != null && log.createTime.getTime() > maxDate - 1)
                continue;

            if (visitor != null)
                if (!visitor.onRead(logs, log))
                    return false;
        }
        return true;
    }

    private byte[] compressed;

    byte[] getCompressed() throws IOException {
        if (compressed == null) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try (TOutputStream out = new TOutputStream(new DeflaterOutputStream(bout))) {
                for (byte[] buff : blocks) {
                    out.writeDyn(buff.length);
                    out.write(buff);
                }
                out.flush();
            }
            compressed = bout.toByteArray();
        }
        return compressed;
    }

    void add(TLog log, int id) throws IOException {

        byte[] block = new LogsPacketWriter(this, log).build(id);

        blocks.add(0, block);
        ++logsCount;

        if (minId == Integer.MAX_VALUE)
            minId = id;
        if (id > maxId)
            maxId = id;

        long date = log.createTime.getTime();
        if (minDate > date)
            minDate = date;
        if (maxDate < date)
            maxDate = date;

        sources.inc(log.source);
        kinds.inc(log.kind.name());
        for (String tag : log.tags)
            tags.inc(tag);
        if (log.device != null)
            devices.inc(log.device);
        for (String s : log.addresses)
            addresses.inc(s);
        if (log.user != null)
            users.inc(log.user);

    }

    TLog readLog(byte[] data, LogAttr[] attrs) throws IOException {

        TInputStream in = new TInputStream(data);

        // ByteArrayOutputStream bout = new ByteArrayOutputStream();
        // in.addMirror(bout);
        long id = in.readDyn();
        long created = in.readLong();

        TLog log = new TLog(null, new TDate(created));
        log.id = id;
        log.date = new TDate(in.readLong());

        log.expireDatabase = (int) in.readDyn();
        log.level = in.read();

        TInputStream attrIn = new TInputStream(in);
        long attrsSize = in.readDyn();
        attrIn.setLimit(attrsSize);

        while (attrIn.available() > 0)
            readAttribute(log, attrIn, attrs);

        // in.removeMirror(bout);

        /*  byte xor = 0;
         for (byte i : bout.toByteArray())
         xor ^= i;

         byte rxor = in.readByte();
         if (xor != rxor)
         throw new IOException("Nieprawidłowa suma kontrolna bloku");
         */
        return log;
    }

    private void readAttribute(TLog log, TInputStream in, LogAttr[] attrs) throws IOException {

        LogAttr attr = LogAttr.get((int) in.readDyn());

        TLog.LogValue val = null;
        if (attrs == null || Utils.isIn(attr, attrs))
            val = log.new LogValue(attr,
                    DataType.get((int) in.readDyn()));

        int valuesCount = (int) in.readDyn();
        for (int i = 0; i < valuesCount; i++) {
            byte[] bvalue = new byte[(int) in.readDyn()];
            in.readFully(bvalue);
            if (val != null)
                val.addValue(new String(bvalue, UTF8));
        }

        //   System.out.println("  " + val.toString());
    }

}
