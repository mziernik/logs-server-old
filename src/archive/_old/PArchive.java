package archive._old;

import com.exceptions.EError;
import com.html.core.Node;
import com.html.js.Call;
import com.html.tags.*;
import com.resources.ResFile;
import com.servlet.interfaces.*;
import pages.Common;
import service.handlers.*;

/**
 * Miłosz Ziernik 2013/10/27
 */

/*

 skrypt
 ==, !==
 ~~, !~~

 >= , > , <= , <

 =     - rowne wielkosc bez znaczenia
 ==    - rowne z uwzglednieniem wielkosci
 !=, <>  - rozne z uwzglednieniem wielkosci
 >     - zawiera fraze bez wiekosci
 >=    - zawiera fraze
 !~    

 */
@Endpoint(url = "archive_old", title = "Archiwum")
public class PArchive extends SPage {

    @Override
    protected void onRequest() throws Exception {

        link(ResFile.service, ResFile.jQuery);

        head.linkJavaScript("./common.js");
        head.linkJavaScript("./pages/archive.js");
        head.linkCSS("./styles.css");
        head.linkCSS("./console.css");
        head.linkCSS("./skin.css");
        head.linkCSS("./console.css");
        head.linkCSS("./pages/archive.css");
        link(ResFile.service, ResFile.layer);

        Node dMenu = Common.buildMenu(this);
        dMenu.style().boxShadow("2px 2px 2px #333")
                .border("1px solid #234");

        body.br();
        buildPageCounter(1);

        Tag div = body.div().id("filters");
        div.table().id("tblFilters");
        div.div().id("preFilterApply")
                .input(InputType.button)
                .value("Zastosuj")
                .id("btnFilterApply")
                .onClick(new Call("filters.apply"));

        body.br();

        body.div().id("filters-info");

        Tag main = body.div();
        main.cls("s-main");

        Table tbl = main.table();
        tbl.id("console");

        body.br();
        buildPageCounter(2);

    }

    @Endpoint
    public void getFilterActions() throws Exception {
        returnCustom(Filters.getActions(params.getStr("key")).toString(), "application/json");
    }

    @Endpoint
    public void applyFilters() throws Exception {
      /*  Table tbl = session.filters.parse(this, params.getStr("filters"));
        session.filters.apply();
        if (tbl != null)
            returnTagContent(tbl);*/
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

    private void error(Exception e) {

        Tag div = body.div();
        div.style().backgroundColor("red").border("1px solid black");
        div.text(EError.toString(e));
    }

    private void buildPageCounter(int nr) {

        Tag dPage = body.div().cls("current_page");
        dPage.button("Poprzenia")
                .id("btnPrevPage" + nr);
        dPage.input(InputType.number)
                .value(1)
                .id("edtCurrentPage" + nr);
        dPage.label("/");
        dPage.input(InputType.number)
                .value(1)
                .readOnly(true)
                .id("edtTotalPages" + nr);;
        dPage.button("Następna")
                .id("btnNextPage" + nr);
    }

}
