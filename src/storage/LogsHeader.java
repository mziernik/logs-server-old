package storage;

import com.Utils;
import com.io.*;
import com.utils.Counter;
import com.utils.date.TDate;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static storage.common.Module.UTF8;

public class LogsHeader {

    public final byte[] signature = "mL0G".getBytes();
    //-----------
    boolean fLocal = true; // w wersji lokalnej nie jest zapisywana data serwera
    boolean fClosed = false; // plik zamknięty, nie można go już edytować
    //====================
    byte version = 1;
    byte flags = 0;
    public int logsCount = 0;
    public int attributesCount = 0;
    public int totalRawSize = 0; // sumaryczny rozmiar nieskompresowanych danych
    public TDate created = new TDate();

    public long minDate = Long.MAX_VALUE;
    public long maxDate = Long.MIN_VALUE;
    int headerSize;

    public UUID fileUid = UUID.randomUUID();
    public final LogsFile logs;

    public final Counter<String> sources = new Counter<>();
    public final Counter<String> addresses = new Counter<>();
    public final Counter<String> devices = new Counter<>();
    public final Counter<String> users = new Counter<>();
    public final Counter<String> tags = new Counter<>();
    public final Counter<String> kinds = new Counter<>();
    final Counter[] counters = new Counter[]{sources, tags, kinds, addresses, devices, users};

    final List<LogsPacket> packets = new LinkedList<>();

    LogsHeader(LogsFile logs, TInputStream in) throws IOException {
        this.logs = logs;
        if (in == null)
            return;

        int fileSize = in.available();
        if (fileSize <= 10)
            return;

        byte[] buff = new byte[signature.length];
        in.readFully(buff);
        if (!Arrays.equals(buff, signature))
            throw new IOException("Nieprawidłowa sygnatura pliku");

        version = in.readByte();
        flags = in.readByte();
        fileUid = new UUID(in.readLong(), in.readLong());
        created = new TDate(in.readLong());
        logsCount = (int) in.readDyn();
        attributesCount = (int) in.readDyn();
        totalRawSize = (int) in.readDyn();
        minDate = in.readLong();
        maxDate = in.readLong();

        for (Counter<String> cntr : counters) {

            long entries = in.readDyn();
            for (int i = 0; i < entries; i++) {

                int cnt = (int) in.readDyn();
                byte[] val = new byte[(int) in.readDyn()];
                in.read(val);
                cntr.put(new String(val, UTF8), cnt);
            }

        }

        headerSize = (int) in.readDyn();
        int pcksSize = (int) in.readDyn();

        buff = new byte[pcksSize];
        in.readFully(buff);

        buff = IOUtils.read(new InflaterInputStream(new ByteArrayInputStream(buff)));

        TInputStream pckIn = new TInputStream(buff);
        while (pckIn.available() > 0) {
            LogsPacket pck = new LogsPacket(logs, pckIn);
            packets.add(pck);
        }

        if (in.position() > headerSize)
            throw new IOException("Nieprawidłowy rozmiar nagłówka ("
                    + in.position() + " <> " + headerSize);

        byte[] empty = new byte[headerSize - (int) in.position()];
        in.readFully(empty);

        for (byte b : empty)
            if (b != 0)
                throw new IOException("Non empty");

    }

    @Override
    public String toString() {

        return "Plik: " + logs.file.getName() + "\n"
                + "Rozmiar pliku: " + Utils.formatSize(logs.file.length()) + "\n"
                + "Rozmiar danych: " + Utils.formatSize(totalRawSize) + "\n"
                + "Ilość logów: " + Utils.formatValue(logsCount) + "\n"
                + "Ilosć wartości: " + Utils.formatValue(attributesCount) + "\n"
                + "Ilość pakietów: " + Utils.formatValue(packets.size()) + "\n"
                + "Zakres dat: "
                + (minDate != Long.MAX_VALUE ? new TDate(minDate).toString(false) : "-")
                + " - " + (maxDate != Long.MIN_VALUE ? new TDate(maxDate).toString(false) : "-");

    }

    public byte[] build() throws IOException {
        TOutputStream out = new TOutputStream(true);
        out.write(signature);
        out.writeByte(version);
        out.writeByte(flags);
        out.writeLong(fileUid.getMostSignificantBits());
        out.writeLong(fileUid.getLeastSignificantBits());
        out.writeLong(created.getTime());
        out.writeDyn(logsCount);
        out.writeDyn(attributesCount);
        out.writeDyn(totalRawSize);
        out.writeLong(minDate);
        out.writeLong(maxDate);

        for (Map<String, Integer> map : counters) {
            out.writeDyn(map.size());
            for (Entry<String, Integer> en : map.entrySet()) {
                byte[] val = en.getKey().getBytes(UTF8);
                out.writeDyn(en.getValue());
                out.writeDyn(val.length);
                out.write(val);
            }
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        // przelicz wstepnie rozmiar naglowka
        try (DeflaterOutputStream dout = new DeflaterOutputStream(bout)) {
            int position = (int) out.length();
            for (LogsPacket block : packets) {
                block.position = position;
                dout.write(block.build());
                position += block.packetSize;
            }
            dout.flush();
        }

        int aproxSize = (int) out.length() + bout.size() + 100;

        bout.reset();
        try (DeflaterOutputStream dout = new DeflaterOutputStream(bout)) {
            int position = aproxSize;
            for (LogsPacket block : packets) {
                block.position = position;
                dout.write(block.build());
                position += block.packetSize;
            }
            dout.flush();
        }

        out.writeDyn(aproxSize);
        out.writeDyn(bout.size());
        out.write(bout.toByteArray());

        if (aproxSize > 0 && out.length() > aproxSize)
            throw new IOException("Nieprawidłowy rozmiar nagłówka "
                    + out.length() + " > " + aproxSize);

        //      aproxSize -  out.length()
        while (out.length() < aproxSize)
            out.write(0);

        return out.memory();
    }

}
