package com.api_rest.est.controllers;

import com.api_rest.est.domain.users.*;
import com.api_rest.est.domain.users.dto.ClientResponseDTO;
import com.api_rest.est.domain.users.dto.ClientResquestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
public class ClientController {

    //depedencia da service
    private final ClientService service;

    //injecao de dependencia
    public ClientController(ClientService service){
        this.service = service;
    }

    //metodo post
    @PostMapping
    //ResponseEntity permite manipular o corpo, cabecalho e status para retorna o response
    //@RequestBody @Valid pega o o json e valida pelo ClientResquestDTO, se nao validar ja devolve o 400
    public ResponseEntity<ClientResponseDTO> register(@RequestBody @Valid ClientResquestDTO dto){
        //usa o metodo do service e retorna o clientSaved pra o response
        ClientResponseDTO response = service.register(dto);
        //retorna a resposta HTTP 201 e o body
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
