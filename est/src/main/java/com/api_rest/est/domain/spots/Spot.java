package com.api_rest.est.domain.spots;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "spots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Spot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotType type;

    @Column(nullable = false)
    private boolean occupied = false;
}
