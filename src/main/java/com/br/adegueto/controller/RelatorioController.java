package com.br.adegueto.controller;

import com.br.adegueto.dto.ProdutoMaisVendidoResponse;
import com.br.adegueto.dto.RelatorioEstoqueResponse;
import com.br.adegueto.dto.RelatorioVendasResponse;
import com.br.adegueto.dto.ResultadoBusca;
import com.br.adegueto.model.Perfil;
import com.br.adegueto.security.RequerPerfil;
import com.br.adegueto.service.RelatorioService;
import com.br.adegueto.service.VendaService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Relatórios: somente Administrador. */
@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
@RequerPerfil(Perfil.ADMINISTRADOR)
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/vendas")
    public RelatorioVendasResponse vendas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorioService.resumoVendas(inicio, fim);
    }

    @GetMapping("/produtos-mais-vendidos")
    public ResultadoBusca<ProdutoMaisVendidoResponse> produtosMaisVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Integer limite) {
        var ranking = relatorioService.produtosMaisVendidos(inicio, fim, limite);
        return ResultadoBusca.de(ranking, VendaService.MENSAGEM_NAO_ENCONTRADA);
    }

    @GetMapping("/estoque")
    public RelatorioEstoqueResponse estoque() {
        return relatorioService.posicaoEstoque();
    }
}
