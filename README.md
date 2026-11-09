# Adegueto

Sistema de gerenciamento de adega (backend Spring Boot 4.1.2 + frontend HTML/CSS/JS).

## Requisitos
- Java 17
- Maven 3.9+
- PostgreSQL (banco `adegueto`, gerenciado por outro membro da equipe)

## Variáveis de ambiente
| Variável | Descrição | Padrão |
|---|---|---|
| DB_URL | URL JDBC | jdbc:postgresql://localhost:5432/adegueto |
| DB_USER | Usuário do banco | postgres |
| DB_PASSWORD | Senha do banco | (vazio) |
| ADMIN_LOGIN / ADMIN_SENHA / ADMIN_NOME | Administrador inicial | admin / (obrigatória) / Administrador |

## Executar
    mvn spring-boot:run

Frontend servido em http://localhost:8080 (a partir de `src/main/resources/static`).
