package cloud.adamind.saio.payments.service.email.model;

import lombok.Builder;

@Builder
public record TicketEmailModel(
        String transaction_id,
        String payment_description,
        String event_date,
        String event_location,
        String customer_name,
        String customer_email,
        String amount,
        String qr_code


) implements EmailTemplateModel {
    @Override
    public String getTemplateName() {
        return "ticket-mail.html";
    }




}