package rhino;

import logs.TLog;
import com.utils.collections.HashList;
import com.context.intf.OnContextInitialized;
import com.mlogger.Log;
import com.context.AppContext;
import com.*;
import com.json.JObject;
import console.Notifier;
import java.io.*;
import java.lang.Thread.State;
import java.nio.charset.*;
import java.util.*;
import java.util.logging.*;
import org.mozilla.javascript.*;
import pages.*;
import service.*;

public class Scripts implements Runnable {

    final static Set<Scripts> engines = new LinkedHashSet<>();

    private String script;
    private String base;
    private RhinoSandbox rhino;
    private final Object sync = new Object();
    private final HashList<TLog> queue = new HashList<>();
    private final Thread thread = new Thread(this);
    private final Callback callback = new Callback(this);
    private NativeObject api = null;
    public String userName = "milosz.ziernik";

    @OnContextInitialized
    public static void onContextInitialized() {
        //    engines.add(new Scripts());
    }

    public static void addLog(TLog log) {
        if (!AppContext.initialized || AppContext.terminated)
            return;

        if (Utils.isInStackTrace(Scripts.class.getName() + ".run")
                || log.errorStack.toString().contains(
                        Scripts.class.getName() + ".run"))
            return;

        for (Scripts en : engines)
            synchronized (en.sync) {
                en.queue.add(log);
                en.sync.notify();
                if (en.thread.getState() == State.NEW)
                    en.thread.start();
            }
    }

    public Scripts() {
        thread.setName("Rhino scripts");
        try {
            this.script = Resources.getString("rhino.js");
            this.base = Resources.getString("rhino_base.js");
        } catch (IOException ex) {
            Logger.getLogger(Scripts.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            rhino = new RhinoSandbox();
            rhino.evaluate("base", base);
            api = (NativeObject) rhino.get("api");
            rhino.put(api, "callback", callback);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.getLogger(Scripts.class.getName()).log(Level.SEVERE, null, e);
            return;
        }

        while (!thread.isInterrupted())
            try {
                TLog log = null;
                synchronized (sync) {
                    if (queue.isEmpty())
                        sync.wait();
                    log = queue.first();
                }

                if (log == null)
                    continue;

                synchronized (sync) {
                    queue.remove(log);
                }

                if (log instanceof Common.InternalLogObject)
                    continue;

                /*   CacheData cd = BaseContext.getResourceFile("rhino_base.js");
                 rhino.evaluate("base", new String(cd.getData(),
                 Charset.forName("UTF-8")));
                 cd.delete();
                 */
                try {
                    String js = Resources.getString("rhino.js");
                    rhino.evaluate("script", js);

                    long ts = System.nanoTime();

                    rhino.callMethod(api, "addLog", getJson(log));
                    // rhino.evaluate("add_log", "api.addLog({});");

                } catch (RhinoException e) {
                    Log.warning(e);
                    Notifier.error(userName, "Błąd w skrypcie:\n\n"
                            + e.details() + "\n\nLinia: " + e.lineNumber()
                            + ", kolumna: " + e.columnNumber()
                            + (e.lineSource() != null ? ":\n" + e.lineSource()
                                    : ""));
                }

            } catch (InterruptedException ex) {
                return;
            } catch (Exception e) {
                e.printStackTrace();
                Logger.getLogger(Scripts.class.getName()).log(Level.SEVERE, null, e);
            }

    }

    public String getJson(TLog log) {
        JObject json = new JObject();
        json.options.quotaNames(true)
                .singleLine(true)
                .acceptNulls(true);

        json.put("kind", log.kind.name());
        json.put("value", log.value);
        json.put("date", log.date.getTime());
        json.put("created", log.createTime.getTime());

        json.arrayC("addresses").addAll(log.addresses);

        return json.toString();
    }
}
