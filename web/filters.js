

var pnlLeft;
var paused = false;

var filters = new Filters();


function Filters() {
    this.list = [];
    this.navigate;
    this.headers;
    this.marks;
    this.kinds;
    this.sources;
    this.tags;
    this.addresses;
    this.devices;
    this.users;
    this.custom;

    addEventListener("load", function() {
        pnlLeft = $id("pnlLeft");

        var filter;

        filters.navigate = filter = new Filter("navigate");
        filter.nonEmpty = false;
        var btn = filter.body.tag("div");
        btn.className = "filters-button";
        btn.text("Wyczyść logi");
        btn.onclick = clearLogs;

        if ($id("managerRights")) {
            filter.body.tag("div").cls("filters-button")
                    .text("Przeładuj usługę").onclick = function() {
                service.confirmAndReload('Czy na pewno przeładować usługę?',
                        '$manager?reload', null, null);
            };

            filter.body.tag("div").cls("filters-button")
                    .text("Testy").onclick = function() {
                service.load("./$test?test.LogObjectTest", true);
            };
        }

        //----------------------------------------------------------------------
        var tag = filter.body.tag("div");
        tag.style.paddingLeft = "8px";
        tag.tag("label").text("Logów na str.: ");
        var edt = tag.tag("input");
        edt.type = "number";
        edt.setAttribute("value", options.maxCount);
        edt.style.width = "60px";
        edt.style.margin = "2px";

        edt.onchange = function(e) {
            var val = parseInt(e.currentTarget.value);
            if (val < 10)
                val = 10;
            if (val > 3000)
                val = 3000;
            options.maxCount = val;
            socket.init();
        };
        //----------------------------------------------------------------------

        var tbl = filter.body.tag("table");
        tbl.className = "btnsZoom";
        var tr = tbl.tag("tr");

        var btnPause = tr.tag("td");
        btnPause.title = "Pauza";
        btnPause.text("||").onclick = function() {
            paused = !paused;
            btnPause.style.backgroundColor = paused ? "#dd0" : null;
            btnPause.style.color = paused ? "black" : null;
            if (!paused)
                socket.init();
        };

        var btnZoomOut = tr.tag("td");
        btnZoomOut.title = "Zmniejsz rozmiar czcionki";
        btnZoomOut.text("-").onclick = function() {
            btnZoomClick(false);
        };

        var btnZoomIn = tr.tag("td");
        btnZoomIn.title = "Zwiększ rozmiar czcionki";
        btnZoomIn.text("+").onclick = function() {
            btnZoomClick(true);
        };

        //--------------------------------------------

        filters.headers = filter = new Filter("headers");
        filter.nonEmpty = false;
        filter.addCheckBox("id", "Id", options.headers);
        filter.addCheckBox("dte", "Data", options.headers);
        filter.addCheckBox("tag", "Tag", options.headers);
        filter.body.tag("div").className = "filter-separator";
        filter.addCheckBox("src", "Źródło", options.headers);
        filter.addCheckBox("adr", "Adres", options.headers);
        filter.addCheckBox("dev", "Urządzenie / UA", options.headers);
        filter.addCheckBox("prc", "ID procesu", options.headers);
        filter.addCheckBox("thr", "ID wątku", options.headers);
        filter.addCheckBox("usr", "Użytkownik", options.headers);

        filters.marks = filter = new Filter("marks");
        filter.nonEmpty = false;
        filter.addCheckBox("src", "Źródło", options.marks);
        filter.addCheckBox("adr", "Adres", options.marks);
        filter.addCheckBox("dev", "Urządzenie / UA", options.marks);
        filter.addCheckBox("prc", "ID procesu", options.marks);
        filter.addCheckBox("thr", "ID wątku", options.marks);
        filter.addCheckBox("usr", "Użytkownik", options.marks);
        filter.addCheckBox("ses", "Sesja", options.marks);
        filter.addCheckBox("req", "Żądanie", options.marks);

        //   pnlLeft.tag("div").className = "filter-separator";
        filters.sources = new Filter("sources");
        //    pnlLeft.tag("div").className = "filter-separator";
        filters.kinds = filter = new Filter("kinds");

        filters.tags = new Filter("tags");
        filters.addresses = new Filter("addresses");
        filters.devices = new Filter("devices");
        filters.versions = new Filter("versions");
        filters.users = new Filter("users");


        filters.others = new Filter("others");

        var tbl = filters.others.body.tag("table");

        var tr = tbl.tag("tr");
        var lbl = tr.tag("td").tag("label");

        filters.others.cbId = lbl.tag("input");
        filters.others.cbId.type = "checkbox";

        lbl.tag("span").text("ID:");

        var edt = filters.others.edtId = tr.tag("td").tag("input");
        edt.type = "number";
        edt.disabled = true;
        edt.cls("filters-others-edit");

        filters.others.cbId.onchange = function() {
            filters.others.edtId.disabled = !filters.others.cbId.checked;
            if (!filters.others.cbId.checked)
                socket.init();
        };

        filters.others.edtId.onchange = function() {
            if (filters.others.cbId.checked && filters.others.edtId.value)
                socket.init();
        };

        tr = tbl.tag("tr");
        lbl = tr.tag("td").tag("label");

        filters.others.cbDate = lbl.tag("input");
        filters.others.cbDate.type = "checkbox";

        lbl.tag("span").text("Data:");

        var edt = filters.others.edtDate = tr.tag("td").tag("input");
        edt.type = "text";
        edt.disabled = true;
        edt.cls("filters-others-edit");

        $(edt).datetimepicker({
            yearOffset: 0,
            lang: 'pl',
            timepicker: false,
            format: 'Y-m-d',
            formatDate: 'Y-m-d',
            //  minDate: '-1970/01/02', // yesterday is minimum date
            maxDate: '+1970-01-01' // and tommorow is maximum date calendar
        });

        tr = tbl.tag("tr");
        tr.tag("td");

        var edt = filters.others.edtTime = tr.tag("td").tag("input");
        edt.type = "text";
        edt.disabled = true;
        edt.cls("filters-others-edit");

        $(edt).datetimepicker({
            datepicker: false,
            lang: 'pl',
            format: 'H:i:s',
            step: 15
        });


        filters.others.edtDate.onchange = filters.others.edtTime.onchange = function() {
            if (filters.others.cbDate.checked
                    && filters.others.edtDate.value
                    && filters.others.edtTime.value)
                socket.init();
        };


        filters.others.cbDate.onchange = function() {

            filters.others.edtDate.disabled = !filters.others.cbDate.checked;
            filters.others.edtTime.disabled = !filters.others.cbDate.checked;

            if (!filters.others.cbDate.checked)
                socket.init();
        };
    });
}

//var reInitTimeout;

Filters.prototype.build = function(data) {

    var add = function(name) {
        var filter = filters[name];
        filter.items = [];
        filter.body.innerHTML = "";
        var hasError = false;
        var oneOrMoreChecked = false;

        for (var i = 0; i < data[name].length; i++) {
            var it = data[name][i];
            var item = filter.addCheckBox(it[0], it[0], it[2]);
            if (it[3])
                item.label.title = it[3];

            oneOrMoreChecked |= (item.checked);

            item.count = it[1];
            item.counter.text("[" + item.count + "]");
        }

        if (!oneOrMoreChecked) {
            options[name] = [];
            /* clearTimeout(reInitTimeout);
             reInitTimeout = setTimeout(function() {
             socket.init();
             }, 10);*/
        }
        filter.header.style.color = hasError ? "red" : null;
        filter.refreshState();
    };

    add("kinds");
    add("sources");
    add("tags");
    add("addresses");
    add("devices");
    add("versions");
    add("users");

    for (var i = 0; i < filters.kinds.items.length; i++) {
        var item = filters.kinds.items[i];
        item.label.text(names.kinds[item.name]);
    }

};


function FilterItem() {
    this.tag;
    this.name;
    this.caption;
    this.label;
    this.counter;
    this.filter;
    this.count = 0;
}

FilterItem.prototype.refresh = function() {
    this.tag.setAttribute("checked", this.checked ? "true" : "false");
};



function Filter(name) {
    filters.list.push(this);
    this.group;
    this.header;
    this.sel;  // zaznacz/odznacz wszystko
    this.body;
    this.name = name;
    var filter = this;

    this.items = new Array();
    this.edits = new Array();
    this.nonEmpty = true;


    this.group = pnlLeft.tag("div");
    this.group.className = "filter-group";
    this.group.id = "f_" + name;
    this.group.filter = this;


    this.header = this.group.tag("div");
    this.header.className = "filter-header";
    this.header.setAttribute("name", name);
    this.header.tag("label").text(names.filters[name]);

    this.sel = this.header.tag("label");
    this.sel.text("✔");
    this.sel.className = "sel";
    this.sel.onclick = function() {
        for (var i = 0; i < filter.items.length; i++) {
            filter.items[i].checked = true;
        }
        filter.onChange();
    };

    this.body = this.group.tag("div");
    this.body.className = "filter-body";
    this.header.body = this.body;
    this.body.style.display = options.expanded.contains(this.name) ? "block" : "none";

    this.header.onclick = function(e) {
        if (e && e.target && e.target.className === "sel")
            return;

        resizeFiltersPanel();

        $(this.body).slideToggle(200, function() {
            var vis = filter.body.style.display !== "none";
            if (vis && !options.expanded.contains(filter.name))
                options.expanded.push(filter.name);
            if (!vis && options.expanded.contains(filter.name))
                options.expanded.remove(filter.name);
            resizeFiltersPanel();
        });

    };
}


Filter.prototype.refreshState = function() {
    if (this.nonEmpty) {
        var changed = false;
        for (var j = 0; j < this.items.length; j++) {
            if (!this.items[j].checked)
                changed = true;
        }


        this.header.style.color = changed ? "yellow" : null;
        this.sel.style.display = changed ? "inline-block" : "none";
    }
};


Filter.prototype.onChange = function() {
    this.refreshState();
    var items = filters[this.name].items;
    options[this.name] = [];

    var allChecked = true;


    for (var i = 0; i < items.length; i++) {
        items[i].refresh();
        allChecked &= items[i].checked;
    }

    if (!allChecked)
        for (var i = 0; i < items.length; i++)
            if (items[i].checked)
                options[this.name].push(items[i].name);


    /*   if (all && this.name !== "headers" && this.name !== "marks")
     options[this.name] = [];
     */
    socket.init();
};

Filter.prototype.addCheckBox = function(name, caption, checked) {
    if (checked instanceof Array) {
        checked = (this.nonEmpty && checked.length === 0) || checked.contains(name);
    }

    var filter = this;

    var item = new FilterItem();
    filter.items.push(item);
    item.name = name;
    item.caption = caption;

    item.tag = this.body.tag("div");
    item.tag.className = "item";

    item.checked = checked;



    item.refresh();

    item.tag.oncontextmenu = function() {
        return false;
    };
    item.tag.onmousedown = function(e) {

        switch (e.button) {
            case 2:// prawy
                for (var i = 0; i < filter.items.length; i++)
                    filter.items[i].checked = false;
                item.checked = true;
                break;
            case 1:// srodkowy
                for (var i = 0; i < filter.items.length; i++)
                    filter.items[i].checked = !filter.items[i].checked;
                break;
            case 0: // lewy
                item.checked = !item.checked;
                break;
        }
        filter.sel.style.display = null;

        if (filter.nonEmpty) {
            var chk = false;
            for (var i = 0; i < filter.items.length; i++) {
                if (filter.items[i].checked)
                    chk = true;
            }
            if (!chk)
                item.checked = true;
        }

        item.refresh();
        filter.onChange();
        return true;
    };

    item.label = item.tag.tag("label");

    if (caption.trim() === "")
        caption = "< brak >";

    var fullName = caption;

    if (caption.length > 32) {
        caption = caption.substring(0, 31).trim() + "…";
    }

    item.label.text(caption);
    if (caption !== fullName)
        item.label.title = fullName;

    item.counter = item.tag.tag("label");
    item.counter.className = "counter";

    return item;
};



function resizeFiltersPanel() {

}


function btnZoomClick(zoom) {
    beginUpdateConsole(false);
    try {
        var z1 = options.fontSize;

        if (zoom === true)
            options.fontSize += options.fontSize * 0.1;
        if (zoom === false)
            options.fontSize -= options.fontSize * 0.1;

        options.fontSize = options.fontSize.round(2, false);

        if (options.fontSize < 6)
            options.fontSize = 6;
        if (options.fontSize > 48)
            options.fontSize = 48;

        updateCssRule("#content", "font-size", options.fontSize + "pt");

    } finally {
        endUpdateConsole();
    }
}

