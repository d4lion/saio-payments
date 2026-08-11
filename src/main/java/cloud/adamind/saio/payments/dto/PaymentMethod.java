package cloud.adamind.saio.payments.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentMethod {
    public String type;
    public String userType;
    public String paymentDescription;
}
