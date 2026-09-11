import cloud.adamind.saio.payments.infrastructure.qr.ZxingQrCodeGenerator;
import cloud.adamind.saio.payments.infrastructure.s3.S3StorageAdapter;
import cloud.adamind.saio.payments.infrastructure.email.EmailTemplateProcessor;
import cloud.adamind.saio.payments.model.TicketEmailModel;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public class EmailPreviewTest {

    public static void main(String[] args) {

        EmailTemplateProcessor processor = new EmailTemplateProcessor();

        ZxingQrCodeGenerator zxingQrCodeGenerator = new ZxingQrCodeGenerator();
        String qrCodeUrl = "https://saioxv.s3.amazonaws.com/users/1018234129/qr-code/f77409bb-e70e-4ef1-a0cb-5f0d2cc5cafc.png";

        System.out.println("QR Code URL: " + qrCodeUrl);

        TicketEmailModel model = TicketEmailModel.builder()
                .amount("70000")
                .customer_email("test@gmail.com")
                .event_date("2 DE OCTUBRE")
                .event_location("UNAL")
                .customer_name("Danniel Martinez Tamayo")
                .payment_description("Ticket Standar")
                .transaction_id("123456789")
                .qr_code(qrCodeUrl)
                .build();

        String html = processor.render(model);

        try {
            Files.writeString(
                    java.nio.file.Path.of("ticket.html"),
                    html);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
