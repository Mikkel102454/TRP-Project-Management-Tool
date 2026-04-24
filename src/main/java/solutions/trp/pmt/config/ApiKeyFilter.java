package solutions.trp.pmt.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${api.key}")
    private String expectedKey;

    private static final String HEADER_NAME = "X-API-Key";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();

        String cleanPath = path.substring(context.length());
        String method = request.getMethod();

        // Only apply to specific GET endpoints
        if (!method.equals("GET")) return true;
        return !(
                cleanPath.startsWith("/api/time/") ||
                        cleanPath.equals("/api/user") ||
                        cleanPath.equals("/api/project") ||
                        cleanPath.equals("/api/project/details")
        );
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isLoggedIn = auth != null &&
                !(auth instanceof AnonymousAuthenticationToken) &&
                auth.isAuthenticated();

        if (isLoggedIn) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(HEADER_NAME);
        System.out.println(apiKey);

        if (apiKey != null && apiKey.equals(expectedKey)) {

            UsernamePasswordAuthenticationToken auth2 =
                    UsernamePasswordAuthenticationToken.authenticated(
                            "api-user",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_API"))
                    );

            SecurityContextHolder.getContext().setAuthentication(auth2);

            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Unauthorized");
    }
}