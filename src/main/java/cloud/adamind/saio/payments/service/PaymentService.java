package cloud.adamind.saio.payments.service;

import cloud.adamind.saio.payments.dto.WompiWebhookEvent;

public class PaymentService {
    public void processEvent(WompiWebhookEvent event) {

        System.out.println("Processing event " + event);

        if (event.getEvent().equalsIgnoreCase("transaction.updated") ) {
            System.out.println("Validating transaction update of transaction: " + event.getData().getTransaction().getId());
            System.out.println("Now redirecting to: " + event.getData().getTransaction().getRedirectUrl());

        }

        }


    }




