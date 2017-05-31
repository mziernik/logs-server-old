package pages;

import com.exceptions.Http404FileNotFoundException;
import com.servlet.handlers.Page;
import com.servlet.interfaces.Endpoint;

@Endpoint(url = "logi")
public class TmpLogi extends Page {

    @Override
    protected void onRequest() throws Exception {
        if (request.url.contains("tomcat.eclicto.pl"))
            sendRedirect("http://logi.eclicto.pl");
        else
            throw new Http404FileNotFoundException(this);
    }

}
