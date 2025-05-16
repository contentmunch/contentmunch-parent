package com.contentmunch.foundation.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RequestResponseLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization","cookie","set-cookie");

    private static final Set<String> SENSITIVE_KEYS = Set.of("password","token","secret");
    private static final int MAX_LOG_BODY_SIZE = 8192; // 8 KB

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){
        return !request.getRequestURI().startsWith("/api");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException{

        var wrappedRequest = new ContentCachingRequestWrapper(request);
        var wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest,wrappedResponse);
        } finally {
            logRequest(wrappedRequest);
            logResponse(wrappedResponse);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request){
        var headers = Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(h -> h,request::getHeader));
        String body = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);

        LOG.info("Incoming Request: method={} uri={} headers={} body={}",request.getMethod(),request.getRequestURI(),
                maskHeaders(headers),maskBody(body));
    }

    private void logResponse(ContentCachingResponseWrapper response){
        String contentType = response.getContentType();
        byte[] content = response.getContentAsByteArray();

        if (contentType != null && !contentType.contains("application/json") && !contentType.contains("text")) {
            LOG.info("Outgoing Response: status={} headers={} bodySkippedDueToContentType={}",response.getStatus(),
                    maskHeaders(getResponseHeaders(response)),contentType);
            return;
        }
        String body = safeBodyPreview(content);
        LOG.info("Outgoing Response: status={} headers={} body={}",response.getStatus(),
                maskHeaders(getResponseHeaders(response)),maskBody(body));
    }

    private Map<String, String> maskHeaders(Map<String, String> headers){
        return headers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e -> {
            if (SENSITIVE_HEADERS.contains(e.getKey().toLowerCase(Locale.ROOT))) {
                return "***";
            }
            return e.getValue();
        }));
    }

    private String safeBodyPreview(byte[] content){
        if (content.length > MAX_LOG_BODY_SIZE) {
            return new String(content, 0, MAX_LOG_BODY_SIZE, StandardCharsets.UTF_8) + "...(truncated)";
        }
        return new String(content, StandardCharsets.UTF_8);
    }

    private String maskBody(String body){
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(body);

            if (root.isObject()) {
                SENSITIVE_KEYS.stream().filter(((ObjectNode) root)::has)
                        .forEach(key -> ((ObjectNode) root).put(key,"***"));

                return mapper.writeValueAsString(root);
            }
        } catch (IOException ignored) {
            // Return the original body if parsing fails
        }

        return body;
    }

    private Map<String, String> getResponseHeaders(ContentCachingResponseWrapper response){
        return response.getHeaderNames().stream().collect(Collectors.toMap(Function.identity(),
                h -> String.join(", ",response.getHeaders(h)),(v1,v2) -> v1.equals(v2) ? v1 : v1 + ", " + v2));
    }

}
