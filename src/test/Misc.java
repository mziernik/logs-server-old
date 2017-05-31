package test;

import com.context.intf.OnContextInitialized;
import com.mlogger.Log;
import com.mlogger.MHandler;
import com.context.AppContext;
import com.servlet.handlers.*;
import java.util.*;
import java.util.logging.*;

public class Misc extends TestClass {

    public void saveUserConfig() throws Exception {
  //      AppContext.sessions.peekFirst().user.config.save();
    }

    public void logger() {
        java.util.logging.Logger.getLogger(Misc.class.getName()).log(Level.INFO, "aaaaaaaaaaaaaaaaa");
    }

    @OnContextInitialized
    public static void ctx() {

        Handler[] handlers = Logger.getLogger("").getHandlers();

        Properties props = System.getProperties();

        ClassLoader cl1 = MHandler.class.getClassLoader();

        Object sl = props.get("$mlogger.SLF4JLogger");
        Log.debug(null, sl, sl);

    }

}
