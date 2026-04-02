package com.contentmunch.foundation.logging;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.contentmunch.foundation.logging.data.LokiLog;
import com.contentmunch.foundation.logging.data.LokiStream;
import com.contentmunch.foundation.logging.data.LokiStreams;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.Setter;

@Setter
public class LokiAppender extends AppenderBase<ILoggingEvent> {

    private String lokiUrl;
    private String appName;
    private String environment;
    private String username;
    private String password;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final RestTemplate restTemplate = getRestTemplate();
    private static final Logger fallbackLogger = LoggerFactory.getLogger("LOKI_FALLBACK");

    private final BlockingQueue<LokiLog> logsQueue = new ArrayBlockingQueue<>(1000);

    private final RateLimiter rateLimiter = RateLimiter.of("lokiRateLimiter",getRateLimiterConfig());
    private final Retry retry = Retry.of("lokiRetry",getRetryConfig());
    private final CircuitBreaker circuitBreaker = CircuitBreaker.of("lokiCircuitBreaker",getCircuitBreakerConfig());

    @Override
    public void start(){
        super.start();
        executor.submit(this::runLogBatchJob);
    }

    @Override
    protected void append(ILoggingEvent loggingEvent){

        // 1. Start with the regular message (works for INFO, DEBUG, etc.)
        StringBuilder logBuilder = new StringBuilder(loggingEvent.getFormattedMessage());

        IThrowableProxy throwableProxy = loggingEvent.getThrowableProxy();

        // 2. If it's an ERROR/WARN with an exception, add the extra context
        if (throwableProxy != null) {
            logBuilder.append("\n").append(ThrowableProxyUtil.asString(throwableProxy));
        }

        // 3. This 'logBuilder.toString()' now holds either the plain message
        // OR the message + stack trace.
        var lokiLog = LokiLog.builder().timestamp(loggingEvent.getTimeStamp()).log(logBuilder.toString()).build();

        if (!logsQueue.offer(lokiLog)) {
            fallbackLogger.warn("Queue full, dropping log: {}",lokiLog.log());
        }
    }

    private void runLogBatchJob(){

        while (true) {
            try {
                var firstLog = logsQueue.poll(500,TimeUnit.MILLISECONDS);
                if (firstLog == null) {
                    continue;
                }

                if (!circuitBreaker.tryAcquirePermission() || !rateLimiter.acquirePermission()) {
                    fallbackLogger.warn("Circuit open or rate limited, skipping this cycle");
                    fallbackLogger.warn("Fallback log {}",firstLog.log());
                    continue;
                }

                List<LokiLog> lokiLogs = new ArrayList<>();
                lokiLogs.add(firstLog);
                logsQueue.drainTo(lokiLogs,99);
                if (!lokiLogs.isEmpty()) {
                    try {
                        sendWithRetry(lokiLogs);
                        circuitBreaker.onSuccess(0,TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        circuitBreaker.onError(0,TimeUnit.MILLISECONDS,e);
                        fallbackLogger.error("Failed to push logs",e);
                        for (LokiLog log : lokiLogs) {
                            if (!logsQueue.offer(log)) {
                                fallbackLogger.warn("Queue full, dropping log: {}",log.log());
                            }
                        }
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                fallbackLogger.error("Batch Logger Failure",e);
            }
        }
    }

    private void sendWithRetry(List<LokiLog> lokiLogs){
        try {
            Runnable resilientTask = () -> sendLokiLogs(lokiLogs);
            resilientTask = Retry.decorateRunnable(retry,resilientTask);
            resilientTask.run();
        } catch (Exception e) {
            fallbackLogger.error("Failed to push logs after resilience pipeline",e);
        }
    }

    private void sendLokiLogs(List<LokiLog> lokiLogs){
        try {
            var lokiStream = LokiStream.builder().app(appName).environment(environment).build();
            var lokiStreams = LokiStreams.from(lokiStream,lokiLogs);
            var payload = objectMapper.writeValueAsString(lokiStreams);

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(username,password);
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            fallbackLogger.info("Sending logs to Loki");
            restTemplate.postForEntity(lokiUrl,entity,String.class);

        } catch (HttpServerErrorException.BadGateway e) {
            // Ignore 502 specifically
            fallbackLogger
                    .info("LokiAppender received 502 Bad Gateway. Ignoring since logs may have reached the server.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RestTemplate getRestTemplate(){
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(1000);
        return new RestTemplate(factory);
    }

    private RateLimiterConfig getRateLimiterConfig(){
        return RateLimiterConfig.custom().limitRefreshPeriod(Duration.ofSeconds(1)).limitForPeriod(20)
                .timeoutDuration(Duration.ZERO).build();
    }

    private RetryConfig getRetryConfig(){
        return RetryConfig.custom().maxAttempts(3).waitDuration(Duration.ofMillis(200)).build();
    }

    private CircuitBreakerConfig getCircuitBreakerConfig(){
        return CircuitBreakerConfig.custom().failureRateThreshold(50).waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(10).build();
    }
}
