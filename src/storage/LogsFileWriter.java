package storage;

import com.*;
import com.io.TInputStream;
import com.io.TOutputStream;
import com.mlogger.Log;
import com.threads.TThread;
import java.io.*;
import java.util.*;

import static storage.common.Module.BUFFER_SIZE;

public class LogsFileWriter extends TThread {

    private static LogsFileWriter instance;

    private final static List<LogsPacket> queue = new LinkedList<>();

    public static void add(LogsPacket packet) {
        if (instance == null) {
            instance = new LogsFileWriter();
            instance.start();
        }

        synchronized (queue) {
            queue.add(packet);
            queue.notifyAll();
        }

    }

    public static List<LogsPacket> getQueue() {
        synchronized (queue) {
            return Utils.asList(queue);
        }
    }

    public LogsFileWriter() {
        super("Log File Writer");
    }

    @Override
    protected void execute() throws Exception {

        while (isRunning())
            try {

                LinkedList<LogsPacket> list = new LinkedList<>();

                if (queue.isEmpty())
                    synchronized (queue) {
                        queue.wait();
                    }

                if (queue.isEmpty())
                    continue;

                synchronized (queue) {
                    list.addAll(queue);
                    queue.clear();
                }

                write(list);

            } catch (InterruptedException e) {
                return;
            } catch (Throwable e) {
                Log.error(e);
                e.printStackTrace();
            }

    }

    private void write(LinkedList<LogsPacket> packets) throws IOException, InterruptedException {

        if (packets.isEmpty())
            return;

        LogsFile logs = LogsStorage.current();

        if (!logs.file.exists() || logs.file.length() != logs.fileSize) {
            LogsStorage.createNewLogs();
            for (LogsPacket pck : packets)
                LogsFileWriter.add(pck);
            return;
        }

        // skompresuj wszystko
        for (LogsPacket pck : packets)
            pck.getCompressed();

        logs.beginWrite();
        try {

            LogsHeader header = logs.header;

            int oryginalHeaderSize = logs.header.headerSize;

            for (LogsPacket pck : packets) {

                for (byte[] b : pck.blocks)
                    header.totalRawSize += b.length;

                header.attributesCount += pck.attributesCount;
                header.logsCount += pck.logsCount;
                if (header.minDate > pck.minDate)
                    header.minDate = pck.minDate;
                if (header.maxDate < pck.maxDate)
                    header.maxDate = pck.maxDate;

                pck.packetSize = pck.getCompressed().length;

                header.sources.incAll(pck.sources);
                header.kinds.incAll(pck.kinds);
                header.tags.incAll(pck.tags);
                header.devices.incAll(pck.devices);
                header.users.incAll(pck.users);
                header.addresses.incAll(pck.addresses);
            }

            //  System.out.println("Zapisuję " + packets.size() + " pakietów");
            // weryfikacja ------------------------------------------------- usunac ------------------------------------------------
        /*    int readed = currentPacket.readData(new TInputStream(dBlock), this, null, null, null);
             if (readed != currentPacket.logsCount)
             throw new IOException("Błąd weryfikacji");
             */
            File temp = new Path(logs.file).changeExtension("~mlog").toFile();

            try (TOutputStream out = new TOutputStream(temp)) {

                int oryginalDataSize = (int) logs.file.length() - header.headerSize;

                int size = 0;
                for (LogsPacket pck : header.packets)
                    size += pck.packetSize;

                if (size != oryginalDataSize)
                    throw new IOException("Nieprawidłowy rozmiar danych "
                            + Utils.formatValue(oryginalDataSize)
                            + " <> " + Utils.formatValue(size));

                synchronized (header) {
                    for (LogsPacket pck : packets)
                        header.packets.add(0, pck);
                }
                byte[] bheader = header.build();
                header.headerSize = bheader.length; // zapisz rozmiar nowego naglowka
                out.write(bheader);
                // odwracamy kolejność pakietów, aby zapisywać od końca
                Collections.reverse(packets);
                for (LogsPacket pck : packets)
                    out.write(pck.getCompressed());

                try (TInputStream in = new TInputStream(logs.file, BUFFER_SIZE)) {
                    in.skipTo(oryginalHeaderSize);

                    if (in.available() != oryginalDataSize)
                        throw new IOException("Nieprawidłowy rozmiar pliku");

                    byte[] buffer = new byte[1024 * 100];
                    int read;
                    while ((read = in.read(buffer)) > 0)
                        out.write(buffer, 0, read);
                }

                TConsole.printTs("Zapisuję plik logów %s, rozmiar: %s, log: %s, pck: %s",
                        logs.file.getName(),
                        Utils.formatSize(logs.file.length()),
                        Utils.formatValue(header.logsCount),
                        Utils.formatValue(header.packets.size()));

            }

            int size = 0;
            for (LogsPacket pck : header.packets)
                size += pck.packetSize;

            if (size != temp.length() - header.headerSize)
                throw new IOException("2 Nieprawidłowy rozmiar danych "
                        + Utils.formatValue(logs.file.length() - header.headerSize)
                        + " <> " + Utils.formatValue(size));

            logs.file.delete();
            boolean renamed = temp.renameTo(logs.file);

            for (int i = 0; i < 10 && !renamed; i++) {
                renamed = temp.renameTo(logs.file);
                Thread.sleep(10);
            }

            logs.fileSize = logs.file.length();

        } finally {
            logs.endWrite();
        }

        // opóźnienie, aby zapis nie odbywał się zbyt często
        Thread.sleep(1000);

    }
}
