var options = {
    version : 7,
    fontSize : 10,
    maxCount : 300,
    autoRefresh : true,
    expanded : ["navigate"],
    sources : [],
    kinds : [],
    headers : ["id", "dte", "tag"],
    marks : [],
    tags : [],
    addresses : [],
    devices : [],
    users : [],
    versions : [],
    //---------------
    requests : [],
    sessions : [],
    dates : []
};


function displayLog(log) {
    // zwraca roznice rozmiaru tabeli
    if (!log)
        return;

    lastLogId = log.id;

    var tbl = $id("console");

    var tr = tbl.tag("tr");
    tr.className = "s-log-line";
    log.row = tr;
    tr.setAttribute("id", "tr" + log.id);

    tr.onclick = function() {
        // jesli zaznaoczno tekst to nie wsyswietlaj szczegolow

        /* var sel = getSelection();
         if (sel.rangeCount)
         for (var i = 0; i < sel.rangeCount; i++)
         if (sel.getRangeAt(i).cloneContents().textContent)
         return;
         */
        var det = $id("det" + log.id);
        if (det && det.style.maxHeight !== "0px") {
            det.style.maxHeight = "0px";
            return;
        }

        if (window.socket)
            socket.send("details", {details : log.id});
        else {
            ajax.post("./archive?getDetails&id=" + log.id, {}, function(resp) {
                if (resp.error)
                    return;
                showDetails(JSON.parse(resp.responseText));
            });
        }
    };

    if (log.internal)
        tr.className += log.tag && log.tag === "warning"
                ? " logIntWarning" : " logInt";

    var td;
    var tag;


    if (options.headers.contains("id")) {
        td = tr.tag("td");
        td.text(log.idFrmt + ".");
        td.title = "Identyfikator";
        td.className = "s-log-id";
    }

    if (log.dte) {
        td = tr.tag("td");

        td.text(log.dte);
        td.className = "s-log-date";
        if (log.diffX)
            td.className += " s-log-time-diff";
        if (log.diff)
            td.title = "Różnica czasu: " + log.diff;

    }

    if (log.marks) {
        td = tr.tag("td");
        //  td.className = "clrse s-log-marks";
        td.className = "clrs s-log-marks";
        var addMark = function(name, label) {
            var mark = log.marks[name];
            if (!mark)
                return;
            tag = td.tag("div");
            tag.title = label + ": " + (log[name] ? log[name] : "<brak>");
            tag.innerHTML = "&nbsp;";
            tag.style.backgroundColor = mark ? mark : "#222";

        };

        addMark("src", "Źródło");
        addMark("adr", "Adres");
        addMark("dev", "Urządzenie");
        addMark("prc", "ID procesu");
        addMark("thr", "ID wątku");
        addMark("usr", "Użytkownik");
        addMark("ses", "Sesja");
        addMark("req", "Wątek");
    }

    td = tr.tag("td");
    td.style.width = "100%";

    var div = td.tag("div");
    var det = td.tag("div");
    det.setId("det" + log.id);
    det.setAttribute("class", "s-log-details");
    det.style.maxHeight = "0px";

    div.className = "dLine s-lk_" + log.knd;

    tag = div.tag("div");
    tag.className = "info s-log-header";

    var addNfo = function(name, title) {
        var val = log[name];
        if (val === undefined || !options.headers.contains(name))
            return;
        if (tag.children.length > 0)
            tag.tag("span").text(", ");
        tag.tag("span").text(val).title = title;
    };

    addNfo("src", "Źródło: " + log.src);
    addNfo("thr", "Wątek: " + log.thr);
    addNfo("prc", "Proces: " + log.prc);
    addNfo("adr", "Adres: " + log.adr);
    addNfo("dev", "Urządzenie: " + log.dev);
    addNfo("usr", "Użytkownik: " + log.usr);


    if (log.tag && log.tag.length > 0) {
        tag = div.tag("span");
        tag.className = "ltype s-log-tag";
        tag.text("[" + log.tag + "]");
        tag.title = "Tag";
    }


    tag = div.tag("span");
    tag.className = "s-log-value";
    tag.text(log.val);


    if (log.fcl)
        div.style.color = log.fcl;

    if (log.bcl)
        div.style.backgroundColor = log.bcl;

    if (log.com) {
        tag = div.tag("div");
        tag.className = "comment s-log-comment";
        tag.text(log.com);
    }

}


function showDetails(log) {
    if (!log || !log.id || !$id("tr" + log.id))
        return;

    var tag = $id("det" + log.id);
    tag.innerHTML = "";


    tag.addEventListener("click", function(e) {
        e.cancelBubble = true;
    }, true);

    tag = tag.tag("div").cls("s-log-details-sub");

    var tbl = tag.tag("table");

    var add = function(name, value) {
        if (!value)
            return;
        var tr = tbl.tag("tr");
        if (name) {
            tr.tag("td").text(name);
            tr.tag("td").linkify(value);
        } else {
            var td = tr.tag("td");
            td.setAttribute("colspan", "2");
            td.linkify(value);
        }
        tr.setAttribute("class", "details-line");
    };

    add("Data klienta:", log.cdate);
    add("Data serwera:", log.sdate);
    add("Rodzaj:", log.kind);
    add("Źródło:", log.src);
    add("Wersja:", log.ver);
    add("Adres:", log.adr);
    add("Urządzenie:", log.devf);
    add("Logger", log.lgr);
    add("Poziom", log.lvl);

    add("Metoda", log.mth);

    add("Protokół:", log.prot);
    add("Id procesu:", log.proc);
    add("Wątek:", log.thr);
    add("Instancja:", log.inst);
    add("Sesja:", log.ses);
    add("Żądanie:", log.req);
    add("Użytkownik:", log.user);
    add("Komentarz:", log.comm);

    if (log.url && log.url.length > 0) {
        var tt = tbl.tag("tr");
        tt.tag("td").text("Url:");
        tt = tt.tag("td");
        for (var k = 0; k < log.url.length; k++)
            tt.tag("div").linkify(log.url[k]);
    }

    add("Kolor:", log.col);
    add("Tło:", log.back);

    add("UID:", log.uid);
    add("Tagi:", log.tag);
    /*
     if (log.atr) {
     var td = tbl.tag("tr").tag("td");
     td.setAttribute("colspan", "2");
     td.text(" ");
     for (var name in log.attr) {
     add(name + ":", log.attr[name]);
     }
     }
     */
    if (log.atr) {
        for (var gr in log.atr) {
            var td = tbl.tag("tr").tag("td");
            td.setAttribute("colspan", "2");
            if (gr)
                td.tag("h4").text(gr);
            else
                td.text(" ");

            var group = log.atr[gr];
            for (var i = 0; i < group.length; i++) {
                var g = group[i];
                if (g.length === 2)
                    add(g[0], g[1]);
                else
                    add(null, g[0]);
            }

        }
    }

    var addData = function(data) {

        var type = data.type;
        var name = data.name;
        var value = data.value;


        var hh = tag.tag("h4");

        hh.tag("label").text(name);

        if (data.frmt) {
            for (var ff in data.frmt) {
                var lbl = hh.tag("label").cls("s-log-details-checkbox");
                lbl.title = "Format RAW / " + ff;
                var cb = lbl.tag("input");

                cb.type = "checkbox";
                cb.checked = true;
                cb.onclick = function(e) {

                };

                lbl.tag("span").text(ff);
                tag.tag("p").innerHTML = data.frmt[ff];
            }
            return;
        }

        var processListBlock = function(stack, className) {
            var moreLabel = function(lbl) {
                return "------------------------------ " + lbl + " ------------------------------";
            };


            var ul;
            var prevBlockNr;
            var idx = 0;
            stack.forEach(function(item) {
                var s = item.val;

                if (prevBlockNr !== item.block) {
                    ul = null;
                    idx = 0;
                }

                prevBlockNr = item.block;

                if (!ul && className === "s-log-details-call_stack") {
                    ul = tag.tag("ul");
                    ul.cls(className);
                } else

                if (!ul) {
                    tag.tag("div").text(s).css({
                        marginLeft : "8px",
                        marginTop : "10px"
                    });
                    ul = tag.tag("ul");
                    ul.cls(className);

                    return;
                }
                if (!s)
                    return;

                var t = ul.tag("li");
                t.text(s);
                if (item.same)
                    t.cls("repeated");

                ++idx;

                if (idx === 5) {
                    var lbl = tag.tag("label")
                            .cls("s-log-details-more_label")
                            .text(moreLabel("Więcej"));
                    ul = tag.tag("ul");
                    ul.cls(className);
                    ul.css({
                        margin : 0,
                        display : "none"
                    });

                    lbl.list = ul;

                    lbl.onclick = function(e) {
                        var list = e.target.list;
                        if (list.style.display === "none") {
                            list.style.display = "block";
                            e.target.text(moreLabel("Mniej"));
                        } else {
                            list.style.display = "none";
                            e.target.text(moreLabel("Więcej"));
                        }
                        resizeDetails();
                    };
                }

            });
        };


        if (type === "error_stack") {

            for (var i = 0; i < value.length; i++) {

                var stack = [];
                var blockNr = 0;

                value.forEach(function(s) {
                    if (!blockNr || s.startsWith("Caused by: "))
                        ++blockNr;
                    stack.push({
                        val : s,
                        block : blockNr,
                        same : false
                    });
                });

                for (var i = 0; i < stack.length; i++) {

                    for (var j = i + 1; j < stack.length; j++) {
                        var b1 = stack[i];
                        var b2 = stack[j];
                        if (b1.block !== b2.block && b1.val === b2.val)
                            b1.same = b2.same = true;
                    }
                }
            }


            processListBlock(stack, "s-log-details-error_stack");
            return;
        }




        if (type === "call_stack") {
            var stack = [];
            for (var i = 0; i < value.length; i++)
                stack.push({
                    val : value[i],
                    block : 1
                });

            processListBlock(stack, "s-log-details-call_stack");
            return;
        }


        tag.tag("p")
                .linkify(value.replace())
                .cls("pClass");
        /* else {
         var lst = data.value.split("\n");
         var ul = tag.tag("ul");
         lst.forEach(function(s) {
         if (s !== "")
         ul.tag("li").text(s);
         });
         ul.cls(pClass);
         }*/


    };


    if (log.data)
        for (var i = 0; i < log.data.length; i++)
            addData(log.data[i]);

    if (log.est)
        addData({
            type : "error_stack",
            name : "Stos błędów",
            value : log.est
        });

    if (log.cst)
        addData({
            type : "call_stack",
            name : "Stos metod",
            value : log.cst
        });


    var tbtns = tag.tag("div");
    tbtns.style.textAlign = "right";
    if (log.raw) {
        tbtns.tag("button")
                .text("Źródło")
                .addEventListener("mouseup", function() {
                    new Layer("./console?displaySource", {
                        id : log.id
                    });
                });
    }
    tag.tag("br");

    var resizeDetails = function() {
        tag.parentNode.style.maxHeight = tag.scrollHeight + "px";
        tag.parentNode.style.marginLeft = (-tag.parentNode.parentNode.offsetLeft) + "px";
    };

    setTimeout(function() {
        resizeDetails();
    }, 1);
}


addEventListener("load", function() {

    showMainMenu(false);
    $id("preMenu").onmouseover = function() {
        showMainMenu(true);
    };
    $id("preMenu").onmouseout = function() {
        showMainMenu(false);
    };
});

var hideMenuTimeout;

function showMainMenu(state) {
    var preMenu = $id("preMenu");
    preMenu.className = state ? "menu_trans_in" : "menu_trans_out";

    if (hideMenuTimeout)
        clearTimeout(hideMenuTimeout);

    if (state) {
        preMenu.style.top = "0";
        preMenu.style.padding = 0;
    }
    else
        hideMenuTimeout = setTimeout(function() {
            hideMenuTimeout = null;
            $id("preMenu").style.padding = null;
            $id("preMenu").style.top = "-22px";
        }, 2000);
}