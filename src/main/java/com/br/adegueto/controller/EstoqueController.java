package com.br.adegueto.controller;

import com.br.adegueto.dto.EstoqueResponse;
import com.br.adegueto.dto.MovimentacaoRequest;
import com.br.adegueto.dto.ResultadoBusca;
import com.br.adegueto.service.EstoqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Acesso liberado a Administrador e Funcionário (decisão D4). */
@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    public static final String MENSAGEM_SEM_PRODUTOS = "produto não encontrado";
    public static final String MENSAGEM_SEM_ESTOQUE_BAIXO = "nenhum produto com estoque baixo";

    private final EstoqueService estoqueService;

    @GetMapping
    public ResultadoBusca<EstoqueResponse> listar() {
        var itens = estoqueService.listar().stream().map(EstoqueResponse::de).toList();
        return ResultadoBusca.de(itens, MENSAGEM_SEM_PRODUTOS);
    }

    @GetMapping("/baixo")
    public ResultadoBusca<EstoqueResponse> listarBaixo() {
        var itens = estoqueService.listarBaixo().stream().map(EstoqueResponse::de).toList();
        return ResultadoBusca.de(itens, MENSAGEM_SEM_ESTOQUE_BAIXO);
    }

    @PostMapping("/{produtoId}/entrada")
    public EstoqueResponse entrada(@PathVariable Long produtoId, @RequestBody MovimentacaoRequest dados) {
        return EstoqueResponse.de(estoqueService.entrada(produtoId, dados.quantidade()));
    }

    @PostMapping("/{produtoId}/saida")
    public EstoqueResponse saida(@PathVariable Long produtoId, @RequestBody MovimentacaoRequest dados) {
        return EstoqueResponse.de(estoqueService.saida(produtoId, dados.quantidade()));
    }
}
