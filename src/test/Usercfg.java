package test;

import com.servlet.handlers.Page;
import com.servlet.interfaces.Endpoint;
import service.handlers.UserConfig;

@Endpoint(url = "user-config")
public class Usercfg extends Page {

    @Override
    protected void onRequest() throws Exception {

        UserConfig cfg = (UserConfig) user.config();

        cfg.filterScript = "abc";

        cfg.save();

    }

}
