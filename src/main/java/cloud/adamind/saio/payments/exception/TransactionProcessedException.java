package cloud.adamind.saio.payments.exception;

public class TransactionProcessedException extends RuntimeException {
    public TransactionProcessedException(String message) {
        super(message);
    }
}
