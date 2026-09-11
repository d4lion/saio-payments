package cloud.adamind.saio.payments.infrastructure.sqs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.net.URI;

public class SqsPublisherAdapter {

    private static final Logger log = LoggerFactory.getLogger(SqsPublisherAdapter.class);
    private final SqsClient sqsClient;
    private final String defaultQueueUrl;

    public SqsPublisherAdapter() {
        this.defaultQueueUrl = System.getenv("PAYMENT_QUEUE_URL");

        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.US_EAST_1);

        String endpointUrl = System.getenv("AWS_ENDPOINT_URL");
        if (endpointUrl == null || endpointUrl.isBlank()) {
            endpointUrl = System.getenv("SQS_ENDPOINT_URL");
        }

        if (endpointUrl != null && !endpointUrl.isBlank()) {
            log.info("Configuring SQSClient with local endpoint: {}", endpointUrl);
            builder.endpointOverride(URI.create(endpointUrl))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")
                    ));
        }

        this.sqsClient = builder.build();
    }

    public SqsPublisherAdapter(SqsClient sqsClient, String defaultQueueUrl) {
        this.sqsClient = sqsClient;
        this.defaultQueueUrl = defaultQueueUrl;
    }

    public String sendMessage(String messageBody) {
        if (defaultQueueUrl == null || defaultQueueUrl.isBlank()) {
            throw new IllegalStateException("La variable de entorno PAYMENT_QUEUE_URL no está configurada.");
        }
        return sendMessage(defaultQueueUrl, messageBody);
    }

    public String sendMessage(String queueUrl, String messageBody) {
        log.info("Sending message to SQS queueUrl: {}", queueUrl);

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .build();

        SendMessageResponse response = sqsClient.sendMessage(request);
        log.info("Message sent successfully to SQS with messageId: {}", response.messageId());
        return response.messageId();
    }
}
