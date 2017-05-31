function $id(id){
    return document.getElementById(id);
}

function submitBase(){
    var txt = $id("taB").value;
    txt = Base64.encode(txt);
    $id("putFileB").value = txt;
}

function submitHex(){
    var txt = $id("taH").value;
    var res = "";
    
    for (var i = 0;  i < txt.length;  i++) {
        var s =  txt.charCodeAt(i).toString(16);
        while (s.length % 2 != 0) s = "0" + s;        
        res += s;       
    }    
    $id("putFileH").value = res;
}

