package com.br.adegueto.security;

import com.br.adegueto.exception.AcessoNegadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Valida autenticação e perfil antes de cada requisição da API.
 * Exceções lançadas aqui são convertidas em JSON pelo GlobalExceptionHandler.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final SessaoUsuario sessaoUsuario;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod metodoPublico && metodoPublico.hasMethodAnnotation(Publico.class)) {
            return true;
        }

        UsuarioLogado logado = sessaoUsuario.obterObrigatorio(request.getSession(false));

        if (handler instanceof HandlerMethod metodo) {
            RequerPerfil exigido = metodo.getMethodAnnotation(RequerPerfil.class);
            if (exigido == null) {
                exigido = AnnotatedElementUtils.findMergedAnnotation(metodo.getBeanType(), RequerPerfil.class);
            }
            if (exigido != null && !Arrays.asList(exigido.value()).contains(logado.perfil())) {
                throw new AcessoNegadoException();
            }
        }
        return true;
    }
}
