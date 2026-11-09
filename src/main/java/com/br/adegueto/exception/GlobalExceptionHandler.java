package com.br.adegueto.exception;

import com.br.adegueto.dto.ErroResponse;
import java.time.LocalDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Converte exceções em respostas JSON padronizadas: {timestamp, status, erro, mensagem}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({CredenciaisInvalidasException.class, NaoAutenticadoException.class})
    public ResponseEntity<ErroResponse> naoAutorizado(AdeguetoException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> proibido(AcessoNegadoException ex) {
        return resposta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({DadoInvalidoException.class, QuantidadeInvalidaException.class})
    public ResponseEntity<ErroResponse> requisicaoInvalida(AdeguetoException ex) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({LoginDuplicadoException.class, EstoqueInsuficienteException.class,
            ProdutoComVendasException.class})
    public ResponseEntity<ErroResponse> conflito(AdeguetoException ex) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({ProdutoInexistenteException.class, VendaInexistenteException.class})
    public ResponseEntity<ErroResponse> naoEncontrado(AdeguetoException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "valor inválido para o parâmetro \"" + ex.getName() + "\"");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "corpo da requisição ausente, malformado ou com valor inválido");
    }

    /** D5: duas operações alteraram o mesmo estoque ao mesmo tempo; a segunda deve ser repetida. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErroResponse> concorrencia(ObjectOptimisticLockingFailureException ex) {
        return resposta(HttpStatus.CONFLICT, "o estoque foi alterado por outra operação; tente novamente");
    }

    /** Rede de segurança para corridas na restrição UNIQUE do banco. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException ex) {
        return resposta(HttpStatus.CONFLICT, "a operação viola a integridade dos dados");
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem) {
        ErroResponse corpo = new ErroResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensagem);
        return ResponseEntity.status(status).body(corpo);
    }
}
