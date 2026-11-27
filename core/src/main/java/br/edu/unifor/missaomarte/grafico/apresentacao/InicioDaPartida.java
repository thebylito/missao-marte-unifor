package br.edu.unifor.missaomarte.grafico.apresentacao;

import br.edu.unifor.missaomarte.aplicacao.Modo;
import br.edu.unifor.missaomarte.dominio.Dificuldade;
import br.edu.unifor.missaomarte.grafico.online.Bilhete;
import br.edu.unifor.missaomarte.grafico.online.Falha;
import br.edu.unifor.missaomarte.grafico.online.PartidaParaEnviar;
import br.edu.unifor.missaomarte.grafico.online.RegistroDePartida;
import br.edu.unifor.missaomarte.grafico.online.Resposta;
import br.edu.unifor.missaomarte.grafico.online.ServidorDoJogo;
import br.edu.unifor.missaomarte.grafico.online.Sessao;
import br.edu.unifor.missaomarte.pilotagem.PartidaDePilotagem;
import br.edu.unifor.missaomarte.pilotagem.horda.DescricaoDaHorda;
import br.edu.unifor.missaomarte.pilotagem.horda.MontadorDaHorda;
import java.util.Optional;
import java.util.function.Consumer;

/** Guarda o bilhete e a sessão daquela partida, mesmo se a resposta só chegar depois do fim. */
public final class InicioDaPartida implements Resposta<Bilhete> {
    private final Optional<Sessao> sessao;
    private final ServidorDoJogo servidor;
    private boolean aguardando;
    private Bilhete bilhete;
    private Runnable enviarAoReceber;

    private InicioDaPartida(Optional<Sessao> sessao, ServidorDoJogo servidor) {
        this.sessao = sessao;
        this.servidor = servidor;
    }

    public static InicioDaPartida semBilhete(Optional<Sessao> sessao, ServidorDoJogo servidor) {
        return new InicioDaPartida(sessao, servidor);
    }

    public static void pedirHorda(Sessao piloto, ServidorDoJogo servidor,
                                  DescricaoDaHorda horda,
                                  Resposta<ControladorDaPilotagem> resposta) {
        servidor.iniciarHorda(piloto, horda.dificuldade(), horda.numero(), Resposta.de(bilhete -> {
            if (bilhete == null || bilhete.bilhete() == null || bilhete.bilhete().isBlank()) {
                resposta.falha(new Falha(Falha.Motivo.RESPOSTA_INVALIDA, "O servidor não entregou o bilhete."));
                return;
            }
            PartidaDePilotagem partida;
            try {
                var montagem = MontadorDaHorda.montar(horda);
                partida = new PartidaDePilotagem(piloto.nome(), horda.dificuldade(), montagem.caverna(), montagem.config());
                partida.definirVentoConstante(montagem.ventoConstante());
                partida.definirRoteiro(montagem.roteiro());
            } catch (IllegalArgumentException e) {
                resposta.falha(new Falha(Falha.Motivo.RESPOSTA_INVALIDA, "A descrição da Horda é inválida."));
                return;
            }
            resposta.sucesso(new ControladorDaPilotagem(partida, horda.numero(), bilhete.bilhete(), horda.caverna().id()));
        }, resposta::falha));
    }

    public static InicioDaPartida pedir(Optional<Sessao> sessao, ServidorDoJogo servidor,
                                       Modo modo, Dificuldade dificuldade) {
        var inicio = new InicioDaPartida(sessao, servidor);
        sessao.ifPresent(piloto -> {
            inicio.aguardando = true;
            servidor.iniciarPartida(piloto, modo, dificuldade, inicio);
        });
        return inicio;
    }

    public EnvioDoResultado enviar(PartidaParaEnviar partida, Consumer<RegistroDePartida> aoRegistrar,
                                    Consumer<Falha> aoFalhar) {
        var estado = sessao.isPresent() ? EnvioDoResultado.Estado.ENVIANDO : EnvioDoResultado.Estado.CONVIDADO;
        var envio = new EnvioDoResultado(estado, partida.horda() != null, aoRegistrar, aoFalhar);
        sessao.ifPresent(piloto -> {
            Runnable registrar = () -> servidor.registrarPartidaComMoedas(piloto,
                    bilhete == null ? partida : partida.comBilhete(bilhete.bilhete()), envio);
            if (aguardando) {
                enviarAoReceber = registrar;
            } else {
                registrar.run();
            }
        });
        return envio;
    }

    @Override
    public void sucesso(Bilhete recebido) {
        bilhete = recebido;
        aguardando = false;
        if (enviarAoReceber != null) {
            Runnable registrar = enviarAoReceber;
            enviarAoReceber = null;
            registrar.run();
        }
    }

    @Override
    public void falha(Falha falha) {
        sucesso(null);
    }
}
