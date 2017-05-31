package storage;

import com.*;
import com.context.AppContext;
import com.io.*;
import com.mlogger.Log;
import com.mlogger.LogAttr;
import com.threads.QueueThread;
import com.utils.date.TDate;
import java.io.*;
import java.util.*;
import logs.TLog;
import pages.Common;
import storage.common.LogsVisitor;

public class LogsStorage {

    final static int RAW_PACKET_SIZE = 1024 * 100; // szacowany rozmiar nieskompresowanego pakietu
    final static int WRITE_IDLE_TIME = AppContext.devMode() ? 5000 : 60000;
//60000; // czas bezczynności, po kótrym logi 
    // zostana zapisane do pliku nawet jeśli rozmiar pakietu nie osiagnal założonego rozmiaru (60 sekund)
    final static LogsQueue queue = new LogsQueue();
    private static LogsFile current;
    public final static List<LogsFile> all = new LinkedList<>();
    static int allLogsCount = 0;

    private static boolean initialized;

    public static int getAllLogsCount() {
        return allLogsCount;
    }

    public static List<LogsFile> readLogs(Integer maxId,
            Long maxDate, LogAttr[] attrs, LogsVisitor intf)
            throws IOException, InterruptedException {
        initialize();

        List<LogsPacket> packets = LogsFileWriter.getQueue();

        /*       synchronized (current().currentPacketSync) {
         LogsPacket pck = current().currentPacket;
         if (pck != null)
         packets.add(0, pck);
         }
         */
        for (LogsPacket pck : packets)
            if (maxDate == null || pck.minDate <= maxDate) {
                byte[] bpacket = current().currentPacket.getCompressed();
                pck.readData(new TInputStream(bpacket), pck.logs, maxId, maxDate, attrs, intf);
            }

        List<LogsFile> list = new LinkedList<>();

        for (LogsFile logs : all) {
            if (maxDate != null && logs.header.minDate > maxDate)
                continue;
            list.add(logs);
            if (!logs.read(maxId, maxDate, attrs, intf))
                break;
        }

        return list;
    }

    public static void add(TLog log) throws IOException {
        if (!CLogsStorage.enabled.value())
            return;
        initialize();
        queue.add(log);
    }

    public static void addAll(Collection<TLog> logs) throws IOException {
        if (!CLogsStorage.enabled.value())
            return;
        initialize();
        queue.addAll(logs);
    }

    public static void onError(Throwable e) {
        e.printStackTrace();
        Common.addInternalError(e);
        Log.error(e).tag("LogsStorage");
    }

    public static void initialize() throws IOException {

        if (initialized)
            return;

        try {
            for (Path path : new SearchFiles(AppContext.logsPath.toString(), true))
                if (path.endsWith(".mlog"))
                    try {
                        LogsFile logs = new LogsFile(path.toFile());

                        if (!logs.header.fClosed)
                            if (current == null || current.header.maxDate < logs.header.maxDate)
                                current = logs;

                        allLogsCount += logs.header.logsCount;

                        all.add(logs);
                    } catch (Throwable e) {
                        onError(e);
                    }

            // posortuj, aby uzyskać właściwą kolejność
            Collections.sort(all, new Comparator<LogsFile>() {

                @Override
                public int compare(LogsFile o1, LogsFile o2) {
                    return o2.header.created.compareTo(o1.header.created);
                }

            });

            initialized = true;

        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void flush() throws IOException {
        current().flush();
    }

    public static LogsFile current() throws IOException {
        if (current == null)
            createNewLogs();
        return current;
    }

    static void createNewLogs() throws IOException {

        if (current != null)
            current.flush();

        current = new LogsFile(FileUtils.getUniqueFileName(AppContext.logsPath.getFile(
                AppContext.serviceName + " " + new TDate().toString("yyyy-MM-dd") + ".mlog")));

        TConsole.printTs("Utworzono plik logów " + current.file.getName());

        synchronized (all) {
            all.add(0, current);
        }

    }

    public static LogsFile getCurrent() throws IOException {
        initialize();
        return current;
    }

    public static int getQueueSize() {
        return 0;
    }

    /**
     Kolejka logów
     @author user
     */
    static class LogsQueue extends QueueThread<TLog> {

        public LogsQueue() {
            super("Storage Logs Queue");
            setIdleTime(WRITE_IDLE_TIME);
        }

        @Override
        protected boolean canProcess(LinkedList<TLog> queue) {
            return initialized;
        }

        @Override
        protected void onIdle() throws Exception {
            //       if (current != null)                current.flush();
        }

        @Override
        protected void onException(Throwable e) {
            LogsStorage.onError(e);
        }

        @Override
        protected void processQueueSync(LinkedList<TLog> items) throws Exception {

            current(); // utworz, jesli nie istnieje

            if (!current.header.created.isSameDay(new TDate())) {
                System.out.println("-------------- ZMIANA DATY ---------------------");
                createNewLogs();
            }

            boolean exists;
            current.beginRead();
            try {
                exists = current.file.exists();
            } finally {
                current.endRead();
            }

            if (!exists) {
                System.out.println("-------------- PLIK NIE ISTNIEJE ---------------------");
                createNewLogs();
            }

            // Aby wielokrotnie nie odwoływać się do pliku indeksu, 
            // zbuduj listę wszystkich wartości, a nastepnie pobierz pozycję 
            // wszystkich na raz. Zmniejszy to ilość operacji I/O 
            current.writeLogs(items);

        }

    }
}
