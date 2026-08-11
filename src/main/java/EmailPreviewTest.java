import cloud.adamind.saio.payments.service.QrCodeService;
import cloud.adamind.saio.payments.service.S3Service;
import cloud.adamind.saio.payments.service.email.EmailTemplateProcessor;
import cloud.adamind.saio.payments.service.email.model.TicketEmailModel;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public class EmailPreviewTest {

    public static void main(String[] args) {

        EmailTemplateProcessor processor =
                new EmailTemplateProcessor();

        QrCodeService qrCodeService = new QrCodeService();
        S3Service s3 = new S3Service();

        byte[] qrCode = qrCodeService.generate("1018234129");

        String qrCodeUrl = s3.uploadBytes(
                "saioxv",
                String.format("users/%s/qr-code/%s.png", "1018234129", UUID.randomUUID()),
                qrCode,
                "image/png"
        );

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
                    html
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
