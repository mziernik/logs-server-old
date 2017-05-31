package pages;

import com.StrUtils;
import com.html.js.Call;
import com.html.tags.InputType;
import com.html.modules.CodeMirror;
import com.io.IOUtils;
import com.servlet.handlers.Page;
import com.servlet.interfaces.Endpoint;
import com.utils.date.TDate;
import java.io.InputStream;
import logs.Logs.RawPacket;
import logs.Processor;
import logs.parsers.LogSource;

import static com.resources.ResFile.service;

@Endpoint(url = "generator")
public class LogsGenerator extends Page {
    
    @Endpoint
    public void save() throws Exception {
        try (InputStream in = request.input.getInputStream()) {
            byte[] buff = IOUtils.read(in);
            
            Integer cnt = params.getInt("count", 1);
            if (cnt < 1)
                cnt = 1;
            
            for (int i = 0; i < cnt; i++) {
                
                if ((i % 1000) == 0)
                    Thread.sleep(10);
                
                String str = new String(buff, "UTF-8");
                
                str = str.replace("%date%", new TDate().toString(true));
                
                str = str.replace("%count%", "" + (i + 1));
                
                str = str.replace("%text1%", StrUtils.generateText(1, 10).replace("\n", "\\n"));
                
                str = str.replace("%text2%", StrUtils.generateText(10, 300).replace("\n", "\\n"));
                
                Processor.instance.add(
                        new RawPacket("localhost", str.getBytes("UTF-8"),
                                LogSource.internal));
                
            }
            
        }
    }
    
    @Override
    protected void onRequest() throws Exception {
        
        link(service);
        link("/pages/generator.js");
        
        body.style().backgroundColor("black").color("#ccc");
        
        CodeMirror codemirror = new CodeMirror(this, body,
                CodeMirror.CodemirrorMode.javaScript, "code");
        
        codemirror.text("{\n"
                + "    aaaa : \"213213\",  // komentarz\n"
                + "    bbb: 111,\n"
                + "    c: true,\n"
                + "    /*  tablica */\n"
                + "    sdf: [ 1, \"324\", \"sdfsfwe we ef\"],\n"
                + "    \"sdfds\": null\n"
                + "}")
                .id("code");
        
        head.styles(".CodeMirror").border("1px solid #666");
        
        body.button()
                .text("Wyślij")
                .onClick(new Call("btnSendClick"))
                .style()
                .padding("4px 10px")
                .margin("10px");
        
        body.input(InputType.number)
                .value(1)
                .id("edtCount")
                .style().width("60px");
        
        body.br();
        body.br();
        
        body.div("%text1%");
        body.div("%text2%");
        
        body.button()
                .text("Reset")
                .onClick(new Call("btnResetClick"))
                .style()
                .margin("10px");
        
    }
    
}
