package com.br.adegueto.service;

import com.br.adegueto.exception.CredenciaisInvalidasException;
import com.br.adegueto.model.Usuario;
import com.br.adegueto.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * RN001/RF02: localiza o usuário e valida as credenciais.
     * Login inexistente e senha incorreta geram a mesma exceção, para não revelar quais logins existem.
     */
    @Transactional(readOnly = true)
    public Usuario login(String login, String senha) {
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) {
            throw new CredenciaisInvalidasException();
        }
        Usuario usuario = usuarioRepository.findByLogin(login.trim())
                .orElseThrow(CredenciaisInvalidasException::new);
        if (!usuario.autenticar(senha, passwordEncoder)) {
            throw new CredenciaisInvalidasException();
        }
        return usuario;
    }
}
