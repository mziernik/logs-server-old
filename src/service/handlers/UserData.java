package service.handlers;

import com.servlet.handlers.BaseSession;
import com.servlet.users.*;
import java.util.*;

/**
 * Miłosz Ziernik
 * 2013/06/13 
 */
public class UserData extends BaseUserData {

    public UserData(BaseUsersHandler handler) {
        super(handler);
    }
    
    //   public final List<String> archiveTables = new LinkedList<>();
    @UserDataField(title = "Imię", required = true)
    public String firstname;
    @UserDataField(title = "Nazwisko", required = true)
    public String lastname;
    @UserDataField(title = "Adres e-mail", required = true, htmlInputType = "email")
    public String email;

    @UserDataField(group = "DropBox", title = "Login")
    public String dropBoxUsername;
    @UserDataField(group = "DropBox", title = "Hasło")
    public String dropBoxpassword;

}
