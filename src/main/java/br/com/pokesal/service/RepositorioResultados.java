package br.com.pokesal.service;

import br.com.pokesal.model.ResultadoBatalha;
import java.util.List;

/**
 * Contrato para persistencia e consulta dos resultados das batalhas do torneio.
 */
public interface RepositorioResultados {

  /**
   * Persiste o resultado consolidado de uma batalha encerrada.
   *
   * @param resultado dados do resultado da batalha.
   */
  void salvar(ResultadoBatalha resultado);

  /**
   * Lista todos os resultados persistidos ate o momento.
   *
   * @return lista de resultados registrados.
   */
  List<ResultadoBatalha> listarTodos();
}
