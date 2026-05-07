package io.github.wahhh.bacp.common.web;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authenticated principal carrying RBAC codes for Spring Security.
 */
@Data
@Builder
public class LoginUser implements UserDetails {

    private Long userId;

    private String username;

    @Builder.Default
    private Set<String> permissions = new HashSet<>();

    @Builder.Default
    private Set<String> roles = new HashSet<>();

    /**
     * Maps permission strings to {@link GrantedAuthority}.
     *
     * @return authorities for {@link org.springframework.security.access.prepost.PreAuthorize}
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    /**
     * Password is not stored on this principal (handled via credential chain).
     *
     * @return empty string
     */
    @Override
    public String getPassword() {
        return "";
    }

    /**
     * Login name.
     *
     * @return username
     */
    @Override
    public String getUsername() {
        return username;
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
