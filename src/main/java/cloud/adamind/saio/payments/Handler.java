package cloud.adamind.saio.payments;

import cloud.adamind.saio.payments.dto.WompiWebhookEvent;
import cloud.adamind.saio.payments.security.WompiSignatureVerifier;
import cloud.adamind.saio.payments.service.PaymentService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.util.Map;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {



    private final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private WompiSignatureVerifier  verifier =  new WompiSignatureVerifier();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request,
            Context context) {

            final PaymentService paymentService = new PaymentService();

        WompiWebhookEvent event = null;

        try {
            event = mapper.readValue(request.getBody(), WompiWebhookEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (verifier.isValid(event)) {
            paymentService.processEvent(event);
        } else {
            return unauthorized();
        }


            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("{statusCode: 200}, message: OK}");
    }


    private APIGatewayProxyResponseEvent unauthorized() {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(401)
                .withHeaders(Map.of(
                        "Content-Type", "application/json"
                ))
                .withBody("""
                    {
                      "success": false,
                      "message": "Firma inválida"
                    }
                    """);
    }



}
