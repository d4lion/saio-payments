package cloud.adamind.saio.payments.repository;

import cloud.adamind.saio.payments.model.DatabaseUserModel;
import com.google.cloud.firestore.Firestore;
import lombok.Builder;

public class UsersRepository {
    private final Firestore db;

    public UsersRepository(Firestore db) {
        this.db = db;
    }

    public void save(DatabaseUserModel user, String authUserId) {
        db.collection("users")
                .document(authUserId)
                .set(user);
    }

}
