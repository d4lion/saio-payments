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
            // Pre-compila plantillas en Cold Start
            engine.getTemplate("templates/ticket-mail.html");
            engine.getTemplate("templates/ticket-pdf.html");
        } catch (Exception ignored) {
        }
    }

    public String renderTemplate(String templatePath, Object model) {
        try {
            PebbleTemplate template = engine.getTemplate(templatePath);
            StringWriter writer = new StringWriter();
            template.evaluate(writer, Map.of("model", model));
            return writer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error renderizando plantilla " + templatePath, e);
        }
    }

    public String render(EmailTemplateModel model) {
        return renderTemplate("templates/" + model.getTemplateName(), model);
    }



}
