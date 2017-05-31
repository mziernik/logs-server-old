package servers;

import com.threads.*;
import java.util.*;

public class LogServer extends TThread {

    protected static final Set<LogServer> servers = new LinkedHashSet<>();

    public LogServer(String name) {
        super(name);
        servers.add(this);
    }

    @Override
    protected void execute() throws Exception {

    }

}
