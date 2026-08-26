package com.example.gateway;

import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

  private static final List<String> PUBLIC_PATHS = List.of("/api/auth/login", "/api/auth/register");
  private final JwtService jwtService;
  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGatewayFilter.class);

  @Autowired
  public JwtAuthenticationGatewayFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    if (PUBLIC_PATHS.contains(path)) {
      return chain.filter(exchange);
    }

    String token = extractToken(exchange.getRequest());
    System.out.println("token = " + token);
    if (token == null) {
      return reject(exchange, HttpStatus.UNAUTHORIZED);
    }
    try {
      String userId = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
      String role = jwtService.extractClaim(token, claims -> claims.get("roles", String.class));
      ServerWebExchange mutated =
          exchange
              .mutate()
              .request(
                  r ->
                      r.headers(
                          h -> {
                            h.set("X-User-Id", userId);
                            h.set("X-User-Role", role);
                          }))
              .build();
      return chain.filter(mutated);
    } catch (JwtException e) {
      return reject(exchange, HttpStatus.UNAUTHORIZED);
    }
  }

  private String extractToken(ServerHttpRequest request) {
    String authHeader = request.getHeaders().getFirst("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      System.out.println(authHeader.substring(7));
      return authHeader.substring(7);
    }
    return null;
  }

  private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
    exchange.getResponse().setStatusCode(status);
    return exchange.getResponse().setComplete();
  }

  @Override
  public int getOrder() {
    return -1;
  }
}
