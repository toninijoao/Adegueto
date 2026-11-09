package com.br.adegueto.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private Usuario usuarioComSenha(String senha) {
        return new Funcionario("João", "joao", encoder.encode(senha));
    }

    @Test
    void credenciaisValidasAutenticam() {
        assertThat(usuarioComSenha("segredo123").autenticar("segredo123", encoder)).isTrue();
    }

    @Test
    void senhaIncorretaERecusada() {
        assertThat(usuarioComSenha("segredo123").autenticar("outra", encoder)).isFalse();
    }

    @Test
    void senhaNulaERecusada() {
        assertThat(usuarioComSenha("segredo123").autenticar(null, encoder)).isFalse();
    }

    @Test
    void senhaNaoFicaEmTextoPuro() {
        Usuario usuario = usuarioComSenha("segredo123");
        assertThat(usuario.getSenhaHash()).isNotEqualTo("segredo123");
    }

    @Test
    void cadaSubclasseInformaSeuPerfil() {
        assertThat(new Administrador("Ana", "ana", "h").getPerfil()).isEqualTo(Perfil.ADMINISTRADOR);
        assertThat(new Funcionario("Beto", "beto", "h").getPerfil()).isEqualTo(Perfil.FUNCIONARIO);
    }
}
