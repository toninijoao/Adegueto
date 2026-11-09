package com.br.adegueto.security;

import com.br.adegueto.model.Perfil;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Restringe um endpoint (ou controller inteiro) aos perfis informados. */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequerPerfil {

    Perfil[] value();
}
