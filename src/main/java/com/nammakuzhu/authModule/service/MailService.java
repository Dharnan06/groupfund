package com.nammakuzhu.authModule.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import java.nio.charset.StandardCharsets;


@Service
public class MailService {

    private final JavaMailSender javaMailSender;

    MailService(JavaMailSender javaMailSender){
        this.javaMailSender=javaMailSender;
    }

    public void sendOtpEmail(String toEmail, Integer otp){
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("GroupFund Email Verification OTP");
        message.setText("Your OTP is " + otp + ". It is valid for 5 minutes.");

        javaMailSender.send(message);
    }

    @Value("classpath:templates/otp-email.html")
    private Resource otpEmailTemplate;

    @Async
    public void sendProfessionalOtpEmail(String toEmail, Integer otp){
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(toEmail);
            helper.setSubject("GroupFund Email Verification OTP");
            String htmlContent = new String(
                    otpEmailTemplate.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            htmlContent = htmlContent.replace("{{OTP}}", otp.toString());
            helper.setText(htmlContent, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email");
        }
    }
}
