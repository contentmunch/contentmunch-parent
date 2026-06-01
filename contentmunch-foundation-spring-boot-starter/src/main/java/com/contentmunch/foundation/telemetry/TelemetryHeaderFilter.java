package com.contentmunch.foundation.telemetry;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

public class TelemetryHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        var span = Span.current();
        var context = span.getSpanContext();

        if (context.isValid()) {
            response.setHeader("X-Trace-Id", context.getTraceId());
            response.setHeader("X-Span-Id", context.getSpanId());
        }
        filterChain.doFilter(request, response);
    }
}
