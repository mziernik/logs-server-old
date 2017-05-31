package storage;

import com.TConsole;
import com.io.*;
import com.mlogger.LogAttr;
import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import logs.TLog;
import storage.common.*;

public class LogsFile extends Module {

    final File file;
    final LogsHeader header; // sync
    long fileSize;
    private int lastLogId;
    boolean initialized = false;

    boolean fileLocked = false;

    private final AtomicInteger currentReaders = new AtomicInteger();
    private boolean writeLock = false;

    void beginRead() throws InterruptedException {
        while (writeLock)
            Thread.sleep(1);
        synchronized (currentReaders) {
            currentReaders.incrementAndGet();
        }
    }

    void endRead() {
        synchronized (currentReaders) {
            currentReaders.decrementAndGet();
        }
    }

    void beginWrite() throws InterruptedException {
        writeLock = true;
        while (currentReaders.get() > 0)
            Thread.sleep(1);
    }

    void endWrite() {
        writeLock = false;
    }

    public LogsFile(File file) throws IOException {

        fileSize = file.length();
        this.file = file;
        try {
            boolean isNew = !file.exists();
            if (isNew)
                file.createNewFile();

            try (TInputStream in = new TInputStream(
                    new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE))) {
                header = new LogsHeader(this, in);
                if (isNew || file.length() == 0)
                    return;
                lastLogId = header.logsCount;
                // Timestamp ts = new Timestamp();
                //readFile(in);
                TConsole.printParams("Szczegóły pliku logu", header.toString());
                // ts.consoleDiff("Czas wczytywania");
            }

        } finally {
            initialized = true;
        }
    }

    @Override
    public String toString() {
        return header.toString();
    }

    private void readFile(TInputStream in) throws IOException {

        LogsVisitor visitor = new LogsVisitor() {

            @Override
            public boolean onRead(LogsFile logs, TLog log) {
                // Console.printlnTs("Log " + log.id + " " + log.createTime.toString(TDate.TIME_MS));
                // for (LogValue val : log.values)
                //     Console.println("  " + val);
                return true;
            }
        };

        for (LogsPacket pck : header.packets)
            if (!pck.readData(in, this, null, null, null, null))
                return;

    }
    //---------------------------- currentPacket ------------------------------
    LogsPacket currentPacket;
    final Object currentPacketSync = new Object();

    public void flush() {
        synchronized (currentPacketSync) {
            if (currentPacket != null && !currentPacket.blocks.isEmpty())
                LogsFileWriter.add(currentPacket);
            currentPacket = null;
        }
    }

    void writeLogs(LinkedList<TLog> items) throws IOException {

        for (TLog log : items)
            synchronized (currentPacketSync) {

                if (currentPacket == null)
                    currentPacket = new LogsPacket(this);
                currentPacket.add(log, ++lastLogId);

                int totalSize = 0;
                for (byte[] buff : currentPacket.blocks)
                    totalSize += buff.length;

                if (totalSize > LogsStorage.RAW_PACKET_SIZE)
                    flush();
            }

    }
    //-------------------------------------------------------------------------

    public boolean read(Integer maxId, Long maxDate, LogAttr[] attrs, LogsVisitor intf)
            throws IOException, InterruptedException {

        beginRead();
        try {
            try (TInputStream in = new TInputStream(
                    new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE))) {

                boolean checkPos = true;
                for (LogsPacket pck : header.packets) {
                    if (checkPos) {
                        if (maxId != null && pck.minId > maxId)
                            continue;
                        if (maxDate != null && pck.minDate > maxDate)
                            continue;
                    }

                    if (checkPos)
                        in.skipTo(pck.getPosition());

                    if (!pck.readData(in, this, maxId, maxDate, attrs, intf))
                        return false;
                    checkPos = false;
                }
            }
        } finally {
            endRead();
        }

        return true;
    }

}
