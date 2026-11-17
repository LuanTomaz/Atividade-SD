package rpcudp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class MessageCodec {
    static final byte VERSION = 1;
    static final byte REQUEST = 1;
    static final byte RESPONSE = 2;
    static final byte OK = 0;
    static final byte ERROR = 1;
    static final int MAX_PAYLOAD = 60_000;

    private MessageCodec() {
    }

    static byte[] encodeRequest(long requestId, int operationId, byte[] arguments) throws IOException {
        return encode(REQUEST, requestId, operationId, OK, arguments);
    }

    static Request decodeRequest(byte[] data, int length) throws IOException {
        Packet packet = decode(data, length, REQUEST);
        return new Request(packet.requestId, packet.operationId, packet.payload);
    }

    static byte[] encodeResponse(long requestId, boolean success, byte[] result) throws IOException {
        return encode(RESPONSE, requestId, 0, success ? OK : ERROR, result);
    }

    static Response decodeResponse(byte[] data, int length) throws IOException {
        Packet packet = decode(data, length, RESPONSE);
        return new Response(packet.requestId, packet.status == OK, packet.payload);
    }

    private static byte[] encode(byte type, long requestId, int operationId, byte status,
                                 byte[] payload) throws IOException {
        if (payload == null || payload.length > MAX_PAYLOAD) {
            throw new IOException("Payload ausente ou excede o limite de " + MAX_PAYLOAD + " bytes");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DataOutputStream writer = new DataOutputStream(output);
        writer.writeByte(VERSION);
        writer.writeByte(type);
        writer.writeLong(requestId);
        writer.writeInt(operationId);
        writer.writeByte(status);
        writer.writeInt(payload.length);
        writer.write(payload);
        writer.flush();
        return output.toByteArray();
    }

    private static Packet decode(byte[] data, int length, byte expectedType) throws IOException {
        if (data == null || length < 19) {
            throw new IOException("Datagrama menor que o cabecalho do protocolo");
        }
        try {
            DataInputStream reader = new DataInputStream(new ByteArrayInputStream(data, 0, length));
            int version = reader.readUnsignedByte();
            int type = reader.readUnsignedByte();
            long requestId = reader.readLong();
            int operationId = reader.readInt();
            byte status = reader.readByte();
            int payloadLength = reader.readInt();
            if (version != VERSION || type != expectedType) {
                throw new IOException("Versao ou tipo de mensagem invalido");
            }
            if (payloadLength < 0 || payloadLength > MAX_PAYLOAD || payloadLength != reader.available()) {
                throw new IOException("Tamanho de payload invalido");
            }
            byte[] payload = new byte[payloadLength];
            reader.readFully(payload);
            return new Packet(requestId, operationId, status, payload);
        } catch (EOFException exception) {
            throw new IOException("Mensagem truncada", exception);
        }
    }

    static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    static String text(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }

    static final class Request {
        final long requestId;
        final int operationId;
        final byte[] arguments;

        Request(long requestId, int operationId, byte[] arguments) {
            this.requestId = requestId;
            this.operationId = operationId;
            this.arguments = arguments;
        }
    }

    static final class Response {
        final long requestId;
        final boolean success;
        final byte[] result;

        Response(long requestId, boolean success, byte[] result) {
            this.requestId = requestId;
            this.success = success;
            this.result = result;
        }
    }

    private static final class Packet {
        final long requestId;
        final int operationId;
        final byte status;
        final byte[] payload;

        Packet(long requestId, int operationId, byte status, byte[] payload) {
            this.requestId = requestId;
            this.operationId = operationId;
            this.status = status;
            this.payload = payload;
        }
    }
}
