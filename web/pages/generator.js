function btnSendClick() {

    localStorage.setItem("generator-code", code.getValue());


    $id("code").sendAjax("?save&count=" + $id("edtCount").value, function (http) {

    });


}

function btnResetClick() {
    if (!confirm("Czy na pewno zresetować zawartość?"))
        return;
    localStorage.removeItem("generator-code");
    location.reload();
}


addEventListener("load", function () {

    var cc = localStorage.getItem("generator-code");

    if (cc)
        code.setOption("value", cc);

    code.setOption("extraKeys", {
        "Ctrl-Enter": btnSendClick
    });
});