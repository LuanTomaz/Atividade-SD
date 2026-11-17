package rpcudp;

import java.io.IOException;

public final class CalculatorService {
    public static final int ADD = 1;
    public static final int SUBTRACT = 2;
    public static final int MULTIPLY = 3;
    public static final int DIVIDE = 4;

    private CalculatorService() {
    }

    public static byte[] execute(int operationId, byte[] arguments) throws IOException {
        String[] values = MessageCodec.text(arguments).trim().split(",", -1);
        if (values.length != 2) {
            throw new IOException("A operacao exige exatamente dois inteiros");
        }
        long left;
        long right;
        try {
            left = Long.parseLong(values[0].trim());
            right = Long.parseLong(values[1].trim());
        } catch (NumberFormatException exception) {
            throw new IOException("Argumentos devem ser inteiros", exception);
        }

        long result;
        switch (operationId) {
            case ADD:
                result = Math.addExact(left, right);
                break;
            case SUBTRACT:
                result = Math.subtractExact(left, right);
                break;
            case MULTIPLY:
                result = Math.multiplyExact(left, right);
                break;
            case DIVIDE:
                if (right == 0) {
                    throw new IOException("Divisao por zero");
                }
                result = left / right;
                break;
            default:
                throw new IOException("OperationId desconhecido: " + operationId);
        }
        return MessageCodec.utf8(Long.toString(result));
    }
}
