package com.example.postandreplyservice.service.remote;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class FeignClientInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // Set the token value in the request header
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGVtYWlsLmNvbSIsInBlcm1pc3Npb25zIjpbeyJhdXRob3JpdHkiOiJlbWFpbCJ9LHsiYXV0aG9yaXR5Ijoibm9ybWFsIn1dLCJpZCI6Mn0.Fgfs0NzaXujnN1J1PzTzuBn7IYiav5vZTUycP0-drwY";
        System.out.println("token: " + token);
        template.header("Authorization", "Bearer " + token);
    }
}

