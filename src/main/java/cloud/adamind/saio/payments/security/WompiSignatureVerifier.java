package cloud.adamind.saio.payments.security;

import cloud.adamind.saio.payments.dto.Signature;
import cloud.adamind.saio.payments.dto.Transaction;
import cloud.adamind.saio.payments.dto.WompiWebhookEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;

public class WompiSignatureVerifier {


    public boolean isValid(WompiWebhookEvent event) {
        StringBuilder checkSumChain = new StringBuilder();
        Signature signature = event.getSignature();
        Transaction transaction = event.getData().getTransaction();

        checkSumChain.append(transaction.getId());
        checkSumChain.append(transaction.getStatus());
        checkSumChain.append(transaction.getAmountInCents());
        checkSumChain.append(event.getTimestamp());
        checkSumChain.append(System.getenv("WOMPI_EVENTS_SECRET"));

        String calculatedHash = DigestUtils.sha256Hex(checkSumChain.toString());

        System.out.println(calculatedHash);

        return signature.getChecksum().equals(calculatedHash);

    }
}
