package test;

import static com.resources.ResFile.*;

import com.servlet.handlers.*;
import com.servlet.interfaces.*;
import com.servlet.javascript.Standby;
import com.servlet.javascript.events.*;
import com.mlogger.Log;

@Endpoint(url = "jstest")
public class JsTest extends Page {

    @Override
    protected void onRequest() throws Exception {

        link(utilsJs, serviceJs, serviceCss, layerJs, layerCss,
                jQuery, popupJs, popupCss);

        final String reqid = request.requestId;
        body.button()
                .text("hjgkjhgjhg");

    }

}
