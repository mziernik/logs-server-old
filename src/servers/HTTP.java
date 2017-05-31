package servers;

import com.exceptions.EError;
import com.io.*;
import java.io.*;


//@WebServlet(name = "HTTP Log", urlPatterns = {"/api/addLog", "/api/addLog/"}, asyncSupported = false)
public class HTTP{} /* extends HttpServlet {

    private Writer writer;

    public void write(String name, String value) throws IOException {

        Json.escape(name, writer, false);
        if (value != null) {
            writer.append(": ");
            Json.escape(value, writer, false);
        }
        writer.append("\n");
        writer.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("text/plain; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        writer = resp.getWriter();
        try {

            String ct = req.getContentType();

            byte[] data;
            try (ServletInputStream in = req.getInputStream()) {
                data = IOUtils.copy(req.getInputStream());
            }

            RawPacket packet = new RawPacket(req.getRemoteAddr(), data, LogSource.http);
            packet.process();

            if (packet.priority)
                write("confirm", packet.priorityUid.toString());

        } catch (Exception e) {
            resp.setStatus(500);
            write("error", Exceptions.toString(e));
            Log.warning(e);
        } finally {
            writer.close();
        }
    }
}*/
