package it.gls.dipendenti.auth.controller;

import it.gls.dipendenti.auth.service.CustomUserDetailsService;
import it.gls.dipendenti.auth.model.Role;
import it.gls.dipendenti.auth.model.User;
import it.gls.dipendenti.auth.repository.RoleRepository;
import it.gls.dipendenti.auth.repository.UserRepository;
import it.gls.dipendenti.config.JwtUtil;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
                          CustomUserDetailsService userDetailsService,
                          JwtUtil jwtUtil,
                          UserRepository userRepository,
                          RoleRepository roleRepository
                          ) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Autentica l'utente
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            // Carica i dettagli dell'utente
            final UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());

            // Genera il token JWT
            final String jwt = jwtUtil.generateToken(userDetails);

            // Ottengo l'user
            User user = userRepository.findByUsername(request.username()).orElseThrow();
            // Ottengo i ruoli
            List<Role> roles = roleRepository.findByUserId(user.id());

            // Ritorna il token
            return ResponseEntity.ok(new AuthResponse(jwt, new UserDTO(
                    user.id(),
                    user.username(),
                    user.email(),
                    user.companyId()
            ), roles.stream().map(Role::name).toList()));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid username or password", HttpStatus.UNAUTHORIZED.value()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid token format", HttpStatus.UNAUTHORIZED.value()));
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            // Verifica che il token sia ancora valido (non troppo vecchio)
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(token, userDetails)) {
                // Genera nuovo token
                String newToken = jwtUtil.generateToken(userDetails);
                return ResponseEntity.ok(new RefreshResponse(newToken));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Token expired", HttpStatus.UNAUTHORIZED.value()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Token refresh failed", HttpStatus.UNAUTHORIZED.value()));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid token format", HttpStatus.UNAUTHORIZED.value()));
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(token, userDetails)) {
                return ResponseEntity.ok(new ValidationResponse(true, username));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid or expired token", HttpStatus.UNAUTHORIZED.value()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Token validation failed", HttpStatus.UNAUTHORIZED.value()));
        }
    }

    // DTOs
    public record LoginRequest(String username, String password) {}

    public record AuthResponse(String token, UserDTO user, List<String> roles) {}

    public record ValidationResponse(boolean valid, String username) {}

    public record UserDTO(
            Long id,
            String username,
            String email,
            Long companyId
    ) {}

    public record RefreshResponse(String token) {}
}
