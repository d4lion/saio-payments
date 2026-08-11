package cloud.adamind.saio.payments.service;

import cloud.adamind.saio.payments.config.FirebaseConfig;
import cloud.adamind.saio.payments.dto.CustomerReference;
import cloud.adamind.saio.payments.dto.Transaction;
import cloud.adamind.saio.payments.dto.WompiWebhookEvent;
import cloud.adamind.saio.payments.exception.TransactionProcessedException;
import cloud.adamind.saio.payments.exception.UnauthorizedException;
import cloud.adamind.saio.payments.model.DatabaseUserModel;
import cloud.adamind.saio.payments.repository.TransactionsRepository;
import cloud.adamind.saio.payments.repository.UsersRepository;
import cloud.adamind.saio.payments.security.WompiSignatureVerifier;
import cloud.adamind.saio.payments.service.email.EmailTemplateProcessor;
import cloud.adamind.saio.payments.service.email.model.TicketEmailModel;
import cloud.adamind.saio.payments.util.Role;
import cloud.adamind.saio.payments.util.WompiTransactionUpdates;
import com.google.cloud.Timestamp;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.resend.core.exception.ResendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;

public class PaymentService {
    // Utils
    private final FirebaseConfig firebaseConfig = new FirebaseConfig();

    // Seguridad
    private final WompiSignatureVerifier verifier = new WompiSignatureVerifier();

    // Repositorios
    private final UsersRepository usersRepository = new UsersRepository(firebaseConfig.firestore());
    private final TransactionsRepository transactionsRepository = new TransactionsRepository(firebaseConfig.firestore());

    // Constantes de correo
    private static final String MAILING_FROM = System.getenv("MAILING_FROM");
    private static final String MAILING_PAYMENT_SUCCESS_SUBJECT = System.getenv("MAILING_PAYMENT_SUCCESS_SUBJECT");

    // Constantes del evento
    private static final String SAIO_EVENT_LOCATION =  System.getenv("SAIO_EVENT_LOCATION");
    private static final String SAIO_EVENT_DAY = System.getenv("SAIO_EVENT_DAY");
    private static final String TRANSACTION_UPDATED_EVENT = "transaction.updated";

    // Logs y Trace
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public void processApprovedTransaction(WompiWebhookEvent event) {
        Transaction transaction = event.getData().getTransaction();

        // Customer
        String customerLegalId = transaction.getCustomerData().getLegalId();

        // Transaction
        String transactionId =  transaction.getId();

        sendTransactionEmailConfirmation(event, generateUrlQrCode(customerLegalId, transactionId));

        createUserAndSaveTransaction(event);

        log.info("Event processed successfully: {}", event.getEvent());

    }

    public void process(WompiWebhookEvent event) {

        Transaction transaction = event.getData().getTransaction();
        String transactionStatus = transaction.getStatus();

        log.info("Processing event: {}", event);

        // La transaccion ya fue procesada antes
        if (transactionsRepository.exists(transaction.getId())) {
            log.info("Ignoring transaction {} because it has already been processed", transaction.getId());
            throw new TransactionProcessedException(MessageFormat.format("La transacción {0} ya ha sido procesada", transaction.getId()));
        }

        // La firma no es valida, se precesantaron modificaciones en el payload
        if (!verifier.isValid(event)) {
            log.warn("Invalid event received: {}", event);
            throw new UnauthorizedException("Firma del evento invalida");
        }

        // El evento no es de tipo transaction.updated, se ignora
        if (!TRANSACTION_UPDATED_EVENT.equalsIgnoreCase(event.getEvent())) {
            log.info("Ignoring unsupported event type: {}", event.getEvent());
            return;
        }

        // La transacción es diferente de aprovada
        if (!WompiTransactionUpdates.APPROVED.getStatus().equalsIgnoreCase(transactionStatus)) {
            log.info("Ignoring transaction {} because its status is {}", transaction.getId(), transactionStatus);
            return;
        }

        processApprovedTransaction(event);

    }

    private String generateUrlQrCode(String customerLegalId, String transactionId) {
        final S3Service s3Service = new S3Service();
        final QrCodeService qrCodeService = new QrCodeService();


        // Se realiza la generacion del QrCode en tiempo de ejecución
        byte[] qrCode = qrCodeService.generate(customerLegalId);

        return s3Service.uploadBytes(
                "saioxv",
                String.format("users/%s/qr-code/%s.png", customerLegalId, transactionId),
                qrCode,
                "image/png"
        );


    }

    private void createUserAndSaveTransaction(WompiWebhookEvent event) {
        final FirebaseAuthService auth = new FirebaseAuthService(firebaseConfig);

        Transaction transaction = event.getData().getTransaction();

        // Customer
        String customerEmail = transaction.getCustomerEmail();
        String customerFullName = transaction.getCustomerData().getFullName();
        String customerLegalId = transaction.getCustomerData().getLegalId();
        String customerPhoneNumber = transaction.getCustomerData().getPhoneNumber();

        // Datos usados para la creacion del usuario
        String accountUserEmail = "";
        String accountUserLegalId = "";

        for (CustomerReference customerReference : transaction.getCustomerData().getCustomerReferences()) {
            if ("correo del asistente".equalsIgnoreCase(customerReference.getLabel()) ) {
                accountUserEmail = customerReference.getValue();
            } else if ("cedula del asistente".equalsIgnoreCase(customerReference.getLabel())) {
                accountUserLegalId = customerReference.getValue();
            }
        }


        try {

            UserRecord user = auth.createUser(
                    accountUserEmail,
                    accountUserLegalId
            );

            usersRepository.save(
                    DatabaseUserModel.builder()
                            .cedula(accountUserLegalId)
                            .correo(accountUserEmail)
                            .fechaCreacion(String.valueOf(Timestamp.now()))
                            .nombre(customerFullName)
                            .puntos(0)
                            .rol(String.valueOf(Role.ASISTENTE).toLowerCase())
                            .telefono(customerPhoneNumber)
                            .uid(user.getUid())
                            .build(),
                    user.getUid()
            );


        } catch (FirebaseAuthException e) {
            throw new RuntimeException(e);
        }

        saveTransaction(event);
    }

    private void saveTransaction(WompiWebhookEvent event) {
        try {
            transactionsRepository.save(event);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendTransactionEmailConfirmation(WompiWebhookEvent event, String qrCodeUrl) {
        final EmailService emailService = new EmailService();
        final EmailTemplateProcessor templateProcessor = new EmailTemplateProcessor();

        Transaction transaction = event.getData().getTransaction();

        // Customer
        String customerEmail = transaction.getCustomerEmail();
        String customerFullName = transaction.getCustomerData().getFullName();

        // Transaction
        String transactionId =  transaction.getId();
        String transactionAmountInCents = transaction.getAmountInCents().toString();
        String transactionPaymentDescription = transaction.getPaymentMethod().getPaymentDescription();

        TicketEmailModel model = TicketEmailModel.builder()
                .transaction_id(transactionId)
                .amount(transactionAmountInCents)
                .customer_email(customerEmail)
                .customer_name(customerFullName)
                .event_date(SAIO_EVENT_DAY)
                .event_location(SAIO_EVENT_LOCATION)
                .payment_description(transactionPaymentDescription)
                .qr_code(qrCodeUrl)
                .build();

        String HTML = templateProcessor.render(model);

        try {
            emailService.sendEmail(
                    MAILING_FROM,
                    customerEmail,
                    MAILING_PAYMENT_SUCCESS_SUBJECT,
                    HTML
            );
        } catch (ResendException e) {
            throw new RuntimeException(MessageFormat.format("Error realizando el envio del correo para: {0}", customerEmail), e);
        }
    }


}




