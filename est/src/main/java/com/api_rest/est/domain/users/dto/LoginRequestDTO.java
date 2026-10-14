package com.api_rest.est.domain.users.dto;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.EnableMBeanExport;

public record LoginRequestDTO( @NotBlank @Email  String email, @NotBlank String password) {


}
