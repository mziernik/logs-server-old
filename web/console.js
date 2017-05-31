
service.setConfig("options");
// w przypadku aktualziacji
if (options.version !== 7)
    options = {
        version: 7,
        fontSize: 10,
        maxCount: 300,
        autoRefresh: true,
        expanded: ["navigate"],
        sources: [],
        kinds: [],
        headers: ["id", "dte", "tag"],
        tags: [],
        addresses: [],
        devices: [],
        users: [],
        versions: [],
        //---------------
        requests: [],
        sessions: [],
        dates: []
    };


if (!window.WebSocket) {
    alert("Przeglądarka nie spełnia minimalnych wymagań technicznych.\n"
            + "(Brak obsługi funkcji HTML5: WebSocket)");
    document.close();
}

var socket;
var lastLogId = 0;
var scrollToEnd = true;


var socket = new JsonSocket("consoleSocket");
socket.autoReconnect = true;

socket.onmessage = function (data, msg) {

    if (data.print)
        console.log(data.print);

    if (data.details) {
        showDetails(data.details);
        return;
    }

    if (data.alertMsg)
        new CenterBox({
            text: data.alertMsg,
            error: false,
            zIndex: 100
        });

    if (data.errorMsg)
        new CenterBox({
            text: data.errorMsg,
            error: true,
            zIndex: 100
        });

    if (data.toRemove) {
        for (var i = 0; i < data.toRemove.length; i++) {
            var tr = $id("tr" + data.toRemove[i]);
            if (tr)
                tr.remove();
        }
    }

    if (data.filters)
        filters.build(data.filters);

    //   console.log("kolejka " + data.queue);

    if (paused)
        return;


    if (data.statuses)
        statuses.process(data.statuses);


    if (data.logs)
        processLogs(data.logs);


    busy(data.hasMore);

};


socket.onopen = function () {
    setTimeout(function () {
        socket.init();
    }, 100);
};

socket.onerror = function () {
    busy(false);
};

socket.onclose = function () {
    busy(false);
};

var socketInitTimeout;

socket.init = function () {
    busy(true);
    clearTimeout(socketInitTimeout);
    socketInitTimeout = setTimeout(function () {
        $id("console").innerHTML = "";
        logs = [];

        if (!options.maxCount || options.maxCount < 10)
            options.maxCount = 300;

        socket.send("init", {
            init: {
                maxCount: options.maxCount,
                sources: options.sources,
                kinds: options.kinds,
                headers: options.headers,
                marks: options.marks,
                tags: options.tags,
                addresses: options.addresses,
                devices: options.devices,
                users: options.users,
                versions: options.versions,
                maxId: filters.others.cbId.checked ? filters.others.edtId.value : null,
                maxDate: filters.others.cbDate.checked ? filters.others.edtDate.value
                        + " " + filters.others.edtTime.value : null
            }
        });

    }, 10);
};

var logsQueue = [];
var queueProcessing = false;

function busy(state) {
    $id("wait").style.opacity = state ? 1 : 0;
}


function processLogs(list) {

    scrollToEnd = pnlMain.scrollHeight - pnlMain.scrollTop
            - pnlMain.clientHeight <= 50;

    for (var i = 0; i < list.length; i++)
        logsQueue.push(list[i]);

    if (logsQueue.length > 5)
        busy(true);

    if (!queueProcessing) {
        queueProcessing = true;
        setTimeout(processQueue, 1);
    }
}

function processQueue() {
    var tblHeightDiff = 0;

    var tbl = $id("console");
    var tblHeightDiff = tbl.clientHeight;


    tblHeightDiff -= tbl.clientHeight;

    while (logsQueue.length > 0) {
        var log = logsQueue.shift();
        var dd = displayLog(log);
        if (dd && dd.tblHeightDiff)
            tblHeightDiff += dd.tblHeightDiff;

        if (!filters.others.cbId.checked)
            filters.others.edtId.value = log.id;

        if (!filters.others.cbDate.checked) {
            var tt = log._tme.split(" ");
            filters.others.edtDate.value = tt[0];
            filters.others.edtTime.value = tt[1];
        }
    }


    while (tbl.rows.length > options.maxCount)
        tbl.rows[0].remove();

    queueProcessing = false;
    setTimeout(function () {
        if (!queueProcessing)
            busy(false);
    }, 100);

    resizeTopPanel();

    if (scrollToEnd)
        $id("tbottom").scrollIntoView();
    else
    if (tblHeightDiff > 0)
        pnlMain.scrollTop -= tblHeightDiff;

}


var names = {
    filters: {
        navigate: "Nawigacja",
        headers: "Nagłówki",
        marks: "Znaczniki",
        kinds: "Rodzaj",
        sources: "Źródło",
        tags: "Tag",
        addresses: "Adres",
        versions: "Wersja",
        devices: "Urządzenie / UA",
        users: "Użytkownik",
        others: "Pozostałe"
    },
    kinds: {
        request: "Żądanie",
        debug: "Debug",
        event: "Zdarzenie",
        log: "Log",
        info: "Informacja",
        trace: "Szczegóły",
        warning: "Ostrzeżenie",
        error: "Błąd",
        query: "Zapytanie"
    }
};



window.addEventListener("beforeunload", function () {
    socket.close();

    unloading = true;
    window.localStorage.setItem("options", window.JSON.stringify(options));
}, false);


function consoleFiltersChanged() {
    lastId = -1;
    consoleHttpAborted = true;
    if (consoleHttp)
        consoleHttp.abort();
    reloadConsole();
}




function clearLogs() {
    $id("console").innerHTML = "";
    filters.sources.items = [];
    filters.sources.body.innerHTML = "";
    filters.tags.items = [];
    filters.tags.body.innerHTML = "";
    filters.addresses.items = [];
    filters.addresses.body.innerHTML = "";
    filters.devices.items = [];
    filters.devices.body.innerHTML = "";
    filters.versions.items = [];
    filters.versions.body.innerHTML = "";
    filters.users.items = [];
    filters.users.body.innerHTML = "";

    for (var i = 0; i < filters.kinds.items.length; i++) {
        var it = filters.kinds.items[i];
        it.count = 0;
        it.counter.text("[0]");
    }

    socket.send("clearLogs", {clearLogs: lastLogId});
}


var state = {
    statuses: {
        currentHash: "",
        globalHash: ""
    },
    logs: {
        currentHash: "",
        globalHash: ""
    }

};


var statuses = new function () {
    this.tree = {};
    this.expanded = [];

    setInterval(function () {

        function visit(node) {

            if (node.groups)
                for (var name in node.groups) {
                    var group = node.groups[name];
                    visit(group);
                }

            if (node.items)
                for (var name in node.items) {
                    var item = node.items[name];
                    --item.left;

                    if (item.left <= 0) {
                        if (item.tag)
                            item.tag.remove();
                        delete node.items[name];
                    }

                }
        }

        if (paused)
            return;

        visit(statuses.tree);

    }, 1000);


    addEventListener("beforeunload", function () {
        localStorage.setItem("statuses_expanded", JSON.stringify(statuses.expanded));
    });

    addEventListener("load", function () {
        var exp = localStorage.getItem("statuses_expanded");
        if (!exp)
            return;
        statuses.expanded = JSON.parse(exp);
    });

};

statuses.process = function (stats) {

    var pnl = $id("pnlTop");
    pnl.style.display = "block";
    resizeTopPanel();

    $id("splTop").style.display = "block";

    pnl.innerHTML = "";


    function visit(parentTag, group, level, path) {
        var tag = parentTag.tag("div");
        tag.cls("stat-group");
        var tt = tag.tag("div");
        tt.cls("stat-header");

        var marker = tt.tag("span").cls("stat-header-marker");
        tt.tag("span").text(group.cap);
        tt.css({
            paddingLeft: (level * 24) + "px"
        });

        if (group.com)
            tt.tag("label").text(group.com).cls("stat-item-comment");



        tag = tag.tag("div").cls("stat-body");
        tag.style.display = statuses.expanded.contains(path) ? "inline-block" : "none";
        marker.text(tag.style.display === "none" ? "[+]" : "[-]");

        tt.onmousedown = function () {
            statuses.expanded.remove(path);

            if (tag.style.display === "none")
                statuses.expanded.push(path);
            // zredukuj rozmiar tablicy
            statuses.expanded.limit(50, true);

            marker.text(tag.style.display === "none" ? "[-]" : "[+]");

            $(tag).slideToggle({
                duration: 200,
                progress: function () {
                    resizeTopPanel();
                }
            }, function () {

            });
        };

        for (var name in group) {

            switch (name.charAt(0)) {
                case "g":
                    visit(tag, group[name], level + 1, path + "/" + name.substring(1));
                    break;

                case "i":
                    var item = group[name];

                    var tt = tag.tag("div");
                    item.tag = tt;
                    tt.css({
                        paddingLeft: ((level + 1) * 24) + "px",
                        color: item.fcl ? item.fcl : null,
                        backgroundColor: item.bcl ? item.bcl : null
                    });
                    tt.cls("stat-item");

                    if (item.tags)
                        if (item.val)
                            tt.tag("label").text(item.tags).cls("stat-item-tags");

                    if (item.val)
                        tt.tag("label").text(item.val).cls("stat-item-value");
                    if (item.com)
                        tt.tag("label").text(item.com).cls("stat-item-comment");
                    if (item.prg)
                        tt.tag("div")
                                .cls("stat-item-pre_progress")
                                .tag("div")
                                .cls("stat-item-progress")
                                .css({
                                    width: (item.prg * 100) + "%"
                                });



                    break;
            }
        }




        return tag;
    }



    for (var grpKey in stats)
        visit(pnl, stats[grpKey], 0, grpKey);

}
;

