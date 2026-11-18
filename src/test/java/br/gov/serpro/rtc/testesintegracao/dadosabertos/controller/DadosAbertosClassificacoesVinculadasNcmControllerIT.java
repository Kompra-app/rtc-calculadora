package br.gov.serpro.rtc.testesintegracao.dadosabertos.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A lista de classificações vinculadas a um NCM tem de coincidir com a crítica
 * do cálculo (NcmAplicavelService): para o arroz polido (10063021) em 2027, o
 * anexo aceita 200003, 200038 e 515001 e recusa 200034, cuja exceção cobre o
 * NCM; 000001 não tem anexo e não entra na lista.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-testes.yml")
@ActiveProfiles("testes")
class DadosAbertosClassificacoesVinculadasNcmControllerIT {

    private static final String ENDPOINT = "/calculadora/dados-abertos/classificacoes-tributarias/ncm/vinculadas";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void teste_controller_listarVinculadasPorNcmSegueACriticaDoCalculo() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                .param("ncm", "10063021")
                .param("data", "2027-01-01")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", contains("200003", "200038", "515001")))
            .andExpect(jsonPath("$", not(hasItem("200034"))))
            .andExpect(jsonPath("$", not(hasItem("000001"))));
    }

    @Test
    void teste_controller_listarVinculadasPorNcmInexistenteDeveRetornarNotFound() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                .param("ncm", "99999999")
                .param("data", "2027-01-01")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value(containsString("99999999")));
    }

    @Test
    void teste_controller_listarVinculadasPorNcmSemDataDeveRetornarBadRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                .param("ncm", "10063021")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value(containsString("Required parameter 'data' is not present.")));
    }
}
