package cloud.adamind.saio.payments.util;

public enum WompiTransactionUpdates {
    APPROVED("APPROVED"),
    VOIDED("VOIDED"),
    DECLINED("DECLINED"),
    ERROR("ERROR");


    private final String status;

    WompiTransactionUpdates(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

}
