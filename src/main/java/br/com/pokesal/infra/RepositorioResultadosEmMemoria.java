package br.com.pokesal.infra;

import br.com.pokesal.model.ResultadoBatalha;
import br.com.pokesal.service.RepositorioResultados;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementacao em memoria de {@link RepositorioResultados} para armazenamento de batalhas.
 */
public class RepositorioResultadosEmMemoria implements RepositorioResultados {
  private final List<ResultadoBatalha> resultados = new ArrayList<>();

  @Override
  public void salvar(ResultadoBatalha resultado) {
    if (resultado == null) {
      throw new IllegalArgumentException("O resultado da batalha nao pode ser nulo.");
    }
    this.resultados.add(resultado);
  }

  @Override
  public List<ResultadoBatalha> listarTodos() {
    return Collections.unmodifiableList(resultados);
  }
}
