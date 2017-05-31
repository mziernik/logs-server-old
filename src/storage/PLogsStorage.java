package storage;

import archive._old.Filters;
import com.Utils;
import com.html.core.Node;
import com.html.js.Ajax;
import com.html.js.Call;
import com.html.tags.*;
import com.json.*;
import com.mlogger.LogAttr;
import com.resources.ResFile;
import com.servlet.interfaces.Endpoint;
import com.utils.Char;
import com.utils.collections.Strings;
import com.utils.date.TDate;
import com.utils.date.TimeDiff;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import logs.Logs;
import logs.TLog;
import pages.Common;
import service.Config;
import service.handlers.SPage;
import storage.common.LogsVisitor;

import static com.resources.ResFile.*;
import static console.Console.formatValue;

@Endpoint(url = "stor")
public class PLogsStorage extends SPage {

    @Override
    protected void onRequest() throws Exception {

        link(utilsJs, serviceJs, serviceCss, layerJs, layerCss,
                jQuery, popupJs, popupCss);

        link(ResFile.service, ResFile.jQuery);
        head.link("/common.js",
                "/styles.css",
                "/console.css",
                "/skin.css",
                "/pages/archive.js",
                "/pages/archive.css",
                "/pages/archive-filters.js");

        link(ResFile.service, ResFile.layer);

        Node dMenu = Common.buildMenu(this);
        dMenu.style().boxShadow("2px 2px 2px #333")
                .border("1px solid #234");

        body.button("Flush").onClick(new Ajax("?flush"));

        Tag div = body.div().id("filters");
        div.table().id("tblFilters");
        div.div().id("preFilterApply")
                .input(InputType.button)
                .value("Zastosuj")
                .id("btnFilterApply")
                .onClick(new Call("filters.apply"));

        Tag main = body.div()
                .id("main")
                .cls("s-main");

        Table tbl = main.table();
        tbl.id("console");

        for (LogsFile logs : Utils.asList(LogsStorage.all)) {
            body.hr();
            tbl = body.table();
            tbl.tbodyTr().setCells("Plik:", logs.file.getName());
            tbl.tbodyTr().setCells("Rozmiar pliku:", Utils.formatSize(logs.file.length()));
            tbl.tbodyTr().setCells("Rozmiar wartości:", Utils.formatSize(logs.header.totalRawSize));
            tbl.tbodyTr().setCells("Logów:", Utils.formatValue(logs.header.logsCount));
            tbl.tbodyTr().setCells("Atrybuty:", Utils.formatValue(logs.header.attributesCount));
            tbl.tbodyTr().setCells("Daty:", new TDate(logs.header.minDate).toString(false)
                    + " - " + new TDate(logs.header.maxDate).toString(false));
            tbl.tbodyTr().setCells("Źródło:", logs.header.sources);
            tbl.tbodyTr().setCells("Tag:", logs.header.tags);
            tbl.tbodyTr().setCells("Typ:", logs.header.kinds);
            tbl.tbodyTr().setCells("Adres:", logs.header.addresses);
            tbl.tbodyTr().setCells("Urządzenie:", logs.header.devices);
            tbl.tbodyTr().setCells("Użytkownik:", logs.header.users);
        }

    }

    @Endpoint
    public void flush() throws IOException {
        LogsStorage.flush();
    }

    @Endpoint
    public void getFilterActions() throws Exception {
        returnCustom(Filters.getActions(params.getStr("key")).toString(), "application/json");
    }

    @Endpoint
    public void applyFilters() throws Exception {
        Table tbl = session.filters.parse(this, params.getStr("filters"));
        session.filters.apply();
        if (tbl != null)
            returnInnerHTML(tbl);
    }

    @Endpoint
    public void getNewFilterRow() throws Exception {
        Table tbl = body.table();

        String data = params.getStr("data", "");
        if (!data.isEmpty()) {
            Filters filters = new Filters();
            filters.parse(this, data);
            return;
        }

        Filters.Filter.buildRow(tbl.tbodyTr(), null);
        returnHTML(tbl.tbody);
    }

    @Endpoint
    public void getLogs() throws Exception {

        final Integer maxId = params.getInt("maxId", null);
        final Long maxDate = params.getLong("maxDate", null);

        final JObject json = new JObject();
        json.options.compactMode(true);

        final int maxLogs = CLogsStorage.pageLogsLimit.value(300);

        final JArray jlogs = json.arrayC("logs");
        json.put("maxLogs", maxLogs);

        LogsStorage.readLogs(maxId, maxDate, null, new LogsVisitor() {

            int cnt = -1;

            @Override
            public boolean onRead(final LogsFile logs, final TLog log) {
                ++cnt;

                if (cnt >= (maxDate != null && maxId != null ? maxLogs / 2
                        : maxLogs))
                    return false;

                final JObject jl = jlogs.object();

                jl.put("id", log.id);
                jl.put("ts", log.createTime.getTime());

                jl.put("idFrmt", logs.header.fileUid.toString().substring(0, 6)
                        + " " + Utils.formatValue(log.id));
                jl.put(LogAttr.kind.key, log.getVal(LogAttr.kind).peekFirst());

                for (TLog.LogValue val : log.values)
                    jl.put(val.attr.key, val.getValues().toString());

                if (log instanceof Common.InternalLogObject)
                    jl.put("internal", true);

                jl.put("_tme", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(log.date));

                String format = new TDate(log.date).isSameDay(new Date())
                        ? "HH:mm:ss.SSS" : "dd MMM HH:mm:ss.SSS";
                if (!log.includeMilliseconds)
                    format = format.replace(".SSS", "");
                TimeDiff diff = new TimeDiff(log.createTime.getTime() - new TDate(log.date).getTime());
                Integer tol = Config.CConsole.timeTolerance.value();

                jl.put(LogAttr.date.key, new SimpleDateFormat(format).format(log.date));
                jl.put("diff", diff.toString());
                if (tol != null && Math.abs(diff.time) > 1000 * tol)
                    jl.put("diffX", true);

                Object val = log.getVal(LogAttr.value).peekFirst();

                String sval = val != null ? Utils.toString(val) : "";
                if (sval == null)
                    sval = "";
                sval = sval.replace("\r", "").replace("\n", " " + Char.returnKey + " ");

                jl.put(LogAttr.value.key, formatValue(sval, 200));

                return true;
            }

        });

        returnJson(json);

    }

}
