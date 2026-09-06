package cloud.adamind.saio.payments.infrastructure.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResendEmailAdapter {

    private final static Logger log = LoggerFactory.getLogger(ResendEmailAdapter.class);

    private final Resend resend;

    public ResendEmailAdapter(){
       this.resend = new Resend(System.getenv("RESEND_MAILING_API_KEY"));
       log.info("Resend email service started");
    }

    public void sendEmail(String from, String to, String subject, String htmlSource) throws ResendException {
        sendEmailWithAttachment(from, to, subject, htmlSource, null, null);
    }

    public void sendEmailWithAttachment(String from, String to, String subject, String htmlSource, String attachmentFileName, byte[] attachmentBytes) throws ResendException {
        log.debug("Sending email from {} to {} with subject {}", from, to, subject);

        CreateEmailOptions.Builder builder = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .html(htmlSource);

        if (attachmentFileName != null && attachmentBytes != null) {
            String base64Content = java.util.Base64.getEncoder().encodeToString(attachmentBytes);
            Attachment attachment = Attachment.builder()
                    .fileName(attachmentFileName)
                    .content(base64Content)
                    .build();
            builder.attachments(attachment);
        }

        CreateEmailOptions params = builder.build();
        log.debug("Email params: {}", params);
        try {
            CreateEmailResponse data = resend.emails().send(params);
            log.info("Email sent to: {}", to);
        } catch (ResendException e) {
            log.error("Email sent to {} failed", to, e);
            throw e;
        }
    }
}
