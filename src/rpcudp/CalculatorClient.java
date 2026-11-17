package rpcudp;

import java.net.InetAddress;

public final class CalculatorClient {
    private CalculatorClient() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Uso: CalculatorClient <host> <porta> <operacao> <a,b>");
            System.err.println("Operacoes: soma, subtracao, multiplicacao, divisao");
            System.exit(2);
        }
        int port = Integer.parseInt(args[1]);
        int operation = operationId(args[2]);
        String[] values = args[3].split(",", -1);
        if (values.length != 2) {
            throw new IllegalArgumentException("Informe os argumentos no formato a,b");
        }
        RemoteRef reference = new RemoteRef(InetAddress.getByName(args[0]), port, 1_000, 2);
        byte[] result = reference.doOperation(operation, MessageCodec.utf8(args[3]));
        System.out.println("Resultado remoto: " + MessageCodec.text(result));
    }

    private static int operationId(String operation) {
        switch (operation.toLowerCase()) {
            case "soma":
                return CalculatorService.ADD;
            case "subtracao":
                return CalculatorService.SUBTRACT;
            case "multiplicacao":
                return CalculatorService.MULTIPLY;
            case "divisao":
                return CalculatorService.DIVIDE;
            default:
                throw new IllegalArgumentException("Operacao desconhecida: " + operation);
        }
    }
}
