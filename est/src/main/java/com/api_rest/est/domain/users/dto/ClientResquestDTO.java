package com.api_rest.est.domain.users.dto;

import com.api_rest.est.domain.users.Client;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

//recebe os dados e da uma verificada se segue as regras
public record ClientResquestDTO(
        @NotBlank(message = "Name is mandatory")
        String name,

        @NotBlank(message = "CPF is mandatory")
        String cpf,

        @NotBlank(message = "EMAIL is mandatory")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "PASSWOED is mandatory")
        @Size(min = 8, message = "The PASSWOED  must be at least 8 characters long")
        String password)
{ }
