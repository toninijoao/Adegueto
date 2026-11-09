package com.br.adegueto.config;

import com.br.adegueto.dto.UsuarioRequest;
import com.br.adegueto.model.Perfil;
import com.br.adegueto.repository.UsuarioRepository;
import com.br.adegueto.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Cria o administrador inicial (decisão D7), caso ainda não exista. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    @Value("${adegueto.admin.nome}")
    private String nome;

    @Value("${adegueto.admin.login}")
    private String login;

    @Value("${adegueto.admin.senha}")
    private String senha;

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByLogin(login)) {
            return;
        }
        if (senha == null || senha.isBlank()) {
            throw new IllegalStateException(
                    "Nenhum administrador inicial existe. Defina a variável de ambiente ADMIN_SENHA para criá-lo.");
        }
        usuarioService.cadastrar(new UsuarioRequest(nome, login, senha, Perfil.ADMINISTRADOR));
        log.info("Administrador inicial \"{}\" criado.", login);
    }
}
