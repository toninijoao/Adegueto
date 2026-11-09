package com.br.adegueto.controller;

import com.br.adegueto.dto.LoginRequest;
import com.br.adegueto.dto.UsuarioResponse;
import com.br.adegueto.model.Usuario;
import com.br.adegueto.security.Publico;
import com.br.adegueto.security.SessaoUsuario;
import com.br.adegueto.security.UsuarioLogado;
import com.br.adegueto.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessaoUsuario sessaoUsuario;

    @Publico
    @PostMapping("/login")
    public UsuarioResponse login(@RequestBody LoginRequest dados, HttpServletRequest request) {
        Usuario usuario = authService.login(dados.login(), dados.senha());
        if (request.getSession(false) != null) {
            request.changeSessionId(); // evita fixação de sessão
        }
        sessaoUsuario.iniciar(request.getSession(true), usuario);
        return UsuarioResponse.de(usuario);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        sessaoUsuario.encerrar(request.getSession(false));
    }

    @GetMapping("/me")
    public UsuarioResponse me(HttpServletRequest request) {
        UsuarioLogado logado = sessaoUsuario.obterObrigatorio(request.getSession(false));
        return new UsuarioResponse(logado.id(), logado.nome(), logado.login(), logado.perfil());
    }
}
