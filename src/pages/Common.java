package pages;

import storage.PLogsStorage;
import com.config.engine.PConfig;
import logs.TLog;
import com.exceptions.EError;
import com.utils.date.TDate;
import com.context.AppContext;
import com.html.core.*;
import com.html.tags.*;
import com.servlet.handlers.*;
import com.servlet.users.*;
import java.lang.reflect.*;
import com.mlogger.LogKind;
import logs.*;

/**
 * Created on : 2012-07-25, 08:37:13 Author : Miłosz Ziernik
 */
public class Common {

    //-----------------------------------------------
    public final static int eventExpire = 72; // 3 dni
    public final static int queyExpire = 24;
    public final static int logExpire = 8760; //rok
    public final static int errorExpire = 8760;
    public static Integer filedExpire = 300; // 5 minut (w sekundach)

    public static class InternalLogObject extends TLog {

        public InternalLogObject(LogKind kind, String value) {
            super(null, 0, null);
            this.kind = kind;
            this.value(value);
            address("localhost");
            source = AppContext.serviceTitle;
            date = new TDate();
        }

        public void add() {
            Collector.instance.add(this);
        }
    }

    public static void addInternalLog(LogKind kind, String value) {
        new InternalLogObject(kind, value).add();
    }

    public static TLog addInternalWarning(Throwable ex) {
        return addInternalError(ex, true);
    }

    public static void addInternalError(Throwable ex) {
        addInternalError(ex, false);
    }

    private static TLog addInternalError(Throwable ex, boolean warning) {

        InternalLogObject log = new InternalLogObject(
                warning ? LogKind.warning : LogKind.error,
                EError.exceptionToStr(ex));

        String det = "";
        for (Field f : ex.getClass().getFields()) {
            if (f.getType() != String.class)
                continue;
            try {
                det += f.getName() + ":\n\"" + f.get(ex) + "\"\n\n";
            } catch (Exception e) {
            }
        }

        Throwable e = ex;

        String cause = "";

        while (e != null) {
            cause += e.getClass().getSimpleName() + ": " + e.getMessage() + "\n";
            e = e.getCause();
        }

        cause += "\n";
        log.details(cause + det);
        log.errorStack.addAll(EError.getStackTraceStr(ex).getList());

        log.add();

        return log;
    }

    public static Node buildMenu(Page page) {
        Node pre = page.body.div().id("preMenu");

        Tag div = pre.div().id("dMenu");
        div.cls("menu_trans_out");

        /*  if (page instanceof LogsMain) {
         Img img = pre.img("img/refresh.png", null);
         img.cls("imgRef");
         img.id = "imgRef";
         img.onclick = "refreshLogs()";
         //      div.s.marginRight = "34px";
         }
         */
        div.a().href("./").text("Konsola").cls(page instanceof PConsole ? "def"
                : null);
        div.span().text("|");

        div.a().href("stor").text("Archiwum").cls(page instanceof PLogsStorage
                ? "def" : null);
        div.span().text("|");

        if (page.session.user.hasRights(Role.admin)) {
            div.a().href("$config").text("Konfig").cls(page instanceof PConfig
                    ? "def" : null);
            div.span().text("|");
        }

        div.a().href("about").text("About").cls(page instanceof About ? "def"
                : null);
        div.span().text("|");

        div.a().href("javascript:showPopupWindow('generator',600,600)").text("Gen");
        div.span().text("|");
        ;

        //  div.a("./", "logi").cls(page instanceof LogsMain ? "def" : null);
        // div.span().text("|";
        //  div.a("console", "konsola").cls(page instanceof ConsoleMain ? "def" : null);
        //  div.span().text("|";
        // div.a("storage", "magazyn").cls(page instanceof Storage ? "def" : null);
        // div.span().text("|";
        //  div.a("about.jsp", "info");
        //  div.span().text("|";
        //    div.a().href("$user").text(page.session.user.username).cls(page instanceof User ? "def" : null);
        return div;
    }

    static final int TYPE_NULL = 0;
    static final int TYPE_BOOLEAN = 1;
    static final int TYPE_BYTE = 2;
    static final int TYPE_SHORT = 3;
    static final int TYPE_INT = 4;
    static final int TYPE_LONG = 5;
    static final int TYPE_BIG_INTEGER = 6;
    static final int TYPE_FLOAT = 7;
    static final int TYPE_DOUBLE = 8;
    static final int TYPE_BIG_DECIMAL = 9;
    static final int TYPE_CHAR = 10;
    static final int TYPE_STRING = 11;
    static final int TYPE_UUID = 12;
    static final int TYPE_DATE = 13;
    static final int TYPE_ARRAY = 14;
    static final int TYPE_SERIALIZED_OBJECT = 19;
}
