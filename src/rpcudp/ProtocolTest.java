package rpcudp;

import java.net.InetAddress;

public final class ProtocolTest {
    private ProtocolTest() {
    }

    public static void main(String[] args) throws Exception {
        assertText("5", CalculatorService.execute(CalculatorService.ADD, MessageCodec.utf8("2,3")));
        assertText("6", CalculatorService.execute(CalculatorService.MULTIPLY, MessageCodec.utf8("2,3")));
        assertText("-1", CalculatorService.execute(CalculatorService.DIVIDE, MessageCodec.utf8("-3,3")));
        assertFails("Divisao por zero", () -> CalculatorService.execute(
                CalculatorService.DIVIDE, MessageCodec.utf8("1,0")));
        assertFails("Argumentos devem ser inteiros", () -> CalculatorService.execute(
                CalculatorService.ADD, MessageCodec.utf8("x,1")));
        assertFails("operacao exige", () -> CalculatorService.execute(
                CalculatorService.ADD, MessageCodec.utf8("1")));

        try (RemoteServer server = new RemoteServer(0)) {
            Thread worker = new Thread(() -> {
                try {
                    RemoteServer.Request request = server.getRequest();
                    server.sendReply(request, CalculatorService.execute(
                            request.getOperationId(), request.getArguments()));
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
            worker.start();
            RemoteRef reference = new RemoteRef(
                    InetAddress.getLoopbackAddress(), server.getPort(), 1_000, 1);
            assertText("42", reference.doOperation(
                    CalculatorService.ADD, MessageCodec.utf8("40,2")));
            worker.join(2_000);
            if (worker.isAlive()) {
                throw new AssertionError("Servidor de teste nao processou a requisicao");
            }
        }
        System.out.println("Todos os testes do protocolo passaram.");
    }

    private static void assertText(String expected, byte[] actual) {
        String value = MessageCodec.text(actual);
        if (!expected.equals(value)) {
            throw new AssertionError("Esperado " + expected + ", recebido " + value);
        }
    }

    private static void assertFails(String expectedFragment, ThrowingOperation operation) throws Exception {
        try {
            operation.run();
            throw new AssertionError("Era esperada uma falha contendo: " + expectedFragment);
        } catch (Exception exception) {
            if (!exception.getMessage().contains(expectedFragment)) {
                throw new AssertionError("Mensagem inesperada: " + exception.getMessage(), exception);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run() throws Exception;
    }
}
