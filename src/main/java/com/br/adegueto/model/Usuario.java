package com.br.adegueto.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Superclasse dos usuários do sistema. Mapeamento: SINGLE_TABLE. */
@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_usuario", discriminatorType = DiscriminatorType.STRING)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, length = 100)
    private String nome;

    @Setter
    @Column(nullable = false, unique = true, length = 50)
    private String login;

    @Setter
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    protected Usuario(String nome, String login, String senhaHash) {
        this.nome = nome;
        this.login = login;
        this.senhaHash = senhaHash;
    }

    /** Perfil de acesso do usuário (definido pela subclasse). */
    public abstract Perfil getPerfil();

    /** RN001: compara a senha informada com o hash armazenado. */
    public boolean autenticar(String senhaInformada, PasswordEncoder encoder) {
        if (senhaInformada == null || encoder == null || senhaHash == null) {
            return false;
        }
        return encoder.matches(senhaInformada, senhaHash);
    }
}
