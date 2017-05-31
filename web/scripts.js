

var dragPoint = -1;
var dragSpliter;

var spliter;
var pnlLeft;
var pnlMain;
var pnlTop;
var splTop;
var filtersHash;
var logsHash;
var hDotTimeout;



addEvent(window, "resize", function() {
    resizeTopPanel();
});

function resizeTopPanel() {
    if (pnlTop && splTop) {

        splTop.style.display = pnlTop.children.length > 0 ? "block" : "none";
        pnlTop.style.display = pnlTop.children.length > 0 ? "block" : "none";

        splTop.style.top = pnlTop.offsetHeight + "px";
        pnlMain.style.top = (splTop.offsetTop + splTop.offsetHeight) + "px";
    }
}

window.addEventListener("load", function() {
    spliter = $id("spliter");
    pnlLeft = $id("pnlLeft");
    pnlMain = $id("pnlMain");
    pnlTop = $id("pnlTop");
    splTop = $id("splTop");



    if (!spliter)
        return;

    spliter.style.left = (pnlLeft.offsetWidth) + "px";
    $id("content").style.left = (pnlLeft.offsetWidth + spliter.offsetWidth) + "px";

    resizeTopPanel();
    resizeFiltersPanel();

    spliter.onmousedown = function(e) {
        dragPoint = e.offsetX ? e.offsetX : e.layerX;
        dragSpliter = spliter;
        setSelectable("#content", false);
    };

    if (splTop)
        splTop.onmousedown = function(e) {
            dragPoint = e.offsetY ? e.offsetY : e.layerY;
            dragSpliter = splTop;
            setSelectable("#content", false);
        };

    document.body.onmousemove = function(e) {
        if (!dragSpliter || dragPoint < 0 || e.button !== 0)
            return true;

        if (dragSpliter === spliter) {
            var d = e.clientX - dragPoint;
            if (d < 0)
                d = 0;
            if (d > 500)
                d = 500;
            if (d > document.body.clientWidth - 100)
                d = document.body.clientWidth - 100;

            spliter.style.left = d + "px";
            pnlLeft.style.width = (spliter.offsetLeft) + "px";
            $id("content").style.left = (spliter.offsetLeft + spliter.offsetWidth) + "px";
        }

        if (dragSpliter === splTop) {
            var d = e.clientY - dragPoint;
            if (d < 0)
                d = 0;

            pnlTop.style.height = (d - 16) + "px";

            resizeTopPanel();
        }


        resizeFiltersPanel();
        return false;
    };


}, false);



window.addEventListener("mouseup", function(e) {
    if (!dragSpliter)
        return;
    if (dragPoint >= 0)
        document.body.onmousemove(e);
    dragPoint = -1;
    dragSpliter = null;

    window.setTimeout(setSelectable, 100, "#content", true);


}, false);



function processLogsRequest(http) {
    if (unloading)
        return null;



    var dd = document.createElement("div");
    dd.innerHTML = http.responseText;



    var xFilters = dd.children[0];
    if (xFilters && xFilters.children.length > 0) {
        /*     
         if (filters.length > 0){        
         pnlLeft.style.backgroundColor = "green";
         window.setTimeout(function(){
         pnlLeft.style.backgroundColor = null;  
         }, 500);
         }
         */
        xFilters.parentNode.removeChild(xFilters);
        pnlLeft.innerHTML = "";
        pnlLeft.appendChild(xFilters);
        reloadFilters();
        return dd.children[0];
    }
    $id("dError").style.display = "none";
    return dd.children[1];
}
