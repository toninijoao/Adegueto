package com.br.adegueto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.br.adegueto.dto.UsuarioRequest;
import com.br.adegueto.exception.DadoInvalidoException;
import com.br.adegueto.exception.LoginDuplicadoException;
import com.br.adegueto.model.Administrador;
import com.br.adegueto.model.Funcionario;
import com.br.adegueto.model.Perfil;
import com.br.adegueto.model.Usuario;
import com.br.adegueto.repository.UsuarioRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private UsuarioRepository repository;
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        repository = mock(UsuarioRepository.class);
        service = new UsuarioService(repository, encoder);
        when(repository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void cadastraFuncionarioComSenhaCriptografada() {
        Usuario criado = service.cadastrar(new UsuarioRequest("Beto", "beto", "senha123", Perfil.FUNCIONARIO));

        assertThat(criado).isInstanceOf(Funcionario.class);
        assertThat(criado.getSenhaHash()).isNotEqualTo("senha123");
        assertThat(encoder.matches("senha123", criado.getSenhaHash())).isTrue();
    }

    @Test
    void cadastraAdministradorQuandoPerfilForAdministrador() {
        Usuario criado = service.cadastrar(new UsuarioRequest("Ana", "ana", "senha123", Perfil.ADMINISTRADOR));
        assertThat(criado).isInstanceOf(Administrador.class);
    }

    @Test
    void removeEspacosDeNomeELogin() {
        Usuario criado = service.cadastrar(new UsuarioRequest("  Beto  ", "  beto ", "senha123", Perfil.FUNCIONARIO));
        assertThat(criado.getNome()).isEqualTo("Beto");
        assertThat(criado.getLogin()).isEqualTo("beto");
    }

    @Test
    void loginDuplicadoELancaExcecaoENaoPersiste() {
        when(repository.existsByLogin("beto")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(new UsuarioRequest("Beto", "beto", "senha123", Perfil.FUNCIONARIO)))
                .isInstanceOf(LoginDuplicadoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void camposObrigatoriosAusentesSaoRejeitados() {
        assertThatThrownBy(() -> service.cadastrar(null)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.cadastrar(new UsuarioRequest(" ", "b", "s", Perfil.FUNCIONARIO)))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.cadastrar(new UsuarioRequest("B", null, "s", Perfil.FUNCIONARIO)))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.cadastrar(new UsuarioRequest("B", "b", " ", Perfil.FUNCIONARIO)))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.cadastrar(new UsuarioRequest("B", "b", "s", null)))
                .isInstanceOf(DadoInvalidoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void limitesDeTamanhoSaoValidados() {
        String longo = "x".repeat(101);
        assertThatThrownBy(() -> service.cadastrar(new UsuarioRequest(longo, "b", "s", Perfil.FUNCIONARIO)))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.cadastrar(new UsuarioRequest("B", "x".repeat(51), "s", Perfil.FUNCIONARIO)))
                .isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> service.cadastrar(new UsuarioRequest("B", "b", "x".repeat(73), Perfil.FUNCIONARIO)))
                .isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void listarRetornaOsUsuariosDoRepositorio() {
        Usuario a = new Funcionario("A", "a", "h");
        when(repository.findAll()).thenReturn(List.of(a));
        assertThat(service.listar()).containsExactly(a);
    }
}
