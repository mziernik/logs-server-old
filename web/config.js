
var tblConfig;
var editor;

addEventListener("load", function() {
    loadEditor();
    tblConfig = $id("tblConfig");
    if (!tblConfig)
        return;

    for (var i = 1; i < tblConfig.rows.length - 1; i++) {
        tblConfig.rows[i].cells[6].className = "tdRemove";
        tblConfig.rows[i].cells[6].onclick = removeConfigClick;
        tblConfig.rows[i].style.cursor = "pointer";
        tblConfig.rows[i].onclick = function(e) {
            if (e.target && e.target.getParent("td")
                    && e.target.getParent("td").cellIndex === 6)
                return;
            location.href = "?edit=" + this.getParent("tr").id;
        };
    }

});



function removeConfigClick() {
    var tr = this.getParent("tr");
    if (!tr || !confirm("Czy na pewno usunąć plik konfiguracyjny?"))
        return true;
    httpGetAsync("./config?remove=" + tr.id, function(http) {
        window.location.reload();
    });

}

function addConfigClick() {
    service.layer('config?layer', null);
}

function loadEditor() {
    var ta = $id("taXML");
    if (!ta)
        return;

    editor = CodeMirror.fromTextArea(ta, {
        mode: 'text/html',
        lineNumbers: true,
        lineWrapping: true,
        wordWrap: true,
        onCursorActivity: function() {
            editor.setLineClass(hlLine, null, null);
            hlLine = editor.setLineClass(editor.getCursor().line, null, "activeline");
            //  editor.matchHighlight("CodeMirror-matchhighlight");  
        },
        // automatyczne domykanie tagów
        extraKeys: {
            "'>'": function(cm) {
                cm.closeTag(cm, '>');
            },
            "'/'": function(cm) {
                cm.closeTag(cm, '/');
            }
        }
    });

    var hlLine = editor.setLineClass(0, "activeline");


}


function getSelectedRange() {
    return {
        from: editor.getCursor(true),
        to: editor.getCursor(false)
    };
}

function autoFormat() {
    CodeMirror.commands["selectAll"](editor);
    var range = getSelectedRange();
    editor.autoFormatRange(range.from, range.to);
}

function saveText() {

    ajax.post({
        url: "edit",
        save: true,
        file: $id("file").value,
        code: s

    }, function(resp) {
        if (resp.status != 200) {
            alertError(resp)
        }

    }, false);

}

function btnCfgSaveClick() {
    editor.save();
    var s = encodeURIComponent($id("taXML").value);

    httpPostAsync("./config?save=" + $id("cid").value, "xml=" + s,
            function(http) {
                if (http.status != 200) {
                    service.error(http);
                    return;
                }

                editor.setValue(http.responseText);

                $id("lblCfgAck").style.display = "inline";
                setTimeout(function() {
                    $id("lblCfgAck").style.display = "none";
                }, 500);
            });


}