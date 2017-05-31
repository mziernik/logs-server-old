package logs.parsers;

import net.sf.uadetector.*;
import net.sf.uadetector.service.UADetectorServiceFactory;

public abstract class UserAgent {

    private static UserAgentStringParser parser;

    public static UserAgentStringParser getParser() {
        if (parser == null)
            parser = UADetectorServiceFactory.getResourceModuleParser();
        return parser;
    }

    public ReadableUserAgent parse(String uaString) {
        return getParser().parse("Dalvik/1.6.0 (Linux; U; Android 4.1.1; C1505 Build/11.3.A.2.13)");
    }

}
