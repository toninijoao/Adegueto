package com.br.adegueto.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.br.adegueto.dto.UsuarioRequest;
import com.br.adegueto.exception.CredenciaisInvalidasException;
import com.br.adegueto.exception.GlobalExceptionHandler;
import com.br.adegueto.exception.LoginDuplicadoException;
import com.br.adegueto.model.Administrador;
import com.br.adegueto.model.Funcionario;
import com.br.adegueto.model.Usuario;
import com.br.adegueto.security.AuthInterceptor;
import com.br.adegueto.security.SessaoUsuario;
import com.br.adegueto.service.AuthService;
import com.br.adegueto.service.UsuarioService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Testa a camada web (rotas, sessão, perfis e formato dos erros) sem Spring Boot e sem banco:
 * os services são simulados.
 */
class AuthUsuarioApiTest {

    private final SessaoUsuario sessaoUsuario = new SessaoUsuario();
    private AuthService authService;
    private UsuarioService usuarioService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        authService = mock(AuthService.class);
        usuarioService = mock(UsuarioService.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService, sessaoUsuario), new UsuarioController(usuarioService))
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

    private static final String NOVO_USUARIO = "{\"nome\":\"Cris\",\"login\":\"cris\",\"senha\":\"abc123\",\"perfil\":\"FUNCIONARIO\"}";

    // ---------- login / sessão ----------

    @Test
    void loginValidoRetornaPerfilECriaSessao() throws Exception {
        when(authService.login("ana", "123")).thenReturn(new Administrador("Ana", "ana", "h"));

        var resultado = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"ana\",\"senha\":\"123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.login").value("ana"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andReturn();

        assertThat(resultado.getRequest().getSession(false)).isNotNull();
    }

    @Test
    void loginInvalidoRetorna401ComFormatoPadrao() throws Exception {
        when(authService.login("ana", "errada")).thenThrow(new CredenciaisInvalidasException());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"ana\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("login ou senha inválidos"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void loginComCorpoMalformadoRetorna400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{nao e json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void meSemSessaoRetorna401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meComSessaoRetornaOUsuarioLogado() throws Exception {
        mvc.perform(get("/api/auth/me").session(funcionario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("beto"))
                .andExpect(jsonPath("$.perfil").value("FUNCIONARIO"));
    }

    @Test
    void logoutEncerraASessao() throws Exception {
        MockHttpSession sessao = funcionario();

        mvc.perform(post("/api/auth/logout").session(sessao)).andExpect(status().isNoContent());

        assertThat(sessao.isInvalid()).isTrue();
    }

    @Test
    void logoutSemSessaoRetorna401() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }

    // ---------- usuários: perfis ----------

    @Test
    void cadastroDeUsuarioSemSessaoRetorna401() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(NOVO_USUARIO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void funcionarioNaoPodeCadastrarUsuarios() throws Exception {
        mvc.perform(post("/api/usuarios").session(funcionario())
                        .contentType(MediaType.APPLICATION_JSON).content(NOVO_USUARIO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void funcionarioNaoPodeListarUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios").session(funcionario())).andExpect(status().isForbidden());
    }

    @Test
    void administradorCadastraUsuarioERecebe201SemSenha() throws Exception {
        when(usuarioService.cadastrar(any(UsuarioRequest.class))).thenReturn(new Funcionario("Cris", "cris", "hash"));

        mvc.perform(post("/api/usuarios").session(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(NOVO_USUARIO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.login").value("cris"))
                .andExpect(jsonPath("$.perfil").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void loginDuplicadoRetorna409() throws Exception {
        when(usuarioService.cadastrar(any(UsuarioRequest.class))).thenThrow(new LoginDuplicadoException("cris"));

        mvc.perform(post("/api/usuarios").session(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(NOVO_USUARIO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void perfilInvalidoNoCorpoRetorna400() throws Exception {
        mvc.perform(post("/api/usuarios").session(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"login\":\"x\",\"senha\":\"1\",\"perfil\":\"CHEFE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void administradorListaUsuarios() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of(new Funcionario("Beto", "beto", "h")));

        mvc.perform(get("/api/usuarios").session(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("beto"))
                .andExpect(jsonPath("$[0].senhaHash").doesNotExist());
    }
}
