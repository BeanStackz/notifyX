package com.beanstack.notifyx.ratelimit.interceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.HandlerInterceptor;

import com.beanstack.notifyx.ratelimit.model.RateLimitResult;
import com.beanstack.notifyx.ratelimit.service.RateLimitService;
import com.beanstack.notifyx.shared.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Configuration
@RequiredArgsConstructor 
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override 
    public boolean preHandle(
        HttpServletRequest request, 
        HttpServletResponse response, 
        Object handler) throws IOException {
            String key = resolveBucketKey(request);
            try{
                RateLimitResult result = rateLimitService.consume(key);
                response.setHeader(
                    "X-RateLimit-Limit", 
                    String.valueOf(result.limit()));

                response.setHeader(
                    "X-RateLimit-Remaining", 
                    String.valueOf(result.remaining()));
                
                if(!result.allowed()) {
                    long retryAfterSeconds = Math.max(
                        1, (long) Math.ceil(result.retryAfterMillis() / 1000.0));
                    response.setStatus(429);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setHeader(
                        "Retry-After", 
                        String.valueOf(retryAfterSeconds));
                    response.setHeader(
                        "X-RateLimit-Reset", 
                        String.valueOf(System.currentTimeMillis()/1000 + retryAfterSeconds));
                    
                    objectMapper.writeValue(
                        response.getOutputStream(),
                        ApiResponse.error("Rate limit exceeded. Try again in " + retryAfterSeconds + " seconds."));
                    return false;
                }
                return true;
            }
            catch (DataAccessException e) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), 
                    ApiResponse.error("Rate limit service unavailable"));
                return false;
            }
        }

        private String resolveBucketKey(HttpServletRequest request) {
            Authentication authentication = (Authentication) request.getUserPrincipal();
            if(authentication != null 
                && authentication.isAuthenticated() 
                && !(authentication instanceof AnonymousAuthenticationToken)) {
                return "notifyx:ratelimit:user:"
                    + hash(authentication.getName());
            } else {
                return "notifyx:ratelimit:ip:"
                + hash(request.getRemoteAddr());
            }
        }

        private String hash(String value) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to generate rate limit key",
                    exception
            );
        }
    }
}
