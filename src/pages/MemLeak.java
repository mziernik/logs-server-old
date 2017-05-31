package pages;

import com.servlet.interfaces.Endpoint;
import logs.Logs;
import service.handlers.SPage;

@Endpoint(url = "memleak")
public class MemLeak extends SPage {

    @Override
    protected void onRequest() throws Exception {
        Logs.all.clear();
        Logs.allKinds.clear();
        Logs.allSources.clear();
        Logs.sources.clear();
        Logs.idxUids.clear();
        System.gc();
    }

}
