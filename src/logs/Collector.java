package logs;

import com.context.AppContext;
import storage.LogsStorage;
import com.threads.QueueThread;
import com.utils.date.Timestamp;
import console.Notifier;
import java.util.*;
import service.Config;

import static logs.Logs.*;

public class Collector extends QueueThread<TLog> {

    public final static Collector instance = new Collector();

    private Collector() {
        super("Logs collector");
        //  setIdleTime(100);
    }

    @Override
    protected boolean canProcess(LinkedList<TLog> queue) {
        return AppContext.initialized;
    }

    @Override
    protected void processQueueSync(final LinkedList<TLog> items) throws Exception {

        Integer limit = Config.CConsole.totalLogsLimit.value(50000);

        for (TLog log : items) {

            log.id = Logs.globalLogId.incrementAndGet();

            //              Console.printlnTs("ID: %d, processQueue", log.id);
            // dodaj do listy źródeł
            final LinkedList<TLog> toRemove = new LinkedList<>();
            addLogSource(log, toRemove);

            if (log.uid != null)
                idxUids.put(log.uid, log);

            synchronized (all) {
                if (all.size() > limit) {
                    Iterator<TLog> itr = all.iterator();
                    for (int i = 0; i < all.size() - limit && itr.hasNext(); i++)
                        toRemove.add(itr.next());
                }

                if (!toRemove.isEmpty())
                    all.removeAll(toRemove);

                for (TLog rem : toRemove) {
                    currentRawDataSize -= rem.rawDataSize;
                    idxUids.remove(rem.uid);
                    if (log.rawData != null)
                        currentCompressedDataSize -= log.rawData.length;

                    for (List<TLog> lst : sources.values())
                        if (lst.contains(rem)) {
                            lst.remove(rem);
                            break;
                        }
                }

                all.add(log);

                currentRawDataSize += log.rawDataSize;
                totalRawDataSize += log.rawDataSize;
                if (log.rawData != null)
                    currentCompressedDataSize += log.rawData.length;
            }

            // stsAdd.addNano(System.nanoTime() - tt);
            //  stsProcessTime.addMs(System.currentTimeMillis() - log.createTime.getTime());
        }

        Notifier.add(items);

        LogsStorage.addAll(items);

    }

    private synchronized static void addLogSource(TLog log, LinkedList<TLog> toRemove) {
        LinkedList<TLog> list = sources.get(log.source);
        if (list == null) {
            list = new LinkedList<>();
            sources.put(log.source, list);
        }
        list.add(log);

        while (list.size() > Config.CConsole.sourceLogsLimit.value(10000))
            toRemove.add(list.pollFirst());
    }
}
