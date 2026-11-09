package com.br.adegueto.dto;

import com.br.adegueto.model.Perfil;

public record UsuarioRequest(String nome, String login, String senha, Perfil perfil) {
}
