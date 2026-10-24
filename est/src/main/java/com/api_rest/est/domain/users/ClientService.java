package com.api_rest.est.domain.users;

import com.api_rest.est.domain.users.dto.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

    //declaracao das dependencias
    private final ClientRepository repository;
    private final PasswordEncoder passwordEncoder;

    //faz a injecao de dependencia
    public ClientService(ClientRepository repository, PasswordEncoder passwordEncoder){
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    //recebe o resquest da controller e promete devolver o response
    public ClientResponseDTO register(ClientRequestDTO dto){
        //regras de negocios
        if (repository.existByCpf(dto.cpf())){
            throw new IllegalArgumentException("CPF already registered");
        }
        if (repository.existByEmail(dto.email())){
            throw new IllegalArgumentException("EMAIL already registered");
        }
        //transfere os dados da dto pra a entidade
        Client client = new Client();
        client.setName(dto.name());
        client.setCpf(dto.cpf());
        client.setEmail(dto.email());

        //criptografa a senha
        client.setPassword(passwordEncoder.encode(dto.password()));


        client.setRole(Role.CLIENT);

        //pega a entidade e salva os dados no banco e adicoina o id do banco ao clientSaved
        Client clientSaved  = repository.save(client);

        //retorna o a resposta sem senha e pela dto
        return new ClientResponseDTO(clientSaved);
    }


}
