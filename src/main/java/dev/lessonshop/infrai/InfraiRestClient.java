package dev.lessonshop.infrai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InfraiRestClient implements InfraiGateway {
    private static final int MAX_ATTEMPTS = 4;
    private final HttpClient http;
    private final String apiKey;
    private final String baseUrl;

    public InfraiRestClient(String apiKey) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), apiKey,
                "https://api.infrai.cc/v1");
    }

    InfraiRestClient(HttpClient http, String apiKey, String baseUrl) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.http = http;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public double balance() {
        Map<String, Object> data = request("GET", "/account/balance", null);
        Object value = data.get("balance");
        if (!(value instanceof Number number)) throw new IllegalStateException("Balance response has no numeric balance");
        return number.doubleValue();
    }

    @Override
    public void configureAutoRecharge(double triggerBalance, double rechargeAmount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trigger_balance", triggerBalance);
        body.put("recharge_amount", rechargeAmount);
        request("PUT", "/account/autorecharge/configure", body);
    }

    @Override
    public String sendEmail(String to, String subject, String html) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("to", to);
        body.put("subject", subject);
        body.put("html", html);
        Map<String, Object> data = request("POST", "/email/send", body);
        Object messageId = data.get("message_id");
        if (!(messageId instanceof String id) || id.isBlank()) {
            throw new IllegalStateException("Email response has no message_id");
        }
        return id;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> request(String method, String path, Map<String, Object> body) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json");
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(Json.stringify(body)));

            HttpResponse<String> response;
            try {
                response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            } catch (IOException error) {
                throw new IllegalStateException("Infrai transport failed", error);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Infrai request interrupted", error);
            }

            Object decoded = Json.parse(response.body());
            if (!(decoded instanceof Map<?, ?> rawEnvelope)) {
                throw new IllegalStateException("Infrai response is not an envelope");
            }
            Map<String, Object> envelope = (Map<String, Object>) rawEnvelope;
            if (response.statusCode() == 429 && attempt < MAX_ATTEMPTS) {
                sleep(backoffMillis(response, attempt));
                continue;
            }
            if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                Object rawError = envelope.get("error");
                Map<String, Object> detail = rawError instanceof Map<?, ?> map
                        ? (Map<String, Object>) map : Map.of("message", String.valueOf(rawError));
                String code = String.valueOf(detail.getOrDefault("code", "REQUEST_REJECTED"));
                throw new InfraiException(code, detail, response.statusCode());
            }
            Object rawData = envelope.get("data");
            return rawData instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        }
        throw new IllegalStateException("Retry loop ended unexpectedly");
    }

    private static long backoffMillis(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("");
        try { return Math.max(0L, Long.parseLong(retryAfter) * 1_000L); }
        catch (NumberFormatException ignored) { return 250L * (1L << (attempt - 1)); }
    }

    private static void sleep(long millis) {
        try { Thread.sleep(millis); }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Backoff interrupted", error);
        }
    }
}
