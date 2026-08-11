package cloud.adamind.saio.payments.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {
    private String id;

    private Integer amountInCents;

    private String reference;

    private String customerEmail;

    private String currency;

    private String paymentMethodType;

    private PaymentMethod paymentMethod;

    private CustomerData customerData;

    private String redirectUrl;

    private String status;

    private Object shippingAddress;

    private Object paymentLinkId;

    private Object paymentSourceId;
}
