
var detailsWindow;
var dLogs;
var expandedRow; // rozwiniety wiersz szczegolow

window.addEventListener("load", function() {
    dLogs = $id("dLogs");
    reloadLogs();
}, false);

function refreshLogs() {
    reloadLogs(getFilterQuery() + "&logsHash=" + $id("logsHash").value);
}

function reloadLogs(params) {
    service.wait(true);

    if ($id("imgRef"))
        $id("imgRef").style.border = "1px solid #aed";

    service.postAsync("logs", params, function(http) {
        service.wait(false);

        if ($id("imgRef"))
            $id("imgRef").style.border = null;

        dLogs = processLogsRequest(http);
        if (!dLogs || dLogs.children.length == 0)
            return;

        pnlMain.innerHTML = null;
        pnlMain.appendChild(dLogs);

        var tbl = $id("tblLogs");
        if (!tbl)
            return;


        var divs = tbl.getElementsByTagName("div");

        for (var i = 1; i < tbl.rows.length; i++) {
            tbl.rows[i].oncontextmenu = function(e) {
                var tr = getParentTag(this, "tr");

                popupMenuSetCaption("miLogs_newer",
                        "Młodsze niż: " + tr.cells[2].children[0].innerHTML);
                popupMenuSetCaption("miLogs_older",
                        "Starsze niż: " + tr.cells[2].children[0].innerHTML);

                popupMenuSetCaption("miLogs_tag",
                        "Tag: \"" + tr.cells[6].children[0].innerHTML + "\"");
                popupMenuSetCaption("miLogs_source",
                        "Źródło: \"" + tr.cells[3].children[0].innerHTML + "\"");

                var val = tr.cells[4].children[0].innerHTML;
                setMenuItemVisible("miLogs_device", val);
                popupMenuSetCaption("miLogs_device", "Urządzenie / UA: \"" + val + "\"");

                popupMenuSetCaption("miLogs_address",
                        "Adres: \"" + tr.cells[5].children[0].innerHTML + "\"");

                val = tr.cells[12].children[0].innerHTML;
                setMenuItemVisible("miLogs_user", val);
                popupMenuSetCaption("miLogs_user", "Użytkownik: \"" + val + "\"");

                val = tr.cells[10].children[0].innerHTML;
                setMenuItemVisible("miLogs_request", val);
                popupMenuSetCaption("miLogs_request", "Żądanie: \"" + val + "\"");

                val = tr.cells[11].children[0].innerHTML;
                setMenuItemVisible("miLogs_session", val);
                popupMenuSetCaption("miLogs_session", "Sesja: \"" + val + "\"");

                showPopupMenu("pmLogs");
                return false;
            }
        }

        for (var i = 0; i < divs.length; i++) {
            divs[i].onclick = getDetails;
        }

        for (var i = 1; i < tbl.rows.length; i++) {
            var t = tbl.rows[i].getAttribute("k");
            tbl.rows[i].style.backgroundColor = i % 2 != 0 ? "#fff" : "#f7f7f7";
            tbl.rows[i].onmouseover = selectRow;
        }

        tbl.rows[0].oncontextmenu = function(e) {
            showPopupMenu('pmColumns');
            return false;
        }


        filter_refreshStates();
        updateColumns(true);

        var foc = $id("focusedLID");
        if (foc) {
            foc.scrollIntoView();

            for (var k = 0; k < foc.cells.length; k++) {
                foc.cells[k].style.borderTop = "2px solid red";
                foc.cells[k].style.borderBottom = "2px solid red";
            }

            setTimeout(function(foc) {
                for (var k = 0; k < foc.cells.length; k++) {
                    foc.cells[k].style.borderTop = null;
                    foc.cells[k].style.borderBottom = null;
                }

            }, 1000, foc);

        }

    })

}

function updateColumns() {

    var tbl = $id("tblLogs");
    if (!tbl || columns.checkboxes.length == 0)
        return;

    for (var i = 0; i < columns.checkboxes.length; i++) {
        var vis = columns.checkboxes[i].checked;

        var nr = parseInt(columns.checkboxes[i].parentNode.getAttribute("c"));

        for (var j = 0; j < tbl.rows.length; j++) {
            tbl.rows[j].cells[nr].style.display = vis ? null : "none";
        }
    }

    filter_refreshStates();
}

function getFilter(name) {
    for (var i = 0; i < filters.length; i++)
        if (filters[i].name == name)
            return filters[i];
    return null;
}

function pmLogsItemClick(mi) {

    var tr = getParentTag(mi.source, "tr");

    var obj = null;

    switch (mi.id) {
        case "miLogs_delete":
            if (!confirm("Czy na pewno usunąć " + $id("logsCnt").value + " logów?"))
                return;

            if (service.getSync("pages.Logs&deleteLogs") == "delOK")
                window.location.reload();
            break;

        case "miLogs_newer":
            obj = $id("edtfdateto");
            obj.value = tr.cells[2].children[0].innerHTML;
            break;

        case "miLogs_older":
            obj = $id("edtfdatefrom");
            obj.value = tr.cells[2].children[0].innerHTML;
            break;

        case "miLogs_tagY":
            obj = _checkboxFilter("tag", tr.cells[6].children[0].innerHTML, true);
            break;

        case "miLogs_tagN":
            obj = _checkboxFilter("tag", tr.cells[6].children[0].innerHTML, false);
            break;

        case "miLogs_sourceY":
            obj = _checkboxFilter("source", tr.cells[3].children[0].innerHTML, true);
            break;

        case "miLogs_sourceN":
            obj = _checkboxFilter("source", tr.cells[3].children[0].innerHTML, false);
            break;

        case "miLogs_deviceY":
            obj = _checkboxFilter("device", tr.cells[4].children[0].innerHTML, true);
            break;

        case "miLogs_deviceN":
            obj = _checkboxFilter("device", tr.cells[4].children[0].innerHTML, false);
            break;

        case "miLogs_addressY":
            obj = _checkboxFilter("address", tr.cells[5].children[0].innerHTML, true);
            break;

        case "miLogs_addressN":
            obj = _checkboxFilter("address", tr.cells[5].children[0].innerHTML, false);
            break;

        case "miLogs_userY":
            obj = _checkboxFilter("username", tr.cells[12].children[0].innerHTML, true);
            break;

        case "miLogs_userN":
            obj = _checkboxFilter("username", tr.cells[12].children[0].innerHTML, false);
            break;

        case "miLogs_session":
            obj = $id("edtfsession");
            obj.value = tr.cells[11].children[0].innerHTML;
            break;

        case "miLogs_request":
            obj = $id("edtfsession");
            obj.value = tr.cells[10].children[0].innerHTML;
            break;
    }


    if (obj) {
        obj = obj.parentNode;
        obj.style.backgroundColor = "#0e0";
        obj.style.border = "1px solid #060";
        obj.style.margin = "-1px";

        var grp = obj.parentNode;
        if (grp.style.display == "none")
            $("#" + grp.id).slideToggle(200);

        setTimeout(function(o) {
            o.style.backgroundColor = null;
            o.style.border = null;
            o.style.margin = null;
        }, 400, obj);

        filterItemChange(obj.filter);
    }
}

function _checkboxFilter(type, name, sel) {
    var filter = getFilter(type);
    var obj;

    for (var i = 0; i < filter.checkboxes.length; i++) {
        var cb = filter.checkboxes[i];

        if (sel)
            cb.checked = false;

        if (cb.name == name) {
            obj = cb;
            cb.checked = sel;
        }
    }

    return obj;
}


function jumpToPage(edt) {
    if (!edt)
        return;
    goTopage(edt.value);
}

function jumpToLog(edt) {
    if (!edt)
        return;
    reloadBody("lid=" + edt.value);
}

function selectRow() {
    var req = this.getAttribute("gr");
    var tbl = $id("tblLogs");
    for (var i = 1; i < tbl.rows.length; i++) {
        var tr = tbl.rows[i];
        var rr = tr.getAttribute("gr");
        var eq = (req != null && rr != null && req != "" && rr != "" && req == rr)
                || tr == this;

        tr.style.backgroundColor =
                this == tr ? "#def"
                : eq ? "#efd"
                : i % 2 != 0 ? "#fff"
                : "#f7f7f7";
    }
}

function _onResize() {
    if (!expandedRow)
        return;
    var div = $id("ddet" + expandedRow.getAttribute("lid"));
    div.style.width = (window.innerWidth - 40) + "px";
}

window.addEventListener("resize", _onResize, false);

function getDetails(e) {
    var tr = this.parentNode.parentNode;
    var id = this.parentNode.parentNode.getAttribute("lid");
    var tbl = $id("tblLogs");

    var topRow = null;
    for (var i = 1; i < tbl.rows.length; i++) {
        var tt = tbl.rows[i];
        if (tt.offsetTop + 60 > document.body.scrollTop) {
            topRow = tt;
            break;
        }
    }

    if (expandedRow != null) {
        var eid = expandedRow.getAttribute("lid");
        var edet = $id("det" + eid);
        //    var eddet = $id("ddet" + eid);
        expandedRow = null;
        $("#ddet" + eid).slideToggle(300, function() {
            edet.parentNode.removeChild(edet);
        });
        if (id == eid)
            return;
    }

    var http = httpGetAsync("details?" + id, function(http) {
        if (http.status != 200) {
            service.error(http);
            return;
        }

        var td = http.tr.parentNode.insertRow(http.tr.rowIndex).tag("td");
        td.setAttribute("colspan", "20");
        td.setAttribute("class", "tdDetails");
        td.setAttribute("id", "det" + http.id);
        td.innerHTML = http.responseText;
        expandedRow = http.tr;
        var div = $id("ddet" + http.id);
        div.style.display = "none";

        $("#ddet" + http.id).slideToggle(300, function() {
            var diff = (tbl.offsetTop + expandedRow.offsetTop + div.offsetHeight + 60) -
                    (document.body.scrollTop + window.innerHeight);
            if (diff > 0) {
                window.scrollBy(0, diff);
            }
            diff = (tbl.offsetTop + expandedRow.offsetTop) - (document.body.scrollTop);
            if (diff < 0) {
                window.scrollBy(0, diff);
            }
        });


        _onResize();

    }, false);

    http.tr = tr;
    http.id = id;
    http.topRow = topRow;

}

function goTopage(page) {
    reloadLogs("page=" + page);
}

