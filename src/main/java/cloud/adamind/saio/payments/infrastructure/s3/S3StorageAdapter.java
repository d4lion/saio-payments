package cloud.adamind.saio.payments.infrastructure.s3;

import org.slf4j.Logger;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3StorageAdapter {

    private final static Logger log = org.slf4j.LoggerFactory.getLogger(S3StorageAdapter.class);

    private final S3Client s3 = S3Client.builder()
            .region(Region.US_EAST_1)
            .build();

    public String uploadBytes(String bucket, String key, byte[] bytes, String contentType) {

        log.info("Uploading file to S3: bucket={}, key={}, contentType={}", bucket, key, contentType);

        PutObjectRequest request =  PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3.putObject(request, RequestBody.fromBytes(bytes));

        log.info("File uploaded to S3: bucket={}, key={}", bucket, key);

        return String.format("https://%s.s3.amazonaws.com/%s", bucket, key);
    }



}
