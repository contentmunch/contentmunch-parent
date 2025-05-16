package com.contentmunch.foundation.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import nl.altindag.log.LogCaptor;

@WebMvcTest(controllers = DummyLoggingController.class)
@Import(RequestResponseLoggingFilter.class)
@AutoConfigureJsonTesters
class RequestResponseLoggingFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldLogMaskedSensitiveData() throws Exception{
        List<String> logs;
        try (LogCaptor logCaptor = LogCaptor
                .forName("com.contentmunch.foundation.logging.RequestResponseLoggingFilter")) {
            logCaptor.setLogLevelToInfo();

            var json = """
                    {
                      "username": "user1",
                      "password": "my-secret-password",
                      "token": "abc123"
                    }
                    """;

            mockMvc.perform(post("/api/dummy/logs").contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization","Bearer real-token").header("Cookie","session-id=xyz").content(json))
                    .andExpect(status().isOk());

            logs = logCaptor.getInfoLogs();
        }
        assertThat(logs).anyMatch(log -> log.contains("Incoming Request") && log.contains("\"password\":\"***\"")
                && log.contains("\"token\":\"***\"") && log.contains("\"username\":\"user1\"")
                && log.contains("Authorization=***") && log.contains("Cookie=***"));

        assertThat(logs).noneMatch(log -> log.contains("my-secret-password") || log.contains("Bearer real-token"));
    }

    @Test
    void shouldNotFilterNonApiPaths() throws Exception{

        List<String> logs;
        try (LogCaptor logCaptor = LogCaptor
                .forName("com.contentmunch.foundation.logging.RequestResponseLoggingFilter")) {
            logCaptor.setLogLevelToInfo(); // Ensure INFO level logs are captured

            mockMvc.perform(get("/public/health").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()); // Or
                                                                                                                       // whatever
                                                                                                                       // your
                                                                                                                       // dummy
                                                                                                                       // controller
                                                                                                                       // returns
                                                                                                                       // for
                                                                                                                       // this
            logs = logCaptor.getInfoLogs();
        }

        assertThat(logs).isEmpty();
    }

    @Test
    void shouldLogPlainTextBodyIfContentTypeIsText() throws Exception{
        List<String> logs;
        try (LogCaptor logCaptor = LogCaptor
                .forName("com.contentmunch.foundation.logging.RequestResponseLoggingFilter")) {
            logCaptor.setLogLevelToInfo(); // Ensure INFO level logs are captured
            mockMvc.perform(get("/api/dummy/text")).andExpect(status().isOk());

            logs = logCaptor.getInfoLogs();
        }

        assertThat(logs)
                .anyMatch(log -> log.contains("Outgoing Response") && log.contains("This is a plain text response"));
    }

    @Test
    void shouldSkipBodyForBinaryResponse() throws Exception{
        List<String> logs;
        try (LogCaptor logCaptor = LogCaptor
                .forName("com.contentmunch.foundation.logging.RequestResponseLoggingFilter")) {
            logCaptor.setLogLevelToInfo();
            mockMvc.perform(get("/api/dummy/binary")).andExpect(status().isOk());

            logs = logCaptor.getInfoLogs();
        }

        assertThat(logs).anyMatch(log -> log.contains("bodySkippedDueToContentType=image/png"));
    }

    @Test
    void shouldTruncateLargeBody() throws Exception{
        List<String> logs;

        String largeJson = """
                {
                  "description": "%s"
                }
                """.formatted("a".repeat(9000));

        try (LogCaptor logCaptor = LogCaptor
                .forName("com.contentmunch.foundation.logging.RequestResponseLoggingFilter")) {
            logCaptor.setLogLevelToInfo();
            mockMvc.perform(post("/api/dummy/logs").contentType(MediaType.APPLICATION_JSON).content(largeJson))
                    .andExpect(status().isOk());

            logs = logCaptor.getInfoLogs();
        }

        assertThat(logs).anyMatch(log -> log.contains("...(truncated)"));

    }

    @Test
    void shouldNotFailOnMalformedJsonBody() throws Exception{
        List<String> logs;
        try (LogCaptor logCaptor = LogCaptor
                .forName("com.contentmunch.foundation.logging.RequestResponseLoggingFilter")) {
            logCaptor.setLogLevelToInfo();
            String invalidJson = "{\"username\": \"user1\", \"password\": ";

            mockMvc.perform(post("/api/dummy/logs").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
                    .andExpect(status().is5xxServerError());

            logs = logCaptor.getInfoLogs();
        }

        assertThat(logs).anyMatch(log -> log.contains("Incoming Request"));
    }
}
