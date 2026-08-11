package cloud.adamind.saio.payments.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrowserInfo {
    public String browserTz;
    public String browserLanguage;
    public String browserUserAgent;
    public String browserColorDepth;
    public String browserScreenWidth;
    public String browserScreenHeight;
}
