package com.api_rest.est.domain.users.dto;

import com.api_rest.est.domain.users.Client;

//record para objetos que apenas carrega dados
//sem senha na resposta por seguranca
public record ClientResponseDTO(Long id, String name, String cpf, String email ) {

    public ClientResponseDTO(Client client){
        //utiliza o construtor do Client mas sem a senha
        this(client.getId(), client.getName(), client.getCpf(), client.getEmail());
    }

}
