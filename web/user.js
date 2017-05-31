function setAlertEmail(){
    $id("dXmpp").style.display = "none";
    $id("dEmail").style.display = "block";
    layer.resize();
}

function setAlertXmpp(){
    $id("dXmpp").style.display = "block";
    $id("dEmail").style.display = "none";
    layer.resize();   
}


function pmItemClick(mi){
    var username = $id("username").value;
    
    switch (mi.id){
        case "miRecipient_Edit":
            var s =  prompt("Adres / Numer", $id("rec" + mi.menu.rid).value);
            if (!s) return;
            service.postAndReload("user?act=edit", 
                "rid=" + mi.menu.rid + "&value="  + escapeUrl(s));
            break;   
        
        case "miRecipient_Remove":
            if (!confirm("Czy na pewno usunąć \"" + $id("rec" + mi.menu.rid).value + "\"?"))
                return;
            service.postAndReload("user?act=remove","rid=" + mi.menu.rid);
            break;   
              
        case "miGroups_Edit":
            var s =  prompt("Nazwa grupy", $id("gid" + mi.menu.gid).value);
            if (!s) return;
            service.postAndReload("user?act=edit", 
                "gid=" + mi.menu.gid + "&value="  + escapeUrl(s));
            break;   
        
        case "miGroups_Remove":
            if (!confirm("Czy na pewno usunąć \"" + $id("gid" + mi.menu.gid).value + "\"?"))
                return;
            service.postAndReload("user?act=remove","gid=" + mi.menu.gid);
            break;  
        
        case "miAlerts_Edit":
            service.layer("user?act=editAlert&aid=" + mi.menu.aid);
            break;   
        
        case "miAlerts_Remove":
            if (!confirm("Czy na pewno usunąć \"" + $id("aid" + mi.menu.aid).value + "\"?"))
                return;
            service.postAndReload("user?act=remove","aid=" + mi.menu.aid);
            break;    
        
        

        case "miNewRecipient_Email":
            var s = prompt("Adres e-mail:", username + "@infover.pl");
            if (!s) return;
            service.postAndReload("user?act=edit&new&rid=1", 
                "value=" + escapeUrl("email://" +s));
            break;
        
        case "miNewRecipient_XMPP":
            var s =  prompt("Kontakt XMPP:", username + "@kolporter.com.pl");
            if (!s) return;
            service.postAndReload("user?act=edit&new&rid=1", 
                "value=" + escapeUrl("xmpp://" +s));
            break; 
        
        case "miNewRecipient_GG":
            var s = prompt("Numer GG:", "");
            if (!s) return;
            service.postAndReload("user?act=edit&new&rid=1", 
                "value=" + escapeUrl("gg://" + s));
            break;  
    
    }
}

function newGroup(){
    var s = prompt("Nazwa grupy", "");
    if (!s) return;
    service.postAndReload("user?act=edit&new&gid=1&value="  + escapeUrl(s));
}

function setEnabled(cb){
    service.postAndReload("user?act=setEnabled&state=" 
        + (cb.checked ? "true" : "false") + "&" + cb.getAttribute("param")); 
}


var draggedRID;
var draggedGRID;

window.addEventListener("load", function(){
  
    var tblR = $id("tblRecipients");
    
    if (!tblR) return;
    
    
    for (var i = 1; i < tblR.rows.length-1; i++) {
        var cell = tblR.rows[i].cells[1];                        
        cell.draggable = true;
        cell.ondragstart = function(e){
            draggedRID = this.getAttribute("rid");
        };  
    }
    
    var gridx = 0;
    
    while (true){
        var tblGR = $id("tblGroupRecipients" + gridx++); 
        if (!tblGR) 
            break;
        
        for (var i = 1; i < tblGR.rows.length-1; i++) {
            var cell = tblGR.rows[i].cells[1];                        
            cell.draggable = true;
            cell.ondragstart = function(e){
                draggedGRID = this.getAttribute("grid");
            };  
        
            document.body.ondragover = function(e){
                if (!draggedGRID) return;
                e.preventDefault(); 
            }
        
            document.body.ondrop = function(e){
                if (!draggedGRID) return;
                e.preventDefault(); 
            
                if (confirm("Czy na pewno usunąć \"" + $id("grid" + draggedGRID).value + "\"?"))            
                    service.postAndReload("user?act=remove&grid=" + draggedGRID);
            
                draggedGRID = null;
            }     
        }
        
        
        tblGR.ondragover = function(e){
            if (draggedGRID) return;
            e.preventDefault();  
        }
    
        tblGR.ondrop = function(e){
            e.preventDefault(); 
            if (draggedGRID) {
                draggedGRID = null;
                return false;               
            }
            service.postAndReload("user?act=addGroupRecipient&gid="
                + this.getAttribute("gid") + "&rid=" + draggedRID);
        }
    }
    
    
   
}, false);