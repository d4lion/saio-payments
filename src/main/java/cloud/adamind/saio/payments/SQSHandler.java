package cloud.adamind.saio.payments;

import cloud.adamind.saio.payments.dto.WompiWebhookEvent;
import cloud.adamind.saio.payments.service.PaymentService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQSHandler implements RequestHandler<SQSEvent, Void> {

    private static final Logger log = LoggerFactory.getLogger(SQSHandler.class);

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv4Addresses", "true");
        System.setProperty("io.grpc.netty.shaded.io.netty.transport.noNative", "true");
    }

    private final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private final PaymentService paymentService;

    public SQSHandler() {
        this.paymentService = new PaymentService();
    }

    public SQSHandler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        if (event == null || event.getRecords() == null || event.getRecords().isEmpty()) {
            log.info("No records received in SQS event.");
            return null;
        }

        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            log.info("Processing SQS Message ID: {}", msg.getMessageId());
            try {
                WompiWebhookEvent wompiEvent = mapper.readValue(msg.getBody(), WompiWebhookEvent.class);
                paymentService.process(wompiEvent);
                log.info("Successfully processed SQS Message ID: {}", msg.getMessageId());
            } catch (Exception e) {
                log.error("Error processing SQS Message ID: {}. Error: {}", msg.getMessageId(), e.getMessage(), e);
                // Lanzar excepción para que SQS reintente el mensaje o lo envíe a la DLQ
                throw new RuntimeException("Fallo al procesar mensaje de SQS " + msg.getMessageId(), e);
            }
        }

        return null;
    }
}
