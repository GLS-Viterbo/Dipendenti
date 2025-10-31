package it.gls.dipendenti.auth.service;

import it.gls.dipendenti.auth.model.CustomUserDetails;
import it.gls.dipendenti.auth.model.Role;
import it.gls.dipendenti.auth.model.User;
import it.gls.dipendenti.auth.repository.RoleRepository;
import it.gls.dipendenti.auth.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CustomUserDetailsService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!user.active()) {
            throw new UsernameNotFoundException("User is not active: " + username);
        }

        List<Role> roles = roleRepository.findByUserId(user.id());

        return new CustomUserDetails(
                user.id(),
                user.username(),
                user.passwordHash(),
                user.companyId(),
                user.active(),
                mapRolesToAuthorities(roles)
        );
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(List<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());
    }
}