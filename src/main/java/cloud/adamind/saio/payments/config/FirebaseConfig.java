package cloud.adamind.saio.payments.config;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FirebaseConfig {
    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv4Addresses", "true");
        System.setProperty("io.grpc.netty.shaded.io.netty.transport.noNative", "true");
    }

    private final Firestore firestore;
    private final FirebaseAuth auth;


    public FirebaseConfig() {

        String serviceAccountJson = System.getenv("FIREBASE_SERVICE_ACCOUNT");

        InputStream inputStream =
                new ByteArrayInputStream(
                        serviceAccountJson.getBytes(StandardCharsets.UTF_8)
                );

        try {
            GoogleCredentials credentials =
                    GoogleCredentials.fromStream((inputStream));

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            firestore = FirestoreClient.getFirestore();
            auth = FirebaseAuth.getInstance();



        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public Firestore firestore() {
        return firestore;
    }

    public FirebaseAuth auth() {
        return auth;
    }

}
