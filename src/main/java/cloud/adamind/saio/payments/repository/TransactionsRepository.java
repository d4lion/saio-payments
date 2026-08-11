package cloud.adamind.saio.payments.repository;

import cloud.adamind.saio.payments.dto.WompiWebhookEvent;
import com.google.cloud.firestore.Firestore;
import lombok.Builder;

import java.util.concurrent.ExecutionException;

public class TransactionsRepository {

    private final Firestore db;

    public TransactionsRepository(Firestore db) {
        this.db = db;
    }


    public void save(WompiWebhookEvent event) throws ExecutionException, InterruptedException {
        db.collection("transactions")
                .document(event.getData().getTransaction().getId())
                .set(event)
                .get();
    }

    public boolean exists(String transactionId) {
        try {
            return db.collection("transactions")
                    .document(transactionId)
                    .get()
                    .get()
                    .exists();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }



}
