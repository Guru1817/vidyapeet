package com.vidyapeet.security;

import com.vidyapeet.common.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Authenticated principal reconstructed from the JWT on each request. Stateless:
 * no database lookup is required to authenticate a request.
 */
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final Long instituteId;
    private final String email;
    private final Role role;

    public UserPrincipal(Long userId, Long instituteId, String email, Role role) {
        this.userId = userId;
        this.instituteId = instituteId;
        this.email = email;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getInstituteId() {
        return instituteId;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
