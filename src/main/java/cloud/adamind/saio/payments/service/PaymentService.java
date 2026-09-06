package cloud.adamind.saio.payments.service;

import cloud.adamind.saio.payments.config.FirebaseConfig;
import cloud.adamind.saio.payments.dto.CustomerReference;
import cloud.adamind.saio.payments.dto.Transaction;
import cloud.adamind.saio.payments.dto.WompiWebhookEvent;
import cloud.adamind.saio.payments.exception.TransactionProcessedException;
import cloud.adamind.saio.payments.exception.UnauthorizedException;
import cloud.adamind.saio.payments.infrastructure.email.EmailTemplateProcessor;
import cloud.adamind.saio.payments.infrastructure.email.ResendEmailAdapter;
import cloud.adamind.saio.payments.infrastructure.pdf.PdfGeneratorAdapter;
import cloud.adamind.saio.payments.infrastructure.qr.ZxingQrCodeGenerator;
import cloud.adamind.saio.payments.infrastructure.s3.S3StorageAdapter;
import cloud.adamind.saio.payments.model.DatabaseUserModel;
import cloud.adamind.saio.payments.model.TicketEmailModel;
import cloud.adamind.saio.payments.repository.TransactionsRepository;
import cloud.adamind.saio.payments.repository.UsersRepository;
import cloud.adamind.saio.payments.security.WompiSignatureVerifier;
import cloud.adamind.saio.payments.util.Role;
import cloud.adamind.saio.payments.util.WompiTransactionUpdates;
import com.google.cloud.Timestamp;
import com.google.firebase.auth.UserRecord;
import com.resend.core.exception.ResendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentService {
    // Executor de Virtual Threads para tareas E/S concurrentes en Java 21
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    // Dependencias e Infraestructura
    private final FirebaseConfig firebaseConfig;
    private final WompiSignatureVerifier verifier;
    private final UsersRepository usersRepository;
    private final TransactionsRepository transactionsRepository;
    private final AccountService accountService;
    private final S3StorageAdapter s3StorageAdapter;
    private final ZxingQrCodeGenerator zxingQrCodeGenerator;
    private final ResendEmailAdapter resendEmailAdapter;
    private final EmailTemplateProcessor templateProcessor;
    private final PdfGeneratorAdapter pdfGeneratorAdapter;

    // Constantes de entorno y etiquetas
    private static final String S3_BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "saioxv");
    private static final String REF_EMAIL_LABEL = "correo del asistente";
    private static final String REF_CEDULA_LABEL = "cedula del asistente";

    // Constantes de correo
    private static final String MAILING_FROM = System.getenv("MAILING_FROM");
    private static final String MAILING_PAYMENT_SUCCESS_SUBJECT = System.getenv("MAILING_PAYMENT_SUCCESS_SUBJECT");

    // Constantes del evento y formato
    private static final String SAIO_EVENT_LOCATION = System.getenv("SAIO_EVENT_LOCATION");
    private static final String SAIO_EVENT_DAY = System.getenv("SAIO_EVENT_DAY");
    private static final String TRANSACTION_UPDATED_EVENT = "transaction.updated";
    private static final Locale COLOMBIAN_LOCALE = Locale.forLanguageTag("es-CO");

    // Logs y Trace
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    /**
     * Constructor por defecto para integración con AWS Lambda o entornos sin DI.
     */
    public PaymentService() {
        this.firebaseConfig = new FirebaseConfig();
        this.verifier = new WompiSignatureVerifier();
        this.usersRepository = new UsersRepository(firebaseConfig.firestore());
        this.transactionsRepository = new TransactionsRepository(firebaseConfig.firestore());
        this.accountService = new AccountService(firebaseConfig);
        this.s3StorageAdapter = new S3StorageAdapter();
        this.zxingQrCodeGenerator = new ZxingQrCodeGenerator();
        this.resendEmailAdapter = new ResendEmailAdapter();
        this.templateProcessor = new EmailTemplateProcessor();
        this.pdfGeneratorAdapter = new PdfGeneratorAdapter();
    }

    /**
     * Constructor con Inyección de Dependencias para testing y desacoplamiento.
     */
    public PaymentService(
            FirebaseConfig firebaseConfig,
            WompiSignatureVerifier verifier,
            UsersRepository usersRepository,
            TransactionsRepository transactionsRepository,
            AccountService accountService,
            S3StorageAdapter s3StorageAdapter,
            ZxingQrCodeGenerator zxingQrCodeGenerator,
            ResendEmailAdapter resendEmailAdapter,
            EmailTemplateProcessor templateProcessor,
            PdfGeneratorAdapter pdfGeneratorAdapter
    ) {
        this.firebaseConfig = firebaseConfig;
        this.verifier = verifier;
        this.usersRepository = usersRepository;
        this.transactionsRepository = transactionsRepository;
        this.accountService = accountService;
        this.s3StorageAdapter = s3StorageAdapter;
        this.zxingQrCodeGenerator = zxingQrCodeGenerator;
        this.resendEmailAdapter = resendEmailAdapter;
        this.templateProcessor = templateProcessor;
        this.pdfGeneratorAdapter = pdfGeneratorAdapter;
    }

    /**
     * Procesa una transacción aprobada.
     *
     * @param event El evento del webhook de Wompi.
     */
    public void processApprovedTransaction(WompiWebhookEvent event) {
        if (event == null || event.getData() == null || event.getData().getTransaction() == null) {
            throw new IllegalArgumentException("El evento o la transacción recibida son nulos");
        }

        // Transaction Data
        Transaction transaction = event.getData().getTransaction();
        String amountInCents = transaction.getAmountInCents() != null ? transaction.getAmountInCents().toString() : "0";

        // Formateo de moneda colombiana (ej: 70000 -> 70.000)
        BigDecimal amountInPesos = new BigDecimal(amountInCents).divide(BigDecimal.valueOf(100));
        NumberFormat numberFormat = NumberFormat.getInstance(COLOMBIAN_LOCALE);
        String amountParsed = numberFormat.format(amountInPesos);

        String paymentDescription = "";
        if (transaction.getPaymentMethod() != null && transaction.getPaymentMethod().getPaymentDescription() != null) {
            paymentDescription = transaction.getPaymentMethod().getPaymentDescription().trim();
        }

        if (paymentDescription.isBlank()) {
            paymentDescription = amountInPesos.compareTo(BigDecimal.valueOf(60000)) > 0 ? "Boleta Supernova" : "Boleta Orbita";
        }

        // Customer
        String customerLegalId = transaction.getCustomerData() != null ? transaction.getCustomerData().getLegalId() : "";
        String customerFullName = transaction.getCustomerData() != null ? transaction.getCustomerData().getFullName() : "";
        String customerPhoneNumber = transaction.getCustomerData() != null ? transaction.getCustomerData().getPhoneNumber() : "";

        // Transaction ID
        String transactionId = transaction.getId();

        // Datos usados para la creación del usuario (con fallback a datos principales)
        String accountUserEmail = transaction.getCustomerEmail();
        String accountUserLegalId = customerLegalId;

        if (transaction.getCustomerData() != null && transaction.getCustomerData().getCustomerReferences() != null) {
            for (CustomerReference customerReference : transaction.getCustomerData().getCustomerReferences()) {
                if (REF_EMAIL_LABEL.equalsIgnoreCase(customerReference.getLabel()) && customerReference.getValue() != null) {
                    accountUserEmail = customerReference.getValue();
                } else if (REF_CEDULA_LABEL.equalsIgnoreCase(customerReference.getLabel()) && customerReference.getValue() != null) {
                    accountUserLegalId = customerReference.getValue();
                }
            }
        }

        final String finalAccountUserEmail = accountUserEmail;
        final String finalAccountUserLegalId = accountUserLegalId;
        final String finalPaymentDescription = paymentDescription;
        final String finalCustomerFullName = customerFullName;

        // Rama 1: Creación de usuario en Firebase Auth y guardado en Firestore
        CompletableFuture<UserRecord> accountTask = CompletableFuture.supplyAsync(() -> {
            UserRecord account = accountService.createOrGetAccount(
                    finalAccountUserEmail,
                    finalAccountUserLegalId
            );

            usersRepository.save(
                    DatabaseUserModel.builder()
                            .uid(account.getUid())
                            .nombre(finalCustomerFullName)
                            .rol(String.valueOf(Role.ASISTENTE))
                            .telefono(customerPhoneNumber)
                            .cedula(finalAccountUserLegalId)
                            .correo(finalAccountUserEmail)
                            .fechaCreacion(Timestamp.now().toString())
                            .boleta(finalPaymentDescription)
                            .build()
            );
            return account;
        }, EXECUTOR);

        // Rama 2: Generación del código QR basado en el Account ID (account.getUid()) y renderizado del PDF oficial
        CompletableFuture<byte[]> pdfTask = accountTask.thenApplyAsync(account -> {
            String accountId = account.getUid();
            byte[] qrBytes = zxingQrCodeGenerator.generate(accountId);
            String qrBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes);

            TicketEmailModel pdfModel = TicketEmailModel.builder()
                    .transaction_id(transactionId)
                    .amount(amountParsed)
                    .customer_email(finalAccountUserEmail)
                    .customer_legal_id(finalAccountUserLegalId)
                    .payment_description(finalPaymentDescription)
                    .qr_code(qrBase64)
                    .build();

            String pdfHtml = templateProcessor.renderTemplate("templates/ticket-pdf.html", pdfModel);
            return pdfGeneratorAdapter.generatePdfFromHtml(pdfHtml);
        }, EXECUTOR);

        // Punto de Sincronización: Esperar a que la generación del PDF (y la cuenta de usuario) finalice
        byte[] pdfBytes = pdfTask.join();

        // Generación del modelo para el correo HTML
        TicketEmailModel mailModel = TicketEmailModel.builder()
                .transaction_id(transactionId)
                .amount(amountParsed)
                .customer_name(customerFullName)
                .customer_email(finalAccountUserEmail)
                .payment_description(paymentDescription)
                .build();

        // Envío de confirmación de compra exitosa con el PDF adjunto
        sendEmailConfirmationOfApprovedTransaction(mailModel, finalAccountUserEmail, pdfBytes);

        // Se guarda la transacción exitosa al finalizar todo el flujo adecuadamente
        transactionsRepository.save(event);

        log.info("Event processed successfully: {}", event.getEvent());
    }

    /**
     * Se encarga de procesar y verificar la veracidad de la transacción en sus estados aprobados y declinados
     * @param event El evento crudo que llega desde el webhook de Wompi
     */
    public void process(WompiWebhookEvent event) {
        if (event == null || event.getData() == null || event.getData().getTransaction() == null) {
            throw new IllegalArgumentException("El evento recibido no contiene datos de transacción válidos");
        }

        Transaction transaction = event.getData().getTransaction();
        String transactionStatus = transaction.getStatus();

        log.info("Processing event: {}", event);

        // La transacción ya fue procesada antes
        if (transactionsRepository.exists(transaction.getId())) {
            log.info("Ignoring transaction {} because it has already been processed", transaction.getId());
            throw new TransactionProcessedException(MessageFormat.format("La transacción {0} ya ha sido procesada", transaction.getId()));
        }

        // La firma no es válida, se presentaron modificaciones en el payload
        if (!verifier.isValid(event)) {
            log.warn("Invalid event received: {}", event);
            throw new UnauthorizedException("Firma del evento inválida");
        }

        // El evento no es de tipo transaction.updated, se ignora
        if (!TRANSACTION_UPDATED_EVENT.equalsIgnoreCase(event.getEvent())) {
            log.info("Ignoring unsupported event type: {}", event.getEvent());
            return;
        }

        // La transacción es diferente de aprobada
        if (!WompiTransactionUpdates.APPROVED.getStatus().equalsIgnoreCase(transactionStatus)) {
            transactionsRepository.save(event);
            log.info("Ignoring transaction {} because its status is {}", transaction.getId(), transactionStatus);
            return;
        }

        processApprovedTransaction(event);
    }

    private String generateUrlQrCode(String customerLegalId, String transactionId) {
        // Se realiza la generación del QrCode en tiempo de ejecución
        byte[] qrCode = zxingQrCodeGenerator.generate(customerLegalId);

        return s3StorageAdapter.uploadBytes(
                S3_BUCKET_NAME,
                String.format("users/%s/qr-code/%s.png", customerLegalId, transactionId),
                qrCode,
                "image/png"
        );
    }

    private void sendEmailConfirmationOfApprovedTransaction(TicketEmailModel model, String emailTo, byte[] pdfBytes) {
        String html = templateProcessor.render(model);

        try {
            resendEmailAdapter.sendEmailWithAttachment(
                    MAILING_FROM,
                    emailTo,
                    MAILING_PAYMENT_SUCCESS_SUBJECT,
                    html,
                    "entrada.pdf",
                    pdfBytes
            );
        } catch (ResendException e) {
            throw new RuntimeException(MessageFormat.format("Error realizando el envío del correo para: {0}", model.getCustomer_email()), e);
        }
    }
}




