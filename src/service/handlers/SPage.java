package service.handlers;

import com.servlet.handlers.Page;
import pages.Common;

/**
 *
 * @author admin
 */
public abstract class SPage extends Page {

    public final UserConfig userConfig;
    //public final SDatabase db;
    public final UserData user;

    public final Session session;

    // public final SSession session;
    protected SPage() {
        super();
        userConfig = (UserConfig) super.userConfig;
        user = (UserData) super.user;
        session = (Session) super.session;
    }

    @Override
    protected boolean onException(Throwable ex, int code) {
        Common.addInternalError(ex);
        return false;
    }

}
