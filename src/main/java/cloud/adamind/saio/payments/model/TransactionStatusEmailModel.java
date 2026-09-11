package cloud.adamind.saio.payments.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TransactionStatusEmailModel implements EmailTemplateModel {

    String transaction_id;
    String customer_email;
    String amount;

    /** Código de estado de Wompi: VOIDED, DECLINED, ERROR */
    String transaction_status;

    /** Etiqueta legible en español del estado */
    String status_label;

    /** Descripción explicativa según el estado */
    String status_description;

    @Override
    public String getTemplateName() {
        return "transaction-status-mail.html";
    }
}
