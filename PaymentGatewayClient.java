package com.abralabs.payment;

import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class PaymentGatewayClient {

    // 💣 TRIGGER: Pillar 1 -> Secret Hunting -> Rule 3 (Hardcoded Credentials)
    // Model buradaki 'Authorization' string'ini ve hardcoded API anahtarını yakalamalıdır.
    // SQL Injection YOK, Deserialization YOK. Sadece Secret Leak var.
    private static final String STRIPE_API_KEY = "sk_live_51M0dGqJ9f8d7s6F5g4h3j2k1l0"; 
    private static final String AWS_ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";

    private final HttpClient httpClient;

    public PaymentGatewayClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public void processPayment(String amount, String currency) {
        String requestBody = String.format("{\"amount\": \"%s\", \"currency\": \"%s\"}", amount, currency);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.stripe.com/v1/charges"))
                .header("Content-Type", "application/json")
                // Hardcoded secret kullanımı burada gerçekleşiyor
                .header("Authorization", "Bearer " + STRIPE_API_KEY) 
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
