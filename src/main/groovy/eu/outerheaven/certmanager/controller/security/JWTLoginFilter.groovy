package eu.outerheaven.certmanager.controller.security

import com.fasterxml.jackson.databind.ObjectMapper
import eu.outerheaven.certmanager.controller.entity.User
import eu.outerheaven.certmanager.controller.repository.UserRepository
import eu.outerheaven.certmanager.controller.service.TotpManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException
import java.security.acl.LastOwnerException;
import java.util.Collection;

public class JWTLoginFilter extends AbstractAuthenticationProcessingFilter {

    @Autowired
    private final UserRepository userRepository

    @Autowired
    private final TotpManager totpManager

    private static final Logger LOG = LoggerFactory.getLogger(JWTLoginFilter)

    public JWTLoginFilter(String url, AuthenticationManager authManager) {
        super(new AntPathRequestMatcher(url));
        setAuthenticationManager(authManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res) throws IOException {

        AccountCredentials creds = new ObjectMapper().readValue(req.getInputStream(), AccountCredentials.class);
        return getAuthenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken(
                        creds.getUsername(),
                        creds.getPassword(),
                        creds.getAuthorities(),
                )
        );
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain, Authentication auth) {

        /*
        AccountCredentials creds = new ObjectMapper().readValue(req.getInputStream(), AccountCredentials.class);

        User user = userRepository.findByUserName(creds.getUsername())
        if(user.mfa){
            if(totpManager.verifyCode(creds.getOtpCode(),user.getSecret())){
                LOG.info("User {} has succesfully validated OTP code on login!",user.userName)
                TokenAuthenticationHelper.addAuthentication(res, auth);
            }else throw new Exception("Invalid OTP code on login attempt!")

        }else{
            TokenAuthenticationHelper.addAuthentication(res, auth);
        }

         */
        TokenAuthenticationHelper.addAuthentication(res, auth);
    }

    static class AccountCredentials {
        private String username;
        private String password;
        private String otpCode;
        private Collection<GrantedAuthority> authorities;

        String getUsername() {
            return username;
        }

        void setUsername(String username) {
            this.username = username;
        }

        String getPassword() {
            return password;
        }

        void setPassword(String password) {
            this.password = password;
        }

        Collection<GrantedAuthority> getAuthorities() {
            return authorities;
        }

        void setAuthorities(Collection<GrantedAuthority> authorities) {
            this.authorities = authorities;
        }

        String getOtpCode() {
            return otpCode
        }

        void setOtpCode(String otpCode) {
            this.otpCode = otpCode
        }
    }
}
