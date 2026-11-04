package br.edu.unifor.missaomarte.aplicacao;

import br.edu.unifor.missaomarte.dominio.RegistroRanking;
import java.io.IOException;
import java.util.List;

/**
 * Onde o ranking é guardado (padrão Repository). A aplicação depende desta interface, não do arquivo JSON
 * (inversão de dependência): a Fase 5 poderá trocar o arquivo por um servidor sem mexer nas regras.
 */
public interface RepositorioRanking {

    /** Registros ordenados do maior para o menor. Se não houver dados ou não for possível ler, lista vazia. */
    List<RegistroRanking> carregar();

    void salvar(List<RegistroRanking> registros) throws IOException;

    void apagar() throws IOException;
}
