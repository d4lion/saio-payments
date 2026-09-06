package cloud.adamind.saio.payments.service;

import cloud.adamind.saio.payments.config.FirebaseConfig;
import cloud.adamind.saio.payments.dto.CustomerReference;
import cloud.adamind.saio.payments.dto.Transaction;
import cloud.adamind.saio.payments.dto.WompiWebhookEvent;
import cloud.adamind.saio.payments.exception.TransactionProcessedException;
import cloud.adamind.saio.payments.exception.UnauthorizedException;
import cloud.adamind.saio.payments.infrastructure.email.ResendEmailAdapter;
import cloud.adamind.saio.payments.infrastructure.qr.ZxingQrCodeGenerator;
import cloud.adamind.saio.payments.infrastructure.s3.S3StorageAdapter;
import cloud.adamind.saio.payments.model.DatabaseUserModel;
import cloud.adamind.saio.payments.repository.TransactionsRepository;
import cloud.adamind.saio.payments.repository.UsersRepository;
import cloud.adamind.saio.payments.security.WompiSignatureVerifier;
import cloud.adamind.saio.payments.infrastructure.email.EmailTemplateProcessor;
import cloud.adamind.saio.payments.model.TicketEmailModel;
import cloud.adamind.saio.payments.util.Role;
import cloud.adamind.saio.payments.util.WompiTransactionUpdates;
import com.google.cloud.Timestamp;
import com.google.firebase.auth.UserRecord;
import com.resend.core.exception.ResendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.text.MessageFormat;

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

    /**
     * Procesa una transacción aprobada.
     *
     * @param event El evento del webhook de Wompi.
     */
    public void processApprovedTransaction(WompiWebhookEvent event) {
        // Se guarda el registro de que es genuina la existencia de la transacción
        transactionsRepository.save(event);

        AccountService accountService = new AccountService(firebaseConfig);

        // Transaction Data
        Transaction transaction = event.getData().getTransaction();
        String amountInCents = transaction.getAmountInCents().toString();
        String amountParsed = String.format("%.2f", Double.parseDouble(amountInCents) / 100);
        String paymentDescription = transaction.getPaymentMethod().getPaymentDescription();

        // Customer
        String customerLegalId = transaction.getCustomerData().getLegalId();
        String customerFullName = transaction.getCustomerData().getFullName();
        String customerPhoneNumber =  transaction.getCustomerData().getPhoneNumber();

        // Transaction
        String transactionId =  transaction.getId();

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

        // Creación del usuario en el sistema Auth de Firebase
        UserRecord account = accountService.createAccount(
                accountUserEmail,
                accountUserLegalId
        );

        // Creación del usuario para su uso en las entidades del sistema
        usersRepository.save(
                DatabaseUserModel.builder()
                        .uid(account.getUid())
                        .nombre(customerFullName)
                        .rol(String.valueOf(Role.ASISTENTE))
                        .telefono(customerPhoneNumber)
                        .cedula(accountUserLegalId)
                        .correo(accountUserEmail)
                        .fechaCreacion(Timestamp.now().toString())
                        .boleta(transaction.getPaymentMethod().getPaymentDescription())
                        .build()
        );


        // Generación del ticket para el correo
        TicketEmailModel ticketModel = TicketEmailModel.builder()
                .transaction_id(transactionId)
                .amount(amountParsed)
                .customer_email(accountUserEmail)
                .payment_description(paymentDescription)
                .qr_code(generateUrlQrCode(accountUserLegalId, transactionId))
                .build();

        // Envio de confirmacíon de compra exitosa
        sendEmailConfirmationOfApprovedTransaction(ticketModel, accountUserEmail);

        log.info("Event processed successfully: {}", event.getEvent());

    }

    /**
     * Se encarga de procesar y verificar la veracidad de la transacción en sus estados aprobados y declinados
     * @param event El evento crudo que llegas desde el webhook de wompi
     */

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
            transactionsRepository.save(event);
            log.info("Ignoring transaction {} because its status is {}", transaction.getId(), transactionStatus);
            return;
        }

        processApprovedTransaction(event);

    }

    private String generateUrlQrCode(String customerLegalId, String transactionId) {
        final S3StorageAdapter s3StorageAdapter = new S3StorageAdapter();
        final ZxingQrCodeGenerator zxingQrCodeGenerator = new ZxingQrCodeGenerator();


        // Se realiza la generacion del QrCode en tiempo de ejecución
        byte[] qrCode = zxingQrCodeGenerator.generate(customerLegalId);

        return s3StorageAdapter.uploadBytes(
                "saioxv",
                String.format("users/%s/qr-code/%s.png", customerLegalId, transactionId),
                qrCode,
                "image/png"
        );


    }

    private void sendEmailConfirmationOfApprovedTransaction(TicketEmailModel model, String emailTo) {
        final ResendEmailAdapter resendEmailAdapter = new ResendEmailAdapter();
        final EmailTemplateProcessor templateProcessor = new EmailTemplateProcessor();

        String HTML = templateProcessor.render(model);

        try {
            resendEmailAdapter.sendEmail(
                    MAILING_FROM,
                    emailTo,
                    MAILING_PAYMENT_SUCCESS_SUBJECT,
                    HTML
            );

        } catch (ResendException e) {
            throw new RuntimeException(MessageFormat.format("Error realizando el envio del correo para: {0}", model.getCustomer_email()), e);
        }
    }

}




