package console;


import com.json.JArray;
import com.json.JObject;
import com.utils.collections.HashList;
import com.utils.collections.Pair;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import logs.Logs;
import logs.TLog;

public class Filters {

    private final Console console;

    public final LGroup sources = new LGroup();
    public final LGroup kinds = new LGroup();
    public final LGroup addresses = new LGroup();
    public final LGroup tags = new LGroup();
    public final LGroup devices = new LGroup();
    public final LGroup versions = new LGroup();
    public final LGroup users = new LGroup();
    public final LGroup[] allGroups = {sources, kinds, addresses, tags, devices, versions, users};

    public long maxId;
    public long maxDate;

    public Filters(Console console) {
        this.console = console;
    }

    void init(JObject jsrc) {
        sources.init(jsrc.arrayD("sources").getValuesStr());
        kinds.init(jsrc.arrayD("kinds").getValuesStr());
        addresses.init(jsrc.arrayD("addresses").getValuesStr());
        tags.init(jsrc.arrayD("tags").getValuesStr());
        devices.init(jsrc.arrayD("devices").getValuesStr());
        versions.init(jsrc.arrayD("versions").getValuesStr());
        users.init(jsrc.arrayD("users").getValuesStr());

        maxId = 0;
        try {
            maxId = Long.valueOf(jsrc.getStr("maxId", "0"));
        } catch (Throwable e) {
        }

        maxDate = 0;

        String sDate = jsrc.getStr("maxDate", "");
        if (!sDate.isEmpty()) {
            try {
                maxDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(sDate).getTime();
            } catch (Throwable ex) {
            }
        }

    }

    void build(JObject json) throws IOException {
        long time = System.nanoTime();

        HashList<TLog> ll = console.sourceLogs.getCopy();

        for (LGroup gr : allGroups)
            gr.counter.clear();

        HashList<TLog> all = Logs.all.getCopy();

        for (TLog log : all)
            sources.addFilter(log.source, "");

        for (TLog log : ll) {
            String addr = "";
            if (log.user != null && !log.user.isEmpty())
                synchronized (Logs.dns) {
                    for (Map.Entry<String, String> ee : Logs.dns.entrySet()) {
                        String s = ee.getValue();
                        if (log.user.equals(s)) {
                            addr = ee.getKey();
                            break;
                        }
                    }
                }

            if (addr != null && !addr.isEmpty())
                addr = "<" + addr + ">";

            if (log.tags.isEmpty())
                tags.addFilter("", "");
            else
                for (String s : log.tags)
                    tags.addFilter(s, "");

            kinds.addFilter(log.kind.name(), "");
            devices.addFilter(log.device, log.deviceFull);
            versions.addFilter(log.version, "");
            for (String s : log.addresses) {
                String sdns = null;
                synchronized (Logs.dns) {
                    sdns = Logs.dns.get(s);
                }
                addresses.addFilter(s, sdns != null && !sdns.isEmpty()
                        ? "< " + sdns + " >" : "");
            }
            users.addFilter(log.user, addr);
        }

        JObject jfilters = json.objectC("filters");
        sources.buildFilter(jfilters.arrayC("sources"));
        kinds.buildFilter(jfilters.arrayC("kinds"));
        tags.buildFilter(jfilters.arrayC("tags"));
        addresses.buildFilter(jfilters.arrayC("addresses"));
        devices.buildFilter(jfilters.arrayC("devices"));
        versions.buildFilter(jfilters.arrayC("versions"));
        users.buildFilter(jfilters.arrayC("users"));

        Console.stsFilters.addNano(System.nanoTime() - time);
    }

    /**
     * Filtruje i dodaje log do listy
     */
    boolean filter(final TLog log) throws IOException {

        if (sources.filter(log.source))
            console.sourceLogs.add(log);
        else
            return false;

        if (maxId > 0 && log.id > maxId)
            return false;

        if (maxDate > 0 && log.date.getTime() - maxDate > 0)
            return false;

        if (log.id < console.minLogId
                || !kinds.filter(log.kind.name())
                || !addresses.filter(log.addresses)
                || !devices.filter(log.device)
                || !users.filter(log.user)
                || !versions.filter(log.version)
                || !tags.filter(log.tags))
            return false;
        console.clientLogs.add(log);
        return true;
    }

    public class LGroup {

        public final Set<String> filter = new HashSet<>();
        final Map<String, Pair<String, Integer>> counter = new TreeMap<>();
        public int count;

        public void init(Collection<String> values) {
            filter.clear();
            filter.addAll(values);
            counter.clear();
            count = 0;
        }

        @Override
        public String toString() {
            return filter.toString();
        }

        public boolean filter(final String value) {
            return filter.isEmpty() || filter.contains(value == null ? "" : value.trim());
        }

        public boolean filter(final Set<String> values) {
            if (filter.isEmpty())
                return true;

            for (String v : values)
                if (filter.contains(v))
                    return true;

            if (values.isEmpty() && filter.contains(""))
                return true;

            return false;
        }

        public void addFilter(String name, String title) {
            if (name == null)
                name = "";
            name = name.trim();
            Pair<String, Integer> pair = counter.get(name);
            if (pair == null)
                pair = new Pair<>(title, 0);
            pair.second++;
            counter.put(name, pair);
        }

        public void buildFilter(JArray arr) {
            if (this == sources && Logs.all.isEmpty())
                return;
            else if (this != sources && console.sourceLogs.isEmpty())
                return;

            Map<String, Pair<String, Integer>> map = new TreeMap<>();
            for (String f : filter)
                map.put(f, new Pair<>("", -1));

            map.putAll(counter);

            for (String s : filter)
                if (counter.get(s) == null)
                    map.put(s, new Pair<>("", -1));

            for (Map.Entry<String, Pair<String, Integer>> ee : map.entrySet()) {
                String f = ee.getKey();
                Pair<String, Integer> pair = ee.getValue();
                if (pair.second > 0)
                    arr.array()
                            .add(f)
                            .add(pair.second)
                            .add(filter.isEmpty() || filter.contains(f))
                            .add(pair.first);
            }

        }
    }
}
