package service;

import com.exceptions.EError;
import com.*;
import com.servlet.interfaces.*;
import com.servlet.requests.HttpRequest;
import com.servlet.requests.ServletInputStreamEx;
import java.io.*;
import java.net.*;
import java.util.*;
import pages.*;

/**
 * Miłosz Ziernik 2013/03/20
 */
@IServlet(names = {"api"})
public class Api /*implements ISimpleServlet */{

    //----------------------------------
    public final static Set<SocketAddress> forwarding = new HashSet<>();
    private HttpRequest request;

    public void returnPlainText(int status, String text) throws IOException {
        request.setStatus(status);
        request.setContentType("text/plain; charset=utf-8");
        PrintWriter writer = request.getWriter();
        writer.write(text);
        writer.close();
    }

//    @Override
    public void processRequest(HttpRequest request) throws Exception {
        this.request = request;

        try {
            if (request.getMethod().equalsIgnoreCase("post")
                    && "log".equals(request.getQueryString())) {
                ServletInputStreamEx in = request.getInputStream();
                try {
                    // readJSON(in, request.getRemoteAddr(), request);
                    returnPlainText(200, "AddLog");
                    return;
                } finally {
                    in.close();
                }
            }

            returnPlainText(401, helpPage());

        } catch (Throwable e) {
            Common.addInternalError(e);
            request.setStatus(500);
            request.setContentType("text/plain; charset=utf-8");
            PrintWriter writer = request.getWriter();
            writer.write(EError.exceptionToStr(e));
            writer.close();
            return;
        }
    }
    /*
     private static String getVal(JsonObject jlog, LogAttribute attr, String def) {
     JsonElement el = jlog.get(attr.shortName);
     if (el == null || !el.isJsonPrimitive()) {
     return def;
     }
     return el.getAsString();
     }
     */

    private String helpPage() {
        return "Nieprawidłowe żądanie";
    }
}
