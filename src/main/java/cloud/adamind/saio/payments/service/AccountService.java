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

    public AccountService(FirebaseAuthAdapter auth) {
        this.auth = auth;
        log.debug("AccountService started with injected auth adapter");
    }

    public UserRecord createOrGetAccount(String email, String password) {
        try {
            log.info("Creating account for email {}", email);
            return auth.createUser(email, password);
        } catch (FirebaseAuthException e) {
            log.warn("Could not create account for email {}: {}. Attempting to fetch existing user.", email, e.getMessage());
            try {
                return auth.getUserByEmail(email);
            } catch (FirebaseAuthException ex) {
                log.error("Failed to create or fetch account for email {}: {}", email, ex.getMessage());
                throw new RuntimeException("Error al crear o recuperar la cuenta para " + email, ex);
            }
        }
    }

    public UserRecord createAccount(String email, String password) {
        return createOrGetAccount(email, password);
    }
}
