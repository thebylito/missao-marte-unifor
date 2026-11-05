package br.edu.unifor.missaomarte.aplicacao;

import java.time.LocalDateTime;

/** Relógio controlado pelo teste. */
final class RelogioFixo implements Relogio {

    long millis;
    LocalDateTime dataHora = LocalDateTime.of(2026, 11, 4, 10, 15, 30);

    @Override
    public long agoraEmMillis() {
        return millis;
    }

    @Override
    public LocalDateTime agora() {
        return dataHora;
    }
}
