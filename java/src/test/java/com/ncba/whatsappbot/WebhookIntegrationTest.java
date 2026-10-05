package com.ncba.whatsappbot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end smoke/parity test exercising routing, JSON binding, the verify
 * handshake and the fast 200 ack. Runs under the {@code dev} profile (signature
 * verification skipped with a blank App Secret) and supplies dummy config so no
 * real secrets or network access are required.
 */
@SpringBootTest(properties = {
        "whatsapp.verify-token=test-verify-token",
        "whatsapp.access-token=test-access-token",
        "whatsapp.phone-number-id=0000000000",
        "whatsapp.app-secret="
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class WebhookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void verify_returns_challenge_for_valid_token() throws Exception {
        mockMvc.perform(get("/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "test-verify-token")
                        .param("hub.challenge", "challenge-123"))
                .andExpect(status().isOk())
                .andExpect(content().string("challenge-123"));
    }

    @Test
    void verify_returns_403_for_invalid_token() throws Exception {
        mockMvc.perform(get("/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "wrong-token")
                        .param("hub.challenge", "challenge-123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void receive_status_payload_returns_200() throws Exception {
        String body = "{\"object\":\"whatsapp_business_account\",\"entry\":[{\"id\":\"0\","
                + "\"changes\":[{\"field\":\"messages\",\"value\":{\"messaging_product\":\"whatsapp\","
                + "\"statuses\":[{\"id\":\"wamid.X\",\"status\":\"delivered\"}]}}]}]}";

        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void root_returns_running_message() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string("WhatsApp Meta Bot is running..."));
    }
}
