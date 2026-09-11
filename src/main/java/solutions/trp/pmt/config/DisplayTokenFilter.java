package solutions.trp.pmt.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class DisplayTokenFilter extends OncePerRequestFilter {
    static final String HEADER_NAME = "X-Display-Token";

    private final String expectedToken;

    public DisplayTokenFilter(@Value("${pmt.display.token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        String cleanPath = path.substring(context.length());

        return !("GET".equals(request.getMethod()) && "/display".equals(cleanPath))
                && !cleanPath.startsWith("/api/display/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        boolean displayPageRequest = isDisplayPageRequest(request);
        String providedToken = displayPageRequest
                ? request.getParameter("token")
                : request.getHeader(HEADER_NAME);

        if (!matches(providedToken)) {
            if (displayPageRequest) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"data\":null,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid display token\"}}");
            }
            return;
        }

        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        UsernamePasswordAuthenticationToken displayAuthentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "display-client",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_DISPLAY"))
                );

        SecurityContextHolder.getContext().setAuthentication(displayAuthentication);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }
    }

    private boolean matches(String providedToken) {
        if (expectedToken == null || expectedToken.isBlank() || providedToken == null || providedToken.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean isDisplayPageRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        String cleanPath = path.substring(context.length());
        return "GET".equals(request.getMethod()) && "/display".equals(cleanPath);
    }
}
