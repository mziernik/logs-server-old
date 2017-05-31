package pages;

import com.html.core.*;
import com.html.tags.*;
import com.servlet.interfaces.Endpoint;
import java.net.*;
import service.handlers.*;

/**
 * Miłosz Ziernik
 * 2013/06/17 
 */
@Endpoint(url = "about")
public class About extends SPage {

    @Override
    protected void onRequest() throws Exception {
        body.div("ssdsf");
        
        Table table = body.table();
        
        table.tbodyTr().setCells("aaa", 24351);
        
        Node text = body.h3().text("Interfejsy:");
        Ul ul = body.ul();
        
    /*    for (SocketAddress sa : serviceConfig.udpServer.addresses)
            ul.li().text(sa.toString());
*/
    }
}
