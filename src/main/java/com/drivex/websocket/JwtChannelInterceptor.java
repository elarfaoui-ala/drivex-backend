package com.drivex.websocket;

import com.drivex.security.DriverUserDetailsService;
import com.drivex.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService                jwtService;
    private final DriverUserDetailsService  userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Client sends token in STOMP Authorization header
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String email       = jwtService.extractSubject(token);
                    UserDetails user   = userDetailsService.loadUserByUsername(email);

                    if (jwtService.isValid(token, user)) {
                        var auth = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                        accessor.setUser(auth);
                        log.debug("WebSocket authenticated: {}", email);
                    }
                } catch (Exception ex) {
                    log.warn("WebSocket JWT error: {}", ex.getMessage());
                }
            }
        }

        return message;
    }
}
