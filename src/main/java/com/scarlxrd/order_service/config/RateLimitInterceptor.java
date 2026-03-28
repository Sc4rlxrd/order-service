package com.scarlxrd.order_service.config;

import com.scarlxrd.order_service.exception.RateLimitException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket createBucket(String path, String method) {

        if (path.startsWith("/orders") && method.equals("POST")) {
            return Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(5)
                            .refillGreedy(5, Duration.ofMinutes(1))
                            .build())
                    .build();
        }

        //  default
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(10, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private Bucket resolveBucket(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        String method = request.getMethod();

        String key = ip + ":" + path + ":" + method;

        return cache.computeIfAbsent(key, k -> createBucket(path, method));
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        Bucket bucket = resolveBucket(request);

        if (bucket.tryConsume(1)) {
            return true;
        }

        throw new RateLimitException("Limit exceeded.");
    }
}
