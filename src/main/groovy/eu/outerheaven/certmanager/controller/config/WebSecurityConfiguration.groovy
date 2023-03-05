package eu.outerheaven.certmanager.controller.config

import eu.outerheaven.certmanager.controller.security.JWTAuthenticationFilter;
import eu.outerheaven.certmanager.controller.security.JWTLoginFilter
import eu.outerheaven.certmanager.controller.service.UserPrincipalDetailsService
import eu.outerheaven.certmanager.controller.util.CustomFilter
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource



@EnableWebSecurity
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserPrincipalDetailsService userPrincipalDetailsService


    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(Arrays.asList("http://localhost:8080", "http://192.168.1.206:8080"));
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        //TODO is this smart to leave like this? I have no fucking idea
        config.addExposedHeader('Content-Disposition')
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        CustomFilter customFilter = new CustomFilter()
        http
                .cors()
                .and()
                .csrf()
                .ignoringAntMatchers("/login")
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .and()
                .authorizeRequests()
                .antMatchers("/onlyforadmin/**").hasAuthority("ADMIN")
                .antMatchers("/secured/**").hasAnyAuthority("USER", "ADMIN")
                .antMatchers("/**").permitAll()
                .and()
                .addFilterBefore(jWTLoginFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JWTAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(customFilter, JWTLoginFilter.class)
                //.addFilterAfter(contentCachingFilter, JWTLoginFilter.class)
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {

       auth.authenticationProvider(authenticationProvider())

    /*
        auth
                .inMemoryAuthentication()
                .withUser("admin").password("{noop}password").authorities("USER", "ADMIN")
                .and()
                .withUser("user").password("{noop}password").authorities("USER");

     */
    }

    DaoAuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider()
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder())
        daoAuthenticationProvider.setUserDetailsService(this.userPrincipalDetailsService)

        return daoAuthenticationProvider
    }

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder()
    }

    @Bean
    JWTLoginFilter jWTLoginFilter() throws Exception {
        return new JWTLoginFilter("/login", authenticationManager());
    }


}
