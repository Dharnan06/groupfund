package com.nammakuzhu.authModule.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
public class MailService {

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.sender-email}")
    private String senderEmail;

    public void sendProfessionalOtpEmail(String toEmail, Integer otp) {
        try {
            String subject = "GroupFund OTP Verification";

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; background-color:#f4f7fb; padding:30px;">
                        <div style="max-width:600px; margin:auto; background:white; border-radius:12px; padding:30px; box-shadow:0 4px 12px rgba(0,0,0,0.08);">
                            <h2 style="color:#2563eb; text-align:center;">GroupFund</h2>
                            <p style="font-size:16px; color:#333;">Thank you for registering with GroupFund.</p>
                            <p style="font-size:16px; color:#333;">Use the OTP below to verify your email address:</p>
                            <div style="text-align:center; margin:30px 0;">
                                <span style="font-size:32px; font-weight:bold; letter-spacing:6px; color:#111827; background:#eef2ff; padding:15px 25px; border-radius:8px;">
                                    %s
                                </span>
                            </div>
                            <p style="font-size:14px; color:#666;">This OTP is valid for a limited time. Please do not share it with anyone.</p>
                            <hr style="border:none; border-top:1px solid #eee; margin:25px 0;">
                            <p style="font-size:12px; color:#999; text-align:center;">© 2026 GroupFund. All rights reserved.</p>
                        </div>
                    </div>
                    """.formatted(otp);

            String jsonPayload = """
                    {
                      "sender": {
                        "name": "GroupFund",
                        "email": "%s"
                      },
                      "to": [
                        {
                          "email": "%s"
                        }
                      ],
                      "subject": "%s",
                      "htmlContent": "%s"
                    }
                    """.formatted(
                    escapeJson(senderEmail),
                    escapeJson(toEmail),
                    escapeJson(subject),
                    escapeJson(htmlContent)
            );

            URI uri = URI.create("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("accept", "application/json");
            connection.setRequestProperty("api-key", brevoApiKey);
            connection.setRequestProperty("content-type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();

            if (responseCode < 200 || responseCode >= 300) {
                throw new RuntimeException("Brevo email failed. Status code: " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send OTP email using Brevo API: " + e.getMessage(), e);
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "")
                .replace("\r", "");
    }
}