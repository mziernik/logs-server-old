package logs;

import logs.Logs;
import com.context.intf.OnContextDestroyed;
import com.context.intf.OnContextInitialized;
import com.threads.*;
import console.*;
import java.util.*;
import com.mlogger.Log;
import logs.Statuses.TStatus;
import service.Config.CConsole;

public class CleanupThread extends TThread {

    private static CleanupThread instance;

    @OnContextInitialized
    public static void init() {
        if (instance != null)
            instance.interrupt();
        instance = new CleanupThread();
        instance.setPriority(Thread.MIN_PRIORITY);
        instance.start();
    }

    @OnContextDestroyed
    public static void ctxDestroy() {
        instance.interrupt();
    }

    public CleanupThread() {
        super("Logs cleanup");
    }

    @Override
    protected void execute() {

        while (isRunning())
            try {
                Thread.sleep(1000);

                Integer alive = CConsole.logAliveTime.value(72 * 60 * 60);
                long now = System.currentTimeMillis();

                boolean statusesChanged = false;
                synchronized (Statuses.all) {

                    Set<String> toRemove = new HashSet<>();

                    for (TStatus stat : Statuses.all.values()) {
                        statusesChanged |= stat.cleanup();
                        if (stat.children.isEmpty() && stat.items.isEmpty())
                            toRemove.add(stat.key);
                    }

                    for (String s : toRemove)
                        Statuses.all.remove(s);
                }

                if (statusesChanged)
                    Notifier.statusesChanged();

                Set<TLog> toRemove = new LinkedHashSet<>();

                synchronized (Logs.all) {
                    for (TLog log : Logs.all) {
                        int val = log.expireConsole != null ? log.expireConsole
                                : alive;
                        if (val > 0 && now - log.createTime.getTime() > val * 1000)
                            toRemove.add(log);
                    }
                }
                Logs.removeAll(toRemove);

                if (!toRemove.isEmpty())
                    Notifier.removeAll(toRemove);

            } catch (InterruptedException ex) {
                return;
            } catch (Exception e) {
                Log.warning(e);
            }

    }

}
