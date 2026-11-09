package com.br.adegueto.controller;

import com.br.adegueto.dto.UsuarioRequest;
import com.br.adegueto.dto.UsuarioResponse;
import com.br.adegueto.model.Perfil;
import com.br.adegueto.security.RequerPerfil;
import com.br.adegueto.service.UsuarioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@RequerPerfil(Perfil.ADMINISTRADOR)
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse cadastrar(@RequestBody UsuarioRequest dados) {
        return UsuarioResponse.de(usuarioService.cadastrar(dados));
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar().stream().map(UsuarioResponse::de).toList();
    }
}
