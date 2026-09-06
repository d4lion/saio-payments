import cloud.adamind.saio.payments.infrastructure.email.EmailTemplateProcessor;
import cloud.adamind.saio.payments.infrastructure.pdf.PdfGeneratorAdapter;
import cloud.adamind.saio.payments.infrastructure.qr.ZxingQrCodeGenerator;
import cloud.adamind.saio.payments.model.TicketEmailModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class PdfPreviewTest {

    public static void main(String[] args) {
        System.out.println("Iniciando prueba local de generación de PDF y vista previa de correo...");

        // 1. Instanciar procesadores
        EmailTemplateProcessor processor = new EmailTemplateProcessor();
        ZxingQrCodeGenerator qrGenerator = new ZxingQrCodeGenerator();
        PdfGeneratorAdapter pdfGenerator = new PdfGeneratorAdapter();

        // 2. Generar código QR en memoria codificando el Account ID (UID)
        String sampleAccountId = "usr_firebase_89123456";
        byte[] qrBytes = qrGenerator.generate(sampleAccountId);
        String qrBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes);

        // 3. Crear modelo de prueba
        TicketEmailModel model = TicketEmailModel.builder()
                .transaction_id("W-89123456")
                .amount("70.000")
                .customer_name("Danniel Martínez Tamayo")
                .customer_email("danniel.martinez@saio.co")
                .customer_legal_id("1020408201")
                .payment_description("Boleta Supernova")
                .event_date("15 y 16 de Octubre 2026")
                .event_location("Universidad Nacional - Sede Medellín")
                .qr_code(qrBase64)
                .build();

        // 4. Renderizar HTML para PDF y generar archivo binario PDF
        String pdfHtml = processor.renderTemplate("templates/ticket-pdf.html", model);
        byte[] pdfBytes = pdfGenerator.generatePdfFromHtml(pdfHtml);

        // 5. Renderizar HTML para cuerpo de correo
        String emailHtml = processor.renderTemplate("templates/ticket-mail.html", model);

        // 6. Guardar en directorio ignora por git (output/)
        try {
            Path outputDir = Path.of("output");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            Path pdfFile = outputDir.resolve("ticket-preview.pdf");
            Path mailFile = outputDir.resolve("ticket-email-preview.html");

            Files.write(pdfFile, pdfBytes);
            Files.writeString(mailFile, emailHtml);

            System.out.println("✅ ¡Vistas previas generadas exitosamente!");
            System.out.println("📄 PDF del Ticket: " + pdfFile.toAbsolutePath());
            System.out.println("✉️ HTML del Correo: " + mailFile.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("❌ Error guardando vistas previas: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
