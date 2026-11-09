package com.br.adegueto.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.adegueto.dto.VendaRequest;
import com.br.adegueto.exception.DadoInvalidoException;
import com.br.adegueto.exception.EstoqueInsuficienteException;
import com.br.adegueto.exception.GlobalExceptionHandler;
import com.br.adegueto.exception.VendaInexistenteException;
import com.br.adegueto.model.Administrador;
import com.br.adegueto.model.Funcionario;
import com.br.adegueto.model.ItemVenda;
import com.br.adegueto.model.Usuario;
import com.br.adegueto.model.Venda;
import com.br.adegueto.model.Vodka;
import com.br.adegueto.security.AuthInterceptor;
import com.br.adegueto.security.SessaoUsuario;
import com.br.adegueto.service.VendaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VendaApiTest {

    private static final String VENDA_JSON = "{\"itens\":[{\"produtoId\":1,\"quantidade\":3}]}";

    private final SessaoUsuario sessaoUsuario = new SessaoUsuario();
    private VendaService vendaService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        vendaService = mock(VendaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new VendaController(vendaService, sessaoUsuario))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addMappedInterceptors(new String[] {"/api/**"}, new AuthInterceptor(sessaoUsuario))
                .build();
    }

    private MockHttpSession sessaoDe(Usuario usuario) {
        MockHttpSession sessao = new MockHttpSession();
        sessaoUsuario.iniciar(sessao, usuario);
        return sessao;
    }

    private MockHttpSession funcionario() {
        return sessaoDe(new Funcionario("Beto", "beto", "h"));
    }

    private MockHttpSession admin() {
        return sessaoDe(new Administrador("Ana", "ana", "h"));
    }

    private static Venda vendaPronta() {
        Venda venda = new Venda(new Funcionario("Beto", "beto", "h"));
        venda.adicionarItem(new ItemVenda(new Vodka("Vodka Pura", new BigDecimal("10.00"), "Polônia"), 3));
        venda.finalizar();
        return venda;
    }

    @Test
    void semSessaoRetorna401() throws Exception {
        mvc.perform(get("/api/vendas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/vendas").contentType(MediaType.APPLICATION_JSON).content(VENDA_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void funcionarioRegistraVendaERecebe201ComItensETotal() throws Exception {
        when(vendaService.registrar(any(), any(VendaRequest.class))).thenReturn(vendaPronta());

        mvc.perform(post("/api/vendas").session(funcionario())
                        .contentType(MediaType.APPLICATION_JSON).content(VENDA_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(30.0))
                .andExpect(jsonPath("$.vendedor").value("Beto"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.itens[0].produtoNome").value("Vodka Pura"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(3))
                .andExpect(jsonPath("$.itens[0].subtotal").value(30.0));
    }

    @Test
    void administradorTambemRegistraVenda() throws Exception {
        when(vendaService.registrar(any(), any(VendaRequest.class))).thenReturn(vendaPronta());

        mvc.perform(post("/api/vendas").session(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(VENDA_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void estoqueInsuficienteRetorna409() throws Exception {
        when(vendaService.registrar(any(), any(VendaRequest.class)))
                .thenThrow(new EstoqueInsuficienteException("Vodka Pura", 9, 2));

        mvc.perform(post("/api/vendas").session(funcionario())
                        .contentType(MediaType.APPLICATION_JSON).content(VENDA_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "estoque insuficiente para \"Vodka Pura\": solicitado 9, disponível 2"));
    }

    @Test
    void vendaSemItensRetorna400() throws Exception {
        when(vendaService.registrar(any(), any(VendaRequest.class)))
                .thenThrow(new DadoInvalidoException("a venda deve possuir ao menos um item"));

        mvc.perform(post("/api/vendas").session(funcionario())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"itens\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historicoVazioRetornaMensagemRN005() throws Exception {
        when(vendaService.historico(any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/vendas").session(funcionario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty())
                .andExpect(jsonPath("$.mensagem").value("nenhuma venda encontrada"));
    }

    @Test
    void historicoRetornaVendasEPassaOPeriodoAoService() throws Exception {
        when(vendaService.historico(eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 11, 30))))
                .thenReturn(List.of(vendaPronta()));

        mvc.perform(get("/api/vendas").param("inicio", "2026-11-01").param("fim", "2026-11-30")
                        .session(funcionario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].valorTotal").value(30.0))
                .andExpect(jsonPath("$.mensagem").doesNotExist());

        verify(vendaService).historico(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
    }

    @Test
    void dataInvalidaNoFiltroRetorna400() throws Exception {
        mvc.perform(get("/api/vendas").param("inicio", "ontem").session(funcionario()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void periodoInvertidoRetorna400() throws Exception {
        when(vendaService.historico(any(), any()))
                .thenThrow(new DadoInvalidoException("a data inicial não pode ser posterior à data final"));

        mvc.perform(get("/api/vendas").param("inicio", "2026-12-01").param("fim", "2026-11-01")
                        .session(funcionario()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obterVendaPorId() throws Exception {
        when(vendaService.obter(7L)).thenReturn(vendaPronta());

        mvc.perform(get("/api/vendas/7").session(funcionario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(10.0));
    }

    @Test
    void obterVendaInexistenteRetorna404() throws Exception {
        when(vendaService.obter(99L)).thenThrow(new VendaInexistenteException());

        mvc.perform(get("/api/vendas/99").session(funcionario()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("venda não encontrada"));
    }
}
