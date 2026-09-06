package cloud.adamind.saio.payments.infrastructure.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

public class PdfGeneratorAdapter {

    private static final Logger log = LoggerFactory.getLogger(PdfGeneratorAdapter.class);

    public byte[] generatePdfFromHtml(String htmlContent) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            log.info("Generating PDF from HTML content");

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, "");
            builder.toStream(os);
            builder.run();

            log.info("PDF generated successfully");
            return os.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF from HTML", e);
            throw new RuntimeException("Error al generar documento PDF del ticket", e);
        }
    }
}
