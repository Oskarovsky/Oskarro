package com.oskarro.oskarito.security;


import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class SignUpRequest {

    @NotBlank(message = "Nazwa użytkownika jest wymagana")
    @Size(min = 3, max = 32, message = "Nazwa: 3–32 znaki, litery, cyfry, kropka, myślnik albo _.")
    @Pattern(regexp = "^[\\p{L}0-9_.-]{3,32}$", message = "Nazwa: 3–32 znaki, litery, cyfry, kropka, myślnik albo _.")
    private String username;

    @NotBlank(message = "Wymagany adres email")
    @Size(max = 50, message = "Email może mieć najwyżej 50 znaków")
    @Email(message = "Niepoprawny email")
    private String email;


    @NotBlank(message = "Wymagane hasło")
    @Size(min = 6, max = 40, message = "Hasło musi zawierać minimum 6 znaków")
    private String password;

    private String company;

    private String hp;

    @AssertTrue(message = "Zaakceptuj regulamin i politykę prywatności")
    private boolean acceptTerms;

}
