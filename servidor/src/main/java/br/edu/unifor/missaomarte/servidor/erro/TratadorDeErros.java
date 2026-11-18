package br.edu.unifor.missaomarte.servidor.erro;

import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Todos os erros da API no mesmo formato, o "Problem Details" da RFC 9457 ({@code application/problem+json}):
 * {@code type}, {@code title}, {@code status} e {@code detail}, mais {@code campos} nos erros de validação. Erros
 * inesperados viram 500 sem detalhes internos (o detalhe fica só no log do servidor).
 */
@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TratadorDeErros.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException erro,
            HttpHeaders cabecalhos, HttpStatusCode status, WebRequest pedido) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "dados inválidos");
        Map<String, String> campos = new TreeMap<>();
        for (FieldError f : erro.getBindingResult().getFieldErrors()) {
            campos.merge(f.getField(), f.getDefaultMessage(), (a, b) -> a + "; " + b);
        }
        problema.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException erro,
            HttpHeaders cabecalhos, HttpStatusCode status, WebRequest pedido) {
        return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "corpo do pedido ausente ou com JSON inválido"));
    }

    @ExceptionHandler(NomeEmUso.class)
    ProblemDetail nomeEmUso(NomeEmUso erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidas.class)
    ProblemDetail credenciaisInvalidas(CredenciaisInvalidas erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, erro.getMessage());
    }

    @ExceptionHandler(NaoEncontrado.class)
    ProblemDetail naoEncontrado(NaoEncontrado erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception erro) {
        LOG.error("erro inesperado", erro);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "erro interno do servidor");
    }
}
