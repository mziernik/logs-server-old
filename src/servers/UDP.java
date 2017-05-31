package servers;

import com.exceptions.EError;
import com.context.intf.OnContextDestroyed;
import com.context.intf.OnContextInitialized;
import com.mlogger.LogKind;
import com.mlogger.MHandler;
import com.*;
import com.threads.TThread;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import logs.Logs.RawPacket;
import pages.*;
import pages.Common.InternalLogObject;
import logs.parsers.LogSource;

public class UDP extends LogServer {

    @OnContextInitialized
    public static void contextInitialized() {

    }

    @OnContextDestroyed
    public static void contextDestroyed() {
        stopServer();
    }

    public final static Set<SocketAddress> interfaces = new HashSet<>();

    public final static List<UDP> udpThreads = new LinkedList<>(); // sync 

    private final static Set<CollectPart> parts = new HashSet<>();
    //****************************************
    private final byte[] data = new byte[65536];
    public final DatagramSocket socket;

    public UDP(SocketAddress address) throws SocketException {
        super("UDP log server");

        try {
            socket = new DatagramSocket(address);
        } catch (SocketException e) {
            throw EError.processSocketException(e, address);
        }

        setName("UDP log server " + address.toString());
    }

    @Override
    public void execute() {
        try {
            try {
                socket.setReceiveBufferSize(32 * 1024 * 1024); // 32 MB bufor
                while (true)
                    try {
                        DatagramPacket datagram = new DatagramPacket(data, data.length);
                        socket.receive(datagram);
                        int len = datagram.getLength();

                        RawPacket packet = new RawPacket(
                                datagram.getAddress().toString().substring(1),
                                Arrays.copyOf(data, len), LogSource.udp);

                        //   new String(data)
                        synchronized (parts) {
                            if (len > 30 && Arrays.equals(
                                    Arrays.copyOf(data, 6), MHandler.signature)) {
                                collectPacket(datagram.getAddress().toString()
                                        .substring(1), Arrays.copyOfRange(data, 6, len));
                                continue;
                            }
                            //---------------------------------
                            // usun przeterminowane czesci
                            long now = new Date().getTime();
                            Set<CollectPart> lst = new HashSet<>();
                            for (CollectPart cp : parts)
                                if (now - cp.date > 30000)
                                    lst.add(cp);
                            parts.removeAll(lst);
                        }
                        //---------------------------------
                        packet.process();
                    } catch (Exception e) {
                        if (e instanceof SocketException)
                            return;
                    }
            } finally {
                socket.close();
            }
        } catch (Exception e) {
            Common.addInternalError(e);
        }

    }

    public class CollectPart {

        public byte[] data;
        public int number;
        public int total;
        public byte[] uid;
        public final long date = new Date().getTime();
        public String address;
    }

    private void collectPacket(String address, byte[] data) throws IOException {
        CollectPart part = new CollectPart();
        part.uid = Arrays.copyOfRange(data, 0, 16);
        part.number = ByteBuffer.wrap(Arrays.copyOfRange(data, 16, 20)).getInt();
        part.total = ByteBuffer.wrap(Arrays.copyOfRange(data, 20, 24)).getInt();
        part.address = address;
        part.data = Arrays.copyOfRange(data, 24, data.length);
        parts.add(part);

        Set<Integer> collect = new HashSet<>();
        Set<CollectPart> prts = new HashSet<>();
        for (CollectPart cp : parts)
            if (Arrays.equals(cp.uid, part.uid)
                    && address.equals(cp.address)
                    && part.total == cp.total) {
                collect.add(cp.number);
                prts.add(cp);
            }

        if (collect.size() == part.total) {
            // zebrano wszystkie czesci pakietu
            ByteArrayOutputStream bout = new ByteArrayOutputStream();

            for (int i = 0; i < part.total; i++)
                for (CollectPart cp : prts)
                    if (cp.number == i + 1)
                        bout.write(cp.data);

            parts.removeAll(prts);

            new RawPacket(part.address, bout.toByteArray(), LogSource.udp).process();
        }
    }

    public static void startServer() throws SocketException {
        stopServer();

        synchronized (udpThreads) {
            for (SocketAddress addr : interfaces)
                try {
                    udpThreads.add(new UDP(addr));
                    Common.addInternalLog(LogKind.log, "Interfejs nasłuchujący: " + addr);
                } catch (Exception e) {
                    InternalLogObject log = new InternalLogObject(LogKind.error,
                            "[" + addr + "] " + EError.exceptionToStr(e));
                    log.errorStack.addAll(EError.getStackTraceStr(e).getList());
                    log.add();
                }

            if (udpThreads.isEmpty())
                Common.addInternalLog(LogKind.warning, "Brak zdefiniowanych interfejsów nasłuchujących");
        }
        for (UDP udp : udpThreads)
            udp.start();
    }

    public static void stopServer() {

        synchronized (udpThreads) {
            for (UDP udp : udpThreads)
                try {
                    udp.interrupt();
                } catch (Exception e) {
                    Common.addInternalError(e);
                }
        }
    }

    @Override
    public TThread interrupt() {

        if (socket != null)
            socket.close();
        super.interrupt();
        synchronized (udpThreads) {
            udpThreads.remove(this);
        }
        
        return super.interrupt();
    }

    @Override
    public void onTerminate(Throwable ex) {
        if (socket != null)
            socket.close();
    }
}
