package com.api_rest.est.domain.users;
import org.springframework.data.jpa.repository.JpaRepository;

//extende metodos basicos do sql pelo JpaRepository

public interface ClientRepository extends JpaRepository<Client, Long>{

    //usa o existsBy para fazer uma consulta. ex: SELECT 1 FROM clientes WHERE cpf = ? LIMIT 1
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);

    default boolean existByCpf(String cpf) {
        return existsByCpf(cpf);
    }

    default boolean existByEmail(String email) {
        return existsByEmail(email);
    }
}
