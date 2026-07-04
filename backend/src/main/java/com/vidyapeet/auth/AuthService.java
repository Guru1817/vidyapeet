package com.vidyapeet.auth;

import com.vidyapeet.auth.dto.AuthResponse;
import com.vidyapeet.auth.dto.LoginRequest;
import com.vidyapeet.auth.dto.RegisterStudentRequest;
import com.vidyapeet.auth.dto.UserSummary;
import com.vidyapeet.common.Role;
import com.vidyapeet.common.exception.Exceptions;
import com.vidyapeet.institute.Institute;
import com.vidyapeet.institute.repository.InstituteRepository;
import com.vidyapeet.security.JwtService;
import com.vidyapeet.security.UserPrincipal;
import com.vidyapeet.user.User;
import com.vidyapeet.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final InstituteRepository instituteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            InstituteRepository instituteRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.instituteRepository = instituteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Institute institute = null;
        User user;

        if (StringUtils.hasText(request.slug())) {
            institute = instituteRepository.findBySlug(request.slug())
                    .orElseThrow(() -> Exceptions.unauthorized("Invalid credentials."));
            user = userRepository.findByInstituteIdAndEmail(institute.getId(), request.email())
                    .orElseThrow(() -> Exceptions.unauthorized("Invalid credentials."));
        } else {
            // No slug => platform owner (SUPER_ADMIN) login.
            user = userRepository.findByEmailAndInstituteIdIsNull(request.email())
                    .orElseThrow(() -> Exceptions.unauthorized("Invalid credentials."));
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw Exceptions.unauthorized("Invalid credentials.");
        }

        return buildAuthResponse(user, institute);
    }

    @Transactional
    public AuthResponse registerStudent(RegisterStudentRequest request) {
        Institute institute = instituteRepository.findBySlug(request.slug())
                .orElseThrow(() -> Exceptions.notFound("No institute found for slug '" + request.slug() + "'."));

        if (userRepository.existsByInstituteIdAndEmail(institute.getId(), request.email())) {
            throw Exceptions.conflict("An account with this email already exists for this institute.");
        }

        User user = new User();
        user.setInstituteId(institute.getId());
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);
        user = userRepository.save(user);

        return buildAuthResponse(user, institute);
    }

    @Transactional(readOnly = true)
    public UserSummary currentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> Exceptions.unauthorized("Account no longer exists."));
        Institute institute = user.getInstituteId() == null
                ? null
                : instituteRepository.findById(user.getInstituteId()).orElse(null);
        return toSummary(user, institute);
    }

    private AuthResponse buildAuthResponse(User user, Institute institute) {
        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(token, toSummary(user, institute));
    }

    private UserSummary toSummary(User user, Institute institute) {
        return new UserSummary(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getInstituteId(),
                institute == null ? null : institute.getSlug());
    }
}
