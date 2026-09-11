package cloud.adamind.saio.payments;

import cloud.adamind.saio.payments.dto.WompiWebhookEvent;
import cloud.adamind.saio.payments.exception.TransactionProcessedException;
import cloud.adamind.saio.payments.exception.UnauthorizedException;
import cloud.adamind.saio.payments.service.PaymentService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.net.HttpURLConnection;
import java.util.Map;


import cloud.adamind.saio.payments.infrastructure.sqs.SqsPublisherAdapter;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv4Addresses", "true");
        System.setProperty("io.grpc.netty.shaded.io.netty.transport.noNative", "true");
    }

    private final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private final PaymentService paymentService;
    private final SqsPublisherAdapter sqsPublisher;

    public Handler() {
        this.paymentService = new PaymentService();
        this.sqsPublisher = new SqsPublisherAdapter();
    }

    public Handler(PaymentService paymentService, SqsPublisherAdapter sqsPublisher) {
        this.paymentService = paymentService;
        this.sqsPublisher = sqsPublisher;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request,
            Context context) {

        WompiWebhookEvent event = null;

        try {
            event = mapper.readValue(request.getBody(), WompiWebhookEvent.class);
        } catch (JsonProcessingException e) {
            return createResponse(HttpURLConnection.HTTP_BAD_REQUEST, Map.of(
                            "status", "error",
                            "message", "Invalid JSON"
                    )
            );
        }

        try {
            paymentService.validateWebhook(event);
            String messageId = sqsPublisher.sendMessage(request.getBody());

            return createResponse(HttpURLConnection.HTTP_OK, Map.of(
                    "status", "queued",
                    "message", "Evento recibido y encolado exitosamente",
                    "message_id", messageId
            ));
        } catch (UnauthorizedException e) {
            return createResponse(HttpURLConnection.HTTP_UNAUTHORIZED, Map.of(
                    "status", "unauthorized",
                    "message", e.getMessage()
            ));
        } catch (TransactionProcessedException e) {
            return createResponse(HttpURLConnection.HTTP_CONFLICT, Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return createResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, Map.of(
                    "status", "error",
                    "message", e.getMessage() != null ? e.getMessage() : "Error interno procesando evento"
            ));
        }
    }

    private APIGatewayProxyResponseEvent createResponse(int statusCode, Object body) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(Map.of(
                            "Content-Type", "application/json"
                    ))
                    .withBody(mapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
