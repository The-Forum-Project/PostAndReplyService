package com.example.postandreplyservice.security;



import com.example.postandreplyservice.exception.InvalidTokenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

//The jwt filter that we want to add to the chain of filters of Spring Security
@Component
public class JwtFilter extends OncePerRequestFilter {

    private JwtProvider jwtProvider;

    @Autowired
    public void setJwtProvider(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String prefixedToken = request.getHeader("Authorization");

        if(prefixedToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<AuthUserDetail> authUserDetailOptional;

        try {
            authUserDetailOptional = jwtProvider.resolveToken(request); // extract jwt from request, generate a userdetails object
        } catch (InvalidTokenException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"Invalid JWT token\"}");
            response.getWriter().flush();
            return;
        }

        if (authUserDetailOptional.isPresent()){
            AuthUserDetail authUserDetail = authUserDetailOptional.get();
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authUserDetail.getId(),
                    null,
                    authUserDetail.getAuthorities()
            ); // generate authentication object

            SecurityContextHolder.getContext().setAuthentication(authentication); // put authentication object in the secruitycontext
        }

        filterChain.doFilter(request, response);

    }
}
