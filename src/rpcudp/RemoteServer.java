package rpcudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public final class RemoteServer implements AutoCloseable {
    private final DatagramSocket socket;
    private final Map<RequestKey, byte[]> completedRequests = new HashMap<>();

    public RemoteServer(int port) throws IOException {
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("Porta invalida");
        }
        socket = new DatagramSocket(port);
    }

    public Request getRequest() throws IOException {
        byte[] buffer = new byte[MessageCodec.MAX_PAYLOAD + 19];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            try {
                MessageCodec.Request request = MessageCodec.decodeRequest(
                        packet.getData(), packet.getLength());
                RequestKey key = new RequestKey(packet.getAddress(), packet.getPort(), request.requestId);
                byte[] previousReply = completedRequests.get(key);
                if (previousReply != null) {
                    sendRaw(previousReply, packet.getAddress(), packet.getPort());
                    continue;
                }
                return new Request(this, request.requestId, request.operationId, request.arguments,
                        packet.getAddress(), packet.getPort());
            } catch (IOException malformed) {
                byte[] error = MessageCodec.encodeResponse(0, false,
                        MessageCodec.utf8("Mensagem invalida: " + malformed.getMessage()));
                sendRaw(error, packet.getAddress(), packet.getPort());
            }
        }
    }

    public void sendReply(Request request, byte[] result) throws IOException {
        if (request == null || request.server != this) {
            throw new IllegalArgumentException("Requisicao nao pertence a este servidor");
        }
        byte[] reply = MessageCodec.encodeResponse(request.requestId, true, result);
        completedRequests.put(request.key(), reply);
        sendRaw(reply, request.clientHost, request.clientPort);
    }

    public void sendError(Request request, String message) throws IOException {
        if (request == null || request.server != this) {
            throw new IllegalArgumentException("Requisicao nao pertence a este servidor");
        }
        byte[] reply = MessageCodec.encodeResponse(request.requestId, false, MessageCodec.utf8(message));
        completedRequests.put(request.key(), reply);
        sendRaw(reply, request.clientHost, request.clientPort);
    }

    private void sendRaw(byte[] data, InetAddress address, int port) throws IOException {
        socket.send(new DatagramPacket(data, data.length, address, port));
    }

    public int getPort() {
        return socket.getLocalPort();
    }

    @Override
    public void close() {
        socket.close();
    }

    public static final class Request {
        private final RemoteServer server;
        private final long requestId;
        private final int operationId;
        private final byte[] arguments;
        private final InetAddress clientHost;
        private final int clientPort;

        private Request(RemoteServer server, long requestId, int operationId, byte[] arguments,
                        InetAddress clientHost, int clientPort) {
            this.server = server;
            this.requestId = requestId;
            this.operationId = operationId;
            this.arguments = arguments;
            this.clientHost = clientHost;
            this.clientPort = clientPort;
        }

        public long getRequestId() {
            return requestId;
        }

        public int getOperationId() {
            return operationId;
        }

        public byte[] getArguments() {
            return arguments.clone();
        }

        private RequestKey key() {
            return new RequestKey(clientHost, clientPort, requestId);
        }
    }

    private static final class RequestKey {
        private final InetAddress host;
        private final int port;
        private final long id;

        RequestKey(InetAddress host, int port, long id) {
            this.host = host;
            this.port = port;
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof RequestKey)) {
                return false;
            }
            RequestKey key = (RequestKey) other;
            return port == key.port && id == key.id && host.equals(key.host);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * host.hashCode() + port) + Long.hashCode(id);
        }
    }
}
