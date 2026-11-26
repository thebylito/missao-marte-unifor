package br.edu.unifor.missaomarte.servidor.horda;

import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDaHorda;
import java.time.Instant;

public record Horda(long numero, Instant inicio, Instant fim, long semente, String sementeAgenda,
                    DescricaoDaHorda descricao) {}
