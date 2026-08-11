package cloud.adamind.saio.payments.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.ArrayList;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerData {
    public String legalId;
    public String deviceId;
    public String fullName;
    public BrowserInfo browserInfo;
    public String phoneNumber;
    public String legalIdType;
    public String deviceDataToken;
    public ArrayList<CustomerReference> customerReferences;
}
