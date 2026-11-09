package com.br.adegueto.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Usuário com privilégios gerenciais. A lógica das operações fica nos Services. */
@Entity
@DiscriminatorValue("ADMINISTRADOR")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Administrador extends Usuario {

    public Administrador(String nome, String login, String senhaHash) {
        super(nome, login, senhaHash);
    }

    @Override
    public Perfil getPerfil() {
        return Perfil.ADMINISTRADOR;
    }
}
