package pages;

import logs.TLog;
import logs.Logs;
import com.exceptions.Http404FileNotFoundException;
import com.exceptions.Http400BadRequestParamException;
import com.utils.hashes.Hex;
import service.Main;
import com.CSV.CSVReader;
import com.google.gson.*;
import com.utils.hashes.Base64;
import com.utils.hashes.Hashes;
import com.utils.hashes.Hashes.Hash;
import com.html.core.*;
import com.html.modules.WindowLayer;
import com.html.tags.*;
import com.servlet.interfaces.*;
import com.servlet.users.*;
import com.utils.*;
import com.xml.XML;
import com.xml.XmlException;
import java.io.*;
import java.net.*;
import java.util.*;
import service.handlers.*;

import static com.resources.ResFile.*;

@Endpoint(url = {"console", ""})
public class PConsole extends SPage {

    @Override
    protected void onRequest() throws Exception {

        new TLog(null, null);

        link(utilsJs, serviceJs, serviceCss, layerJs, layerCss,
                jQuery, popupJs, popupCss);

        //   session.keepAlieve = true;
        head.link("/styles.css",
                "/filters.css",
                "/filters.js",
                "/scripts.js",
                "/common.js",
                "/console.js",
                "/skin.css",
                "/console.css",
                "/statuses.css",
                "/datetimepicker/jquery.datetimepicker.js",
                "/datetimepicker/jquery.datetimepicker.css");

       // js.keepAlive = 600;
        if (!request.proxyName.isEmpty())
            body.inputHidden().id("via_proxy").value(request.proxyName);

        head.setIcon("img/ico.png");
        head.setTitle("Logi, " + Main.hostname);

        Node dMenu = Common.buildMenu(this);
        dMenu.style().boxShadow("2px 2px 2px #333")
                .border("1px solid #234");

        //  Common.buildMenu(this);
        Node pnlLeft = body.div().id("pnlLeft");
        pnlLeft.style().width(request.getCookie("pwidth", 200) + "px");
        body.div().id("spliter");

        Tag content = body.div().id("content");
        content.cls("s-main");

        content.div().id("pnlTop").cls("s-status");
        content.div().id("splTop");

        Node main = content.div().id("pnlMain").cls("s-console");

        Table tbl = main.table();
        tbl.id("console");
        main.div().id("tbottom"); // tag, do ktorego wykonywane jest scrollIntoView
        body.div().id("dStatus");
        body.div().id("dError");
        body.img("img/wait.png").id("wait");
        if (session.user.hasRights(Role.manager))
            body.input(InputType.hidden).id("managerRights");

    }

    @Endpoint
    public void displaySource() throws Exception {

        head.styles("body")
                .font("10pt 'Consolas', 'Courier New'");
        int id = params.getInt("id");
        TLog log = null;
        synchronized (Logs.all) {
            for (TLog ll : Logs.all)
                if (ll.id == id) {
                    log = ll;
                    break;
                }
        }

        if (log == null)
            throw new Http404FileNotFoundException(this);

        WindowLayer layer = new WindowLayer(this);
        layer.caption = "Źródło logu";
        body.code().textToDivs(log.getRawData());
    }

    @Endpoint
    public void displayXML()
            throws IOException, Http400BadRequestParamException, XmlException {
        head.styles("body")
                .font("10pt 'Consolas', 'Courier New'");
        WindowLayer layer = new WindowLayer(this);
        layer.caption = params.getStr("name");
        XML xml = new XML(params.getStr("value"));
        body.code().textToDivs(xml.toString());
    }

    @Endpoint
    public void displayCSV()
            throws IOException, Http400BadRequestParamException {
        head.styles("body, table")
                .font("9pt 'Consolas', 'Courier New'");
        WindowLayer layer = new WindowLayer(this);
        layer.caption = params.getStr("name");

        String val = params.getStr("value");
        List<String[]> list = new CSVReader(new StringReader(val)).readAll();

        Table tbl = body.table();

        tbl.style().borderCollapse("collapse").borderSpacing("0");

        head.styles("table td, table th")
                .border("1px solid #aaa")
                .verticalAlign("top")
                .padding("4px 8px");

        head.styles("table tr:nth-child(odd) td")
                .backgroundColor("#eee");

        int size = list.isEmpty() ? 1 : list.get(0).length;
        if (size < 1)
            size = 1;
        size = (int) Math.round(2000d / (double) size);
        if (size < 150)
            size = 150;

        head.styles("th label, td label")
                .display("block")
                .maxHeight("200px")
                .maxWidth(size + "px")
                .textOverflow("ellipsis")
                .overflowY("auto")
                .overflowX("hidden");

        head.styles("td label").wordBreak("break-all");

        head.styles("table tr:nth-child(even) td")
                .backgroundColor("#f4f4f4");

        if (!list.isEmpty()) {
            Tr tr = tbl.theadTr();
            tr.style().backgroundColor("#ccc");
            for (String s : list.get(0))
                tr.th().label().text(s);
        }

        for (int i = 1; i < list.size(); i++) {
            Tr tr = tbl.tbodyTr();
            for (String s : list.get(i))
                tr.td().label().text(s);
        }
    }

    @Endpoint
    public void displayJSON() throws Exception {

        head.styles("body")
                .font("10pt 'Consolas', 'Courier New'");

        WindowLayer layer = new WindowLayer(this);
        layer.caption = params.getStr("name");

        JsonElement json = new JsonParser().parse(params.getStr("value"));
        StringWriter sw = new StringWriter();

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.create().toJson(json, sw);

        body.code().textToDivs(sw.toString());
    }

    @Endpoint
    public void displayHash() throws Exception {

        WindowLayer layer = new WindowLayer(this);
        layer.caption = params.getStr("name");
        String value = params.getStr("value");

        head.styles("body, table")
                .borderSpacing("4px")
                .font("10pt 'Consolas', 'Courier New'");

        head.styles("label, table td:nth-child(2)")
                .wordBreak("break-all");

        head.styles("table td:nth-child(1)")
                .whiteSpace("nowrap")
                .verticalAlign("top");

        head.styles("h5")
                .marginBottom("4px");

        head.styles("h4")
                .marginTop("8px");

        byte[] bUTF = value.getBytes("UTF-8");
        byte[] bISO = value.getBytes("ISO-8859-2");

        boolean same = Arrays.equals(bISO, bUTF);

        body.label().text("\"" + value + "\"");
        body.br();
        body.br();
        body.hr();

        body.h4().text("Enkodery:");

        if (!same)
            body.h5().text("ISO 8859-2:");

        Table tbl = body.table();
        tbl.tbodyTr().setCells("CRC32:", Hashes.hash(Hash.CRC32, bISO));
        tbl.tbodyTr().setCells("MD5:", Hashes.hash(Hash.MD5, bISO));
        tbl.tbodyTr().setCells("SHA1:", Hashes.hash(Hash.SHA1, bISO));
        tbl.tbodyTr().setCells("SHA256:", Hashes.hash(Hash.SHA256, bISO));
        tbl.tbodyTr().setCells("URI:", URLEncoder.encode(value, "ISO-8859-2")
                .replaceAll("\\+", "%20"));
        tbl.tbodyTr().setCells("Base64:", Base64.encode(bISO));
        tbl.tbodyTr().setCells("HEX:", Hex.toString(bISO));

        if (!same) {
            body.h5().text("UFF-8:");
            tbl = body.table();
            tbl.tbodyTr().setCells("CRC32:", Hashes.hash(Hash.CRC32, bUTF));
            tbl.tbodyTr().setCells("MD5:", Hashes.hash(Hash.MD5, bUTF));
            tbl.tbodyTr().setCells("SHA1:", Hashes.hash(Hash.SHA1, bUTF));
            tbl.tbodyTr().setCells("SHA256:", Hashes.hash(Hash.SHA256, bUTF));
            tbl.tbodyTr().setCells("URI:", URLEncoder.encode(value, "UTF-8")
                    .replaceAll("\\+", "%20"));
            tbl.tbodyTr().setCells("Base64:", Base64.encode(bUTF));
            tbl.tbodyTr().setCells("HEX:", Hex.toString(bUTF));
        }

        body.br();
        body.hr();
        body.h4().text("Dekodery:");

        try {
            byte[] buff = Base64.decode(value);
            body.h5().text("Base64:");
            tbl = body.table();
            tbl.tbodyTr().setCells("HEX:", Hex.toString(buff));
            tbl.tbodyTr().setCells("ISO 8859-2:", new String(buff, "ISO-8859-2"));
            tbl.tbodyTr().setCells("UTF-8:", new String(buff, "UTF-8"));
        } catch (Exception e) {
        }

        try {
            byte[] buff = Hex.toBytes(value);
            body.h5().text("HEX:");

            tbl = body.table();
            tbl.tbodyTr().setCells("ISO 8859-2:", new String(buff, "ISO-8859-2"));
            tbl.tbodyTr().setCells("UTF-8:", new String(buff, "UTF-8"));

        } catch (Exception e) {
        }

        try {
            String sUtf = URLDecoder.decode(value, "UTF-8");
            String sIso = URLDecoder.decode(value, "ISO-8859-2");

            if (value.equals(sUtf))
                sUtf = null;
            if (value.equals(sIso))
                sIso = null;

            if (sUtf != null && sIso != null) {
                body.h5().text("URI:");
                tbl = body.table();
                if (sIso != null)
                    tbl.tbodyTr().setCells("ISO 8859-2:", sIso);
                if (sUtf != null)
                    tbl.tbodyTr().setCells("UTF-8:", sUtf);
            }
        } catch (Exception e) {
        }

        try {
            URI url = new URI(value);

            body.h5().text("URL:");
            tbl = body.table();
            tbl.tbodyTr().setCells("Scheme:", url.getScheme());
            tbl.tbodyTr().setCells("Host:", url.getHost());
            if (url.getPort() > 0)
                tbl.tbodyTr().setCells("Port:", url.getPort());
            tbl.tbodyTr().setCells("Path:", url.getPath());

            if (url.getFragment() != null)
                tbl.tbodyTr().setCells("Fragment:", url.getFragment());
            if (url.getQuery() != null)
                tbl.tbodyTr().setCells("Query:", url.getQuery());
            if (url.getUserInfo() != null)
                tbl.tbodyTr().setCells("UserInfo:", url.getUserInfo());

        } catch (Exception e) {
        }

    }

    @Endpoint
    public void displayHEX() throws Exception {

        WindowLayer layer = new WindowLayer(this);
        layer.caption = params.getStr("name");

        head.styles("table")
                .borderSpacing("0")
                .borderCollapse("collapse");

        head.styles("table td, table th")
                .width("24px")
                .height("20px")
                .border("1px solid #ccc")
                .textAlign("center")
                .font("10pt Consolas, 'Courier New'");

        byte[] buff = Hex.toBytes(params.getStr("value"));

        Table tbl = body.table();
        Tr tr = tbl.theadTr();
        tr.th().text(Char.nbsp);
        for (int i = 0; i < 16; i++)
            tr.th().text(Integer.toHexString(i)).style()
                    .backgroundColor("#aaa")
                    .color("#eee");

        int row = 0;
        for (int i = 0; i < buff.length; i++) {
            if (i % 16 == 0) {
                tr = tbl.tbodyTr();
                tr.td().text(row++).style()
                        .backgroundColor("#aaa")
                        .color("#eee")
                        .fontWeight("bold");
            }

            byte[] cc = new byte[1];
            cc[0] = buff[i];
            tr.td().text(Hex.toString(buff[i]))
                    .title("byte " + i
                            + ", \nHEX: " + Hex.toString(buff[i])
                            + ", \nDEC: " + buff[i]
                            + ", \nASCII \"" + new String(cc, "ISO-8859-2") + "\"");
        }

    }

}
