package com.example.authserver.service;

import com.example.authserver.domain.RegistrationToken;
import com.example.authserver.domain.Role;
import com.example.authserver.domain.User;
import com.example.authserver.domain.UserRole;
import com.example.authserver.dto.request.GenerateTokenRequest;
import com.example.authserver.dto.request.LoginRequest;
import com.example.authserver.dto.request.RegisterRequest;
import com.example.authserver.dto.response.AuthResponse;
import com.example.authserver.dto.response.GenerateTokenResponse;
import com.example.authserver.event.EmailEvent;
import com.example.authserver.exception.*;
import com.example.authserver.repository.RegistrationTokenRepository;
import com.example.authserver.repository.RoleRepository;
import com.example.authserver.repository.UserRepository;
import com.example.authserver.repository.UserRoleRepository;
import com.example.authserver.security.AuthUserDetail;
import com.example.authserver.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService, UserDetailsService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RegistrationTokenRepository tokenRepository;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;

    @Value("${registration.token.expiration.hours}")
    private int tokenExpirationHours;

    @Value("${kafka.topic.email}")
    private String emailTopic;

    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;


    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository,
                           RegistrationTokenRepository tokenRepository,
                           JwtProvider jwtProvider,
                           @Lazy AuthenticationManager authenticationManager,
                           KafkaTemplate<String, EmailEvent> kafkaTemplate) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.tokenRepository = tokenRepository;
        this.jwtProvider = jwtProvider;
        this.authenticationManager = authenticationManager;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Transactional
    public GenerateTokenResponse generateRegistrationToken(GenerateTokenRequest request) {
        // generate token
        String token = UUID.randomUUID().toString();

        // current time + expiration Hours
        LocalDateTime expirationDate = LocalDateTime.now().plusHours(tokenExpirationHours);

        // find HR
        User hrUser = userRepository.findById(request.getCreatedById())
                .orElseThrow(() -> new UserNotFoundException("HR user not found"));

        boolean isHR = hrUser.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getRoleName().equals("ROLE_HR"));

        if (!isHR) {
            throw new AuthException("Only HR can generate registration token");
        }

        // save
        RegistrationToken registrationToken = new RegistrationToken();
        registrationToken.setToken(token);
        registrationToken.setEmail(request.getEmail());
        registrationToken.setExpirationDate(expirationDate);
        registrationToken.setUsedFlag(0);
        registrationToken.setCreateBy(hrUser);
        registrationToken.setCreateDate(LocalDateTime.now());
        registrationToken.setLastModificationDate(LocalDateTime.now());
        tokenRepository.save(registrationToken);

        String registrationLink = "http://localhost:5173/register?token=" + token;

        EmailEvent emailEvent = EmailEvent.builder()
                .to(request.getEmail())
                .subject("You are invited to register")
                .body("Please use the registration link to register: " + registrationLink + "\nYour token is: " + token)
                .build();

        kafkaTemplate.send(emailTopic, emailEvent);

        return GenerateTokenResponse.builder()
                .token(token)
                .email(request.getEmail())
                .expirationDate(expirationDate)
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = user.getUserRoles().stream()
                .map(userRole -> new SimpleGrantedAuthority(userRole.getRole().getRoleName()))
                .collect(Collectors.toList());

        return AuthUserDetail.builder()
                .id(user.getId().longValue())
                .email(user.getEmail())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .enabled(user.getActiveFlag() == 1)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .authorities(authorities)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );
        } catch (UsernameNotFoundException e) {
            throw new UserNotFoundException("User not found");
        } catch (AuthenticationException e) {
            throw new AuthException("Incorrect credentials, please try again.");
        }

        AuthUserDetail userDetail = (AuthUserDetail) authentication.getPrincipal();
        String token = jwtProvider.createToken(userDetail);

        User user = userRepository.findByUsername(userDetail.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .userId(userDetail.getId())
                .email(userDetail.getEmail())
                .role(userDetail.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(",")))
                .build();
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        RegistrationToken regToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid registration token"));

        if (regToken.isExpired()) {
            throw new TokenExpiredException("Registration token has expired");
        }

        if (regToken.getUsedFlag() == 1) {
            throw new InvalidTokenException("Registration token has already been used");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setActiveFlag(1);
        user.setCreateDate(LocalDateTime.now());
        user.setLastModificationDate(LocalDateTime.now());
        userRepository.save(user);

        Role employeeRole = roleRepository.findByRoleName("ROLE_EMPLOYEE")
                .orElseThrow(() -> new RuntimeException("Role not found"));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(employeeRole);
        userRole.setActiveFlag(1);
        userRole.setCreateDate(LocalDateTime.now());
        userRole.setLastModificationDate(LocalDateTime.now());
        userRoleRepository.save(userRole);

        regToken.setUsedFlag(1);
        tokenRepository.save(regToken);
    }

    @Override
    public boolean validateRegistrationToken(String token) {
        RegistrationToken regToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid registration token"));

        if (regToken.isExpired()) {
            throw new TokenExpiredException("Registration token has expired");
        }

        if (regToken.getUsedFlag() == 1) {
            throw new InvalidTokenException("Registration token has already been used");
        }

        return true;
    }
}
