package com.br.adegueto.security;

import com.br.adegueto.exception.NaoAutenticadoException;
import com.br.adegueto.model.Usuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/** Encapsula o acesso ao usuário autenticado guardado na HttpSession. */
@Component
public class SessaoUsuario {

    static final String ATRIBUTO = "usuarioLogado";

    public void iniciar(HttpSession sessao, Usuario usuario) {
        sessao.setAttribute(ATRIBUTO,
                new UsuarioLogado(usuario.getId(), usuario.getNome(), usuario.getLogin(), usuario.getPerfil()));
    }

    public void encerrar(HttpSession sessao) {
        if (sessao != null) {
            sessao.invalidate();
        }
    }

    /** Retorna o usuário logado ou lança NaoAutenticadoException. */
    public UsuarioLogado obterObrigatorio(HttpSession sessao) {
        if (sessao != null && sessao.getAttribute(ATRIBUTO) instanceof UsuarioLogado logado) {
            return logado;
        }
        throw new NaoAutenticadoException();
    }
}
