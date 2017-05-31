var allLogs = [];

var con;
var scrollZone = false;
var maxLogs = 0; // maksymalna ilość logów wyświetlanych jednorazowo na ekranie

addEventListener("load", function(e) {

    service.keepAlive(3 * 60 * 1000);

    con = $id("console");
    document.body.onscroll = consoleScroll;

});


function consoleScroll() {
    if (logsRefreshing)
        return;
    var pos = document.body.scrollTop / (document.body.scrollHeight - window.innerHeight);

    document.title = pos;
    console.log(Math.round(pos * 100));

    if (pos > 0.9) {
        if (!scrollZone) {
            loadLogs(true);
            console.log("LOAD");
        }
        scrollZone = true;
    } else
        scrollZone = false;
}


addEventListener("keydown", function(e) {
    if (logsRefreshing)
        return;

    switch (e.keyCode) {
        case 39: // prawy
            $id("btnNextPage1").onclick();
            break;

        case 37: // lewy
            $id("btnPrevPage1").onclick();
            break;

    }

});


var logsRefreshing = false;

function refresh() {
    loadLogs();

}

function loadLogs() {
    logsRefreshing = true;

    var maxId = null;
    var maxDate = null;

    if (allLogs.length > 0) {
        maxId = allLogs[allLogs.length - 1].id + 1;
        maxDate = allLogs[allLogs.length - 1].ts + 1;
    }

    ajax.post("?getLogs", {
        maxId : maxId,
        maxDate : maxDate
    }, function(resp) {

        logsRefreshing = false;

        if (resp.error)
            return;

        var json = JSON.parse(resp.responseText);

        maxLogs = json.maxLogs;

        json.logs.forEach(function(log) {
            allLogs.push(log);
        });

        var tbl = $id("console");

        // ------------------ separator ----------------------

        var td = tbl.tag("tr").tag("td");
        td.attr({
            colspan : 10
        });
        td.tag("hr");

        // ------------------------------------------------------

        while (tbl.rows.length + json.logs.length > maxLogs)
            tbl.rows[0].remove();

        setTimeout(function() {
            if (json.logs)
                json.logs.forEach(function(log) {
                    displayLog(log);
                });

            console.log("display " + json.logs.length);
        }, 1);


    });


}