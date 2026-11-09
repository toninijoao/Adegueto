package com.br.adegueto.security;

import com.br.adegueto.model.Perfil;
import java.io.Serializable;

/** Dados mínimos do usuário guardados na sessão HTTP. */
public record UsuarioLogado(Long id, String nome, String login, Perfil perfil) implements Serializable {
}
