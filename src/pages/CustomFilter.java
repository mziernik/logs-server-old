package pages;

import com.resources.ResFile;
import com.StrUtils;
import com.html.js.Call;
import com.html.tags.*;
import com.servlet.interfaces.Endpoint;
import service.handlers.*;

/**
 * Miłosz Ziernik 2013/08/02
 */
@Endpoint(url = "custom_filter")
public class CustomFilter extends SPage {

    @Override
    protected void onRequest() throws Exception {
        ResFile.utilsJs.link(head);
        ResFile.serviceJs.link(head);
        ResFile.layerCss.link(head);
        ResFile.layerJs.link(head);

        head.linkCSS("./com/codemirror/codemirror.css");
        head.linkJavaScript("./com/codemirror/codemirror.js");

        TextArea ta = body.textArea();
        ta.id("val");
        ta.name("val");
        ta.style().display("none");
        ta.text("function(){\n};");

        body.script("var editor;\n"
                + "window.addEventListener('load', function() {\n"
                + "    editor = CodeMirror.fromTextArea($id('val'), {\n"
                + "        mode: 'text/javascript',\n"
                + "        styleActiveLine: true,\n"
                + "        autoCloseTags: true,\n"
                + "        lineNumbers: true,\n"
                + "        lineWrapping: true\n"
                + "    });\n"
                + "  });"
                + "\n"
                + "function editSubmit() {\n"
                + "    editor.save();\n"
                + "    service.postAndReload('$edit?"
                + StrUtils.encodeURIComponent(request.getQueryString())
                + "', 'val=' + escapeUrl"
                + "($id('val').value) + '&chk="
                + "');\n"
                + "}");

        body.imageButton(ResFile.save16png)
                .onClick(new Call("editSubmit"))
                .value("Zapisz").style()
                .position("absolute")
                .bottom("2px")
                .right("4px")
                .padding("4px 20px");

        head.styles("body, input")
                .font("10pt Verdana").backgroundColor("#eee");

        head.styles(".CodeMirror")
                .position("absolute")
                .bottom("36px")
                .left("0")
                .top("0")
                .right("0")
                .lineHeight("1.3em")
                .borderBottom("1px solid #888888")
                .backgroundColor("#ffffff");;

        head.styles(".CodeMirror-activeline-background")
                .border("none !important")
                .backgroundColor("#eeeeff")
                .margin("-2px 0");
    }
}
