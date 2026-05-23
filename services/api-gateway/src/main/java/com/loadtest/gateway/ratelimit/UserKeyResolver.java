package com.loadtest.gateway.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Base64;

@Component("userKeyResolver")
public class UserKeyResolver implements KeyResolver {

    private final ObjectMapper objectMapper;

    public UserKeyResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String userId = extractSubject(authHeader.substring(7));
            if (!"unknown".equals(userId)) {
                return Mono.just("user:" + userId);
            }
        }
        // Fallback to IP for requests without a valid token
        return Mono.just("ip:" + resolveIp(exchange));
    }

    private String extractSubject(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return "unknown";
            String payload = parts[1];
            int pad = payload.length() % 4;
            if (pad != 0) payload += "=".repeat(4 - pad);
            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            JsonNode json = objectMapper.readTree(decoded);
            return json.path("sub").asText("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String resolveIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown-ip";
    }
}
