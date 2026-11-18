package br.edu.unifor.missaomarte.servidor.jogador;

import br.edu.unifor.missaomarte.servidor.erro.CredenciaisInvalidas;
import br.edu.unifor.missaomarte.servidor.erro.NaoEncontrado;
import br.edu.unifor.missaomarte.servidor.erro.NomeEmUso;
import br.edu.unifor.missaomarte.servidor.seguranca.ServicoDeTokens;
import java.time.Clock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de jogador: cadastrar, entrar (login) e consultar o próprio perfil. */
@Service
public class ServicoDeJogadores {

    private final RepositorioDeJogadores repositorio;
    private final PasswordEncoder senhas;
    private final ServicoDeTokens tokens;
    private final Clock relogio;

    public ServicoDeJogadores(RepositorioDeJogadores repositorio, PasswordEncoder senhas, ServicoDeTokens tokens,
                              Clock relogio) {
        this.repositorio = repositorio;
        this.senhas = senhas;
        this.tokens = tokens;
        this.relogio = relogio;
    }

    @Transactional
    public JogadorResposta cadastrar(CadastroDeJogador pedido) {
        String nome = pedido.nome().trim();
        if (repositorio.existsByNomeIgnoreCase(nome)) {
            throw new NomeEmUso(nome);
        }
        Jogador novo = repositorio.save(new Jogador(nome, senhas.encode(pedido.senha()), relogio.instant()));
        return JogadorResposta.de(novo);
    }

    /** Nome inexistente e senha errada dão a mesma resposta: não revela quais nomes estão cadastrados. */
    @Transactional(readOnly = true)
    public TokenResposta entrar(Credenciais credenciais) {
        Jogador jogador = repositorio.findByNomeIgnoreCase(credenciais.nome().trim())
                .filter(j -> senhas.matches(credenciais.senha(), j.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidas::new);
        Jwt token = tokens.emitir(jogador);
        return new TokenResposta(token.getTokenValue(), "Bearer", token.getExpiresAt(), JogadorResposta.de(jogador));
    }

    @Transactional(readOnly = true)
    public JogadorResposta perfil(long id) {
        return repositorio.findById(id).map(JogadorResposta::de)
                .orElseThrow(() -> new NaoEncontrado("jogador não encontrado"));
    }
}
