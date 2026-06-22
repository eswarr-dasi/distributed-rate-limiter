package com.eswarr.ratelimiter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

      private final SlidingWindowRateLimiter rateLimiter;
      private static final int LIMIT = 100;
      private static final long WINDOW_SECONDS = 60;

      public RateLimitFilter(SlidingWindowRateLimiter rateLimiter) {
                this.rateLimiter = rateLimiter;
            }

      @Override
      protected void doFilterInternal(HttpServletRequest request,
                                                                          HttpServletResponse response,
                                                                          FilterChain filterChain)
              throws ServletException, IOException {

                        String clientKey = getClientKey(request);
                        long remaining = rateLimiter.getRemaining(clientKey, LIMIT, WINDOW_SECONDS);

                        response.setHeader("X-RateLimit-Limit", String.valueOf(LIMIT));
                        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
                        response.setHeader("X-RateLimit-Window", String.valueOf(WINDOW_SECONDS));

                        if (!rateLimiter.tryAcquire(clientKey, LIMIT, WINDOW_SECONDS)) {
                                      response.setStatus(429);
                                      response.setContentType("application/json");
                                      response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
                                      return;
                                  }

                        filterChain.doFilter(request, response);
                    }

      private String getClientKey(HttpServletRequest request) {
                String forwardedFor = request.getHeader("X-Forwarded-For");
                if (forwardedFor != null && !forwardedFor.isEmpty()) {
                              return forwardedFor.split(",")[0].trim();
                          }
                return request.getRemoteAddr();
            }
  }
