package cloud.adamind.saio.payments.infrastructure.firebase;

import cloud.adamind.saio.payments.config.FirebaseConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirebaseAuthAdapter {

    private final static Logger log =  LoggerFactory.getLogger(FirebaseAuthAdapter.class);

    private final FirebaseAuth auth;


    public FirebaseAuthAdapter(FirebaseConfig config) {
        this.auth = config.auth();
        log.info("Firebase Auth service started");
    }

    public UserRecord createUser(String email, String password) throws FirebaseAuthException {

        log.info("Creating user {}", email);

        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password);

        log.info("User {} created", email);

        return auth.createUser(request);

    }
}
