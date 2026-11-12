package com.beanstack.notifyx.apikey.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.beanstack.notifyx.shared.response.ApiResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component 
@RequiredArgsConstructor 
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private static final String API_KEY_HEADER = "X-NotifyX-API-Key";

    private static final String INGESTION_PATH = "/api/v1/ingest/";

    private final ApiKeyAuthenticationService authenticationService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        return !request.getRequestURI().startsWith(INGESTION_PATH);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String rawApiKey = request.getHeader(API_KEY_HEADER);

        if (rawApiKey == null || rawApiKey.isBlank()) {
            writeUnauthorizedResponse(response);
            return;
        }

        Optional<ApiKeyPrincipal> principal =
            authenticationService.authenticate(rawApiKey);

        if (principal.isEmpty()) {
            writeUnauthorizedResponse(response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                principal.get(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
            );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorizedResponse(
        HttpServletResponse response
    ) throws IOException {

        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
            response.getOutputStream(),
            ApiResponse.error("Invalid or expired API key")
        );
    }
}
