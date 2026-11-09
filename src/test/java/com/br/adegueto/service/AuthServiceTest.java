package com.br.adegueto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.br.adegueto.exception.CredenciaisInvalidasException;
import com.br.adegueto.model.Funcionario;
import com.br.adegueto.model.Usuario;
import com.br.adegueto.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private UsuarioRepository repository;
    private AuthService service;

    @BeforeEach
    void preparar() {
        repository = mock(UsuarioRepository.class);
        service = new AuthService(repository, encoder);
        Usuario joao = new Funcionario("João", "joao", encoder.encode("segredo123"));
        when(repository.findByLogin("joao")).thenReturn(Optional.of(joao));
        when(repository.findByLogin("fantasma")).thenReturn(Optional.empty());
    }

    @Test
    void credenciaisValidasRetornamOUsuario() {
        Usuario usuario = service.login("joao", "segredo123");
        assertThat(usuario.getLogin()).isEqualTo("joao");
    }

    @Test
    void loginComEspacosNasPontasEAceito() {
        assertThat(service.login("  joao  ", "segredo123").getLogin()).isEqualTo("joao");
    }

    @Test
    void senhaIncorretaELancaExcecao() {
        assertThatThrownBy(() -> service.login("joao", "errada")).isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void loginInexistenteGeraAMesmaExcecaoDeSenhaIncorreta() {
        assertThatThrownBy(() -> service.login("fantasma", "qualquer"))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("login ou senha inválidos");
    }

    @Test
    void camposVaziosOuNulosSaoRecusados() {
        assertThatThrownBy(() -> service.login(null, "x")).isInstanceOf(CredenciaisInvalidasException.class);
        assertThatThrownBy(() -> service.login("joao", null)).isInstanceOf(CredenciaisInvalidasException.class);
        assertThatThrownBy(() -> service.login(" ", "x")).isInstanceOf(CredenciaisInvalidasException.class);
        assertThatThrownBy(() -> service.login("joao", " ")).isInstanceOf(CredenciaisInvalidasException.class);
    }
}
