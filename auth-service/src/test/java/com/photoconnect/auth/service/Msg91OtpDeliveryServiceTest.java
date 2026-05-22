package com.photoconnect.auth.service;

import com.photoconnect.auth.config.Msg91Properties;
import com.photoconnect.auth.exception.OtpDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

/**
 * Unit test for {@link Msg91OtpDeliveryService}.
 *
 * <p>{@link MockRestServiceServer} intercepts every request the {@link RestClient}
 * would make, lets us assert on the outgoing request (URL, headers, body), and
 * scripts the response. No real HTTP — no port, no flakiness.</p>
 */
class Msg91OtpDeliveryServiceTest {

    private static final Msg91Properties PROPS = new Msg91Properties(
            "https://msg91.test", "test-auth-key", "TPL-123", "PHOTOC");

    private RestClient client;
    private MockRestServiceServer server;
    private Msg91OtpDeliveryService delivery;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(PROPS.baseUrl())
                .defaultHeader("authkey", PROPS.authKey())
                .defaultHeader("Content-Type", "application/json");
        server = MockRestServiceServer.bindTo(builder).build();
        client = builder.build();
        delivery = new Msg91OtpDeliveryService(client, PROPS);
    }

    @Test
    void deliver_postsToFlow_withAuthkeyAndStrippedPhone() {
        server.expect(requestTo("https://msg91.test/api/v5/flow/"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("authkey", "test-auth-key"))
                .andExpect(jsonPath("$.template_id").value("TPL-123"))
                .andExpect(jsonPath("$.sender").value("PHOTOC"))
                .andExpect(jsonPath("$.recipients[0].mobiles").value("919876543210"))
                .andExpect(jsonPath("$.recipients[0].var").value("123456"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"message\":\"req-abc\",\"type\":\"success\"}",
                        MediaType.APPLICATION_JSON));

        delivery.deliver("+919876543210", "123456");

        server.verify();
    }

    @Test
    void deliver_omitsSenderIfNotConfigured() {
        Msg91Properties noSender = new Msg91Properties(
                "https://msg91.test", "test-auth-key", "TPL-123", null);
        // Rebuild builder bound to the same MockRestServiceServer so this test
        // exercises the no-sender path through the real RestClient.
        RestClient.Builder b = RestClient.builder()
                .baseUrl(noSender.baseUrl())
                .defaultHeader("authkey", noSender.authKey())
                .defaultHeader("Content-Type", "application/json");
        MockRestServiceServer s = MockRestServiceServer.bindTo(b).build();
        Msg91OtpDeliveryService svc = new Msg91OtpDeliveryService(b.build(), noSender);

        s.expect(requestTo("https://msg91.test/api/v5/flow/"))
                .andExpect(jsonPath("$.sender").doesNotExist())
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"type\":\"success\"}", MediaType.APPLICATION_JSON));

        svc.deliver("+919876543210", "111111");
        s.verify();
    }

    @Test
    void deliver_msg91ErrorBody_throwsImmediately_noRetry() {
        // MSG91 quirk: an error returns HTTP 200 with type=error in the body.
        // That's a "permanent" failure from our POV — retrying won't help, so
        // the impl must throw on the first response, not retry.
        server.expect(requestTo("https://msg91.test/api/v5/flow/"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"type\":\"error\",\"message\":\"invalid template\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> delivery.deliver("+919876543210", "123456"))
                .isInstanceOf(OtpDeliveryException.class)
                .hasMessageContaining("invalid template");

        server.verify(); // exactly one request was made
    }

    @Test
    void deliver_transientHttp5xx_retriesThenSucceeds() {
        // Two 503s then a 200: the impl should silently retry and succeed.
        server.expect(requestTo("https://msg91.test/api/v5/flow/"))
                .andRespond(MockRestResponseCreators.withServerError());
        server.expect(requestTo("https://msg91.test/api/v5/flow/"))
                .andRespond(MockRestResponseCreators.withServerError());
        server.expect(requestTo("https://msg91.test/api/v5/flow/"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"type\":\"success\"}", MediaType.APPLICATION_JSON));

        delivery.deliver("+919876543210", "123456");

        server.verify();
    }

    @Test
    void deliver_allRetriesFail_throwsOtpDeliveryException() {
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo("https://msg91.test/api/v5/flow/"))
                    .andRespond(MockRestResponseCreators.withServerError());
        }

        assertThatThrownBy(() -> delivery.deliver("+919876543210", "123456"))
                .isInstanceOf(OtpDeliveryException.class)
                .hasMessageContaining("after 3 attempts");

        server.verify();
        assertThat(true).isTrue(); // sanity
    }
}
