package com.api_rest.est.domain.users;
import org.springframework.data.jpa.repository.JpaRepository;

//extende metodos basicos do sql pelo JpaRepository

public interface ClientRepository extends JpaRepository<Client, Long>{

    //usa o existBy para fazer uma consulta. ex: SELECT 1 FROM clientes WHERE cpf = ? LIMIT 1
    boolean existByCpf(String cpf);
    boolean existByEmail(String email);
}
