package com.br.adegueto.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.adegueto.dto.EstoqueResponse;
import com.br.adegueto.dto.ProdutoMaisVendidoResponse;
import com.br.adegueto.dto.RelatorioEstoqueResponse;
import com.br.adegueto.dto.RelatorioVendasResponse;
import com.br.adegueto.exception.DadoInvalidoException;
import com.br.adegueto.exception.GlobalExceptionHandler;
import com.br.adegueto.model.Administrador;
import com.br.adegueto.model.CategoriaProduto;
import com.br.adegueto.model.Funcionario;
import com.br.adegueto.model.Usuario;
import com.br.adegueto.security.AuthInterceptor;
import com.br.adegueto.security.SessaoUsuario;
import com.br.adegueto.service.RelatorioService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RelatorioApiTest {

    private final SessaoUsuario sessaoUsuario = new SessaoUsuario();
    private RelatorioService relatorioService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        relatorioService = mock(RelatorioService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RelatorioController(relatorioService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addMappedInterceptors(new String[] {"/api/**"}, new AuthInterceptor(sessaoUsuario))
                .build();
    }

    private MockHttpSession sessaoDe(Usuario usuario) {
        MockHttpSession sessao = new MockHttpSession();
        sessaoUsuario.iniciar(sessao, usuario);
        return sessao;
    }

    private MockHttpSession admin() {
        return sessaoDe(new Administrador("Ana", "ana", "h"));
    }

    private MockHttpSession funcionario() {
        return sessaoDe(new Funcionario("Beto", "beto", "h"));
    }

    @Test
    void semSessaoRetorna401() throws Exception {
        mvc.perform(get("/api/relatorios/vendas")).andExpect(status().isUnauthorized());
    }

    @Test
    void funcionarioNaoAcessaNenhumRelatorio() throws Exception {
        mvc.perform(get("/api/relatorios/vendas").session(funcionario())).andExpect(status().isForbidden());
        mvc.perform(get("/api/relatorios/produtos-mais-vendidos").session(funcionario()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/relatorios/estoque").session(funcionario())).andExpect(status().isForbidden());
    }

    @Test
    void administradorObtemOResumoDeVendasDoPeriodo() throws Exception {
        var resumo = new RelatorioVendasResponse(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 2,
                new BigDecimal("100.00"), new BigDecimal("50.00"), null);
        when(relatorioService.resumoVendas(eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 11, 30))))
                .thenReturn(resumo);

        mvc.perform(get("/api/relatorios/vendas").param("inicio", "2026-11-01").param("fim", "2026-11-30")
                        .session(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeVendas").value(2))
                .andExpect(jsonPath("$.faturamento").value(100.0))
                .andExpect(jsonPath("$.ticketMedio").value(50.0));
    }

    @Test
    void rankingRetornaItensEPassaOLimite() throws Exception {
        when(relatorioService.produtosMaisVendidos(any(), any(), eq(3))).thenReturn(List.of(
                new ProdutoMaisVendidoResponse(1L, "Vodka Pura", CategoriaProduto.VODKA, 5, new BigDecimal("50.00"))));

        mvc.perform(get("/api/relatorios/produtos-mais-vendidos").param("limite", "3").session(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].produtoNome").value("Vodka Pura"))
                .andExpect(jsonPath("$.itens[0].quantidadeVendida").value(5))
                .andExpect(jsonPath("$.mensagem").doesNotExist());

        verify(relatorioService).produtosMaisVendidos(null, null, 3);
    }

    @Test
    void rankingVazioRetornaMensagemRN005() throws Exception {
        when(relatorioService.produtosMaisVendidos(any(), any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/relatorios/produtos-mais-vendidos").session(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty())
                .andExpect(jsonPath("$.mensagem").value("nenhuma venda encontrada"));
    }

    @Test
    void limiteInvalidoRetorna400() throws Exception {
        when(relatorioService.produtosMaisVendidos(any(), any(), any()))
                .thenThrow(new DadoInvalidoException("o limite deve ser maior que zero"));

        mvc.perform(get("/api/relatorios/produtos-mais-vendidos").param("limite", "0").session(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void relatorioDeEstoqueRetornaContagensEItens() throws Exception {
        var item = new EstoqueResponse(1L, "Vodka Pura", CategoriaProduto.VODKA, 2, 5, true);
        when(relatorioService.posicaoEstoque()).thenReturn(new RelatorioEstoqueResponse(1, 1, List.of(item)));

        mvc.perform(get("/api/relatorios/estoque").session(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProdutos").value(1))
                .andExpect(jsonPath("$.produtosComEstoqueBaixo").value(1))
                .andExpect(jsonPath("$.itens[0].estoqueBaixo").value(true));
    }
}
