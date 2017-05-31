


addEventListener("load", function (e) {

    var data = location.hash.substring(1);
    if (data)
        try {
            data = window.atob(data);
            JSON.parse(data);
        } catch (e) {
            data = "";
        }

    if (!data)
        try {
            data = localStorage.getItem("current_filter");
            JSON.parse(data);
        } catch (e) {
            data = "";
        }

    filters.addNewRow(data);
    filters.apply(data);
});



function Filters() {
    this.items = [];
}


var filters = new Filters();

function Filter(row) {
    this.tr = row;
    this.cb = row.getElementsByTagName("input")[0];
    this.sel1 = row.getElementsByTagName("select")[0];
    this.sel2 = row.getElementsByTagName("select")[1];
    this.edt = row.getElementsByTagName("input")[1];
    this.btn = row.getElementsByTagName("input")[2];

    filters.items.push(this);

    $id("tblFilters").appendChild(row);

    var f = this;

    this.sel1.onchange = function () {
        f.sel1Change();
    };

    this.btn.onclick = function (e) {
        filters.items.remove(f);
        f.tr.remove();
        filters.refresh();
    };

    this.edt.onfocus = this.edt.onchange = this.edt.onkeydown = function () {
        var it = filters.items.last();
        if (it && it === f)
            filters.addNewRow();
    };

}

Filter.prototype.sel1Change = function () {

    var f = this;
    f.sel2.innerHTML = "";
    f.sel2.disabled = true;
    f.edt.disabled = true;
    f.edt.value = "";

    if (this.sel1.selectedIndex <= 0)
        return;

    ajax.post("?getFilterActions", {
        busy: false,
        params: {
            key: this.sel1.selectedValue()
        }
    },
    function (http) {
        if (http.error)
            return;

        var json = JSON.parse(http.responseText);

        for (var name in json) {
            var opt = f.sel2.tag("option");
            opt.text = (json[name]);
            opt.setAttribute("value", name);
        }

        f.sel2.disabled = false;
        f.edt.disabled = false;
    });
};


Filters.prototype.refresh = function () {
    for (var i = 0; i < filters.items.length; i++) {
        filters.items[i].btn.style.display = i < filters.items.length - 1 ? "block" : "none";
    }
};

Filters.prototype.addRows = function (innerHTML) {


    var tbl = document.createElement("table");
    tbl.innerHTML = innerHTML;

    var hasRows = tbl.tBodies[0].hasAttribute("has_rows");

    if (hasRows) {
        filters.items = [];
        $id("tblFilters").innerHTML = "";
    }


    var rows = tbl.getElementsByTagName("tr");

    var hasValue = false;

    while (rows.length > 0) {

        var f = new Filter(rows[0]);

        hasValue = f.edt.value.trim();
        f.sel2.disabled = !hasValue;
        f.edt.disabled = !hasValue;
    }

};

Filters.prototype.addNewRow = function (data) {

    var tbl = $id("tblFilters");
    if (!tbl)
        return;

    ajax.post("?getNewFilterRow", {params: {data: data}},
    function (http) {
        filters.addRows(http.responseText);
        filters.refresh();
    });

};


Filters.prototype.build = function () {
    var data = [];

    for (var i = 0; i < filters.items.length; i++) {

        var f = filters.items[i];


        var sel1 = f.sel1.selectedValue();
        var sel2 = f.sel2.selectedValue();
        var edt = f.edt.value.trim();

        if (!sel1 || !sel2 || !edt)
            continue;

        var item = [];

        item.push(f.cb.checked);
        item.push(sel1);
        item.push(sel2);
        item.push(edt);

        data.push(item);
    }

    if (data.length === 0)
        return "";

    data = JSON.stringify(data);

    return data;
};

Filters.prototype.apply = function (data) {

    if (!data)
        data = filters.build();

    ajax.post("?applyFilters", {
        params: {
            filters: data
        }
    },
    function (http) {
        if (http.error) {
            localStorage.setItem("current_filter", "");
            window.location.hash = "";
            return;
        }
        localStorage.setItem("current_filter", data);
        window.location.hash = window.btoa(data);

        filters.addRows(http.responseText);

        refresh();
    });

};
