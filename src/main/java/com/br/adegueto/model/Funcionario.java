package com.br.adegueto.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Usuário responsável pelas operações cotidianas. A lógica das operações fica nos Services. */
@Entity
@DiscriminatorValue("FUNCIONARIO")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Funcionario extends Usuario {

    public Funcionario(String nome, String login, String senhaHash) {
        super(nome, login, senhaHash);
    }

    @Override
    public Perfil getPerfil() {
        return Perfil.FUNCIONARIO;
    }
}
