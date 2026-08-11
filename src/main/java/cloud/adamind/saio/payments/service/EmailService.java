package cloud.adamind.saio.payments.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailService {

    private final static Logger log = LoggerFactory.getLogger(EmailService.class);

    private final Resend resend;

    public EmailService(){
       this.resend = new Resend(System.getenv("RESEND_MAILING_API_KEY"));
       log.info("Resend email service started");
    }

    public void sendEmail(String from, String to, String subject, String htmlSource) throws ResendException {
        log.debug("Sending email from {} to {} with subject {}", from, to, subject);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .html(htmlSource)
                .build();
        log.debug("Email params: {}", params);
        try{
            CreateEmailResponse data = resend.emails().send(params);
            log.info("Email sent to: {}", to);

        } catch (ResendException e) {
            log.error("Email sent to {} failed", to, e);

        }

    }



}
