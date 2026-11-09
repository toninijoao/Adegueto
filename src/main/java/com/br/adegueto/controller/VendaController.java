package com.br.adegueto.controller;

import com.br.adegueto.dto.ResultadoBusca;
import com.br.adegueto.dto.VendaRequest;
import com.br.adegueto.dto.VendaResponse;
import com.br.adegueto.security.SessaoUsuario;
import com.br.adegueto.security.UsuarioLogado;
import com.br.adegueto.service.VendaService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administrador e Funcionário podem registrar e consultar vendas. */
@RestController
@RequestMapping("/api/vendas")
@RequiredArgsConstructor
public class VendaController {

    private final VendaService vendaService;
    private final SessaoUsuario sessaoUsuario;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendaResponse registrar(@RequestBody VendaRequest dados, HttpServletRequest request) {
        UsuarioLogado logado = sessaoUsuario.obterObrigatorio(request.getSession(false));
        return VendaResponse.de(vendaService.registrar(logado.id(), dados));
    }

    @GetMapping
    public ResultadoBusca<VendaResponse> historico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        var vendas = vendaService.historico(inicio, fim).stream().map(VendaResponse::de).toList();
        return ResultadoBusca.de(vendas, VendaService.MENSAGEM_NAO_ENCONTRADA);
    }

    @GetMapping("/{id}")
    public VendaResponse obter(@PathVariable Long id) {
        return VendaResponse.de(vendaService.obter(id));
    }
}
