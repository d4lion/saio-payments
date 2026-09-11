package cloud.adamind.saio.payments.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TicketEmailModel implements EmailTemplateModel {
    String transaction_id;

    String customer_email;
    String amount;
    String qr_code;

    @Builder.Default
    String customer_legal_id = "N/A";

    @Builder.Default
    String event_location = "Universidad Nacional - Sede Medellín";

    @Builder.Default
    String event_date = "15 y 16 de Octubre 2026";

    @Builder.Default
    String customer_name = "N/A";

    @Builder.Default
    String payment_description = "Saio Ticket";

    @Builder.Default
    String instruction_video_url = System.getenv().getOrDefault("INSTRUCTION_VIDEO_URL", "https://www.youtube.com/watch?v=dQw4w9WgXcQ");


    @Override
    public String getTemplateName() {
        return "ticket-mail.html";
    }




}