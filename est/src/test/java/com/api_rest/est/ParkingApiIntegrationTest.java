package com.api_rest.est;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.api_rest.est.domain.parking.PaymentMethod;
import com.api_rest.est.domain.parking.dto.EntryRequestDTO;
import com.api_rest.est.domain.parking.dto.ExitRequestDTO;
import com.api_rest.est.domain.pricing.dto.PricingRequestDTO;
import com.api_rest.est.domain.spots.SpotType;
import com.api_rest.est.domain.spots.dto.SpotRequestDTO;
import com.api_rest.est.domain.users.dto.ClientRequestDTO;
import com.api_rest.est.domain.users.dto.LoginRequestDTO;
import com.api_rest.est.domain.vehicles.VehicleType;
import com.api_rest.est.domain.vehicles.dto.VehicleRequestDTO;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ParkingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String adminToken;
    private static String operatorToken;
    private static String clientToken;

    @Test
    @Order(1)
    void rf01_shouldRegisterClientAndNeverReturnPassword() throws Exception {
        var dto = new ClientRequestDTO("João Silva", "12345678901", "joao@email.com", "senha1234");

        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("joao@email.com"))
                .andExpect(jsonPath("$.password").doesNotExist()); // RF01: nunca devolve a senha
    }

    @Test
    @Order(2)
    void rf02_shouldLoginAndHideWhichFieldIsWrongOnFailure() throws Exception {
        // Login inválido não deve dizer qual campo está errado
        var badLogin = new LoginRequestDTO("joao@email.com", "senhaErrada");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password."));

        // Login do Admin, Operador e Cliente
        adminToken = loginAndGetToken("admin@parking.com", "admin1234");
        operatorToken = loginAndGetToken("operator@parking.com", "operator1234");
        clientToken = loginAndGetToken("joao@email.com", "senha1234");
    }

    @Test
    @Order(3)
    void rf03_adminShouldCreateSpots() throws Exception {
        var pcdSpot = new SpotRequestDTO("PCD-01", SpotType.PCD);

        mockMvc.perform(post("/spots")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pcdSpot)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PCD-01"))
                .andExpect(jsonPath("$.occupied").value(false));
    }

    @Test
    @Order(4)
    void rf04_adminShouldMaintainPricingTable() throws Exception {
        var pricing = new PricingRequestDTO(
                VehicleType.CAR,
                new BigDecimal("12.00"),
                new BigDecimal("4.00"),
                new BigDecimal("50.00")
        );

        mockMvc.perform(put("/pricing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pricing)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstHourRate").value(12.00));
    }

    @Test
    @Order(5)
    void rf05_clientShouldRegisterOwnVehicleWithPcdCredential() throws Exception {
        var vehicle = new VehicleRequestDTO("ABC1D23", VehicleType.CAR, false, "PCD-999888");

        mockMvc.perform(post("/vehicles")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehicle)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value("ABC1D23"))
                .andExpect(jsonPath("$.pcdCredential").value("PCD-999888"));
    }

    @Test
    @Order(6)
    void rf06_operatorShouldRegisterEntryAndPreventDuplicateOpenStay() throws Exception {
        var entry = new EntryRequestDTO("ABC1D23");

        // Primeira entrada: aloca vaga PCD-01 compatível
        mockMvc.perform(post("/stays/entry")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entry)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spotCode").value("PCD-01"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        // Segunda tentativa com a mesma placa aberta deve ser recusada (400 Bad Request)
        mockMvc.perform(post("/stays/entry")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entry)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(7)
    void rf07_clientShouldConsultCurrentFee() throws Exception {
        mockMvc.perform(get("/stays/current-fee/ABC1D23")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value("ABC1D23"))
                .andExpect(jsonPath("$.currentAmount").value(0.00)); // Dentro dos 10 min de tolerância
    }

    @Test
    @Order(8)
    void rf08_operatorShouldRegisterExitAndReleaseSpot() throws Exception {
        var exit = new ExitRequestDTO("ABC1D23", PaymentMethod.PIX);

        mockMvc.perform(post("/stays/exit")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.spotReleased").value(true));
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        var loginDto = new LoginRequestDTO(email, password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }
}
