# Adegueto — Software Design Specification (SDD)

**Versão:** 2.0
**Tipo:** Aplicação web (backend REST + frontend)
**Backend:** Java 17, Spring Boot 4.1.2, Maven, Spring Web, Spring Data JPA (Hibernate), PostgreSQL, Lombok, DevTools
**Frontend:** HTML5, CSS3 e JavaScript puro (sem frameworks)
**Arquitetura:** MVC em camadas (Controller, Service, Repository, Model)

## Histórico de alterações

| Versão | Alteração |
|---|---|
| 1.0 | Versão inicial: somente backend, sem HTTP. |
| 2.0 | Escopo ampliado: API REST e frontend. Decisões técnicas D1–D9 aprovadas. Revogada a diretriz de não implementar frontend/REST. Entidades com `Long` e `BigDecimal`. |
| 2.1 | Pacote base `com.br.adegueto`; exceções de domínio (`AdeguetoException` e derivadas); enums `Perfil` e `CategoriaProduto`; Etapa 1 implementada. |
| 2.2 | Etapa 2: autenticação por sessão (`@Publico`, `@RequerPerfil`, `AuthInterceptor`), `/api/auth` e `/api/usuarios`, `GlobalExceptionHandler`, admin inicial. Cookie de sessão com `SameSite=Lax`. Login inexistente e senha incorreta retornam o mesmo erro 401. Senha limitada a 72 bytes (limite do BCrypt). |
| 2.3 | Etapa 3 (produtos): regras de cadastro, consulta, alteração e exclusão (seção 7.2). |
| 2.4 | Etapa 4 (estoque): movimentação manual, estoque baixo e conflito de concorrência (seção 7.3). |
| 2.5 | Etapa 5 (vendas): registro transacional, baixa de estoque e histórico por período (seção 7.4). |
| 2.6 | Etapa 6 (relatórios): resumo de vendas, produtos mais vendidos e posição de estoque (seção 7.5). Backend concluído. |

> Os requisitos funcionais (RF01–RF11) e as regras de negócio (RN001–RN005) da v1.0 permanecem válidos, salvo onde indicado abaixo.

---

# 1. Objetivo

Servir como fonte central de referência para o desenvolvimento do Adegueto, garantindo requisitos atendidos, regras de negócio respeitadas, arquitetura em camadas preservada e separação clara entre frontend e backend. Em caso de informação ambígua ou contraditória, não inventar comportamento: apresentar o problema e aguardar aprovação.

# 2. Visão geral

**Nome:** Adegueto — sistema de gerenciamento operacional de uma adega comercial (projeto acadêmico).
**Problemas atendidos:** falta de controle automatizado de estoque, registro manual de vendas, dificuldade de consulta, ausência de alerta de estoque baixo, ausência de controle de acesso por perfil, dificuldade de consultar histórico e gerar relatórios.
**Público-alvo:** Administradores (gestão) e Funcionários (operação diária).
**Escopo (v2.0):** backend e frontend. O frontend consome exclusivamente a API REST.

# 3. Stack tecnológica

| Tecnologia | Utilização |
|---|---|
| Java 17 | Linguagem |
| Spring Boot 4.1.2 | Framework principal |
| Spring Web | API REST |
| Spring Data JPA + Hibernate | Persistência (aprovado) |
| PostgreSQL + driver | Banco relacional (administrado por outro membro da equipe) |
| Lombok | Redução de código repetitivo |
| DevTools | Produtividade em desenvolvimento |
| spring-security-crypto | Somente BCrypt para senhas (aprovado, D2) |
| Maven | Build |
| HTML5 / CSS3 / JavaScript (ES modules) | Frontend |

Não introduzir tecnologias adicionais sem justificativa e aprovação.

# 4. Arquitetura

```
Navegador (HTML/CSS/JS)  --JSON/HTTP-->  Controller (@RestController)
                                              |
                                          Service (regras, @Transactional)
                                              |
                                          Repository (Spring Data JPA)
                                              |
                                          PostgreSQL
```

- **Model:** entidades de domínio e comportamentos intrínsecos (herança, polimorfismo, encapsulamento).
- **Controller:** `@RestController`. Recebe HTTP, converte DTOs, define status codes. **Sem regra de negócio.**
- **Service:** validações, regras de negócio, transações, exceções de domínio.
- **Repository:** persistência. Sem regra de negócio.
- **DTO:** fronteira da API. Entidades JPA nunca são expostas (a senha não pode vazar).
- **Tratamento de erros:** `@RestControllerAdvice` converte exceções em JSON padronizado.
- **Frontend:** servido pelo Spring Boot em `src/main/resources/static` (evita CORS). Código e responsabilidades permanecem separados do backend; a comunicação ocorre somente via API.
- **Regra de ouro:** o frontend não duplica regras de negócio. Valida apenas o básico (campo vazio) por usabilidade; ocultar itens de menu por perfil é apenas visual, e a autorização real é do backend.

# 5. Modelagem de domínio

## 5.1 Usuario (abstract, `SINGLE_TABLE`)
`id: Long`, `nome`, `login` (único), `senhaHash`, `perfil`.
`autenticar(String senhaInformada, PasswordEncoder encoder): boolean` compara a senha com o hash.
Subclasses: **Administrador** e **Funcionario**. Os métodos conceituais do diagrama original (`gerarRelatorio`, `gerenciarProdutos`, `registrarVenda`, `consultarProdutos`) **não contêm lógica**: representam os perfis, e a lógica reside nos Services. O que diferencia os perfis é a autorização.

## 5.2 Produto (abstract, `JOINED`)
`id: Long`, `nome`, `preco: BigDecimal`, `ativo: boolean`; associação 1:1 com Estoque.
Especializações: **Vinho** (tipo, origem, uva, safra), **Whisky** (tipo, origem, idade), **Gin** (estilo, origem), **Vodka** (origem).

## 5.3 Estoque
`id: Long`, `quantidade`, `estoqueMin`, `versao` (`@Version`, controle otimista).
- `adicionar(int)`: exige quantidade positiva.
- `remover(int)`: exige quantidade positiva e não superior ao saldo.
- `checaMin()`: `quantidade <= estoqueMin` (RN002).
Nunca permite saldo negativo.

## 5.4 Venda
`id: Long`, `data: LocalDateTime`, `valorTotal: BigDecimal`; associada ao **Usuario** que a registrou (Administrador ou Funcionário) e agregada a **ItemVenda** (mínimo 1).
- `adicionarItem(ItemVenda)`, `calcularTotal()` (soma dos subtotais), `finalizar()` (exige ao menos 1 item).

## 5.5 ItemVenda
`quantidade`, `precoUnitario: BigDecimal` (preço no momento da venda, preservando o histórico), associado a um Produto.
- `calcularSubtotal()` = `quantidade × precoUnitario` (RN004).

## 5.6 Persistência
IDs `Long` com `IDENTITY`. Valores monetários em `NUMERIC(12,2)` e `BigDecimal` com arredondamento `HALF_UP`. Esquema gerado por `ddl-auto=update` em desenvolvimento (sem Flyway).

# 6. Regras de negócio

- **RN001 — Autenticação:** credenciais válidas concedem acesso; inválidas são recusadas.
- **RN002 — Estoque mínimo:** `checaMin()` retorna `true` quando `quantidade <= estoqueMin`.
- **RN003 — Atualização de estoque:** toda entrada usa `adicionar()` e toda saída/venda usa `remover()`, de forma consistente com a operação de origem.
- **RN004 — Cálculo de vendas:** subtotal = quantidade × precoUnitario; total = soma dos subtotais.
- **RN005 — Buscas vazias:** resposta com lista vazia e mensagem "produto não encontrado" (produtos) ou "nenhuma venda encontrada" (histórico).

# 7. Requisitos funcionais

Mantidos RF01–RF11 da v1.0 (cadastro de usuários, login, cadastro/consulta/alteração/exclusão de produtos, controle de estoque, baixa pós-venda, registro de vendas, histórico, estoque baixo e relatórios), agora expostos pela API da seção 8 e consumidos pelas telas da seção 9.

## 7.1 Decisões que complementam os requisitos
- **Exclusão de produto (RF05):** produto com vendas associadas não pode ser excluído (HTTP 409). Produto nunca vendido pode ser excluído fisicamente.
- **Movimentação manual de estoque (RF06):** entrada e saída permitidas a ambos os perfis.
- **Concorrência (RF07/RF08):** bloqueio otimista em Estoque; em conflito, HTTP 409. Uma venda é transacional: se um item falhar, nada é gravado.
- **Relatórios (RF11), escopo mínimo:** (a) resumo de vendas por período (quantidade de vendas, faturamento, ticket médio); (b) produtos mais vendidos; (c) posição de estoque com destaque dos itens abaixo do mínimo. Exibição em tela, sem exportação.
- **Administrador inicial:** criado na inicialização a partir das propriedades `adegueto.admin.*` (variáveis de ambiente), se não existir.

## 7.2 Regras de produtos (Etapa 3)
- **Cadastro (RF03):** categoria, nome, preço e quantidade inicial são obrigatórios; a quantidade inicial pode ser 0. O estoque mínimo é opcional e, se omitido, assume **0**.
- **Atributos específicos obrigatórios:** Vinho (tipo, origem, uva, safra > 0); Whisky (tipo, origem, idade > 0); Gin (estilo, origem); Vodka (origem).
- **Consulta (RF04):** `GET /api/produtos?nome=&categoria=`; nome com busca parcial sem distinguir maiúsculas. Retorna `{ itens, mensagem }`; com `itens` vazio, `mensagem` = "produto não encontrado" (RN005).
- **Alteração (RF05):** altera nome, preço, atributos específicos e estoque mínimo. **A categoria não pode ser alterada** e a quantidade em estoque só muda por movimentações (RF06).
- **Exclusão (RF05/D3):** recusada com HTTP 409 se o produto aparece em alguma venda; caso contrário remove o produto e seu estoque.
- Produto inexistente: HTTP 404 ("produto não encontrado").

## 7.3 Regras de estoque (Etapa 4)
- `GET /api/estoque`: posição de todos os produtos (ordem alfabética). `GET /api/estoque/baixo` (RF10/RN002): produtos com `quantidade <= estoqueMin`. Ambos retornam `{ itens, mensagem }`; sem itens, `mensagem` = "produto não encontrado" (estoque) ou "nenhum produto com estoque baixo".
- `POST /api/estoque/{produtoId}/entrada` e `/saida` (RF06/RN003) com corpo `{ "quantidade": n }`, `n > 0`. Disponíveis a Administrador e Funcionário (D4).
- Saída maior que o saldo: HTTP 409 e o saldo não muda. Quantidade ausente, zero ou negativa: HTTP 400. Produto sem estoque/inexistente: HTTP 404.
- Concorrência (D5): conflito de versão em `Estoque` retorna HTTP 409 ("tente novamente").
- Uma baixa de estoque feita por venda (RF07) usa o mesmo `Estoque.remover()` (Etapa 5).

## 7.4 Regras de vendas (Etapa 5)
- `POST /api/vendas` com `{ "itens": [ { "produtoId", "quantidade" } ] }`. O vendedor é o usuário da sessão (Administrador ou Funcionário). Responde 201 com a venda, itens, subtotais e total calculados **pelo backend**.
- **Transação única (RF07/RF08):** verifica o estoque, cria os itens com o preço do momento, calcula subtotais e total (RN004), finaliza a venda, baixa o estoque (RN003) e grava tudo. Se qualquer item falhar, **nada é gravado**.
- Produto repetido na mesma venda tem as quantidades somadas em um único item. Cada linha precisa ter quantidade > 0.
- Estoque insuficiente: HTTP 409 com o nome do produto. Produto inexistente: 404. Venda sem itens ou quantidade inválida: 400. Vendas simultâneas no mesmo estoque: a segunda recebe 409 ("tente novamente") pelo bloqueio otimista (D5).
- **Histórico (RF09):** `GET /api/vendas?inicio=AAAA-MM-DD&fim=AAAA-MM-DD`; datas inclusivas e opcionais; mais recentes primeiro; data inicial posterior à final é 400. Sem resultados: `{ itens: [], mensagem: "nenhuma venda encontrada" }` (RN005). `GET /api/vendas/{id}`: detalhe (404 se inexistente).

## 7.5 Relatórios (Etapa 6, RF11)
Somente Administrador. Os dados são agregados em memória a partir das consultas de vendas e estoque.
- `GET /api/relatorios/vendas?inicio=&fim=`: `{ inicio, fim, quantidadeVendas, faturamento, ticketMedio, mensagem }`. Ticket médio = faturamento / quantidade de vendas, arredondado a 2 casas. Sem vendas: valores zero e `mensagem` = "nenhuma venda encontrada" (RN005).
- `GET /api/relatorios/produtos-mais-vendidos?inicio=&fim=&limite=`: ranking por quantidade vendida (empate por nome), com quantidade e receita por produto. `inicio`/`fim` opcionais; `limite` padrão 10 (deve ser > 0). Retorna `{ itens, mensagem }`.
- `GET /api/relatorios/estoque`: `{ totalProdutos, produtosComEstoqueBaixo, itens }`, onde cada item traz `estoqueBaixo` (RN002).
- Sem exportação; apresentação em tela (D6).

# 8. Contrato da API REST (prefixo `/api`)

| Método | Rota | Requisito | Acesso |
|---|---|---|---|
| POST | `/auth/login` | RF02 | Público |
| POST | `/auth/logout` | RF02 | Autenticado |
| GET | `/auth/me` | RF02 | Autenticado |
| POST | `/usuarios` | RF01 | Admin |
| GET | `/usuarios` | RF01 | Admin |
| POST | `/produtos` | RF03 | Admin |
| GET | `/produtos?nome=&categoria=` | RF04 | Ambos |
| GET | `/produtos/{id}` | RF04 | Ambos |
| PUT | `/produtos/{id}` | RF05 | Admin |
| DELETE | `/produtos/{id}` | RF05 | Admin |
| GET | `/estoque` | RF06 | Ambos |
| POST | `/estoque/{produtoId}/entrada` | RF06 | Ambos |
| POST | `/estoque/{produtoId}/saida` | RF06 | Ambos |
| GET | `/estoque/baixo` | RF10 | Ambos |
| POST | `/vendas` | RF07, RF08 | Ambos |
| GET | `/vendas?inicio=&fim=` | RF09 | Ambos |
| GET | `/vendas/{id}` | RF09 | Ambos |
| GET | `/relatorios/vendas?inicio=&fim=` | RF11 | Admin |
| GET | `/relatorios/produtos-mais-vendidos?inicio=&fim=&limite=` | RF11 | Admin |
| GET | `/relatorios/estoque` | RF11 | Admin |

**Convenções:** JSON; datas em ISO-8601; busca vazia retorna `200` com lista vazia e campo `mensagem` (RN005).
**Erros:** `{ "timestamp", "status", "erro", "mensagem" }` com os códigos 400 (validação), 401 (não autenticado), 403 (sem permissão), 404 (inexistente) e 409 (conflito, como estoque insuficiente ou exclusão incompatível).
**Autenticação:** sessão HTTP (cookie `JSESSIONID`) com interceptor que valida login e perfil (`@RequerPerfil`). Senhas armazenadas com BCrypt.

# 9. Frontend

- JavaScript puro (ES modules), SPA leve com roteamento por hash, cliente HTTP único (`api.js`).
- Identidade visual: tema escuro em tons de vinho/bordô e dourado, títulos em fonte serifada, coerente com uma adega.
- Telas: Login, Dashboard (alertas de estoque baixo), Produtos, Estoque, Nova venda (carrinho e total), Histórico de vendas, Usuários (admin), Relatórios (admin).
- O total exibido no carrinho é informativo; o valor oficial é o calculado pelo backend.

# 10. Estrutura de diretórios

```
adegueto/
├── pom.xml
├── docs/SDD-Adegueto-v2.0.md
├── src/main/java/com/br/adegueto/
│   ├── AdeguetoApplication.java   (pacote base: com.br.adegueto)
│   ├── config/       security/     model/     repository/
│   ├── service/      controller/   dto/       exception/
├── src/main/resources/
│   ├── application.properties
│   └── static/       (index.html, app.html, css/, js/, js/pages/)
└── src/test/java/com/br/adegueto/
```

# 11. Validações e exceções

Validações prioritariamente no Service: campos obrigatórios, login único, preço e quantidades positivos, estoque mínimo não negativo, dados específicos de cada categoria. Exceções de negócio específicas (sem exceções genéricas): credenciais inválidas, usuário/produto inexistente, estoque insuficiente, quantidade inválida, exclusão incompatível, acesso negado.

# 12. Segurança

Perfis Administrador e Funcionario. Somente administradores cadastram usuários, cadastram/alteram/excluem produtos e acessam relatórios. Ambos os perfis consultam produtos, histórico e estoque baixo, movimentam estoque e registram vendas. Senhas nunca em texto puro; a senha do administrador inicial vem de variável de ambiente.

# 13. Qualidade, testes e entrega

- Orientação a objetos, baixo acoplamento, alta coesão, código simples e didático.
- Testes unitários: autenticação, estoque mínimo, adicionar/remover, subtotal, total, estoque insuficiente, registro de vendas, consultas vazias. Testes de integração para serviços + persistência.
- Cada etapa encerra com relatório: o que foi feito, classes, requisitos atendidos, testes e pendências. Nenhuma funcionalidade é dada como concluída sem verificação.

# 14. Decisões (todas aprovadas)

| Id | Decisão |
|---|---|
| D1 | Sessão HTTP com interceptor, sem Spring Security completo |
| D2 | BCrypt via spring-security-crypto |
| D3 | Bloquear exclusão de produto com vendas (409) |
| D4 | Entrada/saída manual para ambos os perfis |
| D5 | `@Version` em Estoque; conflito retorna 409 |
| D6 | Relatórios mínimos (vendas por período, mais vendidos, posição de estoque) |
| D7 | Administrador inicial via `CommandLineRunner` e variáveis de ambiente |
| D8 | `ddl-auto=update`, sem Flyway |
| D9 | Frontend em JS puro, SPA com hash routing, tema vinho/dourado |

# 15. Planejamento

| Etapa | Entrega |
|---|---|
| 0 | `pom.xml`, `application.properties`, pacotes, SDD v2.0 |
| 1 | Model + enums + testes unitários |
| 2 | Repositories, exceções, handler global, autenticação, sessão, `/auth` e `/usuarios`, seed do admin |
| 3 | Produtos |
| 4 | Estoque |
| 5 | Vendas e histórico |
| 6 | Relatórios |
| 7 | Frontend base (layout, tema, login, router, `api.js`) |
| 8 | Telas de Produtos, Estoque e Usuários |
| 9 | PDV, Histórico, Relatórios, Dashboard |
| 10 | Revisão final e README |

# 16. Diretrizes obrigatórias

1. Este documento é a referência central; mantê-lo atualizado a cada mudança aprovada.
2. Não introduzir funcionalidades sem respaldo nos requisitos nem alterar regras sem autorização.
3. Não concentrar regras em controllers, nem em repositories; não duplicá-las no frontend.
4. Não tomar decisões técnicas pendentes sem aprovação prévia.
5. Priorizar soluções simples, didáticas e compatíveis com o escopo acadêmico.
