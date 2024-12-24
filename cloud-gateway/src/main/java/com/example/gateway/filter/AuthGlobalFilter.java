package com.example.gateway.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;
    private static final List<String> WHITELIST = List.of(
        "/auth/login",
        "/auth/register",
        "/auth/refresh",
        "/auth/validate",
        "/public/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (WHITELIST.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 获取token
        String token = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (token == null || !token.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 调用security服务验证token
        return webClient.get()
            .uri("lb://cloud-security/auth/validate")
            .header(HttpHeaders.AUTHORIZATION, token)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .flatMap(response -> {
        boolean isValid = (boolean) response.get("isValid");
        if (isValid) {
            String username = (String) response.get("username");
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) response.get("roles");
            
            // 将用户信息添加到请求头中，传递给下游微服务
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", username)
                .header("X-User-Roles", String.join(",", roles))
                .build();
            
            // 使用修改后的请求构建新的 ServerWebExchange
            ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();
            
            return chain.filter(modifiedExchange);
                } else {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            })
            .onErrorResume(error -> {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            });
    }

    @Override
    public int getOrder() {
        return -100; // 确保这个过滤器最先执行
    }
} 