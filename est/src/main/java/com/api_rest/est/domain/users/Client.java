package com.api_rest.est.domain.users;

import jakarta.persistence.*;
import lombok.*;
import org.apache.el.stream.StreamELResolverImpl;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

//basicamente a tabela do db
public class Client extends User{


    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(unique = true)
    private String pcdCredential;


}
