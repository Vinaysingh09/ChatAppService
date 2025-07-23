package com.chatapp.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class WebSocketAuthenticationInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    public WebSocketAuthenticationInterceptor(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            System.out.println("\n🔄 ====== WebSocket Handshake Started ======");
            System.out.println("📍 Request URI: " + request.getURI());
            System.out.println("📍 Remote Address: " + request.getRemoteAddress());
            System.out.println("📍 Headers: " + request.getHeaders());
            
            // Extract token from query parameter
            String token = extractTokenFromQuery(request);
            
            System.out.println("🔍 WebSocket handshake - Token received: " + (token != null ? "YES" : "NO"));
            
            if (StringUtils.hasText(token)) {
                System.out.println("🔍 Token length: " + token.length());
                System.out.println("🔍 Token preview: " + token.substring(0, Math.min(20, token.length())) + "...");
                System.out.println("🔍 Validating JWT token...");
                
                try {
                    if (tokenProvider.validateToken(token)) {
                        String username = tokenProvider.getUsernameFromToken(token);
                        System.out.println("✅ JWT valid for user: " + username);
                        
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        System.out.println("✅ User details loaded successfully");
                        
                        // Store user details in WebSocket session attributes
                        attributes.put("user", userDetails);
                        attributes.put("username", username);
                        
                        // Set authentication context for this handshake
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        System.out.println("✅ WebSocket authentication successful for: " + username);
                        System.out.println("✅ ====== Handshake Completed Successfully ======\n");
                        return true; // Allow connection
                    } else {
                        System.err.println("❌ JWT validation failed - token is invalid");
                    }
                } catch (Exception e) {
                    System.err.println("❌ JWT validation error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            } else {
                System.err.println("❌ No JWT token provided in query parameter");
                System.err.println("❌ Query string was: " + request.getURI().getQuery());
            }
        } catch (Exception e) {
            System.err.println("❌ WebSocket authentication error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
        
        System.err.println("❌ ====== Handshake Failed ======\n");
        return false; // Reject connection
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Clean up security context after handshake
        SecurityContextHolder.clearContext();
        
        if (exception != null) {
            System.err.println("❌ WebSocket handshake error: " + exception.getMessage());
        } else {
            System.out.println("✅ WebSocket handshake completed successfully");
        }
    }

    private String extractTokenFromQuery(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        System.out.println("🔍 Full query string: " + query);
        
        if (StringUtils.hasText(query)) {
            try {
                Map<String, String> queryParams = UriComponentsBuilder.fromUriString("?" + query)
                        .build()
                        .getQueryParams()
                        .toSingleValueMap();
                
                String token = queryParams.get("token");
                if (token != null) {
                    System.out.println("✅ Token found in query parameters");
                } else {
                    System.out.println("❌ No 'token' parameter found in query string");
                    System.out.println("💡 Available parameters: " + queryParams.keySet());
                }
                return token;
            } catch (Exception e) {
                System.err.println("❌ Error parsing query parameters: " + e.getMessage());
                return null;
            }
        }
        System.out.println("❌ No query string present in request");
        return null;
    }
} 