package service.handlers;

import com.servlet.users.*;
import javax.naming.AuthenticationException;
import javax.naming.NamingException;

public class Ldap extends BaseLdap {

    public Ldap() throws AuthenticationException {
    }

    public Ldap(String url, String domain, String username, String password) throws AuthenticationException {
        super(url, domain, username, password);
    }

    @Override
    public void getUserInfo(BaseUserData user) throws AuthenticationException, NamingException {
        super.getUserInfo(user);
        String name = user.ldapAttributes.get("cn");
        user.email = user.ldapAttributes.get("mail");
        if (name != null && name.contains(" ")) {
            user.firstname = name.substring(0, name.indexOf(" "));
            user.lastname = name.substring(name.indexOf(" ") + 1, name.length());
        }
    }

}
