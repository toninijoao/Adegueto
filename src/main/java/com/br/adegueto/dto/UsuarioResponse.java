package com.br.adegueto.dto;

import com.br.adegueto.model.Perfil;
import com.br.adegueto.model.Usuario;

/** Representação pública de um usuário. Nunca contém senha nem hash. */
public record UsuarioResponse(Long id, String nome, String login, Perfil perfil) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getLogin(), usuario.getPerfil());
    }
}
