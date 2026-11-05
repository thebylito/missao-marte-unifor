package br.edu.unifor.missaomarte.aplicacao;

import java.time.LocalDateTime;

/** Fonte de tempo. Injetada para que os testes não dependam do relógio real. */
public interface Relogio {

    long agoraEmMillis();

    LocalDateTime agora();

    Relogio DO_SISTEMA = new Relogio() {
        @Override
        public long agoraEmMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public LocalDateTime agora() {
            return LocalDateTime.now();
        }
    };
}
