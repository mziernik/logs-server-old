package storage.common;

import java.io.*;
import java.nio.charset.Charset;
import storage.LogsStorage;

public class Module {

    public static int BUFFER_SIZE = 1024 * 100;
    public static Charset UTF8 = Charset.forName("UTF-8");

    /* public static long readDyn(InputStream in) throws IOException {
     long value = 0;
     int shift = 0;
     while (in.available() > 0 || shift > 7) {
     int v = in.read();
     if (v == -1)
     break;
     value |= (v & 0x7f) << shift;
     shift += 7;
     if (v < 0x80)
     break;
     }
     if (value < 0)
     throw new IOException("Wartość " + value + " mniejsza niż 0");
     return value;
     }*/
    public static long readDyn(DataInput in) throws IOException {
        long value = 0;
        int shift = 0;

        while (shift <= 7) {
            int v = in.readByte();
            if (v == -1)
                break;
            value |= (v & 0x7f) << shift;
            shift += 7;
            if (v < 0x80)
                break;
        }
        if (value < 0)
            throw new IOException("Wartość " + value + " mniejsza niż 0");
        return value;
    }

}
