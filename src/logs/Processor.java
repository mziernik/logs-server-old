package logs;

import com.context.AppContext;
import com.google.gson.JsonSyntaxException;
import com.mlogger.*;
import com.mlogger.handlers.HandlerException;
import com.mlogger.handlers.LogFlag;
import com.threads.MultiThread;
import com.utils.collections.Strings;
import com.utils.date.TDate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.InflaterOutputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import logs.Logs.RawPacket;
import logs.parsers.*;
import pages.Common;
import service.Config;

public class Processor extends MultiThread<RawPacket> {

    public final static Processor instance = new Processor();

    private Processor() {
        super("RawPacket Processor", null, 3);
    }

    @Override
    public boolean onException(Throwable e) {
        Common.addInternalError(e);
        return true;
    }

    @Override
    public void execute(RawPacket packet) throws Exception {

        //     Console.printlnTs("ID: %d, doProcess Raw", id);
        int pos = MHandler.signature.length;

        byte[] data = packet.data;

        { // do usuniecia w przyszlej wersji
            byte[] oldSign = {(byte) 0xFA, (byte) 0xFB, (byte) 0xFC,
                (byte) 0xEA, (byte) 0xEB, (byte) 0xEC};

            if (data.length > oldSign.length
                    && Arrays.equals(Arrays.copyOf(data, oldSign.length), oldSign)) {
                data = Arrays.copyOfRange(data, oldSign.length, data.length);
            }
        }

        //    new String(Arrays.copyOf(data, pos));
        if (data.length > pos && Arrays.equals(Arrays.copyOf(data, pos), MHandler.signature)) {

            byte version = data[pos++];
            if (version != 1)
                throw new HandlerException("Nieprawidłowa wersja obiektu (" + version + ")");

            int size = ByteBuffer.wrap(Arrays.copyOfRange(data, pos, pos + 4)).getInt();
            pos += 4;

            LogFlag.LogFlags flags = new LogFlag.LogFlags(null);
            flags.fromByte(data[pos++]);
            //"1b889b70-e18e-11e3-8b68-0800200c9a66"
            packet.encrypted = flags.contains(LogFlag.encrypted);
            packet.compressed = flags.contains(LogFlag.compressed);
            packet.priority = flags.contains(LogFlag.priority);

            if (flags.contains(LogFlag.token)) {
                ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(data, pos, pos + 16));
                packet.token = new UUID(bb.getLong(), bb.getLong(8));
                pos += 16;
            }

            if (packet.priority) {
                ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(data, pos, pos + 16));
                packet.priorityUid = new UUID(bb.getLong(), bb.getLong(8));
                pos += 16;
            }

            data = Arrays.copyOfRange(data, pos, data.length);

            if (packet.encrypted) { // zaszyfrowany
                try {
                    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    SecretKeySpec secretKey = new SecretKeySpec(
                            MessageDigest.getInstance("MD5").digest("1234".getBytes()), "AES");
                    cipher.init(Cipher.DECRYPT_MODE, secretKey);
                    data = cipher.doFinal(data);
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
            }

            if (packet.compressed) { // skompresowany
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                try (InflaterOutputStream iout = new InflaterOutputStream(bout)) {
                    iout.write(data);
                }
                data = bout.toByteArray();
            }

            //   new String(data);
            packet.data = data;
        }

        boolean parsed = false;

        try {
            parsed = LJson.read(packet);
        } catch (JsonSyntaxException e) {
            if (AppContext.devMode())
                Log.warning(e).details(new String(packet.data, "UTF-8"));
        } catch (Exception e) {
            Common.addInternalError(e);
            return;
        }

        if (!parsed)
            parsed = Squid.read(packet);

        if (!parsed)
            parsed = SysLog.read(packet);

        if (!parsed) {
            String val = new String(packet.data, "UTF-8");
            TLog log = new TLog(val, 0, null);
            log.kind = LogKind.debug;
            log.source = "unknown";
            log.date = new TDate();
            log.address(packet.address);
            log.value(val);
            packet.logs.add(log);
        }

        for (TLog log : packet.logs) {

            //      Console.printlnTs("ID: %d, Add", packet.id);
            if (log.uid != null && Config.CConsole.omitNonUnique.value() && Logs.idxUids.get(log.uid) != null)
                continue;

            log.setMark(LogAttr.source, log.source);
            log.setMark(LogAttr.address, new Strings(log.addresses).toString(", "));
            log.setMark(LogAttr.device, log.device);
            if (log.processId != null && log.processId > 0)
                log.setMark(LogAttr.processId, log.processId);
            if (log.threadId != null && log.threadId > 0)
                log.setMark(LogAttr.threadId, log.threadId);
            log.setMark(LogAttr.user, log.user);
            log.setMark(LogAttr.session, log.session);
            log.setMark(LogAttr.request, log.request);

            if (!log.addresses.isEmpty() && log.user != null && !log.user.isEmpty())
                synchronized (Logs.dns) {
                    Logs.dns.put(log.addressesSimple.iterator().next(), log.user);
                }
        }

    }

    @Override
    public void postExecuteOrder(RawPacket packet, long order, Throwable exception) {
        for (TLog log : packet.logs)
            Collector.instance.add(log);
    }

}
