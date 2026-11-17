package rpcudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.ThreadLocalRandom;

public final class RemoteRef {
    private final InetAddress host;
    private final int port;
    private final int timeoutMillis;
    private final int maxRetries;

    public RemoteRef(InetAddress host, int port, int timeoutMillis, int maxRetries) {
        if (port < 1 || port > 65_535 || timeoutMillis < 1 || maxRetries < 0) {
            throw new IllegalArgumentException("Configuracao de RemoteRef invalida");
        }
        this.host = host;
        this.port = port;
        this.timeoutMillis = timeoutMillis;
        this.maxRetries = maxRetries;
    }

    public byte[] doOperation(int operationId, byte[] arguments) throws IOException {
        long requestId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        byte[] request = MessageCodec.encodeRequest(requestId, operationId, arguments);
        byte[] buffer = new byte[MessageCodec.MAX_PAYLOAD + 19];

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeoutMillis);
            DatagramPacket outgoing = new DatagramPacket(request, request.length, host, port);
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                socket.send(outgoing);
                try {
                    while (true) {
                        DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                        socket.receive(incoming);
                        MessageCodec.Response response = MessageCodec.decodeResponse(
                                incoming.getData(), incoming.getLength());
                        if (response.requestId != requestId) {
                            continue;
                        }
                        if (!response.success) {
                            throw new IOException("Operacao remota rejeitada: "
                                    + MessageCodec.text(response.result));
                        }
                        return response.result;
                    }
                } catch (SocketTimeoutException timeout) {
                    if (attempt == maxRetries) {
                        throw new IOException("Tempo esgotado aguardando resposta após "
                                + (maxRetries + 1) + " tentativa(s)", timeout);
                    }
                }
            }
        }
        throw new IOException("Falha inesperada na invocacao remota");
    }
}
