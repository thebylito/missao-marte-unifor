package br.edu.unifor.missaomarte.teste;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um método de teste. Substituto temporário do @Test do JUnit 5 enquanto o projeto não tem
 * gerenciador de dependências (o JUnit entra na Fase 2, com o Gradle).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Teste {
}
