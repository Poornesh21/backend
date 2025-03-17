package com.mobicomm;

import com.mobicomm.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.MailSender;

@SpringBootApplication
public class PrepaidRechargeWebsiteBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrepaidRechargeWebsiteBackendApplication.class, args);
    }

}
