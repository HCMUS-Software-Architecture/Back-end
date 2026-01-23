package org.example.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate;

@Component
public class JwtAuthFilter implements GlobalFilter {

    private final String SECRET_KEY;

    @Autowired
    public JwtAuthFilter(@Value("${token.secret}") String SECRET_KEY) {
        this.SECRET_KEY = SECRET_KEY;
    }

    public static final List<String> openApiEndpoints = List.of(
            "/api/auth/login",
            "/api/health/**",
            "/v3/**",
            "/ws/**",
            "/swagger-ui/**"
    );
    private Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints.stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Kiểm tra xem đây có phải route cần bảo vệ không
        if (this.isSecured.test(request)) {
            String authHeader = request.getHeaders().getFirst("Authorization");
            System.out.println(authHeader);

            // 2a. Nếu là route private nhưng không có token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return this.onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                // 2b. Xác thực token (đã bao gồm kiểm tra hết hạn, chữ ký)
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(SECRET_KEY.getBytes())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String userIdStr = claims.get("user_id", String.class);

                // 3. Thêm header vào request
                request = request.mutate()
                        .header("X-User-Id", userIdStr)
                        .build();

            } catch (ExpiredJwtException | SignatureException | MalformedJwtException e) {
                // 2c. Nếu token không hợp lệ (hết hạn, sai chữ ký...)
                return this.onError(exchange, "Invalid Token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                return this.onError(exchange, "Token parsing error", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // 4. Cho đi tiếp (nếu là route public, hoặc nếu là route private và token hợp lệ)
        return chain.filter(exchange.mutate().request(request).build());

    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        // Bạn có thể set body lỗi tại đây nếu muốn
        return response.setComplete();
    }

}
