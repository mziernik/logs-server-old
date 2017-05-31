package logs;

import com.utils.collections.Strings;
import com.utils.date.TDate;
import com.Utils;
import com.utils.hashes.Hex;
import com.io.IOUtils;
import com.json.JObject;
import com.mlogger.*;
import com.utils.date.Timestamp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import net.sf.uadetector.*;
import pages.Common;
import logs.parsers.UserAgent;

public class TLog extends Log implements Comparable<TLog> {

    public final List<LogValue> values = new LinkedList<>();

    public LinkedList<Object> getVal(LogAttr attr) {
        LinkedList<Object> lst = new LinkedList<>();
        for (LogValue val : values)
            if (val.attr == attr) {
                lst.addAll(val.values);
                break;
            }
        return lst;
    }

    public class LogValue implements Iterable<Object> {

        public final LogAttr attr;
        public final DataType type;
        private final List<Object> values = new LinkedList<>();

        public LogValue(LogAttr attr, DataType type) {
            this.attr = attr;
            this.type = type;
            TLog.this.values.add(this);
        }

        public Strings getValues() {
            Strings list = new Strings().separator(", ");
            list.addAll(values);
            return list;
        }

        public LogValue addValue(Object value) {
            values.add(value);
            return this;
        }

        @Override
        public Iterator<Object> iterator() {
            return values.iterator();
        }

        @Override
        public String toString() {
            return attr + (type != null && type != DataType.text
                    ? " [" + type + "]" : "")
                    + ": " + new Strings(values).toString(", ");
        }

    }

    public static int instanceCount = 0;
    //public final static TLog log = new TLog("<dummy>");
    // public final Set<LogAttr> allAttributes = new LinkedHashSet<>();

    public long id;
    public TDate createTime = new TDate();
    public String protocol;
    public boolean includeMilliseconds = true;
    public String deviceFull;
    public byte[] rawData;
    public int rawDataSize;
    public int ver; // wersja logu
    //public final Map<LogAttr, Pair<Object, Integer>> marks = new TreeMap<>();
    public final Map<LogAttr, String> marks = new LinkedHashMap<>();
    public final LinkedHashSet<String> addressesSimple = new LinkedHashSet<>();

    @Override
    protected void finalize() throws Throwable {
        instanceCounter(false);
        super.finalize();
    }

    private void setRawData(String value) {
        if (value == null)
            return;
        byte[] buffer = value.getBytes(Utils.UTF8);
        rawDataSize = buffer.length;
        // long time = System.currentTimeMillis();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            Deflater deflater = new Deflater();
            deflater.setStrategy(Deflater.FILTERED);
            deflater.setLevel(5);
            try (DeflaterOutputStream out = new DeflaterOutputStream(bout, deflater);) {
                out.write(buffer);
            }
            rawData = bout.toByteArray();

            /*   System.out.println("comprr " + (System.currentTimeMillis() - time)
             + "   " + Math.round(100 - 100d * (double) rawData.length / (double) buffer.length));
             */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getRawData() {
        if (rawData != null)
            try {
                try (InflaterInputStream in = new InflaterInputStream(
                        new ByteArrayInputStream(rawData));) {
                    return new String(IOUtils.read(in), Utils.UTF8);
                }
            } catch (Exception e) {
                Common.addInternalError(e);
            }
        return null;
    }

    public String getAsJson() {

        JObject json = new JObject();
        json.options.singleLine(true);

        JObject jlog = json.objectC("details");
        jlog.put("Id", id);
        jlog.put("Adres", new Strings(addresses).toString(", "));
        jlog.put("Komentarz", comment);
        jlog.put("Data serwera ", createTime);
        jlog.put("Data klienta", date);
        jlog.put("Urządzenie", deviceFull);
        jlog.put("Instancja", instance);
        jlog.put("Rodzaj", kind.name);
        jlog.put("Proces", processId);
        jlog.put("Kolor czcionki", color);
        jlog.put("Kolor tła", background);

        jlog.put("Protokół", protocol);
        jlog.put("Żądanie", request);
        jlog.put("Sesja", session);
        jlog.put("Źródło", source);

        jlog.put("Tagi", new Strings(tags).toString(", "));
        jlog.put("Id wątku", threadId);
        jlog.put("UID", uid);
        jlog.put("URL", new Strings(urls).toString(", "));
        jlog.put("Użytkownik", user);
        jlog.put("Wartość", value);

        JObject jobj = jlog.objectC("Dane");
        /*
         for (Map.Entry<String, String> en : data.value.entrySet())
         jobj.put(en.getKey(), en.getValue());

         if (!attributes.isEmpty() || ! !groupAttr.isEmpty()) {
         jobj = jlog.objectC("Atrybuty");
         for (Map.Entry<String, String> en : attributes.value.entrySet())
         jobj.put(en.getKey(), en.getValue());

         for (Map.Entry<String, Map<String, String>> en : groupAttr.value.entrySet()) {
         JObject o = jobj.objectC(en.getKey());
         for (Map.Entry<String, String> ee : en.getValue().entrySet())
         o.put(ee.getKey(), ee.getValue());
         }
         }
         */
        return json.toString();
    }

    // wspólny konstruktor
    private TLog(TDate createTime, String rawData, int ver, UUID uid) {
        super(uid);
        instanceCounter(true);
        this.createTime = createTime;
        this.ver = ver;
        if (rawData != null)
            setRawData(rawData);
    }

    public TLog(String rawData, int ver, UUID uid) {
        this(new TDate(), rawData, ver, uid);
    }

    public TLog(UUID uid, TDate createTime) {
        this(createTime, null, -1, uid);
    }

    private synchronized static void instanceCounter(boolean isNew) {
        instanceCount += isNew ? 1 : -1;
    }

    public void setMark(LogAttr attr, Object value) {
        Object val = value;
        if (val == null || val.toString().isEmpty())
            return;

        // obliczanie haszu koloru
        byte[] buff = val.toString().getBytes(Charset.forName("UTF-8"));

        CRC32 crc32 = new CRC32();
        crc32.update(buff);
        int mark = (int) crc32.getValue();

        buff = ByteBuffer.allocate(4).putInt(mark).array();

        for (int i = 1; i < buff.length; i++)
            buff[i] = (byte) ((double) (buff[i] & 0xFF) * 0.6d + 0.4d * 255d);

        marks.put(attr, "#" + Hex.toString(ByteBuffer.wrap(buff).getInt() & 0xFFFFFF).substring(2));
    }

    public void setDevice(String sDevice) {
        deviceFull = sDevice;
        device = sDevice;

        ReadableUserAgent ua = UserAgent.getParser().parse(sDevice);

        OperatingSystem os = ua.getOperatingSystem();
        UserAgentType type = ua.getType();
        VersionNumber ver = ua.getVersionNumber();

        if (os != OperatingSystem.EMPTY)
            attribute("User Agent", "OS", os.getName());

        if (type != UserAgentType.UNKNOWN)
            attribute("User Agent", "Type", ua.getTypeName());

        if (ver != VersionNumber.UNKNOWN)
            attribute("User Agent", "Version", ver.toVersionString());

        String s = ua.getProducer();

        if (!s.isEmpty() && !s.equalsIgnoreCase("unknown"))
            attribute("User Agent", "Producer", s);

        s = ua.getName();
        if (!s.isEmpty() && !s.equalsIgnoreCase("unknown"))
            attribute("User Agent", "Name", s);

        if (os != OperatingSystem.EMPTY && type != UserAgentType.UNKNOWN)
            device = os.getName() + ", " + ua.getName()
                    + (ver != VersionNumber.UNKNOWN ? " " + ver.getMajor() : "");
    }

    @Override
    public int compareTo(TLog o) {
        return Long.compare(id, o.id);
    }

    /**
     Formatowanie adresu do postaci prostej (bez numeru portu)
     @param address 
     */
    public void addAddress(String address) {
        addresses.add(address);
        addressesSimple.add(address);
    }
}
