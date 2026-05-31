package com.example.authserver.service;

import com.example.authserver.domain.*;
import com.example.authserver.dto.request.LoginRequest;
import com.example.authserver.dto.request.RegisterRequest;
import com.example.authserver.dto.response.AuthResponse;
import com.example.authserver.exception.*;
import com.example.authserver.repository.*;
import com.example.authserver.security.AuthUserDetail;
import com.example.authserver.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RegistrationTokenRepository tokenRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;
    private Role mockRole;
    private UserRole mockUserRole;

    @BeforeEach
    public void setUp() {
        mockRole = new Role();
        mockRole.setId(1);
        mockRole.setRoleName("ROLE_EMPLOYEE");

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setUsername("alice");
        mockUser.setEmail("alice@gmail.com");
        mockUser.setPassword("123456");
        mockUser.setActiveFlag(1);

        mockUserRole = new UserRole();
        mockUserRole.setUser(mockUser);
        mockUserRole.setRole(mockRole);
        mockUserRole.setActiveFlag(1);

        mockUser.setUserRoles(List.of(mockUserRole));
    }
    // ===== Login Tests =====

    @Test
    public void testLogin_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("alice");
        request.setPassword("123456");

        Authentication authentication = mock(Authentication.class);
        AuthUserDetail userDetail = AuthUserDetail.builder()
                .username("alice")
                .password("123456")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetail);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser));
        when(jwtProvider.createToken(any())).thenReturn("mockToken");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("alice", response.getUsername());
        assertEquals("mockToken", response.getToken());
    }

    @Test
    public void testLogin_UserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("unknown");
        request.setPassword("123456");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new UsernameNotFoundException("User not found"));

        assertThrows(UserNotFoundException.class, () -> authService.login(request));
    }

    @Test
    public void testLogin_WrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("alice");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid password"));

        assertThrows(AuthException.class, () -> authService.login(request));
    }

    @Test
    public void testLogin_AccountDisabled() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("alice");
        request.setPassword("123456");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("Account is disabled"));

        assertThrows(AuthException.class, () -> authService.login(request));
    }

    // ===== Register Tests =====

    @Test
    public void testRegister_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setToken("validtoken");
        request.setUsername("newuser");
        request.setEmail("newuser@gmail.com");
        request.setPassword("123456");

        RegistrationToken regToken = new RegistrationToken();
        regToken.setToken("validtoken");
        regToken.setEmail("newuser@gmail.com");
        regToken.setExpirationDate(LocalDateTime.now().plusHours(3));
        regToken.setUsedFlag(0);

        when(tokenRepository.findByToken("validtoken")).thenReturn(Optional.of(regToken));
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@gmail.com")).thenReturn(false);
        when(roleRepository.findByRoleName("ROLE_EMPLOYEE")).thenReturn(Optional.of(mockRole));

        assertDoesNotThrow(() -> authService.register(request));
    }

    @Test
    public void testRegister_InvalidToken() {
        RegisterRequest request = new RegisterRequest();
        request.setToken("invalidtoken");
        request.setUsername("newuser");
        request.setEmail("newuser@gmail.com");
        request.setPassword("123456");

        when(tokenRepository.findByToken("invalidtoken")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> authService.register(request));
    }

    @Test
    public void testRegister_TokenExpired() {
        RegisterRequest request = new RegisterRequest();
        request.setToken("expiredtoken");
        request.setUsername("newuser");
        request.setEmail("newuser@gmail.com");
        request.setPassword("123456");

        RegistrationToken regToken = new RegistrationToken();
        regToken.setToken("expiredtoken");
        regToken.setExpirationDate(LocalDateTime.now().minusHours(1));
        regToken.setUsedFlag(0);

        when(tokenRepository.findByToken("expiredtoken")).thenReturn(Optional.of(regToken));

        assertThrows(TokenExpiredException.class, () -> authService.register(request));
    }

    @Test
    public void testRegister_TokenAlreadyUsed() {
        RegisterRequest request = new RegisterRequest();
        request.setToken("usedtoken");
        request.setUsername("newuser");
        request.setEmail("newuser@gmail.com");
        request.setPassword("123456");

        RegistrationToken regToken = new RegistrationToken();
        regToken.setToken("usedtoken");
        regToken.setExpirationDate(LocalDateTime.now().plusHours(3));
        regToken.setUsedFlag(1);

        when(tokenRepository.findByToken("usedtoken")).thenReturn(Optional.of(regToken));

        assertThrows(InvalidTokenException.class, () -> authService.register(request));
    }

    @Test
    public void testRegister_UsernameAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setToken("validtoken");
        request.setUsername("alice");
        request.setEmail("newuser@gmail.com");
        request.setPassword("123456");

        RegistrationToken regToken = new RegistrationToken();
        regToken.setToken("validtoken");
        regToken.setExpirationDate(LocalDateTime.now().plusHours(3));
        regToken.setUsedFlag(0);

        when(tokenRepository.findByToken("validtoken")).thenReturn(Optional.of(regToken));
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    public void testRegister_EmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setToken("validtoken");
        request.setUsername("newuser");
        request.setEmail("alice@gmail.com");
        request.setPassword("123456");

        RegistrationToken regToken = new RegistrationToken();
        regToken.setToken("validtoken");
        regToken.setExpirationDate(LocalDateTime.now().plusHours(3));
        regToken.setUsedFlag(0);

        when(tokenRepository.findByToken("validtoken")).thenReturn(Optional.of(regToken));
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("alice@gmail.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
    }
}
