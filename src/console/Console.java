package console;

import logs.Statuses;
import logs.TLog;
import logs.Logs;
import com.utils.collections.HashList;
import com.utils.collections.Strings;
import com.utils.date.TimeDiff;
import com.utils.date.TDate;
import com.mlogger.LogKind;
import com.mlogger.LogAttr;
import com.*;
import com.json.JArray;
import com.json.JObject;
import com.utils.*;

import static com.Utils.*;

import com.servlet.websockets.IWebSocket;
import com.servlet.websockets.JsonWebSocket;
import com.servlet.websockets.WebSocketSession;
import java.io.*;
import java.lang.Thread.State;
import java.text.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.mlogger.statistics.TimeDiffStatistics;
import com.utils.date.*;
import pages.Common.InternalLogObject;
import service.*;
import logs.Statuses.TStatus;
import logs.Statuses.TStatusItem;
import service.Config.CConsole;

/**
 * Miłosz Ziernik 2013/07/03
 */
@IWebSocket(url = "consoleSocket")
public class Console extends JsonWebSocket implements Runnable {

    public final Thread thread = new Thread(this);

    public final Filters filters = new Filters(this);
    //----------------------------------------------------

    public final Set<String> headers = new HashSet<>();
    public final Set<String> marks = new HashSet<>();

    //---------------------
    //public final Set<TLog> clientLogs = new LinkedHashSet<>();  // wysłane logi
    //private final LinkedHashSet<TLog> allNotified = new LinkedHashSet<>();
    public final HashList<TLog> sourceLogs = new HashList<>(); // wszystkie logi z wybranych źródeł
    //public final HashList<TLog> processedLogs = new HashList<>();
    public final HashList<TStatus> clientStatuses = new HashList<>();

    public final HashList<TLog> clientLogs = new HashList<>(); // bieżące logi po odfiltrowaniu

    public int maxCount = 300;
    public long minLogId = 0;

    public final Notifier notifier = new Notifier(this);

    public final static ConcurrentLinkedQueue<Console> allSockets = new ConcurrentLinkedQueue<>();

    public Console(WebSocketSession session) {
        super(session);
        allSockets.add(this);
        thread.setName("Console notifier, " + connInfo);
        thread.setPriority(3);
    }

    static TimeDiffStatistics stsFilters = new TimeDiffStatistics("Logi", "Get Filters", null);

    @Override
    public void onClose() {
        thread.interrupt();
        allSockets.remove(this);
        super.onClose();
    }

    /**
     Synchronizacja list, ogólne sprżatanie
     */
    private void cleanup() {
        HashList<TLog> allLogs = Logs.all.getCopy();

        synchronized (sourceLogs) {
            HashList<TLog> toRemove = sourceLogs.getCopy();
            toRemove.removeAll(allLogs);
            sourceLogs.removeAll(toRemove);
        }

        synchronized (clientLogs) {
            HashList<TLog> toRemove = clientLogs.getCopy();
            toRemove.removeAll(allLogs);
            clientLogs.removeAll(toRemove);
        }

    }

    @Override
    public void onMessage(String action, JObject in) throws Exception {

        if (action.equals("getFilters")) {
            //  getFilters();
            return;
        }
        if (in.has("init"))
            init(in.objectF("init"));

        if (in.has("details"))
            getDetails(in.getLong("details", -1l));

        if (in.has("clearLogs"))
            minLogId = in.getLong("clearLogs", 0l);
    }

    public synchronized void init(JObject jsrc) throws Exception {

        if (httpSession == null || !httpSession.user.authorized)
            throw new Error("Brak autoryzacji");

        maxCount = jsrc.getInt("maxCount", 300);
        if (maxCount < 10)
            maxCount = 10;
        if (maxCount > 3000)
            maxCount = 3000;

        clientLogs.clear();
        sourceLogs.clear();
        headers.clear();
        marks.clear();

        filters.init(jsrc);

        headers.addAll(jsrc.arrayD("headers").getValuesStr());
        marks.addAll(jsrc.arrayD("marks").getValuesStr());

        JObject json = createJson();
        sendStatuses(json);

        filterAndSendLogs(json, Logs.all.getCopy());

        write(json);

        if (thread.getState() == State.NEW)
            thread.start();
    }

    @Override
    public void run() {

        long lastCleanupTime = System.currentTimeMillis();

        while (!thread.isInterrupted())
            try {
                JObject json = createJson();

                if (notifier.isEmpty())
                    synchronized (notifier) {
                        notifier.wait(1000);
                    }

                if (System.currentTimeMillis() - lastCleanupTime > 5000) {
                    lastCleanupTime = System.currentTimeMillis();
                    cleanup();
                }

                if (notifier.isEmpty())
                    continue;

                Notifier ntf = notifier.clone();

                if (ntf.statusesChanged)
                    sendStatuses(json);

                if (ntf.alert != null)
                    json.put("alertMsg", ntf.alert);

                if (ntf.error != null)
                    json.put("errorMsg", ntf.error);

                if (!ntf.removed.isEmpty()) {
                    JArray jrem = json.arrayC("toRemove");
                    for (TLog ll : ntf.removed)
                        jrem.add(ll.id);

                    clientLogs.removeAll(ntf.removed);
                    sourceLogs.removeAll(ntf.removed);
                }

                if (!ntf.added.isEmpty())
                    filterAndSendLogs(json, ntf.added);

                if (!json.isEmpty())
                    write(json);

                Thread.sleep(10); // odetchnij troche
            } catch (InterruptedException e) {
                return;
            } catch (Throwable e) {
                write(e);
            }
    }

    private void filterAndSendLogs(JObject json, final Collection<TLog> logs) throws IOException {

        // for (TLog l : logs)
        //     com.Console.printlnTs("ID: %d, Console -> filterAndSendLogs", l.id);
        LinkedHashSet<TLog> toSend = new LinkedHashSet<>();

        for (TLog log : logs)
            if (filters.filter(log))
                toSend.add(log);

        if (toSend.size() > maxCount) {
            TLog[] arr = new TLog[toSend.size()];
            toSend.toArray(arr);
            toSend.clear();
            TLog[] arr2 = Arrays.copyOfRange(arr, arr.length - maxCount, arr.length);
            toSend.addAll(Arrays.asList(arr2));
        }

        if (!notifier.added.isEmpty() || toSend.size() >= maxCount)
            json.put("hasMore", true);

        JArray jlogs = json.arrayC("logs");

        for (final TLog log : toSend) {

            //     com.Console.printlnTs("ID: %d, Console -> toSend", log.id);
            final JObject jl = jlogs.object();

            jl.put("id", log.id);
            jl.put("idFrmt", Utils.formatValue(log.id));
            jl.put(LogAttr.kind.key, log.kind.name());

            new Runnable() {

                private void add(LogAttr attr, Object value) {
                    if (attr.isIn(headers) || marks.isEmpty() || attr.isIn(marks))
                        jl.put(attr.key, value);
                }

                private void add(LogAttr attr, Set<String> values) {
                    if (attr.isIn(headers) || marks.isEmpty() || attr.isIn(marks))
                        if (values != null && !values.isEmpty())
                            jl.put(attr.key, new Strings(values).toString(", "));
                }

                @Override
                public void run() {
                    add(LogAttr.source, log.source);
                    add(LogAttr.threadId, log.threadId);
                    add(LogAttr.processId, log.processId);
                    add(LogAttr.address, log.addresses);
                    add(LogAttr.device, log.device);
                    add(LogAttr.version, log.version);
                    add(LogAttr.user, log.user);
                    add(LogAttr.tags, log.tags);
                    add(LogAttr.session, log.session);
                    add(LogAttr.request, log.request);
                    add(LogAttr.background, log.background);
                    add(LogAttr.comment, log.comment);
                    add(LogAttr.color, log.color);
                }
            }.run();

            if (log instanceof InternalLogObject)
                jl.put("internal", true);

            jl.put("_tme", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(log.date));

            if (LogAttr.date.isIn(headers)) {
                String format = new TDate(log.date).isSameDay(new Date())
                        ? "HH:mm:ss.SSS" : "MM-dd HH:mm:ss.SSS";
                if (!log.includeMilliseconds)
                    format = format.replace(".SSS", "");
                TimeDiff diff = new TimeDiff(log.createTime.getTime() - new TDate(log.date).getTime());
                Integer tol = CConsole.timeTolerance.value();

                jl.put(LogAttr.date.key, new SimpleDateFormat(format).format(log.date));
                jl.put("diff", diff.toString());
                if (tol != null && Math.abs(diff.time) > 1000 * tol)
                    jl.put("diffX", true);
            }

            //------------------- znaczniki -------------------------------
            if (!marks.isEmpty()) {
                JObject jmarks = jl.objectC("marks");

                for (LogAttr attr : LogAttr.values())
                    if (attr.isIn(marks))
                        jmarks.put(attr.key, log.marks.get(attr) != null
                                ? log.marks.get(attr) : "#000");

            }

            String val = log.value != null && log.value.value != null
                    ? log.value.value.toString() : "";
            if (val == null)
                val = "";
            val = val.replace("\r", "").replace("\n", " " + Char.returnKey + " ");

            jl.put(LogAttr.value.key, formatValue(val,
                    log.kind == LogKind.query ? 80 : 200));

        }

        filters.build(json);

        //------------------------------------------------------
    }

    private synchronized void getDetails(Long id) throws IOException {
        TLog log = null;

        for (TLog ll : sourceLogs)
            if (ll.id == id) {
                log = ll;
                break;
            }

        if (log == null)
            return;

        //  json.toString()
        Details det = new Details(log);
        det.options.quotaNames(true).singleLine(true);
        write(det);
    }

    public static String formatValue(String val, int len) {
        final String shy = Character.toString((char) 173); // SoftHypen

        val = val.trim().replace("\r\n", " ").replace("\n", " ").replace("\r", " ");

        if (val.length() > len)
            val = val.substring(0, len - 3).trim() + "[…]";

        //    val.length()
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);

            if (c == ' ')
                idx = 0;

            if (idx > 10)
                if ((c < '0' && c != ' ')
                        || (c > '9' && c < 'A')
                        || (c > 'Z' && c < 'a')
                        || (c > 'z' && c < '~')) {
                    sb.append(shy);
                    idx = 0;
                }

            sb.append(c);
            ++idx;
            if (idx > 30) {
                sb.append(shy);
                idx = 0;
            }
        }

        return sb.toString();
    }

    private void sendStatuses(JObject json) throws IOException {

        if (!CConsole.sendStatusses.value() || Statuses.all.isEmpty())
            return;

        JObject jstats = json.objectC("statuses");

        synchronized (Statuses.all) {
            for (TStatus status : Statuses.all.values()) {

                new Object() {

                    private void visit(TStatus status, JObject obj) {

                        obj.put("cap", status.caption);
                        obj.put("com", status.comment);

                        for (TStatus st : status.children.values())
                            visit(st, obj.objectC("g" + coalesce(st.key, "")));

                        for (TStatusItem item : status.items.values()) {
                            JObject jit = obj.objectC("i" + coalesce(item.key));

                            jit.put("left", item.left());
                            if (!coalesce(item.value, "").isEmpty())
                                jit.put("val", item.value);

                            if (!item.tags.isEmpty())
                                jit.put("tags", new Strings(item.tags).toString(", "));

                            if (!coalesce(item.comment, "").isEmpty())
                                jit.put("com", item.comment);
                            if (!coalesce(item.color, "").isEmpty())
                                jit.put("fcl", item.color);
                            if (!coalesce(item.color, "").isEmpty())
                                jit.put("bcl", item.background);
                            if (item.progress != null)
                                jit.put("prg", item.progress);
                        }

                    }
                }.visit(status, jstats.objectC(coalesce(status.key, "")));

            }
        }

    }

    public String getUserName() {
        return httpSession != null && httpSession.user != null
                ? httpSession.user.username : null;
    }

    private JObject createJson() {
        JObject json = new JObject();
        json.options.singleLine(true);
        json.options.quotaNames(true);
        return json;
    }

}
