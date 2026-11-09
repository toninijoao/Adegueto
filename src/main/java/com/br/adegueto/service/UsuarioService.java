package com.br.adegueto.service;

import com.br.adegueto.dto.UsuarioRequest;
import com.br.adegueto.exception.DadoInvalidoException;
import com.br.adegueto.exception.LoginDuplicadoException;
import com.br.adegueto.model.Administrador;
import com.br.adegueto.model.Funcionario;
import com.br.adegueto.model.Usuario;
import com.br.adegueto.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final int NOME_MAX = 100;
    private static final int LOGIN_MAX = 50;
    /** Limite do BCrypt: bytes além do 72º seriam ignorados. */
    private static final int SENHA_MAX_BYTES = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /** RF01: valida, verifica login duplicado, criptografa a senha e persiste. */
    @Transactional
    public Usuario cadastrar(UsuarioRequest dados) {
        if (dados == null) {
            throw new DadoInvalidoException("os dados do usuário são obrigatórios");
        }
        String nome = textoObrigatorio(dados.nome(), "nome", NOME_MAX);
        String login = textoObrigatorio(dados.login(), "login", LOGIN_MAX);
        validarSenha(dados.senha());
        if (dados.perfil() == null) {
            throw new DadoInvalidoException("o perfil é obrigatório");
        }
        if (usuarioRepository.existsByLogin(login)) {
            throw new LoginDuplicadoException(login);
        }

        String hash = passwordEncoder.encode(dados.senha());
        Usuario usuario = switch (dados.perfil()) {
            case ADMINISTRADOR -> new Administrador(nome, login, hash);
            case FUNCIONARIO -> new Funcionario(nome, login, hash);
        };
        return usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    private static String textoObrigatorio(String valor, String campo, int max) {
        if (valor == null || valor.isBlank()) {
            throw new DadoInvalidoException("o campo " + campo + " é obrigatório");
        }
        String limpo = valor.trim();
        if (limpo.length() > max) {
            throw new DadoInvalidoException("o campo " + campo + " deve ter no máximo " + max + " caracteres");
        }
        return limpo;
    }

    private static void validarSenha(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new DadoInvalidoException("o campo senha é obrigatório");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_MAX_BYTES) {
            throw new DadoInvalidoException("a senha deve ter no máximo " + SENHA_MAX_BYTES + " bytes");
        }
    }
}
