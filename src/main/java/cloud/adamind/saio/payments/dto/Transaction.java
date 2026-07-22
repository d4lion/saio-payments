package cloud.adamind.saio.payments.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {
    private String id;

    private Integer amountInCents;

    private String reference;

    private String customerEmail;

    private String currency;

    private String paymentMethodType;

    private String redirectUrl;

    private String status;

    private Object shippingAddress;

    private Object paymentLinkId;

    private Object paymentSourceId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getAmountInCents() {
        return amountInCents;
    }

    public void setAmountInCents(Integer amountInCents) {
        this.amountInCents = amountInCents;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethodType() {
        return paymentMethodType;
    }

    public void setPaymentMethodType(String paymentMethodType) {
        this.paymentMethodType = paymentMethodType;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Object shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public Object getPaymentLinkId() {
        return paymentLinkId;
    }

    public void setPaymentLinkId(Object paymentLinkId) {
        this.paymentLinkId = paymentLinkId;
    }

    public Object getPaymentSourceId() {
        return paymentSourceId;
    }

    public void setPaymentSourceId(Object paymentSourceId) {
        this.paymentSourceId = paymentSourceId;
    }
}
