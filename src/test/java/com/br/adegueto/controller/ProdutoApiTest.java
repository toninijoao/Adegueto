package com.br.adegueto.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.adegueto.dto.ProdutoRequest;
import com.br.adegueto.exception.GlobalExceptionHandler;
import com.br.adegueto.exception.ProdutoComVendasException;
import com.br.adegueto.exception.ProdutoInexistenteException;
import com.br.adegueto.model.Administrador;
import com.br.adegueto.model.CategoriaProduto;
import com.br.adegueto.model.Estoque;
import com.br.adegueto.model.Funcionario;
import com.br.adegueto.model.Produto;
import com.br.adegueto.model.Usuario;
import com.br.adegueto.model.Vinho;
import com.br.adegueto.security.AuthInterceptor;
import com.br.adegueto.security.SessaoUsuario;
import com.br.adegueto.service.ProdutoService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProdutoApiTest {

    private static final String VINHO_JSON = "{\"categoria\":\"VINHO\",\"nome\":\"Malbec\",\"preco\":89.90,"
            + "\"quantidadeInicial\":12,\"estoqueMin\":3,\"tipo\":\"Tinto\",\"origem\":\"Argentina\","
            + "\"uva\":\"Malbec\",\"safra\":2020}";

    private final SessaoUsuario sessaoUsuario = new SessaoUsuario();
    private ProdutoService produtoService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        produtoService = mock(ProdutoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProdutoController(produtoService))
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

    private static Produto malbec() {
        Produto p = new Vinho("Malbec", new BigDecimal("89.90"), "Tinto", "Argentina", "Malbec", 2020);
        p.vincularEstoque(new Estoque(2, 3)); // abaixo do mínimo
        return p;
    }

    @Test
    void semSessaoTodasAsRotasRetornam401() throws Exception {
        mvc.perform(get("/api/produtos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/produtos/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void funcionarioConsultaProdutosComEstoqueBaixoSinalizado() throws Exception {
        when(produtoService.buscar(any(), any())).thenReturn(List.of(malbec()));

        mvc.perform(get("/api/produtos").session(funcionario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].nome").value("Malbec"))
                .andExpect(jsonPath("$.itens[0].categoria").value("VINHO"))
                .andExpect(jsonPath("$.itens[0].uva").value("Malbec"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                .andExpect(jsonPath("$.itens[0].estoqueBaixo").value(true))
                .andExpect(jsonPath("$.mensagem").doesNotExist());
    }

    @Test
    void buscaSemResultadoRetorna200ComMensagemRN005() throws Exception {
        when(produtoService.buscar(any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/produtos").param("nome", "xyz").session(funcionario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isEmpty())
                .andExpect(jsonPath("$.mensagem").value("produto não encontrado"));
    }

    @Test
    void filtroDeCategoriaChegaAoService() throws Exception {
        when(produtoService.buscar(eq("mal"), eq(CategoriaProduto.VINHO))).thenReturn(List.of(malbec()));

        mvc.perform(get("/api/produtos").param("nome", "mal").param("categoria", "VINHO").session(funcionario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].nome").value("Malbec"));
    }

    @Test
    void categoriaInvalidaNaBuscaRetorna400() throws Exception {
        mvc.perform(get("/api/produtos").param("categoria", "CERVEJA").session(funcionario()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void obterInexistenteRetorna404() throws Exception {
        when(produtoService.obter(99L)).thenThrow(new ProdutoInexistenteException());

        mvc.perform(get("/api/produtos/99").session(funcionario()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("produto não encontrado"));
    }

    @Test
    void funcionarioNaoPodeCadastrarAlterarNemExcluir() throws Exception {
        mvc.perform(post("/api/produtos").session(funcionario())
                        .contentType(MediaType.APPLICATION_JSON).content(VINHO_JSON))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/produtos/1").session(funcionario())
                        .contentType(MediaType.APPLICATION_JSON).content(VINHO_JSON))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/produtos/1").session(funcionario())).andExpect(status().isForbidden());
    }

    @Test
    void administradorCadastraProdutoERecebe201() throws Exception {
        when(produtoService.cadastrar(any(ProdutoRequest.class))).thenReturn(malbec());

        mvc.perform(post("/api/produtos").session(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(VINHO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Malbec"))
                .andExpect(jsonPath("$.preco").value(89.90));
    }

    @Test
    void administradorAtualizaProduto() throws Exception {
        when(produtoService.atualizar(eq(1L), any(ProdutoRequest.class))).thenReturn(malbec());

        mvc.perform(put("/api/produtos/1").session(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(VINHO_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("VINHO"));
    }

    @Test
    void administradorExcluiProdutoSemVendas() throws Exception {
        mvc.perform(delete("/api/produtos/1").session(admin())).andExpect(status().isNoContent());
    }

    @Test
    void exclusaoDeProdutoComVendasRetorna409() throws Exception {
        doThrow(new ProdutoComVendasException()).when(produtoService).excluir(1L);

        mvc.perform(delete("/api/produtos/1").session(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
