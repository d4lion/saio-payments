package cloud.adamind.saio.payments.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class QrCodeService {

    private static final Logger log = LoggerFactory.getLogger(QrCodeService.class);

    public byte[] generate(String content) {

        log.info("Generating QR code for: {}", content);

        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(
                            content,
                            BarcodeFormat.QR_CODE,
                            300,
                            300,
                            Map.of(
                                    EncodeHintType.MARGIN, 1
                            )
                    );

            log.info("Generated QR code for: {}", content);

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            log.info("Writing QR code to output stream for: {}", content);

            MatrixToImageWriter.writeToStream(matrix, "PNG", output);

            log.info("Successfully wrote QR code to output stream for: {}", content);

            return output.toByteArray();

        } catch (WriterException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Error generating QR code", e);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Error writing QR code to stream", e);
        }

    }


}
