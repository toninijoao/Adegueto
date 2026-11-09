package com.br.adegueto.controller;

import com.br.adegueto.dto.ProdutoRequest;
import com.br.adegueto.dto.ProdutoResponse;
import com.br.adegueto.dto.ResultadoBusca;
import com.br.adegueto.model.CategoriaProduto;
import com.br.adegueto.model.Perfil;
import com.br.adegueto.security.RequerPerfil;
import com.br.adegueto.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequerPerfil(Perfil.ADMINISTRADOR)
    public ProdutoResponse cadastrar(@RequestBody ProdutoRequest dados) {
        return ProdutoResponse.de(produtoService.cadastrar(dados));
    }

    @GetMapping
    public ResultadoBusca<ProdutoResponse> buscar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) CategoriaProduto categoria) {
        var produtos = produtoService.buscar(nome, categoria).stream().map(ProdutoResponse::de).toList();
        return ResultadoBusca.de(produtos, ProdutoService.MENSAGEM_NAO_ENCONTRADO);
    }

    @GetMapping("/{id}")
    public ProdutoResponse obter(@PathVariable Long id) {
        return ProdutoResponse.de(produtoService.obter(id));
    }

    @PutMapping("/{id}")
    @RequerPerfil(Perfil.ADMINISTRADOR)
    public ProdutoResponse atualizar(@PathVariable Long id, @RequestBody ProdutoRequest dados) {
        return ProdutoResponse.de(produtoService.atualizar(id, dados));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequerPerfil(Perfil.ADMINISTRADOR)
    public void excluir(@PathVariable Long id) {
        produtoService.excluir(id);
    }
}
