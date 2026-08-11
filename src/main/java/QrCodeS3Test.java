import cloud.adamind.saio.payments.service.QrCodeService;
import cloud.adamind.saio.payments.service.S3Service;

import java.util.UUID;

public class QrCodeS3Test {

    public static void main(String[] args) {

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

    }


}
