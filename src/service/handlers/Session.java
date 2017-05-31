package service.handlers;

import archive._old.Filters;
import com.servlet.handlers.*;
import com.servlet.requests.HttpRequest;
import com.servlet.wrappers.SessionChannel;

/**
 * Miłosz Ziernik 2013/11/02
 */
public class Session extends BaseSession {
    
    public final Filters filters = new Filters();

    public Session(SessionChannel ses, HttpRequest request) throws Exception {
        super(ses, request);
    }

}
