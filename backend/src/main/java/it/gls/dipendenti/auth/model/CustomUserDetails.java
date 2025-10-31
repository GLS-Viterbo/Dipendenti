package it.gls.dipendenti.auth.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final Long companyId;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long userId,
                             String username,
                             String password,
                             Long companyId,
                             boolean active,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.companyId = companyId;
        this.active = active;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

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
        return active;
    }
}