package service.handlers;

import com.servlet.handlers.BaseSession;
import com.servlet.users.BaseUserConfig;
import com.servlet.users.BaseUserData;

/**
 * Miłosz Ziernik
 * 2013/06/10 
 */
public class UserConfig extends BaseUserConfig {

    public String filterScript;

    public UserConfig(BaseUserData user) {
        super(user);
    }

}
