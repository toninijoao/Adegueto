package com.br.adegueto.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.adegueto.exception.EstoqueInsuficienteException;
import com.br.adegueto.exception.GlobalExceptionHandler;
import com.br.adegueto.exception.ProdutoInexistenteException;
import com.br.adegueto.exception.QuantidadeInvalidaException;
import com.br.adegueto.model.Administrador;
import com.br.adegueto.model.Estoque;
import com.br.adegueto.model.Funcionario;
import com.br.adegueto.model.Usuario;
import com.br.adegueto.model.Vodka;
import com.br.adegueto.security.AuthInterceptor;
import com.br.adegueto.security.SessaoUsuario;
import com.br.adegueto.service.EstoqueService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EstoqueApiTest {

    private final SessaoUsuario sessaoUsuario = new SessaoUsuario();
    private EstoqueService estoqueService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        estoqueService = mock(EstoqueService.class);
        mvc = MockMvcBuilders.standaloneSetup(new EstoqueController(estoqueService))
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

    private static Estoque estoque(int quantidade, int minimo) {
        Vodka produto = new Vodka("Vodka Pura", BigDecimal.TEN, "Polônia");
        Estoque e = new Estoque(quantidade, minimo);
        produto.vincularEstoque(e);
        return e;
    }

    private static final String QTD_5 = "{\"quantidade\":5}";

    @Test
    void semSessaoRetorna401() throws Exception {
        mvc.perform(get("/api/estoque")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/estoque/1/entrada").contentType(MediaType.APPLICATION_JSON).content(QTD_5))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void funcionarioEAdministradorConsultamOEstoque() throws Exception {
        when(estoqueService.listar()).thenReturn(List.of(estoque(2, 3)));

        for (MockHttpSession sessao : List.of(funcionario(), admin())) {
            mvc.perform(get("/api/estoque").session(sessao))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.itens[0].produtoNome").value("Vodka Pura"))
                    .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                    .andExpect(jsonPath("$.itens[0].estoqueBaixo").value(true))
                    .andExpect(jsonPath("$.mensagem").doesNotExist());
        }
    }

    @Test
    void estoqueBaixoSemResultadosRetornaMensagemInformativa() throws Exception {
        when(estoqueService.listarBaixo()).thenReturn(List.of());

        mvc.perform(get("/api/estoque/baixo").session(funcionario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty())
                .andExpect(jsonPath("$.mensagem").value("nenhum produto com estoque baixo"));
    }

    @Test
    void estoqueBaixoRetornaOsProdutosAbaixoDoMinimo() throws Exception {
        when(estoqueService.listarBaixo()).thenReturn(List.of(estoque(1, 5)));

        mvc.perform(get("/api/estoque/baixo").session(funcionario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].estoqueMin").value(5));
    }

    @Test
    void funcionarioRegistraEntrada() throws Exception {
        when(estoqueService.entrada(eq(1L), eq(5))).thenReturn(estoque(15, 2));

        mvc.perform(post("/api/estoque/1/entrada").session(funcionario())
                        .contentType(MediaType.APPLICATION_JSON).content(QTD_5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(15));
    }

    @Test
    void administradorRegistraSaida() throws Exception {
        when(estoqueService.saida(eq(1L), eq(5))).thenReturn(estoque(5, 2));

        mvc.perform(post("/api/estoque/1/saida").session(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(QTD_5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(5));
    }

    @Test
    void estoqueInsuficienteRetorna409() throws Exception {
        when(estoqueService.saida(any(), any())).thenThrow(new EstoqueInsuficienteException(9, 3));

        mvc.perform(post("/api/estoque/1/saida").session(funcionario())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":9}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("estoque insuficiente: solicitado 9, disponível 3"));
    }

    @Test
    void quantidadeInvalidaRetorna400() throws Exception {
        when(estoqueService.entrada(any(), any())).thenThrow(new QuantidadeInvalidaException("quantidade inválida"));

        mvc.perform(post("/api/estoque/1/entrada").session(funcionario())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void produtoInexistenteRetorna404() throws Exception {
        when(estoqueService.entrada(any(), any())).thenThrow(new ProdutoInexistenteException());

        mvc.perform(post("/api/estoque/99/entrada").session(funcionario())
                        .contentType(MediaType.APPLICATION_JSON).content(QTD_5))
                .andExpect(status().isNotFound());
    }

    @Test
    void conflitoDeConcorrenciaRetorna409() throws Exception {
        when(estoqueService.saida(any(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Estoque.class, 1L));

        mvc.perform(post("/api/estoque/1/saida").session(funcionario())
                        .contentType(MediaType.APPLICATION_JSON).content(QTD_5))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("o estoque foi alterado por outra operação; tente novamente"));
    }
}
