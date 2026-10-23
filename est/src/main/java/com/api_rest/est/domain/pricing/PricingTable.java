package com.api_rest.est.domain.pricing;

import com.api_rest.est.domain.vehicles.VehicleType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pricing_tables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PricingTable {

    public static final int TOLERANCE_MINUTES = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private VehicleType vehicleType; // CAR ou MOTORCYCLE

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal firstHourRate; // Valor da primeira hora

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal additionalFractionRate; // Valor de cada fração de 15 min

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyCap; // Teto por diária
}