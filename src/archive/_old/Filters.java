package archive._old;

import archive._old.Filters.Filter;
import com.Utils;
import com.database.Database;
import com.html.tags.InputType;
import com.html.tags.Select;
import com.html.tags.Table;
import com.html.tags.Tr;
import com.json.JArray;
import com.json.JCollection;
import com.json.JElement;
import com.json.JObject;
import com.json.JSON;
import com.json.JValue;
import com.mlogger.Log;
import com.mlogger.*;

import static com.mlogger.LogAttr.*;

import com.servlet.handlers.Page;
import com.utils.collections.Strings;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;

public class Filters extends LinkedList<Filter> {

    class FifoClass {

        String value;
        int index;
        int token;
    };

    public final ArrayList<Integer> ids = new ArrayList<>();
    public final Strings display = new Strings();
    private String currentQuery;
    private ArrayList<FifoClass> fifo;

    public Table parse(Page page, String sJson) throws SQLException {
        if (sJson == null || sJson.trim().isEmpty())
            sJson = "[]";

        JElement json = JSON.parse(sJson);
        if (json == null)
            return null;

        clear();

        display.clear();

        for (JArray arr : ((JCollection) json).getArrays()) {
            Filter f = new Filter(arr);
            if (f.key != null && f.operator != null && f.value != null)
                add(f);
        }

        Strings joins = new Strings().allowNulls(false);
        Strings wheres = new Strings().allowNulls(false);
        //   Strings disp = new Strings().prefix("(").surfix(")");
        int idx = 0;

        boolean hasDateLimit = false;

        for (Filter f : this) {
            if (f.enabled) {
                hasDateLimit |= Utils.isIn(f.key, "dte", "db_date")
                        && Utils.isIn(f.operator, "equals", "moreThan", "lessThan");
            }
        }
/*
        if (!hasDateLimit && CArchive.defaultTimeRange.value() > 0) {
            JArray arr = new JArray();
            arr.add(true);
            arr.add("db_date")
                    .add("moreThan")
                    .add(new TDate()
                            .addDays(-CArchive.defaultTimeRange.value())
                            .toString("yyyy-MM-dd"));

            add(arr);
        }
*/
        int[] tabsort = new int[size()];
        for (int i = 0; i < size(); i++) {
            tabsort[i] = i;
        }

        //sort list by keys and operators for init
        for (int i = 1; i < size(); i++) {
            int j = i;
            while (j > 0) {
                if ((get(tabsort[j]).key.compareTo(get(tabsort[j - 1]).key) < 0)) {
                    int p = tabsort[j];
                    tabsort[j] = tabsort[j - 1];
                    tabsort[j - 1] = p;
                }
                if ((get(tabsort[j]).key.compareTo(get(tabsort[j - 1]).key) == 0)
                        && (getOperatorWeight(get(tabsort[j]).operator) < getOperatorWeight(get(tabsort[j - 1]).operator))) {
                    int p = tabsort[j];
                    tabsort[j] = tabsort[j - 1];
                    tabsort[j - 1] = p;
                }
                j--;
            }
        }

        //enum keys        
        String idslist = "";

        Strings numkeys = new Strings();
        for (Filter f : this) {
            if (f.enabled) {
                if (!numkeys.contains(f.key)) {
                    numkeys.add(f.key);
                }
            }
        }

        for (LogAttr attr : LogAttr.values()) {
            if (numkeys.contains(attr.key)) {
                if (("dte".equals(attr.key)) || ("db_date".equals(attr.key)) || ("lvl".equals(attr.key))) {
                    continue;
                }

                if (!idslist.equals("")) {
                    idslist += ", ";
                }
                idslist += String.valueOf(attr.getId());
            }
        }

        String preselect
                = "SELECT l.logs_id FROM logs.logs l\n";

        if (!idslist.equals(""))
            preselect
                    += "JOIN logs.attributes lattr ON lattr.logs_id = l.logs_id AND lattr.attr_type = ANY(ARRAY[" + idslist + "])\n"
                    + "JOIN logs.values lvals on lvals.values_id = ANY(lattr.values)\n";

        preselect += "WHERE \n";

        //build 'where' clauses and set display label                
        ArrayList<Filter> condlist = new ArrayList<>();
        Filter actfilter = null;
        String condstr = "", label = "";
        int countexp = 0;
        for (int fromidx = -1, count = 0; count <= size() && size() > 0;) {
            if (actfilter == null) {
                actfilter = get(tabsort[count]);
                fromidx = count;
                count++;
            }
            boolean w2 = count == size();
            boolean w1 = false;
            if (!w2)
                w1 = !actfilter.key.equals(get(tabsort[count]).key);

            if (w1 || w2) {
                int toidx = count - 1;
                condlist.clear();
                for (int j = fromidx; j <= toidx; j++) {
                    if (get(tabsort[j]).enabled)
                        condlist.add(get(tabsort[j]));
                }

                if (!condlist.isEmpty()) {
                    countexp++;

                    //get conditions in brackets
                    String tmp1 = getConditionsWhereClauses(false, "AND", "OR", condlist);
                    wheres.add(tmp1);

                    boolean braces = !(tmp1.startsWith("(") && tmp1.endsWith(")"));

                    if (countexp == 2) {
                        condstr = "(" + condstr + ")";
                    }

                    if (braces && countexp > 1) {
                        tmp1 = "(" + tmp1 + ")";
                    }

                    if (countexp > 1) {
                        tmp1 = " AND " + tmp1;
                    }

                    //get conditions in brackets                
                    String tmp2 = getConditionsWhereClauses(true, "ORAZ", "LUB", condlist);
                    braces = !(tmp2.startsWith("(") && tmp2.endsWith(")"));

                    if (countexp == 2) {
                        label = "(" + label + ")";
                    }

                    if (braces && countexp > 1) {
                        tmp2 = "(" + tmp2 + ")";
                    }

                    if (countexp > 1) {
                        tmp2 = " ORAZ " + tmp2;
                    }
                    condstr += tmp1;
                    label += tmp2;
                }
                fromidx = count;
                if (count < size())
                    actfilter = get(tabsort[count]);
            }
            count++;
        }

        preselect += condstr;

        display.add(label);
/*
        currentQuery = "SELECT logs_id FROM logs.logs\n"
                + "WHERE logs_id IN ("
                + preselect + "\n"
                + "LIMIT " + CArchive.maxResults.value(10000) + "\n"
                + ")"
                + "ORDER BY server_date DESC;";
*/
        if (page == null) {
            return null;
        }

        Table tbl = page.body.table();

        try {
            for (Filters.Filter f : this) {
                Filters.Filter.buildRow(tbl.tbodyTr(), f);
                tbl.tbody.attr("has_rows", "true");
            }

            Filters.Filter.buildRow(tbl.tbodyTr(), null);

        } catch (Exception e) {
            Log.error(e);
        }

        return tbl;
    }

    public void apply() throws SQLException, InterruptedException {
        if (currentQuery == null)
            return;

    }

    public Filter add(JArray item) {
        Filter filter = new Filter(item);
        add(filter);
        return filter;
    }

    public static class Filter {

        boolean enabled = true;
        String key;
        String operator;
        String value;
        String sql_replacement;
        String display_name;

        private Filter(JArray item) {

            LinkedList<JValue> values = item.getValues();

            if (values.size() < 3)
                return;

            JValue first = values.getFirst();
            if (first.isBoolean()) {
                enabled = first.asBoolean();
                values.poll();
            }

            if (values.size() < 3)
                return;

            key = values.poll().asString();
            operator = values.poll().asString();
            value = values.poll().asString();
        }

        public static void buildRow(Tr tr, final Filter filter) throws Exception {

            tr.td().input(InputType.checkbox).checked(filter != null
                    ? filter.enabled : true);

            final Select sel = tr.td().select();
            sel.option("", "");

            final String key = filter != null ? filter.key : "";

            JObject obj = getActions(key);

            new Runnable() {

                private void add(LogAttr attr) {
                    sel.option(attr.title, attr.key).selected(attr.key.equals(key));
                }

                @Override
                public void run() {
                    add(LogAttr.source);
                    add(LogAttr.date);
                    sel.option("Data serwera", "db_date").selected("db_date".equals(key));
                    add(LogAttr.kind);
                    add(LogAttr.level);
                    add(LogAttr.tags);
                    add(LogAttr.address);
                    add(LogAttr.device);
                    add(LogAttr.user);
                    add(LogAttr.version);
                    add(LogAttr.url);
                    add(LogAttr.flags);
                    add(LogAttr.attributes);
                    add(LogAttr.value);
                    add(LogAttr.details);
                    add(LogAttr.data);
                    add(LogAttr.instance);
                    add(LogAttr.session);
                    add(LogAttr.request);
                    add(LogAttr.callStack);
                    add(LogAttr.errorStack);
                    add(LogAttr.comment);
                    add(LogAttr.logger);
                    add(LogAttr.processId);
                    add(LogAttr.threadId);
                    add(LogAttr.threadName);
                }
            }.run();

            Select select = tr.td().select();

            if (key != null && !key.isEmpty())
                for (JValue jv : obj.getValues()) {
                    select.option(jv.asString(), jv.getName())
                            .selected(filter != null
                                            ? jv.getName().equals(filter.operator)
                                            : false);
                }

            tr.td().input(InputType.text).value(filter != null ? filter.value
                    : null);

            tr.td().input(InputType.button).value("x");
        }

    }

    private String getExpr(Filter filter, String castvar, boolean display, boolean first) {
        if (filter == null) {
            return null;
        }

        String where = "";

        switch (filter.key) {
            case "dte":
                if (display)
                    where = "Data klienta";
                else
                    where = "l.client_date";
                break;

            case "db_date":
                if (display)
                    where = "Data serwera";
                else
                    where = "l.server_date";
                break;

            case "knd":
                if (display) {
                    where = LogAttr.kind.title;
                }
                else {
                    if (first) {
                        where = "l.kind = lvals.values_id AND ";
                    }
                    where += "lvals.value";
                }
                break;

            case "src":
                if (display) {
                    where = LogAttr.source.title;
                }
                else {
                    if (first) {
                        where = "l.source = lvals.values_id AND ";
                    }
                    where += "lvals.value";
                }
                break;
            case "lvl":
                if (display)
                    where = LogAttr.level.title;
                else
                    where = "l.level";
                break;

            default:
                for (LogAttr attr : LogAttr.values()) {
                    if (attr.key.equals(filter.key)) {

                        if (first) {
                            where = "lattr.attr_type = " + attr.getId() + " AND ";
                        }
                        if (display)
                            where = attr.title;
                        else
                            where += "lvals.value";
                    }
                }
        }

        if (where.equals("")) {
            return "";
        }

        String cast = castvar;

        if (cast == null) {
            cast = "";
        }

        if (!cast.isEmpty()) {
            cast = "::" + cast;
        }

        String value = filter.value;

        value = (cast.isEmpty() ? "'" : "") + Database.escapeSQL(value) + (cast.isEmpty()
                ? "'" : "");
        switch (filter.operator) {
            case "equals":
                return where + cast + " = " + value;

            case "diff":
                return where + cast + " <> " + value;

            case "contains":
                if (display) {
                    return where + cast + " ZAWIERA '" + value.substring(1, value.length() - 1) + "'";
                }
                else
                    return where + cast + " ILIKE '%" + value.substring(1, value.length() - 1) + "%'";

            case "exclude":
                if (display) {
                    return where + cast + " NIE ZAWIERA '" + value.substring(1, value.length() - 1) + "'";
                }
                else
                    return where + cast + " NOT ILIKE '%" + value.substring(1, value.length() - 1) + "%'";

            case "moreThan":
                return where + cast + " > " + value;

            case "lessThan":
                return where + cast + " < " + value;
        }

        return null;
    }

    public static JObject getActions(String key) throws Exception {
        JObject obj = new JObject();

        obj.put("equals", "Równy");
        obj.put("diff", "Różny od");

        LogAttr attr = null;
        for (LogAttr la : LogAttr.values())
            if (la.key.equals(key)) {
                attr = la;
                break;
            }

        if (Utils.isIn(attr, source, device, address, user, tags, value,
                details, instance, session, request, url, version)) {
            obj.put("contains", "Zawiera");
            obj.put("exclude", "Nie zawiera");
        }

        if (key.equals("db_date") || Utils.isIn(attr, date, level, threadId,
                processId, threadPriority)) {
            obj.put("moreThan", "Wiekszy niż");
            obj.put("lessThan", "Mniejszy niż");
        }
        return obj;
    }

    private String getConditionsWhereClauses(boolean display, String and_word, String or_word, ArrayList<Filter> filtersgroup) {
        int cnt;
        int count = 0;
        int idxfrom = 0, idxto = 0, idxstart = 0;
        int minval = Integer.MAX_VALUE;
        int maxval = Integer.MIN_VALUE;

        boolean bminval = false;
        boolean bmaxval = false;
        boolean w2 = false, w3 = false;
        boolean dodps = false;
        boolean global_brackets0 = false;
        boolean global_brackets1 = false;
        boolean local_brackets = false;

        String operator;
        String tmp2, tmp3;
        String global_operator = or_word;

        FifoClass fc;
        fifo = new ArrayList<>();

        while (true) {
            if (idxfrom == filtersgroup.size())
                break;
            Filter f0 = null;
            Filter f1 = filtersgroup.get(idxfrom);
            Filter f2 = filtersgroup.get(idxto);
            Filter f3 = null;

            String tmp;
            int cmp;

            idxstart = idxto;
            while (f1.operator.equals(f2.operator)) {
                idxto++;
                if (idxto == filtersgroup.size()) {
                    f3 = null;
                    break;
                }
                f2 = filtersgroup.get(idxto);
                f3 = f2;
            }

            idxto--;
            if (f1.operator.equals("moreThan") || f1.operator.equals("lessThan")) {
                w2 = true;
                boolean w1 = !(f2.operator.equals("moreThan") || f2.operator.equals("lessThan")) && (idxfrom == idxto);
                if (idxto - idxstart >= 0) {
                    tmp = getFieldsToString(filtersgroup, idxfrom, idxto, false, display, and_word, or_word);
                    for (int i = idxfrom; i <= idxto; i++) {
                        f2 = filtersgroup.get(i);
                        if (f1.operator.equals("moreThan")) {
                            cmp = getIntegerValue(f2.value);
                            minval = Math.min(minval, cmp);
                            bminval = true;
                        }
                        if (f1.operator.equals("lessThan")) {
                            cmp = getIntegerValue(f2.value);
                            maxval = Math.max(maxval, cmp);
                            bmaxval = true;
                        }
                    }

                    cnt = 0;

                    if (!tmp.equals(""))
                        queueAdd(tmp, idxto - idxstart + 1, 2);

                    if (bmaxval && bminval || w1 || f3 == null) {
                        cnt = 0;
                        tmp = "";
                        boolean w4 = maxval > minval;
                        boolean wor = false;

                        if (w4) {
                            while ((fc = queueGet(2)) != null) {
                                if (!tmp.equals("")) {
                                    tmp += " " + and_word + " ";
                                }
                                tmp += fc.value;
                                cnt += fc.index;
                            }
                        }
                        else {
                            while ((fc = queueGet(2)) != null) {
                                if (!tmp.equals("")) {
                                    tmp += " " + or_word + " ";
                                    wor = true;
                                }
                                tmp += fc.value;
                                cnt += fc.index;
                            }
                        }
                        bmaxval = false;
                        bminval = false;
                        maxval = Integer.MIN_VALUE;
                        minval = Integer.MAX_VALUE;

                        tmp2 = "";
                        operator = "";
                        while ((fc = queueGet(0)) != null) {
                            tmp3 = fc.value;
                            tmp2 += operator;
                            operator = "";
                            if (tmp3.equals(" " + or_word + " ") || tmp3.equals(" " + and_word + " ")) {
                                operator = tmp3;
                            }
                            else
                                tmp2 += tmp3;
                        }

                        if (!tmp2.equals("")) {
                            if (w4) {
                                if (global_operator.equals(and_word)) {
                                    global_brackets0 = true;
                                }
                                operator = " " + or_word + " ";
                            }
                            else {
                                operator = " " + and_word + " ";
                            }
                            tmp2 = tmp2 + " " + operator + " ";
                        }

                        if (!w4 && cnt > 1 && wor) {
                            if (global_operator.equals(and_word)) {
                                global_brackets0 = true;
                            }
                            tmp2 += "(" + tmp + ")";
                        }
                        else
                            tmp2 += tmp;

                        queueAdd(tmp2, cnt, 0);
                    }
                    idxto++;
                    idxfrom = idxto;
                    continue;
                }
            }
            else {
                if (w2) {
                    cnt = 0;
                    tmp = "";
                    fc = queueGet(0);
                    if (fc != null) {
                        tmp += fc.value;
                        cnt += fc.index;
                    }

                    tmp2 = "";
                    while ((fc = queueGet(2)) != null) {
                        if (!tmp.equals(""))
                            tmp2 = " " + and_word + " ";
                        tmp2 += fc.value + " ";
                        cnt += fc.index;
                        tmp += tmp2;
                    }
                    queueAdd(tmp, cnt, 0); // <> are prepared 
                    queueAdd(" " + global_operator + " ", 3, 0);
                }

                //patrzymy na poprzedni i kolejny operator jesli nie ma AND, to nie uzywamy nawiasow
                boolean w1 = false;
                if (f0 != null) {
                    if (getOperatorLink(f0, f1, and_word, or_word) == and_word) {
                        w1 = true;
                    }
                    if (!f1.operator.equals(f2.operator)) {
                        if (getOperatorLink(f1, f2, and_word, or_word) == and_word) {
                            w1 = true;
                        }
                    }
                }

                boolean braces = false;
                operator = getOperatorLink(f1, f1, and_word, or_word);
                local_brackets |= operator.equals(or_word);

                if (operator.equals(or_word)) {
                    braces = true;
                }

                braces &= w1;

                String ps = getFieldsToString(filtersgroup, idxfrom, idxto, braces, display, and_word, or_word);

                operator = "";
                if (!f1.operator.equals(f2.operator)) {
                    operator = getOperatorLink(f1, f2, and_word, or_word);
                    if (operator.equals(or_word)) {
                        global_brackets1 = true;
                    }

                    ps = ps + " " + operator + " ";
                }

                tmp = "";
                fc = queueGet(1);
                if (fc != null) {
                    tmp += fc.value;
                }

                tmp += ps;
                queueAdd(tmp, 0, 1);

                idxto++;
                idxfrom = idxto;
                w2 = false;
                w3 = true;
            }
            if (f3 == null)
                break;
            f0 = f1;
        }

        String tmp1 = "";

        while ((fc = queueGet(0)) != null) {
            tmp1 += fc.value;
        }

        while ((fc = queueGet(3)) != null) {
            tmp1 += fc.value;
        }

        tmp2 = "";
        while ((fc = queueGet(1)) != null) {
            tmp2 += fc.value;
        }

        if ((global_brackets0 && global_operator.equals(and_word)) && (global_brackets1 || local_brackets)) {
            tmp2 = "(" + tmp2 + ")";
        }
        return tmp1 + tmp2;
    }

    private void queueAdd(String s, int key, Integer token) {
        FifoClass fc = new FifoClass();
        fc.index = key;
        fc.value = s;
        fc.token = token;
        fifo.add(fc);
    }

    private FifoClass queueGet(Integer token) {
        if (fifo.isEmpty())
            return null;

        FifoClass result = null;
        for (FifoClass s : fifo) {
            if (token == null) {
                result = s;
                break;
            }
            else {
                if (s.token == token) {
                    result = s;
                    break;
                }
            }

        }
        if (result != null)
            fifo.remove(result);
        return result;
    }

    private int getIntegerValue(String value) {
        return Integer.parseInt(value.replace("-", "").replace(".", "").replace(",", ""));
    }

    private String getFieldsToString(ArrayList<Filter> filters, int from, int to, boolean braces, boolean display, String and_word, String or_word) {
        Filter f, flast = null;
        String result = "";
        String tmp = "";
        String operator;
        int cnt = 0;
        int tfrom = from;

        braces &= (from < to);
        for (; from <= to; from++) {
            f = filters.get(from);

            if (flast != null) {
                operator = getOperatorLink(flast, f, and_word, or_word);

                if (!or_word.equals(operator)) {
                    if (cnt > 0) {
                        if (braces) {
                            result += "(" + tmp + ")";
                        }
                    }
                    cnt = 0;
                }
                else {
                    cnt++;
                }
                tmp += " " + operator + " ";
            }

            tmp += " " + getExpr(f, "", display, from == tfrom) + " ";
            flast = f;
        }
        if (cnt > 0 && braces) {
            tmp = "(" + tmp + ")";
        }
        result = tmp;
        return result;
    }

    private int getOperatorWeight(String operator) {
        switch (operator) {
            case "lessThan":
                return 1;

            case "moreThan":
                return 1;

            case "equals":
                return 2;

            case "contains":
                return 3;

            case "diff":
                return 4;

            case "exclude":
                return 5;
        }
        return 7;
    }

    private String getOperatorLink(Filter f1, Filter f2, String and_word, String or_word) {
        if (f1.operator.equals("lessThan") && f2.operator.equals("lessThan")) {
            return and_word;
        }

        if (f1.operator.equals("moreThan") && f2.operator.equals("moreThan")) {
            return and_word;
        }

        if (f1.operator.equals("equals") && f2.operator.equals("equals")) {
            return or_word;
        }

        if (f1.operator.equals("exclude") && f2.operator.equals("exclude")) {
            return and_word;
        }

        if (f1.operator.equals("diff") && f2.operator.equals("diff")) {
            return and_word;
        }

        if (f1.operator.equals("contains") && f2.operator.equals("contains")) {
            return and_word;
        }

        return or_word;
    }
;
}
