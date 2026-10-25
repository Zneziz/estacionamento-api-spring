package com.api_rest.est.domain.parking.dto;

import com.api_rest.est.domain.parking.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExitRequestDTO(
        @NotBlank(message = "License plate is required")
        String licensePlate,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        // RF09: Saída sem ticket cobra uma diária a mais (opcional, default false)
        Boolean lostTicket,

        // RF11: Convênio informado na saída (opcional, no máximo um por permanência)
        Long partnerStoreId
) {
    public ExitRequestDTO(String licensePlate, PaymentMethod paymentMethod) {
        this(licensePlate, paymentMethod, false, null);
    }

    public boolean isLostTicket() {
        return Boolean.TRUE.equals(lostTicket);
    }
}
