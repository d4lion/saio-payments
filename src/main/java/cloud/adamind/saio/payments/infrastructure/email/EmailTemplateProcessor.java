package cloud.adamind.saio.payments.infrastructure.email;

import cloud.adamind.saio.payments.model.EmailTemplateModel;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.StringWriter;
import java.util.Map;

public class EmailTemplateProcessor {

    private final PebbleEngine engine = new PebbleEngine
            .Builder()
            .build();

    public EmailTemplateProcessor() {
        try {
            // Pre-compila la plantilla en Cold Start
            engine.getTemplate("templates/ticket-mail.html");
        } catch (Exception ignored) {
        }
    }

    public String render(EmailTemplateModel model) {
        try {
            PebbleTemplate template = engine.getTemplate(
                    "templates/" + model.getTemplateName()
            );

            StringWriter writer = new StringWriter();

            template.evaluate(
                    writer,
                    Map.of("model", model)
            );

            return writer.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }


        return "";
    }



}
