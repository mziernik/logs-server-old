package rhino;

import com.Resources;
import com.config.engine.interfaces.Cfg;
import com.config.engine.items.CBool;
import logs.Logs;
import com.context.intf.OnContextDestroyed;
import com.context.intf.OnContextInitialized;
import com.context.AppContext;
import java.nio.charset.*;
import java.util.*;
import logs.Logs.RawPacket;
import org.mozilla.javascript.*;
import service.*;
import service.Config.CConsole;

public class PreFilter implements Runnable {

    public final static PreFilter instance = new PreFilter();
    public final static Thread thread = new Thread(instance);

    @Cfg(parent = CConsole.class, name = "Główny filtr aktywny")
    public final static CBool preScriptEnabled = new CBool("console.pre_script_enabled", false);

    static final LinkedList<RawPacket> queue = new LinkedList<>();

    @OnContextInitialized
    public static void onContextInitialized() {
        //   thread.start();
    }

    @OnContextDestroyed
    public static void onContextDestroy() {
        //   thread.interrupt();
    }

    public static void add(RawPacket packet) throws Exception {

        if (!preScriptEnabled.value()) {

            return;
        }

        synchronized (queue) {
            queue.add(packet);
        }
        synchronized (instance) {
            instance.notify();
        }

        return;
    }

    @Override
    public void run() {
        thread.setName("Rhino PreFilter");
        try {
            RhinoSandbox rhino = new RhinoSandbox();
            rhino.evaluate("prefilter.js", Resources.getString("prefilter.js"));

            NativeFunction get = (NativeFunction) rhino.get("filter");

            while (!thread.isInterrupted())
                try {

                    if (queue.isEmpty())
                        synchronized (instance) {
                            instance.wait();
                        }

                    RawPacket packet;
                    synchronized (queue) {
                        packet = queue.poll();
                    }
                    if (packet == null)
                        continue;

                    rhino.put("log", new String(packet.data, "UTF-8"));
                    rhino.put("address", packet.address);

                 //   Logs.processAsync(packet);
                } catch (InterruptedException ex) {
                    return;
                } catch (Exception e) {
                    if (AppContext.devMode())
                        e.printStackTrace();
                }
        } catch (Exception e) {
            if (AppContext.devMode())
                e.printStackTrace();
        }
    }

}
