package com.photoconnect.auth.service;

import com.photoconnect.auth.config.Msg91Properties;
import com.photoconnect.auth.exception.OtpDeliveryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Sends OTP codes via MSG91's transactional Flow API.
 *
 * <p>Active only when {@code app.otp.provider=msg91}. The dev-mode impl
 * ({@link DevModeOtpDeliveryService}) has a complementary condition so
 * exactly one bean is created.</p>
 *
 * <h2>MSG91 contract</h2>
 * <pre>
 * POST {baseUrl}/api/v5/flow/
 * Headers: authkey: &lt;key&gt;, Content-Type: application/json
 * Body:    {"template_id":"...","sender":"...","short_url":"0",
 *           "recipients":[{"mobiles":"919876543210","var":"123456"}]}
 *
 * Success: HTTP 200, body {"message":"&lt;request-id&gt;","type":"success"}
 * Failure: HTTP 200 (sic) with {"type":"error","message":"..."}
 *          OR HTTP 4xx/5xx with an error envelope
 * </pre>
 *
 * <p>MSG91 expects mobiles without the leading {@code +} — we strip it here
 * so the rest of the codebase keeps E.164 as the canonical internal format.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.otp.provider", havingValue = "msg91")
public class Msg91OtpDeliveryService implements OtpDeliveryService {

    private static final String FLOW_PATH = "/api/v5/flow/";
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 200;

    private final RestClient http;
    private final Msg91Properties props;

    public Msg91OtpDeliveryService(@Qualifier("msg91RestClient") RestClient http,
                                   Msg91Properties props) {
        this.http = http;
        this.props = props;
    }

    @Override
    public void deliver(String phone, String code) {
        Map<String, Object> body = buildBody(phone, code);

        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Map<?, ?> resp = http.post()
                        .uri(FLOW_PATH)
                        .body(body)
                        .retrieve()
                        .body(Map.class);

                // MSG91 returns 200 even for errors; we have to inspect the body.
                if (resp != null && "success".equals(resp.get("type"))) {
                    log.info("MSG91 delivery ok phone={} requestId={}", phone, resp.get("message"));
                    return;
                }
                // Bad payload from MSG91 → don't retry, the request itself is wrong.
                throw new OtpDeliveryException(
                        "MSG91 rejected request: " + (resp == null ? "null body" : resp.get("message")),
                        null);

            } catch (OtpDeliveryException permanent) {
                throw permanent;
            } catch (RestClientException transientErr) {
                last = transientErr;
                log.warn("MSG91 send attempt {}/{} failed for phone={}: {}",
                        attempt, MAX_ATTEMPTS, phone, transientErr.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    sleep(INITIAL_BACKOFF_MS * (1L << (attempt - 1)));
                }
            }
        }
        throw new OtpDeliveryException(
                "MSG91 unavailable after " + MAX_ATTEMPTS + " attempts", last);
    }

    private Map<String, Object> buildBody(String phone, String code) {
        // MSG91 expects mobiles WITHOUT the '+' prefix.
        String mobile = phone.startsWith("+") ? phone.substring(1) : phone;

        Map<String, Object> recipient = new java.util.HashMap<>();
        recipient.put("mobiles", mobile);
        recipient.put("var", code);

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("template_id", props.templateId());
        body.put("short_url", "0");
        body.put("recipients", List.of(recipient));
        if (props.senderId() != null && !props.senderId().isBlank()) {
            body.put("sender", props.senderId());
        }
        return body;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
