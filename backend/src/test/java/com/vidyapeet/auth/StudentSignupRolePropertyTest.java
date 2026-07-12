package com.vidyapeet.auth;

import com.vidyapeet.auth.dto.AuthResponse;
import com.vidyapeet.auth.dto.RegisterStudentRequest;
import com.vidyapeet.common.Role;
import com.vidyapeet.institute.Institute;
import com.vidyapeet.institute.repository.InstituteRepository;
import com.vidyapeet.security.JwtService;
import com.vidyapeet.user.User;
import com.vidyapeet.user.repository.UserRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: vidyapeeth-v2-upgrades, Property 2: Student sign-up always creates a STUDENT
 *
 * <p>For any valid registration payload submitted from the landing page, the account
 * created by the Auth_Service has role {@code STUDENT}.
 *
 * <p>Validates: Requirements 2.8
 */
class StudentSignupRolePropertyTest {

    /**
     * A pass-through encoder keeps the property fast across many iterations while
     * still exercising the real {@link AuthService#registerStudent} logic (the
     * behaviour under test is role assignment, not password hashing).
     */
    private static final PasswordEncoder PASS_THROUGH_ENCODER = new PasswordEncoder() {
        @Override
        public String encode(CharSequence rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }
    };

    @Property(tries = 100)
    void studentSignUpAlwaysCreatesAStudent(@ForAll("validRegistrations") RegisterStudentRequest request) {
        // Arrange: a fresh institute exists for the requested slug and the email is not taken.
        UserRepository userRepository = mock(UserRepository.class);
        InstituteRepository instituteRepository = mock(InstituteRepository.class);
        JwtService jwtService = mock(JwtService.class);

        Institute institute = new Institute();
        institute.setId(42L);
        institute.setName("Test Institute");
        institute.setSlug(request.slug());

        when(instituteRepository.findBySlug(request.slug())).thenReturn(Optional.of(institute));
        when(userRepository.existsByInstituteIdAndEmail(anyLong(), anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("test-token");

        AuthService authService =
                new AuthService(userRepository, instituteRepository, PASS_THROUGH_ENCODER, jwtService);

        // Act
        AuthResponse response = authService.registerStudent(request);

        // Assert: the persisted user AND the returned summary are both STUDENT,
        // regardless of the payload contents.
        ArgumentCaptor<User> persisted = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(persisted.capture());
        assertThat(persisted.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(response.user().role()).isEqualTo(Role.STUDENT);
    }

    /**
     * Valid landing-page registration payloads: non-blank name, syntactically valid
     * email, password of at least 8 characters, and a non-blank institute slug.
     */
    @Provide
    Arbitrary<RegisterStudentRequest> validRegistrations() {
        Arbitrary<String> slugs = Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .ofMinLength(1)
                .ofMaxLength(20);

        Arbitrary<String> names = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withChars(' ')
                .ofMinLength(1)
                .ofMaxLength(50)
                .filter(s -> !s.isBlank());

        Arbitrary<String> emailLocal = Arbitraries.strings()
                .withCharRange('a', 'z')
                .numeric()
                .ofMinLength(1)
                .ofMaxLength(15);
        Arbitrary<String> emailDomain = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(10);
        Arbitrary<String> emailTld = Arbitraries.of("com", "in", "org", "net", "edu");
        Arbitrary<String> emails = Combinators.combine(emailLocal, emailDomain, emailTld)
                .as((local, domain, tld) -> local + "@" + domain + "." + tld);

        Arbitrary<String> passwords = Arbitraries.strings()
                .withCharRange('!', '~')
                .ofMinLength(8)
                .ofMaxLength(64);

        return Combinators.combine(slugs, names, emails, passwords)
                .as(RegisterStudentRequest::new);
    }
}
