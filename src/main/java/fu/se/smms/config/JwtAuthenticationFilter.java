package fu.se.smms.config;

import io.jsonwebtoken.JwtException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(HandlerExceptionResolver handlerExceptionResolver,
                                   JwtTokenProvider jwtTokenProvider,
                                   UserDetailsService userDetailsService) {
        this.handlerExceptionResolver = handlerExceptionResolver;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        String path = request.getServletPath();
        String method = request.getMethod();
        log.debug("Request: {} {}", method, path);

        if (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/refresh") || path.equals("/api/v1/auth/logout")
                || path.startsWith("/api/v1/auth/register")
                || path.contains("/forgot-password") || path.contains("/reset-password")
                || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/uploads/")) {
            log.debug("Public path - skipping JWT validation: {}", path);
            filterChain.doFilter(request, response);
            return;
        }
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found for path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }
        try {
            final String token = authHeader.substring(7);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = jwtTokenProvider.getUsernameFromToken(token);
            if (username != null && authentication == null) {
                UserDetails userPrincipal = userDetailsService.loadUserByUsername(username);
                if (jwtTokenProvider.validateToken(token)) {
                    log.debug("JWT valid - authenticated user: {}", username);
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userPrincipal,
                                    null,
                                    userPrincipal.getAuthorities()
                            );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            log.warn("Invalid JWT token for path: {} - {}", path, e.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":401,\"message\":\"Token không hợp lệ hoặc đã hết hạn.\"}");
        } catch (Exception e) {
            log.error("JWT authentication failed for path: {} - {}", path, e.getMessage(), e);
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }
}
