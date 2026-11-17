package rpcudp;

public final class CalculatorServer {
    private CalculatorServer() {
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        try (RemoteServer server = new RemoteServer(port)) {
            System.out.println("Servidor de calculadora ouvindo na porta " + server.getPort());
            while (true) {
                RemoteServer.Request request = server.getRequest();
                try {
                    byte[] result = CalculatorService.execute(
                            request.getOperationId(), request.getArguments());
                    server.sendReply(request, result);
                } catch (Exception exception) {
                    server.sendError(request, exception.getMessage());
                }
            }
        }
    }
}
