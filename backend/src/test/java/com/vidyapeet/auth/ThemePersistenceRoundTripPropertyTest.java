package com.vidyapeet.auth;

import com.vidyapeet.auth.dto.ThemeUpdateRequest;
import com.vidyapeet.auth.dto.UserSummary;
import com.vidyapeet.common.Role;
import com.vidyapeet.institute.repository.InstituteRepository;
import com.vidyapeet.security.JwtService;
import com.vidyapeet.security.UserPrincipal;
import com.vidyapeet.user.ThemePreference;
import com.vidyapeet.user.User;
import com.vidyapeet.user.repository.UserRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feature: vidyapeeth-v2-upgrades, Property 3: Theme preference persistence round-trip
 *
 * <p>For any user and any theme value in {@code {LIGHT, DARK}}, persisting the preference
 * via the Theme_Service and then reloading the user's {@code UserSummary} from
 * {@code /api/auth/me} returns the same theme value.
 *
 * <p>Validates: Requirements 4.4, 4.5
 */
class ThemePersistenceRoundTripPropertyTest {

    private static final long USER_ID = 7L;

    /**
     * The theme update and current-user reads do not touch password hashing, so a
     * bare mock keeps the property fast across many iterations.
     */
    private static final PasswordEncoder PASS_THROUGH_ENCODER = mock(PasswordEncoder.class);

    @Property(tries = 100)
    void themePreferencePersistsAndReloads(@ForAll("themes") ThemePreference theme) {
        UserRepository userRepository = mock(UserRepository.class);
        InstituteRepository instituteRepository = mock(InstituteRepository.class);
        JwtService jwtService = mock(JwtService.class);

        // In-memory persisted state: seeded with LIGHT so a genuine round-trip is
        // observed (updateTheme must write, currentUser must read the written value).
        // A single-element holder simulates the users.theme_preference column.
        final ThemePreference[] persisted = {ThemePreference.LIGHT};

        // findById reconstructs a fresh User from the persisted state, mimicking a
        // reload from the database rather than reusing an in-memory instance.
        when(userRepository.findById(USER_ID)).thenAnswer(invocation -> {
            User reloaded = new User();
            reloaded.setId(USER_ID);
            reloaded.setName("Test User");
            reloaded.setEmail("user@example.com");
            reloaded.setPasswordHash("hashed:secret");
            reloaded.setRole(Role.STUDENT);
            reloaded.setInstituteId(null);
            reloaded.setThemePreference(persisted[0]);
            return Optional.of(reloaded);
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            persisted[0] = toSave.getThemePreference();
            return toSave;
        });

        AuthService authService =
                new AuthService(userRepository, instituteRepository, PASS_THROUGH_ENCODER, jwtService);
        UserPrincipal principal = new UserPrincipal(USER_ID, null, "user@example.com", Role.STUDENT);

        // Act: persist the chosen theme, then reload the user summary.
        authService.updateTheme(principal, new ThemeUpdateRequest(theme.name()));
        UserSummary reloaded = authService.currentUser(principal);

        // Assert: the reloaded summary carries exactly the persisted theme.
        assertThat(reloaded.themePreference()).isEqualTo(theme);
    }

    @Provide
    Arbitrary<ThemePreference> themes() {
        return Arbitraries.of(ThemePreference.LIGHT, ThemePreference.DARK);
    }
}
