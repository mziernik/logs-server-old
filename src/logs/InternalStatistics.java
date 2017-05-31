package logs;

import logs.Logs;
import com.Utils;

import static com.Utils.formatValue;

import com.threads.*;
import console.*;
import logs.Statuses.TStatus;
import logs.Statuses.TStatusItem;
import storage.LogsStorage;

public class InternalStatistics extends LoopThread {

    public InternalStatistics() {
        super("InternalStatistics");
        start();
    }

    @Override
    protected void loop() throws Exception {

        TStatus status = new TStatus(null, "logs::this", "Logi");

        new TStatusItem(status, "c")
                .tags("Ilość logów w konsoli")
                .value(formatValue(Logs.all.size()))
                .comment("GC: " + formatValue(TLog.instanceCount));

        new TStatusItem(status, "ls")
                .tags("Rozmiar logów w konsoli")
                .value(Utils.formatSize(Logs.currentRawDataSize)
                        + " (" + Utils.formatSize(Logs.currentCompressedDataSize) + ")");

        new TStatusItem(status, "ts")
                .tags("Łączny rozmiar przetworzonych logów")
                .value(Utils.formatSize(Logs.totalRawDataSize));


        /*new TStatusItem(status, "t")
         .tags("Ilość logów w archiwum")
         .value(formatValue(Archivizer.totalArch));
         */
        int q = Processor.instance.getQueueSize() + Collector.instance.getQueueSize();

        new TStatusItem(status, "q")
                .tags("Kolejka pakietów")
                .value(formatValue(q))
                .color(q > 10000 ? "red" : q > 3000 ? "yellow" : null);

        q = LogsStorage.getQueueSize();
        new TStatusItem(status, "a")
                .tags("Kolejka archiwizacji")
                .value(formatValue(q))
                .color(q > 10000 ? "red" : q > 3000 ? "yellow" : null);

        TLog lFirst = Logs.all.first();
        TLog lLast = Logs.all.last();

        if (lFirst != null)
            new TStatusItem(status, "to")
                    .tags("Najstarszy log w konsoli")
                    .value(lFirst.createTime.toString("dd MMMM HH:mm:ss"));

        if (lLast != null)
            new TStatusItem(status, "tn")
                    .tags("Najnowszy log w konsoli")
                    .value(lLast.createTime.toString("dd MMMM HH:mm:ss"));

        TStatus users = new TStatus(status, "usr", "Użytkownicy");

        for (Console cs : Console.allSockets)
            if (cs.httpSession != null)
                new TStatusItem(users, cs.getUserName()).value(cs.getUserName() + " ["
                        + cs.remoteAddr + "]");

        Statuses.add(status);

    }

}
