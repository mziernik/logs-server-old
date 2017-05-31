package test;

import com.Resources;
import logs.TLog;
import logs.Logs;
import com.utils.collections.Strings;
import com.mlogger.LogElement;
import com.mlogger.Log;
import com.mlogger.LogKind;
import com.mlogger.DataType;
import com.context.AppContext;
import com.servlet.handlers.*;
import com.servlet.interfaces.*;
import java.io.*;
import java.net.SocketAddress;
import java.util.*;
import java.util.logging.*;
import com.mlogger.handlers.UdpHandler;
import servers.UDP;

/**
 * Miłosz Ziernik 2014/01/28
 */
@ITestClass(name = "MLOGGER")
public class MLoggerTest extends TestClass {

    private void method() {
        method();
    }

    public void stackOverflow() throws InterruptedException {
        try {
            method();
        } catch (Throwable e) {
            Log in = new Log(LogKind.error);
            in.setErrorStackTrace(e);
            in.value.value = "Stack Overflow";
            send(in);
        }

    }

    public void error() throws InterruptedException {
        Log in = new Log(LogKind.error);

        try {
            throw new RuntimeException("Błąd RuntimeException");
        } catch (Exception e) {
            try {
                throw new IOException("Błąd IO", e);
            } catch (Exception e1) {
                try {
                    throw new Error(e1);
                } catch (Throwable e2) {
                    in.setErrorStackTrace(e2);
                }
            }
        }

        sendAndGet(in);

    }

    public void przepisywanie_danych_bez_wygasania() throws InterruptedException, Exception {
        test1(false);
    }

    public void przepisywanie_danych_z_wygasaniem() throws InterruptedException, Exception {
        test1(true);
    }

    public void parsery() throws Exception {
        Log log = new Log(LogKind.info);
        log.handlers.add(new CallbackHandler());

        log.value("--- test parserów -----");

        String sXml = Resources.getString("META-INF/resources/xml_compact.xml");
        log.data("---- XML ------ ", "Początek -> " + sXml + "  | Koniec");

        String sJson = Resources.getString("META-INF/resources/json_compact.json");
        log.data("---- JSON ------ ", "Początek -> " + sJson + "  -> Koniec");

        log.data("------ URL --------", "GET http://10.0.2.111/cgi-bin/zycoo.cgi"
                + "?request=%7B%22request%22%3A%22GET_EXTENS%22%2C%22arg%22%3Anull"
                + "%2C%22auth%22%3A%7B%22name%22%3A%22pawel%22%2C%22pass%22%3A%22"
                + "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3%22%2C%22admin%22%3Atrue%7D%7D HTTP/1.1\n");

        log.data("--------- BASE64 ------------------",
                "emHFvMOzxYLEhyBnxJnFm2zEhSBqYcW6xYQgWkHFu8OTxYHEhiBHxJjFmkzEhCBKQcW5xYM=");

        log.data("-------------- HEX ---------------",
                "7a61c5bcc3b3c582c4872067c499c59b6cc485206a61c5bac584205a41c5bbc393c581c4862047c498c59a4cc484204a41c5b9c583");

        sendAndGet(log);
    }

    public void slf4j() {

        //    org.slf4j.Logger logger = LoggerFactory.getLogger("");
        //  logger.error("sdfsdg");
    }

    private void test1(boolean waitFor) throws InterruptedException, Exception {
        TLog out;

        String uid = "unikalny UID " + UUID.randomUUID();

        {
            Log in = new Log(LogKind.info);
            in.handlers.add(new CallbackHandler());
            in.handlers.clear();

            in.expireDatabase = 0;
            in.expireConsole = waitFor ? 1 : null;
            in.source = "Test integracyjny";
            in.version = "Wersja beta";

            in.tag("tag 1", "tag 2", "tag 2");
            in.value("ZAŻÓŁĆ GĘŚLĄ JAŹŃ zaóżłć gęślą jaźń");
            in.details("Szczegóły xxxxxxxxxxxxxx");
            in.comment("// komentarz");
            in.address("127.1.1.1", "127.1.1.1", "192.168.1.1", "255.255.255.255");
            in.user("Użytkownik");
            //  in.device("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36");

            in.device("Linux i686 3.13-1-686-pae #1 SMP Debian 3.13.10-1 (2014-04-15)");
            in.device("Dalvik/1.4.0 (Linux; U; Android 2.3.5; Light Tab 2 Build/GINGERBREAD)");

            in.color = "yellow";
            in.background("#444");
            in.flags.add("flaga 1");
            in.flags.add("flaga 2");

            in.processId = 1254l;
            in.threadId = 57835343l;
            in.threadName = "Nazwa wątku";
            in.threadPriority = -1;

            //    in.uid = uid;
            in.url("http://1", "ftp://aaa/bbb");

            in.request = "żądanie_22342345345345";
            in.session = "sesja_345934058934";
            in.instance = "instancja_946854836455";

            in.data("Nagłówek 1", "kdfjgi340gjsdogi");
            in.data("Nagłówek 2", "sdafkasdfkjas", DataType.html);

            in.attribute("atrybut bez grupy i nazwy");
            in.attribute("atr_nazwa_1", "atr_wartość_1");
            in.attribute("atr_grupa", "atr_nazwa_2", "atr_wartość_2");

            out = sendAndGet(in);
        }
        //-------------------------------------------------------------------------
        {
            assertEquals("expire", out.expireConsole, waitFor ? 1 : null);
            assertEquals("source", out.source, "Test integracyjny");
            assertEquals("version", "Wersja beta");
            assertEquals("kind", out.kind, LogKind.info);
            assertEquals("tags", out.tags, new Strings("tag 1", "tag 2"));
            assertEquals("value", out.value.value, "ZAŻÓŁĆ GĘŚLĄ JAŹŃ zaóżłć gęślą jaźń");
            assertEquals("details", out.details.value, "Szczegóły xxxxxxxxxxxxxx");
            assertEquals("comment", out.comment, "// komentarz");

            Strings addr = new Strings(out.addresses);
            // 2 adresy 127.1.1.1 powinny sie nalozyc i dac jeden, na koncu powinien zostac dopisany 4 adres wyjsciowy
            assertBoolean("address", addr.size() == 4,
                    addr.get(0).equals("127.1.1.1"),
                    addr.get(1).equals("192.168.1.1"),
                    addr.get(2).equals("255.255.255.255")
            );

            assertEquals("user", out.user, "Użytkownik");
            assertEquals("deviceFull", out.deviceFull, "Mozilla/5.0 (Windows NT 6.1; WOW64) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36");

            assertEquals("device", out.device, "Chrome 34, Windows 7");

            assertEquals("color", out.color, "yellow");
            assertEquals("background", out.background, "#444");
            assertEquals("flags", out.flags, new Strings("flaga 1", "flaga 2"));

            assertEquals("processId", out.processId, 1254l);
            assertEquals("threadId", out.threadId, 57835343l);
            assertEquals("threadName", out.threadName, "Nazwa wątku");
            assertEquals("threadPriority", out.threadPriority, -1);
            assertEquals("uid", out.uid, uid);
            assertEquals("url", out.urls, new Strings("http://1", "ftp://aaa/bbb"));

            assertEquals("request", out.request, "żądanie_22342345345345");
            assertEquals("session", out.session, "sesja_345934058934");
            assertEquals("instance", out.instance, "instancja_946854836455");

            {
                Iterator<LogElement.DataObj> dit = out.data.iterator();
                LogElement.DataObj d1 = dit.next();
                LogElement.DataObj d2 = dit.next();

                assertBoolean("data",
                        out.data.size() == 2,
                        d1.name.equals("Nagłówek 1"),
                        d1.value.equals("kdfjgi340gjsdogi"),
                        d1.type == DataType.text,
                        d2.name.equals("Nagłówek 2"),
                        d2.value.equals("sdafkasdfkjas"),
                        d2.type == DataType.html
                );
            }

            {
                assertEquals("attributes groups", out.attributes.size(), 2);

                LogElement.DataPairs attrs = out.attributes.get("");

                Iterator<LogElement.DataPair> it = attrs.iterator();
                LogElement.DataPair d1 = it.next();
                LogElement.DataPair d2 = it.next();

                assertBoolean("attributes 1",
                        attrs.size() == 2,
                        d1.name.equals(""),
                        d1.value.equals("atrybut bez grupy i nazwy"),
                        d2.name.equals("atr_nazwa_1"),
                        d2.value.equals("atr_wartość_1")
                );

                attrs = out.attributes.get("atr_grupa");
                it = attrs.iterator();
                d1 = it.next();

                assertBoolean("attributes 1",
                        attrs.size() == 1,
                        d1.name.equals("atr_nazwa_2"),
                        d1.value.equals("atr_wartość_2")
                );

            }

            if (!waitFor)
                return;

            int cnt = 0;
            while (Logs.all.contains(out)) {
                Thread.sleep(100);
                ++cnt;
                if (cnt > 30)
                    throw new Exception("Upłynął limit czasu oczekiwania na wygaśnięcie logu");
            }

        }
    }

    @ITestMethod
    public void csv() throws InterruptedException {
        Log in = new Log(LogKind.debug);
        in.handlers.add(new CallbackHandler());

        in.value("CSV");
        in.data("CSV", "Kowalski;Jan;Kłodzko\n"
                + "Nowak;Zenon;Szczecin\n"
                + "Brzęczyszczykiewicz;Grzegorz;Łękołody");
        TLog out = sendAndGet(in);

    }

    @ITestMethod
    public void csv2() throws InterruptedException {
        Log in = new Log(LogKind.debug);
        in.handlers.add(new CallbackHandler());
        in.value("CSV");
        in.data("CSV", "\"settings_key\",\"settings_value\",\"settings_keys_description\",\"settings_platforms_id\",\"settings_groups_name\"\n"
                + "\"SMTP_USER\",\"raportyebooki\",\"\",\"1\",\"NOWE PARAMETRY\"\n"
                + "\"STRONICOWANIE_SEPARATOR\",\" \",\"\",,\"NOWE PARAMETRY\"\n"
                + "\"UPLOAD_PATH\",\"/var/www/demo_platforma/admin/upload\",\"\",\"1\",\"NOWE PARAMETRY\"\n"
                + "\"UPLOAD_PATH\",\"/var/www/demo_platforma/admin/upload\",\"\",,\"NOWE PARAMETRY\"\n"
                + "\"TEMP_DIRECTORY\",\"/var/www/tmp/\",\"/var/tmp/\",,\"NOWE PARAMETRY\"\n"
                + "\"GLOWNY_SKLEP_ID\",\"3\",\"\",,\"NOWE PARAMETRY\"\n"
                + "\"TRANSPORT_VAT_ID\",\"4\",\"ID stawki VAT na koszt dostawy\",\"1\",\"NOWE PARAMETRY\"\n"
                + "\"KOSZTYOPERACYJNE_VAT_ID\",\"4\",\"ID stawki VAT na koszty operacyjne\",\"1\",\"NOWE PARAMETRY\"\n"
                + "\"CSK_IU_USER\",\"27764111-C248-4396-B999-9FE4A5DA4D5A\",\"\",,\"NOWE PARAMETRY\"\n"
                + "\"CSK_VERSION\",\"1.1.0.0\",\"\",\"1\",\"NOWE PARAMETRY\"\n"
                + "");
        TLog out = sendAndGet(in);
    }

    @ITestMethod
    public void empty() throws InterruptedException, Exception {
        Log in = new Log(LogKind.event);
        TLog out = sendAndGet(in);
    }

    @ITestMethod(name = "Niekompletne logi")
    public void warning() throws InterruptedException, Exception {
        final String fieldName = "test prop";
        /*
         String field_value = Utils.randomId(20);
         Log in = new Log(null);
         in.source = null;
         in.date = null;
         in.property(fieldName, field_value);
         send(in);
         TLog log = getByDetailsKeyword(field_value);
         if (!(log instanceof InternalLogObject))
         throw new Exception("Nieprawidłowy typ logu");
         if (log.value.isEmpty() || !log.value.value.contains("Nieprawidłowa wartość knd")
         || !log.value.value.contains("request, debug, event, log, warning, error, query"))
         throw new Exception("Nieprawidłowa odpowiedź");
         // -------------------------------------------------------
         field_value = Utils.randomId(20);
         in = new Log(LogKind.debug);
         in.source = null;
         in.date = null;
         in.property(fieldName, field_value);
         send(in);
         log = getByDetailsKeyword(field_value);
         if (!(log instanceof InternalLogObject))
         throw new Exception("Nieprawidłowy typ logu");
         if (log.value.isEmpty() || !log.value.value.contains("Nie znaleziono wartości src"))
         throw new Exception("Nieprawidłowa odpowiedź");
         // -------------------------------------------------------
         field_value = Utils.randomId(20);
         in = new Log(LogKind.debug);
         in.source = "Moduł testowy";
         in.date = null;
         in.property(fieldName, field_value);
         send(in);
         log = getByDetailsKeyword(field_value);
         if (!(log instanceof InternalLogObject))
         throw new Exception("Nieprawidłowy typ logu");
         if (log.value.isEmpty() || !log.value.value.contains("Nie znaleziono wartości dte"))
         throw new Exception("Nieprawidłowa odpowiedź");
         */
    }

    @ITestMethod
    public void test1() throws InterruptedException, Exception {
        Log in = new Log(LogKind.event);
        in.handlers.add(new CallbackHandler());
        in.value("wartość logu")
                .tag("< tag >")
                .details("Szczególy logu")
                .comment("/* komentarz */")
                .user("Imię i Nazwisko")
                .address("8.8.8.8:1234")
                .device("Komputer PC");

        in.source = "Moduł testowy";
        in.url("http://adres/url?sciezka&parametry");
        in.version = "1.2.3 beta";
        in.processId = 324768l;
        in.threadId = 13437083l;
        in.threadName = "Bieżący wątek";
        in.background = "#333";
        in.color = "silver";

        in.errorStack.add("metoda_3 (plik3:452)");
        in.errorStack.add("metoda_2");
        in.errorStack.add("metoda_1 (plik:12)");

        in.callStack.add("terminate (unit:432)");
        in.callStack.add("main[] (root:0)");

        in.instance = "Identyfikator instancji";
        in.request = "Identyfikator żądania";
        in.session = "Identyfikator sesji";

        in.attribute("Atrybut bez nazwy")
                .attribute("null", null)
                .attribute("empty", "")
                .attribute("space", "       ")
                .attribute("Nazwa atrybutu", "Wartość atrybutu");

        for (int i = 1; i < 5; i++)
            in.attribute("Numer", i);

        in.attribute("REQUEST", "Method", "POST")
                .attribute("REQUEST", "Connection", "keep-alive")
                .attribute("REQUEST", "Connection", "keep-alive")
                .attribute("REQUEST", "User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.76 Safari/537.36");

        in.attribute("RESPONSE", "Content-Type", "application/javascript")
                .attribute("RESPONSE", "Pragma", "no-cache")
                .attribute("RESPONSE", "Server", "Apache-Coyote/1.1")
                .attribute("RESPONSE", "Cache-Control", "no-cache, no-store, must-revalidate");

        in.data("pusta", null)
                .data("dana pierwsza", "Wartość pierwszej danej")
                .data("wartość liczbowa", 23423)
                .data("wartość boolean", true);

        /* StringBuilder sb = new StringBuilder();
         for (int i = 0; i < 100000; i++)
         sb.append(".");

         in.data("kropki", sb.toString());
         */
        /*    TLog out = sendAndGet(in);

         assertEquals("Kind",
         in.kind,
         out.kind,
         LogKind.event);

         assertEquals("Tag",
         in.tag,
         out.tag,
         "< tag >");

         assertEquals("Value",
         in.value,
         out.value,
         "wartość logu");

         assertEquals("Comment",
         in.comment,
         out.comment,
         " komentarz ");

         assertEquals("User",
         in.user,
         out.user,
         "Imię i Nazwisko");

         assertEquals("Address",
         in.address,
         out.address,
         "8.8.8.8:1234");

         assertEquals("Device",
         in.device,
         out.device,
         "Komputer PC");

         assertEquals("ProcessId",
         in.processId,
         out.processId,
         324768l);

         assertEquals("ThreadID",
         in.threadId,
         out.threadId,
         13437083l);

         assertEquals("Source",
         in.source,
         out.source,
         "Moduł testowy");

         assertEquals("threadName",
         in.threadName,
         out.threadName,
         "Bieżący wątek");

         assertEquals("background",
         in.background,
         out.background,
         "#333");

         assertEquals("color",
         in.color,
         out.color,
         "silver");

         assertEquals("version",
         in.version,
         out.version,
         "1.2.3 beta");

         assertEquals("url",
         in.url,
         out.url,
         "http://adres/url?sciezka&parametry");

         assertEquals("Atrybut bez nazwy",
         //  in.attributes.get(null).hasValue("Atrybut bez nazwy"),
         out.attributes.value.get(null).hasValue("Atrybut bez nazwy"),
         true);

         assertBoolean("Atrybut null 2",
         // in.attributes.hasName("null"),
         out.attributes.value.get(null).hasName("null"),
         true
         );
         assertEquals("Atrybut null 2",
         //       in.attributes.value.get(null).getValue("null"),
         out.attributes.value.get(null).getValue("null"),
         true);

         assertEquals("Data 1",
         //       in.data.value.getValue("dana pierwsza"),
         out.data.getValue("dana pierwsza"),
         "Wartość pierwszej danej");

         assertEquals("Data int",
         //    in.data.value.getValue("wartość liczbowa").toString(),
         out.data.getValue("wartość liczbowa"),
         "23423");

         assertEquals("Data bool",
         //    in.data.value.getValue("wartość boolean").toString(),
         out.data.getValue("wartość boolean"),
         "true");

         assertEquals("Data empty",
         out.data.getPair("pusta"), null);


         assertEquals("Attributes count",
         out.attributes.size(),
         3);
         */
    }

    private void send(Log log) throws InterruptedException {
        //log.options.clearProtocols();
        /*
         for (servers.UDP udp : servers.UDP.udpThreads)
         log.options.protocols.add(new UdpProtocol(udp.socket.getLocalSocketAddress()));*/
        log.handlers.add(new CallbackHandler());
        log.send();
    }

    private TLog sendAndGet(Log log) throws InterruptedException {
        send(log);

        long time = new Date().getTime();
        while (new Date().getTime() - time < 1000)
            synchronized (Logs.all) {
                Logs.all.wait(10);
                for (TLog tl : Logs.all)
                    if (log.uid.equals(tl.uid)) {
                        return tl;
                    }
            }
        throw new InterruptedException("Nie znaleziono logu " + log.uid);
    }

    private TLog getByDetailsKeyword(String value) throws InterruptedException {
        /* long time = new Date().getTime();
         while (new Date().getTime() - time < 1000)
         synchronized (Logs.all) {
         Logs.all.wait(10);
         for (TLog tl : Logs.all)
         if (!tl.details.isEmpty() && tl.details.value.contains(value))
         return tl;
         }*/
        throw new InterruptedException("Nie znaleziono logu");
    }

    public void JavaLogging() {
        Log.error(new IOException("--IOException--"));

        // Logger.getLogger("").info("------ info -------------");
        Logger.getLogger("").log(Level.SEVERE, null, new IOException("--IOException--"));
    }

    public void longErrorStack() throws InterruptedException {
        Log in = new Log(LogKind.error);
        in.value("long error stack");

        in.callStack.clear();

        in.callStack.add("sun.reflect.NativeMethodAccessorImpl.invoke0");
        in.callStack.add("sun.reflect.NativeMethodAccessorImpl.invoke (NativeMethodAccessorImpl.java:62)");
        in.callStack.add("sun.reflect.DelegatingMethodAccessorImpl.invoke (DelegatingMethodAccessorImpl.java:43)");
        in.callStack.add("java.lang.reflect.Method.invoke (Method.java:483)");
        in.callStack.add("com.servlet.handlers.BPage.doRequest (BPage.java:386)");
        in.callStack.add("");
        in.callStack.add("");
        in.callStack.add("com.servlet.MainServlet.handleRequest (MainServlet.java:240)");
        in.callStack.add("com.servlet.MainServlet.processRequest (MainServlet.java:90)");
        in.callStack.add("context.mapping.CustomHandler.handleRequest (CustomHandler.java:13)");
        in.callStack.add("context.mapping.CustomHandlerAdapter.handle (CustomHandlerAdapter.java:22)");
        in.callStack.add("org.springframework.web.servlet.DispatcherServlet.doDispatch (DispatcherServlet.java:938)");
        in.callStack.add("org.springframework.web.servlet.DispatcherServlet.doService (DispatcherServlet.java:870)");
        in.callStack.add("org.springframework.web.servlet.FrameworkServlet.processRequest (FrameworkServlet.java:961)");
        in.callStack.add("org.springframework.web.servlet.FrameworkServlet.doPost (FrameworkServlet.java:863)");
        in.callStack.add("javax.servlet.http.HttpServlet.service (HttpServlet.java:646)");
        in.callStack.add("org.springframework.web.servlet.FrameworkServlet.service (FrameworkServlet.java:837)");
        in.callStack.add("javax.servlet.http.HttpServlet.service (HttpServlet.java:727)");
        in.callStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:303)");
        in.callStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.callStack.add("org.apache.tomcat.websocket.server.WsFilter.doFilter (WsFilter.java:52)");
        in.callStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:241)");
        in.callStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.callStack.add("org.springframework.web.filter.HiddenHttpMethodFilter.doFilterInternal (HiddenHttpMethodFilter.java:77)");
        in.callStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.callStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:241)");
        in.callStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:330)");
        in.callStack.add("org.springframework.security.web.access.intercept.FilterSecurityInterceptor.invoke (FilterSecurityInterceptor.java:118)");
        in.callStack.add("org.springframework.security.web.access.intercept.FilterSecurityInterceptor.doFilter (FilterSecurityInterceptor.java:84)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.access.ExceptionTranslationFilter.doFilter (ExceptionTranslationFilter.java:113)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.session.SessionManagementFilter.doFilter (SessionManagementFilter.java:103)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.authentication.AnonymousAuthenticationFilter.doFilter (AnonymousAuthenticationFilter.java:113)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter.doFilter (SecurityContextHolderAwareRequestFilter.java:154)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.savedrequest.RequestCacheAwareFilter.doFilter (RequestCacheAwareFilter.java:45)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.session.ConcurrentSessionFilter.doFilter (ConcurrentSessionFilter.java:125)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter.doFilter (DefaultLoginPageGeneratingFilter.java:155)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter.doFilter (AbstractAuthenticationProcessingFilter.java:199)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.authentication.logout.LogoutFilter.doFilter (LogoutFilter.java:110)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.csrf.CsrfFilter.doFilterInternal (CsrfFilter.java:85)");
        in.callStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.header.HeaderWriterFilter.doFilterInternal (HeaderWriterFilter.java:57)");
        in.callStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.context.SecurityContextPersistenceFilter.doFilter (SecurityContextPersistenceFilter.java:87)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter.doFilterInternal (WebAsyncManagerIntegrationFilter.java:50)");
        in.callStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy.doFilterInternal (FilterChainProxy.java:192)");
        in.callStack.add("org.springframework.security.web.FilterChainProxy.doFilter (FilterChainProxy.java:160)");
        in.callStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:241)");
        in.callStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.callStack.add("org.apache.catalina.core.StandardWrapperValve.invoke (StandardWrapperValve.java:220)");
        in.callStack.add("org.apache.catalina.core.StandardContextValve.invoke (StandardContextValve.java:122)");
        in.callStack.add("org.apache.catalina.authenticator.AuthenticatorBase.invoke (AuthenticatorBase.java:501)");
        in.callStack.add("org.apache.catalina.valves.RemoteIpValve.invoke (RemoteIpValve.java:683)");
        in.callStack.add("org.apache.catalina.core.StandardHostValve.invoke (StandardHostValve.java:170)");
        in.callStack.add("org.apache.catalina.valves.ErrorReportValve.invoke (ErrorReportValve.java:98)");
        in.callStack.add("org.apache.catalina.core.StandardEngineValve.invoke (StandardEngineValve.java:116)");
        in.callStack.add("org.apache.catalina.connector.CoyoteAdapter.service (CoyoteAdapter.java:408)");
        in.callStack.add("org.apache.coyote.http11.AbstractHttp11Processor.process (AbstractHttp11Processor.java:1040)");
        in.callStack.add("org.apache.coyote.AbstractProtocol$AbstractConnectionHandler.process (AbstractProtocol.java:607)");
        in.callStack.add("org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun (NioEndpoint.java:1721)");
        in.callStack.add("org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.run (NioEndpoint.java:1679)");
        in.callStack.add("java.util.concurrent.ThreadPoolExecutor.runWorker (ThreadPoolExecutor.java:1142)");
        in.callStack.add("java.util.concurrent.ThreadPoolExecutor$Worker.run (ThreadPoolExecutor.java:617)");
        in.callStack.add("java.lang.Thread.run (Thread.java:744)");

        in.errorStack.add("sun.reflect.NativeMethodAccessorImpl.invoke0");
        in.errorStack.add("sun.reflect.NativeMethodAccessorImpl.invoke (NativeMethodAccessorImpl.java:62)");
        in.errorStack.add("sun.reflect.DelegatingMethodAccessorImpl.invoke (DelegatingMethodAccessorImpl.java:43)");
        in.errorStack.add("java.lang.reflect.Method.invoke (Method.java:483)");
        in.errorStack.add("com.servlet.handlers.BPage.doRequest (BPage.java:386)");
        in.errorStack.add("");
        in.errorStack.add("");
        in.errorStack.add("com.servlet.MainServlet.handleRequest (MainServlet.java:240)");
        in.errorStack.add("com.servlet.MainServlet.processRequest (MainServlet.java:90)");
        in.errorStack.add("context.mapping.CustomHandler.handleRequest (CustomHandler.java:13)");
        in.errorStack.add("context.mapping.CustomHandlerAdapter.handle (CustomHandlerAdapter.java:22)");
        in.errorStack.add("org.springframework.web.servlet.DispatcherServlet.doDispatch (DispatcherServlet.java:938)");
        in.errorStack.add("org.springframework.web.servlet.DispatcherServlet.doService (DispatcherServlet.java:870)");
        in.errorStack.add("org.springframework.web.servlet.FrameworkServlet.processRequest (FrameworkServlet.java:961)");
        in.errorStack.add("org.springframework.web.servlet.FrameworkServlet.doPost (FrameworkServlet.java:863)");
        in.errorStack.add("javax.servlet.http.HttpServlet.service (HttpServlet.java:646)");
        in.errorStack.add("org.springframework.web.servlet.FrameworkServlet.service (FrameworkServlet.java:837)");
        in.errorStack.add("javax.servlet.http.HttpServlet.service (HttpServlet.java:727)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:303)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.errorStack.add("org.apache.tomcat.websocket.server.WsFilter.doFilter (WsFilter.java:52)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:241)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.errorStack.add("org.springframework.web.filter.HiddenHttpMethodFilter.doFilterInternal (HiddenHttpMethodFilter.java:77)");
        in.errorStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:241)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:330)");
        in.errorStack.add("org.springframework.security.web.access.intercept.FilterSecurityInterceptor.invoke (FilterSecurityInterceptor.java:118)");
        in.errorStack.add("org.springframework.security.web.access.intercept.FilterSecurityInterceptor.doFilter (FilterSecurityInterceptor.java:84)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.access.ExceptionTranslationFilter.doFilter (ExceptionTranslationFilter.java:113)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.session.SessionManagementFilter.doFilter (SessionManagementFilter.java:103)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.authentication.AnonymousAuthenticationFilter.doFilter (AnonymousAuthenticationFilter.java:113)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter.doFilter (SecurityContextHolderAwareRequestFilter.java:154)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.savedrequest.RequestCacheAwareFilter.doFilter (RequestCacheAwareFilter.java:45)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.session.ConcurrentSessionFilter.doFilter (ConcurrentSessionFilter.java:125)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter.doFilter (DefaultLoginPageGeneratingFilter.java:155)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter.doFilter (AbstractAuthenticationProcessingFilter.java:199)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.authentication.logout.LogoutFilter.doFilter (LogoutFilter.java:110)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.csrf.CsrfFilter.doFilterInternal (CsrfFilter.java:85)");
        in.errorStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.header.HeaderWriterFilter.doFilterInternal (HeaderWriterFilter.java:57)");
        in.errorStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.context.SecurityContextPersistenceFilter.doFilter (SecurityContextPersistenceFilter.java:87)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter.doFilterInternal (WebAsyncManagerIntegrationFilter.java:50)");
        in.errorStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy.doFilterInternal (FilterChainProxy.java:192)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy.doFilter (FilterChainProxy.java:160)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:241)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.errorStack.add("org.apache.catalina.core.StandardWrapperValve.invoke (StandardWrapperValve.java:220)");
        in.errorStack.add("org.apache.catalina.core.StandardContextValve.invoke (StandardContextValve.java:122)");
        in.errorStack.add("org.apache.catalina.authenticator.AuthenticatorBase.invoke (AuthenticatorBase.java:501)");
        in.errorStack.add("org.apache.catalina.valves.RemoteIpValve.invoke (RemoteIpValve.java:683)");
        in.errorStack.add("org.apache.catalina.core.StandardHostValve.invoke (StandardHostValve.java:170)");
        in.errorStack.add("org.apache.catalina.valves.ErrorReportValve.invoke (ErrorReportValve.java:98)");
        in.errorStack.add("org.apache.catalina.core.StandardEngineValve.invoke (StandardEngineValve.java:116)");
        in.errorStack.add("org.apache.catalina.connector.CoyoteAdapter.service (CoyoteAdapter.java:408)");
        in.errorStack.add("org.apache.coyote.http11.AbstractHttp11Processor.process (AbstractHttp11Processor.java:1040)");
        in.errorStack.add("org.apache.coyote.AbstractProtocol$AbstractConnectionHandler.process (AbstractProtocol.java:607)");
        in.errorStack.add("org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun (NioEndpoint.java:1721)");
        in.errorStack.add("org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.run (NioEndpoint.java:1679)");
        in.errorStack.add("java.util.concurrent.ThreadPoolExecutor.runWorker (ThreadPoolExecutor.java:1142)");
        in.errorStack.add("java.util.concurrent.ThreadPoolExecutor$Worker.run (ThreadPoolExecutor.java:617)");
        in.errorStack.add("java.lang.Thread.run (Thread.java:744)");
        in.errorStack.add("Caused by: sun.reflect.DelegatingMethodAccessorImpl.invoke (DelegatingMethodAccessorImpl.java:43)");
        in.errorStack.add("java.lang.reflect.Method.invoke (Method.java:483)");
        in.errorStack.add("com.servlet.handlers.BPage.doRequest (BPage.java:386)");
        in.errorStack.add("com.servlet.MainServlet.handleRequest (MainServlet.java:240)");
        in.errorStack.add("com.servlet.MainServlet.processRequest (MainServlet.java:90)");
        in.errorStack.add("context.mapping.CustomHandler.handleRequest (CustomHandler.java:13)");
        in.errorStack.add("context.mapping.CustomHandlerAdapter.handle (CustomHandlerAdapter.java:22)");
        in.errorStack.add("org.springframework.web.servlet.DispatcherServlet.doDispatch (DispatcherServlet.java:938)");
        in.errorStack.add("org.springframework.web.servlet.DispatcherServlet.doService (DispatcherServlet.java:870)");
        in.errorStack.add("org.springframework.web.servlet.FrameworkServlet.processRequest (FrameworkServlet.java:961)");
        in.errorStack.add("org.springframework.web.servlet.FrameworkServlet.doPost (FrameworkServlet.java:863)");
        in.errorStack.add("javax.servlet.http.HttpServlet.service (HttpServlet.java:646)");
        in.errorStack.add("org.springframework.web.servlet.FrameworkServlet.service (FrameworkServlet.java:837)");
        in.errorStack.add("javax.servlet.http.HttpServlet.service (HttpServlet.java:727)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:303)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.errorStack.add("org.apache.tomcat.websocket.server.WsFilter.doFilter (WsFilter.java:52)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:241)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.errorStack.add("org.springframework.web.filter.HiddenHttpMethodFilter.doFilterInternal (HiddenHttpMethodFilter.java:77)");
        in.errorStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:241)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:330)");
        in.errorStack.add("org.springframework.security.web.access.intercept.FilterSecurityInterceptor.invoke (FilterSecurityInterceptor.java:118)");
        in.errorStack.add("org.springframework.security.web.access.intercept.FilterSecurityInterceptor.doFilter (FilterSecurityInterceptor.java:84)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.access.ExceptionTranslationFilter.doFilter (ExceptionTranslationFilter.java:113)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.session.SessionManagementFilter.doFilter (SessionManagementFilter.java:103)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.authentication.AnonymousAuthenticationFilter.doFilter (AnonymousAuthenticationFilter.java:113)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter.doFilter (SecurityContextHolderAwareRequestFilter.java:154)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.savedrequest.RequestCacheAwareFilter.doFilter (RequestCacheAwareFilter.java:45)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.session.ConcurrentSessionFilter.doFilter (ConcurrentSessionFilter.java:125)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter.doFilter (DefaultLoginPageGeneratingFilter.java:155)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter.doFilter (AbstractAuthenticationProcessingFilter.java:199)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.authentication.logout.LogoutFilter.doFilter (LogoutFilter.java:110)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.csrf.CsrfFilter.doFilterInternal (CsrfFilter.java:85)");
        in.errorStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.header.HeaderWriterFilter.doFilterInternal (HeaderWriterFilter.java:57)");
        in.errorStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.context.SecurityContextPersistenceFilter.doFilter (SecurityContextPersistenceFilter.java:87)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter.doFilterInternal (WebAsyncManagerIntegrationFilter.java:50)");
        in.errorStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy.doFilterInternal (FilterChainProxy.java:192)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy.doFilter (FilterChainProxy.java:160)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:241)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.errorStack.add("org.apache.catalina.core.StandardWrapperValve.invoke (StandardWrapperValve.java:220)");
        in.errorStack.add("org.apache.catalina.core.StandardContextValve.invoke (StandardContextValve.java:122)");
        in.errorStack.add("org.apache.catalina.authenticator.AuthenticatorBase.invoke (AuthenticatorBase.java:501)");
        in.errorStack.add("org.apache.catalina.valves.RemoteIpValve.invoke (RemoteIpValve.java:683)");
        in.errorStack.add("org.apache.catalina.core.StandardHostValve.invoke (StandardHostValve.java:170)");
        in.errorStack.add("org.apache.catalina.valves.ErrorReportValve.invoke (ErrorReportValve.java:98)");
        in.errorStack.add("org.apache.catalina.core.StandardEngineValve.invoke (StandardEngineValve.java:116)");
        in.errorStack.add("org.apache.catalina.connector.CoyoteAdapter.service (CoyoteAdapter.java:408)");
        in.errorStack.add("org.apache.coyote.http11.AbstractHttp11Processor.process (AbstractHttp11Processor.java:1040)");
        in.errorStack.add("org.apache.coyote.AbstractProtocol$AbstractConnectionHandler.process (AbstractProtocol.java:607)");
        in.errorStack.add("org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun (NioEndpoint.java:1721)");
        in.errorStack.add("org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.run (NioEndpoint.java:1679)");
        in.errorStack.add("java.util.concurrent.ThreadPoolExecutor.runWorker (ThreadPoolExecutor.java:1142)");
        in.errorStack.add("java.util.concurrent.ThreadPoolExecutor$Worker.run (ThreadPoolExecutor.java:617)");
        in.errorStack.add("java.lang.Thread.run (Thread.java:744)");
        in.errorStack.add("Caused by: org.springframework.web.servlet.FrameworkServlet.service (FrameworkServlet.java:837)");
        in.errorStack.add("javax.servlet.http.HttpServlet.service (HttpServlet.java:727)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:303)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.errorStack.add("org.apache.tomcat.websocket.server.WsFilter.doFilter (WsFilter.java:52)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:241)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.errorStack.add("org.springframework.web.filter.HiddenHttpMethodFilter.doFilterInternal (HiddenHttpMethodFilter.java:77)");
        in.errorStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:241)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:330)");
        in.errorStack.add("org.springframework.security.web.access.intercept.FilterSecurityInterceptor.invoke (FilterSecurityInterceptor.java:118)");
        in.errorStack.add("org.springframework.security.web.access.intercept.FilterSecurityInterceptor.doFilter (FilterSecurityInterceptor.java:84)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.access.ExceptionTranslationFilter.doFilter (ExceptionTranslationFilter.java:113)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.session.SessionManagementFilter.doFilter (SessionManagementFilter.java:103)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.authentication.AnonymousAuthenticationFilter.doFilter (AnonymousAuthenticationFilter.java:113)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter.doFilter (SecurityContextHolderAwareRequestFilter.java:154)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.savedrequest.RequestCacheAwareFilter.doFilter (RequestCacheAwareFilter.java:45)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.session.ConcurrentSessionFilter.doFilter (ConcurrentSessionFilter.java:125)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter.doFilter (DefaultLoginPageGeneratingFilter.java:155)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter.doFilter (AbstractAuthenticationProcessingFilter.java:199)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.authentication.logout.LogoutFilter.doFilter (LogoutFilter.java:110)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.csrf.CsrfFilter.doFilterInternal (CsrfFilter.java:85)");
        in.errorStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.header.HeaderWriterFilter.doFilterInternal (HeaderWriterFilter.java:57)");
        in.errorStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.context.SecurityContextPersistenceFilter.doFilter (SecurityContextPersistenceFilter.java:87)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter.doFilterInternal (WebAsyncManagerIntegrationFilter.java:50)");
        in.errorStack.add("org.springframework.web.filter.OncePerRequestFilter.doFilter (OncePerRequestFilter.java:108)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter (FilterChainProxy.java:342)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy.doFilterInternal (FilterChainProxy.java:192)");
        in.errorStack.add("org.springframework.security.web.FilterChainProxy.doFilter (FilterChainProxy.java:160)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.internalDoFilter (ApplicationFilterChain.java:241)");
        in.errorStack.add("org.apache.catalina.core.ApplicationFilterChain.doFilter (ApplicationFilterChain.java:208)");
        in.errorStack.add("org.apache.catalina.core.StandardWrapperValve.invoke (StandardWrapperValve.java:220)");
        in.errorStack.add("org.apache.catalina.core.StandardContextValve.invoke (StandardContextValve.java:122)");
        in.errorStack.add("org.apache.catalina.authenticator.AuthenticatorBase.invoke (AuthenticatorBase.java:501)");
        in.errorStack.add("org.apache.catalina.valves.RemoteIpValve.invoke (RemoteIpValve.java:683)");
        in.errorStack.add("org.apache.catalina.core.StandardHostValve.invoke (StandardHostValve.java:170)");
        in.errorStack.add("org.apache.catalina.valves.ErrorReportValve.invoke (ErrorReportValve.java:98)");
        in.errorStack.add("org.apache.catalina.core.StandardEngineValve.invoke (StandardEngineValve.java:116)");
        in.errorStack.add("org.apache.catalina.connector.CoyoteAdapter.service (CoyoteAdapter.java:408)");
        in.errorStack.add("org.apache.coyote.http11.AbstractHttp11Processor.process (AbstractHttp11Processor.java:1040)");
        in.errorStack.add("org.apache.coyote.AbstractProtocol$AbstractConnectionHandler.process (AbstractProtocol.java:607)");
        in.errorStack.add("org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun (NioEndpoint.java:1721)");
        in.errorStack.add("org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.run (NioEndpoint.java:1679)");
        in.errorStack.add("java.util.concurrent.ThreadPoolExecutor.runWorker (ThreadPoolExecutor.java:1142)");
        in.errorStack.add("java.util.concurrent.ThreadPoolExecutor$Worker.run (ThreadPoolExecutor.java:617)");
        in.errorStack.add("java.lang.Thread.run (Thread.java:744)");

        send(in);
    }

    public static class CallbackHandler extends UdpHandler {

        private static SocketAddress getIntf() {
            for (SocketAddress addr : UDP.interfaces)
                return addr;

            return null;
        }

        public CallbackHandler() {
            super(getIntf());
        }

    }

}
