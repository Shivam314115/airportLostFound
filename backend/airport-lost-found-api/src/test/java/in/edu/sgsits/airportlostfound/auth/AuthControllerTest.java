
package in.edu.sgsits.airportlostfound.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import in.edu.sgsits.airportlostfound.passenger.PassengerProfile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private SecurityContextRepository securityContextRepository;

    @MockitoBean
    private SessionAuthenticationStrategy sessionAuthenticationStrategy;

    @Test
    void validLoginReturnsProfileAndSavesSecurityContext()
            throws Exception {

        PassengerProfile profile = new PassengerProfile(
                42L,
                "Test Passenger",
                "passenger@example.test",
                "9876543210",
                OffsetDateTime.parse("2026-10-20T14:00:00+05:30")
        );

        Authentication authenticated =
                UsernamePasswordAuthenticationToken.authenticated(
                        "passenger@example.test",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_PASSENGER")
                        )
                );

        when(authenticationManager.authenticate(
                any(Authentication.class)
        )).thenReturn(authenticated);

        when(authService.getProfileByEmail("passenger@example.test"))
                .thenReturn(profile);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "PASSENGER@EXAMPLE.TEST",
                                  "password": "Correct-Password-42"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passengerId").value(42))
                .andExpect(jsonPath("$.email")
                        .value("passenger@example.test"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(sessionAuthenticationStrategy).onAuthentication(
                any(Authentication.class),
                any(HttpServletRequest.class),
                any(HttpServletResponse.class)
        );

        verify(securityContextRepository).saveContext(
                any(SecurityContext.class),
                any(HttpServletRequest.class),
                any(HttpServletResponse.class)
        );
    }

    @Test
    void invalidCredentialsReturnUnauthorized() throws Exception {

        when(authenticationManager.authenticate(
                any(Authentication.class)
        )).thenThrow(
                new BadCredentialsException("Invalid credentials")
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "passenger@example.test",
                                  "password": "Wrong-Password-42"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verify(securityContextRepository, never()).saveContext(
                any(SecurityContext.class),
                any(HttpServletRequest.class),
                any(HttpServletResponse.class)
        );
    }

    @Test
    void invalidRequestReturnsBadRequest() throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(authenticationManager, never())
                .authenticate(any(Authentication.class));
    }
}
