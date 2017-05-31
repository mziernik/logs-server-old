package servers;

import logs.parsers.LogSource;
import com.context.intf.OnContextDestroyed;
import com.context.intf.OnContextInitialized;
import com.mlogger.Log;
import com.mlogger.MHandler;
import com.context.AppContext;
import com.exceptions.EError;
import com.json.Escape;
import com.json.JSON;
import com.threads.*;
import java.io.*;
import java.net.*;
import java.util.*;
import logs.Logs.RawPacket;
import pages.Common;

public class TCP extends LogServer {

    private static TCP instance;
    private final Set<TcpConnection> connections = new LinkedHashSet<>();

    @OnContextInitialized
    public static void contextInitialized() {
        if (!AppContext.devMode())
            return;

        instance = new TCP();
        /*   instance.start();*/
    }

    @OnContextDestroyed
    public static void contextDestroyed() throws IOException {
        if (instance != null)
            instance.close();
        instance = null;
    }

    public TCP() {
        super("TCP log server");
    }

    public void close() throws IOException {
        if (server != null)
            server.close();
        instance.interrupt();
    }

    private ServerSocket server;

    @Override
    public void execute() {
        try {
            server = new ServerSocket(5140);

            setName("TCP log server " + server.toString());
            try {

                while (isRunning()) {
                    Socket socket = server.accept();
                    TcpConnection conn = new TcpConnection(socket);

                    conn.setName("TCP Client: " + socket.getRemoteSocketAddress().toString());
                    conn.start();
                }

            } catch (Exception e) {
                Common.addInternalError(e);
            } finally {
                server.close();
            }
        } catch (Exception e) {
            Common.addInternalError(e);
        }
    }

    private class TcpConnection extends TThread {

        private final Socket socket;
        private OutputStreamWriter writer;

        public void write(String name, String value) throws IOException {
            if (writer == null)
                writer = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");

            Escape esc = new Escape();
            esc.toString(name, writer);
            //  JSON.escape(name, null, writer, false);
            if (value != null) {
                writer.append(": ");
                esc.toString(value, writer);
            }
            writer.append("\n");
            writer.flush();
        }

        public TcpConnection(Socket socket) throws SocketException {
            super("TCP client: " + socket.getRemoteSocketAddress().toString());
            this.socket = socket;
            socket.setSoTimeout(5000);
            socket.setReceiveBufferSize(32 * 1024 * 1024);
            Log.log("Podlaczyl sie " + socket.getRemoteSocketAddress().toString());
            synchronized (connections) {
                connections.add(this);
            }
        }

        @Override
        protected void execute() throws Exception {
            try {
                InputStream sin = socket.getInputStream();

                DataInputStream in = new DataInputStream(sin);

                while (isRunning() && socket.isConnected() && !socket.isClosed()) {

                    byte[] buff = new byte[MHandler.signature.length];
                    int read = in.read(buff);

                    // jesli sie rozlaczyl...
                    if (read != 5 || !Arrays.equals(buff, MHandler.signature))
                        return;

                    byte version = in.readByte();
                    int size = in.readInt();
                    byte[] data = new byte[size];
                    int dataSize = in.read(data);

                    // nie udalo sie odczytac calej tresci
                    if (dataSize != data.length)
                        return;

                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    try (DataOutputStream dout = new DataOutputStream(bout);) {

                        dout.write(MHandler.signature);
                        dout.writeByte(1);
                        dout.writeInt(size);
                        dout.write(data);
                    }

                    RawPacket packet = new RawPacket(
                            socket.getRemoteSocketAddress().toString().substring(1),
                            bout.toByteArray(), LogSource.tcp);

                    packet.process();

                    Log.log("Odebrano " + new String(packet.data));

                    if (packet.priority) {
                        write("confirm", packet.priorityUid.toString());
                    }
                }

            } catch (SocketTimeoutException | SocketException e) {
            } catch (Exception e) {
                write("error", EError.toString(e));
                Common.addInternalError(e);
                return;
            } finally {
                socket.close();
                Log.log("Rozłączam " + socket.getRemoteSocketAddress().toString());
                synchronized (connections) {
                    connections.remove(this);
                }
            }
        }

    }
}
