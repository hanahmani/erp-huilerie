package com.huilerie.erp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class MockMvcSupport {

    private MockMvcSupport() {
    }

    public static String login(MockMvc mvc, ObjectMapper om, String username, String password) throws Exception {
        String body = om.writeValueAsString(Map.of("username", username, "password", password));
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = om.readTree(json);
        return node.get("token").asText();
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
