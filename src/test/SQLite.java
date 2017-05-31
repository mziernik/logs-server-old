package test;

import com.database.service.ServiceDB;
import com.Utils;
import com.database.QueryRows;
import com.servlet.handlers.*;
import com.servlet.interfaces.*;
import com.mlogger.Log;

@ITestClass(name = "SQLite")
public class SQLite extends TestClass {

    public void aaaa(){
        
        Log.debug("aaaa").comment("------------");
        
    }
    
    
    @ITestMethod
    public void test1() throws Exception {

        ServiceDB db = new ServiceDB();

        QueryRows sel = db.execute("SELECT * FROM users u\n"
                + "LEFT JOIN  user_attributes ua on ua.users_id = u.users_id");

    }

}
