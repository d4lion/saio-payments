package cloud.adamind.saio.payments.repository;

import cloud.adamind.saio.payments.model.DatabaseUserModel;
import com.google.cloud.firestore.Firestore;
import lombok.Builder;

public class UsersRepository {
    private final Firestore db;

    public UsersRepository(Firestore db) {
        this.db = db;
    }

    public void save(DatabaseUserModel user) {
        db.collection("users")
                .document(user.getUid())
                .set(user);
    }

}
