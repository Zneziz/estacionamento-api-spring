package com.api_rest.est.domain.partners;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "partner_stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PartnerStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int discountHours;

    @Column(nullable = false)
    private boolean active = true;
}
