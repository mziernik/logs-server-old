package logs;

import com.utils.collections.HashList;
import com.utils.date.TDate;
import com.servlet.requests.HttpRequest;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import logs.parsers.*;

/**
 * Miłosz Ziernik 2013/01/23
 */
public abstract class Logs {

    public final static HashList<TLog> all = new HashList<>();
    public static boolean consoleLogListChanged = false;
    public static int maxStatusesCount = 100;
    public static String logsGuid = "";
    public static long totalConsoleLogs;
    public static long currentLogsSize;
    public static long totalLogsSize;
    public final static Map<String, String> dns = new HashMap<>();
    public static long currentRawDataSize;
    public static long totalRawDataSize;
    public static long currentCompressedDataSize;

    final static AtomicLong globalLogId = new AtomicLong();

    //-----------------------------------------------------------
    // ilości poszczególnych wartości logów
    //   public final static HashMap<String, Counter> sources = new HashMap<>();
    //  public final static HashMap<LogKind, Counter> kinds = new HashMap<>();
    public final static LinkedHashMap<String, Integer> allSources = new LinkedHashMap<>();
    public final static LinkedHashMap<String, Integer> allKinds = new LinkedHashMap<>();
    public final static Map<String, LinkedList<TLog>> sources = new LinkedHashMap<>();

    //----------------------------------
    //   private final static TimeDiffStatistics stsAdd = new TimeDiffStatistics("Logi", "Dodawanie", "#da0");
    //private final static TimeDiffStatistics stsAnalyze = new TimeDiffStatistics("Logi", "Analiza", "#afa");
    //  private final static TimeDiffStatistics stsProcessTime = new TimeDiffStatistics("Logi", "Czas przetwarzania", "#eee");
    public final static Map<UUID, TLog> idxUids = new HashMap<>();

    // przykrycie konstruktora
    private Logs() {
    }

    /*
     public Logs() {
     super();
     setName("Logs Manager [" + getId() + "]");
     }

    
    
     @Override
     protected void processQueue(LinkedList<RawPacket> items) throws Exception {

     for (RawPacket item : items) {
     setCurrent(item);
     addPacket(item);
     }
     }
     */
    public static void removeAll(Collection<TLog> logs) {
        if (logs.isEmpty())
            return;

        synchronized (all) {
            all.removeAll(logs);

            for (TLog log : logs)
                if (log != null)
                    idxUids.remove(log.uid);
        }
    }

    /**
     * Zwiększa lub zmniejsza licznik obiektów danego typu
     *
     * @param map
     * @param name
     * @param increment
     */
    public static void counerMap(HashMap<String, Integer> map,
            String name, boolean increment) {

        if (name == null || name.trim().isEmpty())
            name = "<brak>";
        Integer cnt = map.get(name);
        if (cnt == null)
            cnt = 0;
        cnt += increment ? 1 : -1;
        if (cnt <= 0)
            map.remove(name);
        else
            map.put(name, cnt);
    }

    public static class RawPacket {

        public final TDate date;
        public final String address;
        public byte[] data;
        public final LogSource source;
        public UUID token;
        public UUID priorityUid;
        public boolean encrypted;
        public boolean compressed;
        public boolean priority;
        public final int oryginalSize;
        public HttpRequest request;
        public final List<TLog> logs = new LinkedList<>();

        public RawPacket(final String address, final byte[] data,
                final LogSource source) {
            this.address = address;
            this.data = data;
            this.source = source;
            oryginalSize = data.length;
            date = new TDate();

            // Console.printlnTs("ID: %d, New Raw", id);
        }

        @Override
        public final String toString() {
            return new String(data, Charset.forName("UTF-8"));
        }

        public void process() {
            Processor.instance.add(this);
        }

    }

}
