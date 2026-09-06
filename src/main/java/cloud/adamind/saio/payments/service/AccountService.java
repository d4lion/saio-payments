package cloud.adamind.saio.payments.service;


import cloud.adamind.saio.payments.config.FirebaseConfig;
import cloud.adamind.saio.payments.infrastructure.firebase.FirebaseAuthAdapter;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountService {

    // Logger
    private final static Logger log = LoggerFactory.getLogger(AccountService.class);

    private final FirebaseAuthAdapter auth;

    public AccountService(FirebaseConfig firebaseConfig) {
        this.auth = new FirebaseAuthAdapter(firebaseConfig);
        log.debug("AccountService started");
    }

    public UserRecord createAccount(String email, String password) {
        try {
            log.info("Creating account for email {}", email);
            return auth.createUser(
                    email,
                    password
            );
        } catch (FirebaseAuthException e) {
            log.error("Error creating account for email {}: {}", email, e.getMessage());
        }
        return null;
    }


}
