package com.oskarro.oskarito.security.jwt;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JwtResponse {
    private Integer id;
    private String token;
    private String type = "Bearer";
    private String username;
    private String email;
    private List<String> roles;

    public JwtResponse(String accessToken, Integer id, String username, String email,
                       List<String> roles) {
        this.token = accessToken;
        this.username = username;
        this.roles = roles;
        this.email = email;
        this.id = id;
    }

}
