package ru.sportorg.auth;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
class AuthUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    AuthUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return accountRepository.findForAuthentication(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}