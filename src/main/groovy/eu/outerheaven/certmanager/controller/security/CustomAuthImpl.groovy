package eu.outerheaven.certmanager.controller.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority

class CustomAuthImpl implements Authentication {

    @Override
    Collection<? extends GrantedAuthority> getAuthorities() {
        return null
    }

    @Override
    Object getCredentials() {
        return null
    }

    @Override
    Object getDetails() {
        return null
    }

    @Override
    Object getPrincipal() {
        return null
    }

    @Override
    boolean isAuthenticated() {
        return false
    }

    @Override
    void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

    }

    @Override
    String getName() {
        return null
    }
}
