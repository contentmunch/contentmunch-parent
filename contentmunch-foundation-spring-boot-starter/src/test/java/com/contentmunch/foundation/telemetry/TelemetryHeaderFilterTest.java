package com.contentmunch.foundation.telemetry;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

@WebMvcTest
@ContextConfiguration(classes = {DummyTelemetryController.class, TelemetryHeaderFilter.class})
class TelemetryHeaderFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAddTelemetryHeaders() throws Exception{
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

        Tracer tracer = openTelemetry.getTracer("test");

        // Start a real span
        Span span = tracer.spanBuilder("test-span").startSpan();

        try (Scope ignored = span.makeCurrent()) {
            mockMvc.perform(get("/dummy/telemetry/ok")).andExpect(status().isOk())
                    .andExpect(header().exists("X-Trace-Id")).andExpect(header().exists("X-Span-Id"))
                    .andExpect(header().string("X-Trace-Id",not(emptyOrNullString())))
                    .andExpect(header().string("X-Span-Id",not(emptyOrNullString())));
        } finally {
            span.end();
        }
    }
}
