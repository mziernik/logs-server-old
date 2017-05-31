package console;

import com.CSV;
import com.Utils;
import com.html.core.HtmlBuilder;
import com.html.tags.*;
import com.json.*;
import com.utils.collections.Strings;
import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import com.mlogger.Log;
import com.mlogger.LogAttr;
import com.mlogger.LogElement;
import com.xml.XML;
import logs.TLog;

/**
 * Miłosz Ziernik 2014/06/07
 */
public class Details extends JObject {
/*
    public Details(int id) {
        super();

        try {

            ArchiveDB db = new ArchiveDB();

            QueryRow info = db.execute("SELECT \n"
                    + "  l.server_date,\n"
                    + "  l.client_date,\n"
                    + "  l.expire,\n"
                    + "  l.uid,\n"
                    + "  l.level,\n"
                    + "  v1.value as source,\n"
                    + "  v2.value as kind\n"
                    + "FROM logs.logs l\n"
                    + "LEFT JOIN logs.values v1 ON (v1.values_id = l.source)\n"
                    + "LEFT JOIN logs.values v2 ON (v2.values_id = l.kind)\n"
                    + "WHERE l.logs_id = " + id).first();

            if (info == null)
                throw new Exception("Nie znaleziono logu");

            final QueryRows rows = new ArchiveDB().execute(
                    "SELECT attr_type, values as values_ids, array_agg(value) as values FROM ( \n"
                    + "    SELECT * FROM (     \n"
                    + "        SELECT *, unnest(values) as val_id, generate_subscripts(values, 1) as rn FROM ( \n"
                    + "            SELECT attr_type, values, row_number() OVER () as idx \n"
                    + "            FROM logs.attributes \n"
                    + "            WHERE logs_id = %s \n"
                    + "            GROUP BY attr_type, values \n"
                    + "            ORDER BY attr_type,idx ASC \n"
                    + "        ) as __q \n"
                    + "        ORDER BY attr_type, idx ASC, rn ASC \n"
                    + "    ) as q \n"
                    + "    JOIN logs.values ON (values_id = val_id) \n"
                    + "    ORDER BY idx ASC, rn ASC \n"
                    + ") as q2 \n"
                    + "GROUP BY attr_type, values, idx \n"
                    + "ORDER BY idx ASC, attr_type", id);

            TLog log = new TLog(UUID.fromString(info.getStr("uid")), info.getDate("server_date"));
            log.id = id;
            log.date = info.getDate("client_date");
            log.expireDatabase = info.getInt("expire", null);
            log.createTime = info.getDate("server_date");
            log.source = info.getStr("source", null);
            log.kind = LogKind.valueOf(info.getStr("kind"));
            log.level = info.getInt("level", null);

            for (QueryRow row : rows) {
                LogAttr attr = LogAttr.get(row.getInt("attr_type"));
                if (attr == null)
                    continue;

                Strings sVals = row.getAsArray("values");
                if (sVals.isEmpty())
                    continue;
                List<String> values = sVals.getList();
                String value = sVals.first();

                switch (attr) {
                    case address:
                        log.addresses.addAll(values);
                        break;
                    case device:
                        log.device = value;
                        break;
                    case user:
                        log.user = value;
                        break;
                    case instance:
                        log.instance = value;
                        break;
                    case request:
                        log.request = value;
                        break;
                    case session:
                        log.session = value;
                        break;
                    case url:
                        log.urls.addAll(values);
                        break;
                    case tags:
                        log.tags.addAll(values);
                        break;
                    case processId:
                        log.processId = Utils.strLong(value, null);
                        break;
                    case threadId:
                        log.threadId = Utils.strLong(value, null);
                        break;
                    case threadName:
                        log.threadName = value;
                        break;
                    case threadPriority:
                        log.threadPriority = Utils.strInt(value, null);
                        break;
                    case levelName:
                        log.levelName = value;
                        break;
                    case comment:
                        log.comment = value;
                        break;
                    case clazz:
                        log.className = value;
                        break;
                    case background:
                        log.background = value;
                        break;
                    case color:
                        log.color = value;
                        break;
                    case errorStack:
                        log.errorStack.addAll(values);
                        break;
                    case callStack:
                        log.callStack.addAll(values);
                        break;
                    case flags:
                        log.flags.addAll(values);
                        break;
                    case logger:
                        log.loggerName = value;
                        break;
                    case method:
                        log.method = value;
                        break;
                    case version:
                        log.version = value;
                        break;
                    //---------------------------
                    case properties:
                        log.property(value, values.size() > 1 ? values.get(1)
                                : null);
                        break;
                    case attributes:
                        switch (values.size()) {
                            case 1:
                                log.attribute(values.get(0));
                                break;
                            case 2:
                                log.attribute(values.get(0), values.get(1));
                                break;
                            case 3:
                                log.attribute(values.get(0), values.get(1), values.get(2));
                                break;
                        }
                        break;
                    case data:
                        log.data.add(getDataObj(values));
                        break;
                    case value:
                        log.value = getDataObj(values);
                        break;
                    case details:
                        log.details = getDataObj(values);
                        break;

                }

                if (attr == LogAttr.attributes)
                    arrayC(attr.key).array().addAll(values);
                else
                    if (attr.multiple || attr == LogAttr.value)
                        arrayC(attr.key).addAll(values);
                    else
                        put(attr.key, value);

            }

        } catch (Throwable e) {
            Log.error(e);
            clear();
            put("exception", EError.toString(e));
        }
    }
*/
    private LogElement.DataObj getDataObj(List<String> values) {
        if (values == null || values.isEmpty())
            return null;

        String val = values.get(0);

        Integer strInt = Utils.strInt(val, 0);

        return null;

    }

    public Details(TLog log) {
        process(log);
    }

    private void process(TLog log) {

        JObject jlog = objectC("details");
        jlog.put("id", log.id);
        jlog.put("sdate", log.createTime.toString());
        jlog.put("cdate", log.date.toString());
        jlog.put("src", log.source);
        jlog.put("kind", log.kind.name);

        jlog.put("adr", new Strings(log.addresses).toString(", "));
        jlog.put("comm", log.comment);

        jlog.put("dev", log.device);
        jlog.put("devf", log.deviceFull);
        jlog.put("inst", log.instance);

        jlog.put("proc", log.processId);

        jlog.put("mth", new Strings(log.className, log.method).nonEmpty(true).toString("."));

        jlog.put("lvl", new Strings(log.level, log.levelName).nonEmpty(true).toString(", "));
        jlog.put("lgr", new Strings(log.loggerName, log.protocol).nonEmpty(true).toString(", "));

        jlog.put("col", log.color);
        jlog.put("back", log.background);

        jlog.put("req", log.request);
        jlog.put("ses", log.session);

        jlog.put("tag", new Strings(log.tags).toString(", "));
        jlog.put("thr", new Strings(log.threadId, priority(log), log.threadName).nonEmpty(true).toString(", "));
        jlog.put("uid", log.uid);
        jlog.arrayC("url").addAll(log.urls);
        jlog.put("user", log.user);
        jlog.put("ver", log.version);

        jlog.put("raw", log.rawData != null);

        JArray jdata = jlog.arrayC("data");

        List<LogElement.DataObj> data = new LinkedList<>();
        if (log.value != null && !log.value.isEmpty())
            data.add(new LogElement.DataObj(LogAttr.value.title, log.value.value, null));

        if (log.details != null && !log.details.isEmpty())
            data.add(new LogElement.DataObj(LogAttr.details.title, log.details.value, null));

        data.addAll(log.data);

        if (!log.errorStack.isEmpty())
            jlog.put("est", log.errorStack);

        if (!log.callStack.isEmpty())
            jlog.put("cst", log.callStack);

        for (LogElement.DataObj pair : data) {
            final JObject jobj = jdata.object();

            jobj.put("type", pair.type != null ? pair.type.name() : null);
            jobj.put("name", pair.name);
            jobj.put("value", pair.value);

            final String val = pair.value == null ? ""
                    : pair.value.toString().trim();

            try {
                checkJson(val, jobj);
            } catch (Exception e) {
            }

            try {
                checkXml(val, jobj);
            } catch (Exception e) {

            }
            try {
                checkCsv(val, jobj);
            } catch (Exception e) {

            }
            try {
                checkUrl(val, jobj);
            } catch (Exception e) {

            }
        }

        if (!log.attributes.isEmpty()) {
            JObject jobj = jlog.objectC(LogAttr.attributes.key);
            for (Map.Entry<String, LogElement.DataPairs> en : log.attributes.entrySet()) {
                JArray o = jobj.arrayC(Utils.coalesce(en.getKey(), ""));
                for (LogElement.DataPair pair : en.getValue())
                    o.array().add(pair.name).add(pair.value);
            }
        }

    }

    private void checkJson(String val, JObject jobj) {

        if (!val.contains("{") || !val.contains("}"))
            return;

        String sJson = val.substring(val.indexOf("{"), val.lastIndexOf("}") + 1);

        String pre = val.substring(0, val.indexOf("{"));
        String post = val.substring(val.lastIndexOf("}") + 1, val.length());

        JObject json = JSON.parse(sJson).asObject();

        HtmlBuilder html = new HtmlBuilder();
        Tag div = html.body.div();

        div.textToDivs(pre);
        div.textToDivs(json.toString());
        div.textToDivs(post);

        jobj.objectC("frmt").put("JSON", html.toString(div));

    }

    private String priority(Log log) {
        if (log == null || log.threadPriority == null)
            return null;

        if (log.loggerName != null && log.loggerName.toLowerCase().contains("mlogger"))
            switch (log.threadPriority) {
                case 1:
                    return "1 (lowest)";
                case 2:
                    return "2 (lowest)";
                case 3:
                    return "3 (lower)";
                case 4:
                    return "4 (lower)";
                case 5:
                    return "5 (normal)";
                case 6:
                    return "6 (higher)";
                case 7:
                    return "7 (higher)";
                case 8:
                    return "8 (highest)";
                case 9:
                    return "9 (highest)";
                case 10:
                    return "9 (time critical)";
            }

        return Integer.toString(log.threadPriority);
    }

    private void checkXml(String val, JObject jobj) throws Exception {
        if (!val.contains("<") || !val.contains(">"))
            return;

        XML xml = new XML(val.substring(val.indexOf("<"), val.lastIndexOf(">") + 1));

        String pre = val.substring(0, val.indexOf("<"));
        String post = val.substring(val.lastIndexOf(">") + 1, val.length());

        HtmlBuilder html = new HtmlBuilder();
        Tag div = html.body.div();

        div.textToDivs(pre);
        div.textToDivs(xml.toString());
        div.textToDivs(post);

        jobj.objectC("frmt").put("XML", html.toString(div));
    }

    private void checkCsv(final String val, final JObject jobj) throws IOException {

        new Object() {

            boolean check(char c) throws IOException {
                CSV.CSVReader csv = new CSV.CSVReader(new StringReader(val), c);
                List<String[]> all = csv.readAll();
                int len = all.size();
                if (len < 2)
                    return false;

                len = -1;
                for (String[] arr : all)
                    if (len == -1)
                        len = arr.length;
                    else
                        if (arr.length != len || len < 2)
                            return false;

                StringWriter sw = new StringWriter();
                CSV.CSVWriter wr = new CSV.CSVWriter(sw);
                wr.writeAll(all);
                wr.close();

                jobj.put("csv", sw.toString());
                return true;
            }

            public void run() throws IOException {
                if (check(','))
                    return;
                if (check(';'))
                    return;
                if (check('\t'))
                    return;
            }

        }.run();

    }

    private void checkUrl(String val, JObject jobj) throws Exception {
        if (!val.toLowerCase().contains("://"))
            return;

        int start = val.indexOf("://");
        int end = start + 3;

        for (; start >= 0; start--)
            if (val.charAt(start - 1) <= ' ')
                break;

        for (; end < val.length(); end++)
            if (val.charAt(end) <= ' ')
                break;

        if (end - start <= 3)
            return;

        String s1 = val.substring(start, end);
        String s2 = URLDecoder.decode(s1, "UTF-8");
        if (s1.equals(s2))
            return;

        HtmlBuilder html = new HtmlBuilder();

        Tag div = html.body.div();

        div.textToDivs(val.substring(0, start));
        A a = div.a();
        a.href(s1);
        a.textToDivs(s2);
        div.textToDivs(val.substring(end, val.length()));

        jobj.objectC("frmt").put("URI", html.toString(div));

    }

}
