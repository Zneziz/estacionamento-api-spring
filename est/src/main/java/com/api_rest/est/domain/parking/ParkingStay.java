package com.api_rest.est.domain.parking;

import com.api_rest.est.domain.partners.PartnerStore;
import com.api_rest.est.domain.spots.Spot;
import com.api_rest.est.domain.vehicles.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parking_stays")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ParkingStay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @Column(nullable = false)
    private LocalDateTime entryTime;

    private LocalDateTime exitTime;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private boolean paid = false;

    private LocalDateTime paidAt;

    // RF09: Ticket perdido (cobra uma diária a mais sobre o valor calculado)
    @Column(nullable = false)
    private boolean lostTicket = false;

    // RF10: Indica se a saída teve isenção por plano mensal ativo em dia
    @Column(nullable = false)
    private boolean monthlyPlanApplied = false;

    // RF11: No máximo um convênio por permanência
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_store_id")
    private PartnerStore partnerStore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParkingStayStatus status;
}
