package test;

import cloud.mega.MegaFile;
import cloud.mega.MegaHandler;
import com.servlet.handlers.TestClass;
import java.io.IOException;
import java.util.ArrayList;

public class Mega extends TestClass {

    public void login() throws IOException {

        MegaHandler mega = new MegaHandler("jmegatest@migmail.pl", "haslo");

        int login = mega.login();

        String _user = mega.get_user();

        ArrayList<MegaFile> files = mega.get_files();

    }

}
