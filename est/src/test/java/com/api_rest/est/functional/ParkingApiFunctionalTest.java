package com.api_rest.est.functional;

import com.api_rest.est.domain.parking.ParkingStay;
import com.api_rest.est.domain.parking.ParkingStayRepository;
import com.api_rest.est.domain.parking.ParkingStayStatus;
import com.api_rest.est.domain.spots.SpotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ParkingApiFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ParkingStayRepository stayRepository;

    @Autowired
    private SpotRepository spotRepository;

    private String loginAndGetToken(String email, String password) throws Exception {
        String payload = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    @DisplayName("Fluxo Funcional Completo RF01 a RF15: Autenticação, Vagas, Preços, Veículos, Entrada, Saída, Ticket Perdido, Mensalista, Convênio, Histórico e Relatórios")
    void shouldExecuteCompleteParkingWorkflowEndToEnd() throws Exception {
        stayRepository.deleteAll();
        spotRepository.deleteAll();

        // 1. RF01: Cadastro de cliente (senha nunca devolvida na resposta)
        String clientJson = """
                {
                    "name": "Cliente Teste",
                    "cpf": "12345678909",
                    "email": "cliente@teste.com",
                    "password": "password123"
                }
                """;
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cliente Teste"))
                .andExpect(jsonPath("$.password").doesNotExist());

        // 2. RF02: Login com credenciais inválidas não indica qual campo está errado
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cliente@teste.com\",\"password\":\"errada123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password."));

        // Login dos três perfis (Admin e Operador criados na carga inicial + Cliente cadastrado)
        String adminToken = loginAndGetToken("admin@estacionamento.com", "admin1234");
        String operatorToken = loginAndGetToken("operador@estacionamento.com", "operador1234");
        String clientToken = loginAndGetToken("cliente@teste.com", "password123");

        // 3. RF03: Administrador cadastra vagas (4 vagas para permitir 1 mensalista pela regra dos 30%)
        String[] spotCodes = {"C-01", "C-02", "P-01", "E-01"};
        String[] spotTypes = {"CAR", "CAR", "PCD", "ELECTRIC"};
        for (int i = 0; i < spotCodes.length; i++) {
            mockMvc.perform(post("/spots")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("{\"code\":\"%s\",\"type\":\"%s\"}", spotCodes[i], spotTypes[i])))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.occupied").value(false));
        }

        // Cliente não pode cadastrar vaga (403)
        mockMvc.perform(post("/spots")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"C-99\",\"type\":\"CAR\"}"))
                .andExpect(status().isForbidden());

        // 4. RF04: Administrador atualiza tabela de preços
        String pricingJson = """
                {
                    "vehicleType": "CAR",
                    "firstHourRate": 12.00,
                    "additionalFractionRate": 4.00,
                    "dailyCap": 60.00
                }
                """;
        mockMvc.perform(put("/pricing")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pricingJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstHourRate").value(12.00))
                .andExpect(jsonPath("$.toleranceMinutes").value(10));

        // 5. RF05: Cliente cadastra seus veículos
        String vehicle1Json = """
                {
                    "licensePlate": "CAR1001",
                    "type": "CAR",
                    "electric": false,
                    "pcdCredential": "PCD-2026-001"
                }
                """;
        mockMvc.perform(post("/vehicles")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicle1Json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value("CAR1001"))
                .andExpect(jsonPath("$.pcdCredential").value("PCD-2026-001"));

        String vehicle2Json = """
                {
                    "licensePlate": "CAR2002",
                    "type": "CAR",
                    "electric": true
                }
                """;
        mockMvc.perform(post("/vehicles")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicle2Json))
                .andExpect(status().isCreated());

        // 6. RF11: Administrador cadastra loja conveniada (2 horas de desconto)
        MvcResult partnerResult = mockMvc.perform(post("/partners")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Livraria do Campus\",\"discountHours\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountHours").value(2))
                .andReturn();
        long partnerId = objectMapper.readTree(partnerResult.getResponse().getContentAsString()).get("id").asLong();

        // 7. RF06: Operador registra entrada do veículo CAR1001
        mockMvc.perform(post("/stays/entry")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licensePlate\":\"CAR1001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));

        // Simula que a entrada ocorreu há 90 minutos (1h30)
        ParkingStay openStay = stayRepository.findByVehicleLicensePlateIgnoreCaseAndStatus("CAR1001", ParkingStayStatus.OPEN)
                .orElseThrow();
        openStay.setEntryTime(LocalDateTime.now().minusMinutes(90));
        stayRepository.save(openStay);

        // 8. RF07: Consulta valor atual da permanência aberta (1h30 = 12.00 + 2*4.00 = 20.00)
        mockMvc.perform(get("/stays/current-fee/CAR1001")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(20.00));

        // 9. RF08 + RF09 + RF11: Saída com convênio de 2h (abate 1h30 -> 0.00) e ticket perdido (+1 diária de 60.00)
        String exitJson = String.format("""
                {
                    "licensePlate": "CAR1001",
                    "paymentMethod": "PIX",
                    "lostTicket": true,
                    "partnerStoreId": %d
                }
                """, partnerId);
        mockMvc.perform(post("/stays/exit")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exitJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(60.00))
                .andExpect(jsonPath("$.lostTicket").value(true))
                .andExpect(jsonPath("$.partnerStoreName").value("Livraria do Campus"))
                .andExpect(jsonPath("$.spotReleased").value(true));

        // 10. RF10: Cliente contrata plano mensal para CAR2002 (4 vagas * 30% = 1.2 -> permite 1 mensalista)
        mockMvc.perform(post("/monthly-plans")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licensePlate\":\"CAR2002\",\"monthlyFee\":250.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value("CAR2002"))
                .andExpect(jsonPath("$.active").value(true));

        // Tentar contratar um 2º plano mensal com apenas 4 vagas deve ser recusado (ultrapassa 30% das vagas)
        mockMvc.perform(post("/monthly-plans")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licensePlate\":\"CAR1001\",\"monthlyFee\":250.00}"))
                .andExpect(status().isBadRequest());

        // Entrada e saída do veículo mensalista CAR2002 -> não paga na saída (totalAmount = 0.00)
        mockMvc.perform(post("/stays/entry")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licensePlate\":\"CAR2002\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/stays/exit")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licensePlate\":\"CAR2002\",\"paymentMethod\":\"PIX\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(0.00))
                .andExpect(jsonPath("$.monthlyPlanApplied").value(true));

        // 11. RF12: Cliente consulta seu histórico de permanências
        mockMvc.perform(get("/stays/history")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 12. RF13, RF14, RF15: Relatórios do Administrador
        mockMvc.perform(get("/reports/occupancy")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpots").value(4))
                .andExpect(jsonPath("$.totalOccupied").value(0));

        mockMvc.perform(get("/reports/revenue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monthlyRevenue").value(250.00))
                .andExpect(jsonPath("$[0].partnerRevenue").value(60.00))
                .andExpect(jsonPath("$[0].totalRevenue").value(310.00));

        mockMvc.perform(get("/reports/average-stay-time")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }
}
