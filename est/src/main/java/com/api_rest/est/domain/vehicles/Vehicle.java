package com.api_rest.est.domain.vehicles;

import com.api_rest.est.domain.users.Client;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String licensePlate; // Placa única

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType type; // CAR ou MOTORCYCLE

    @Column(nullable = false)
    private boolean electric = false; // Se é elétrico

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client; // Dono do veículo
}
