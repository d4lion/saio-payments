import cloud.adamind.saio.payments.infrastructure.qr.ZxingQrCodeGenerator;
import cloud.adamind.saio.payments.infrastructure.s3.S3StorageAdapter;

import java.util.UUID;

public class QrCodeS3Test {

    public static void main(String[] args) {

        ZxingQrCodeGenerator zxingQrCodeGenerator = new ZxingQrCodeGenerator();
        S3StorageAdapter s3 = new S3StorageAdapter();

        byte[] qrCode = zxingQrCodeGenerator.generate("1018234129");

        String qrCodeUrl = s3.uploadBytes(
                "saioxv",
                String.format("users/%s/qr-code/%s.png", "1018234129", UUID.randomUUID()),
                qrCode,
                "image/png"
        );

        System.out.println("QR Code URL: " + qrCodeUrl);

    }


}
