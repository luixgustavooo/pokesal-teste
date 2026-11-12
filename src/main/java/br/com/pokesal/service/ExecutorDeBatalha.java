package br.com.pokesal.service;

import br.com.pokesal.model.Terreno;
import br.com.pokesal.model.Treinador;

/**
 * Contrato para execucao completa de uma batalha entre dois treinadores.
 */
public interface ExecutorDeBatalha {

  /**
   * Inicializa e conduz uma batalha completa ate o seu encerramento.
   *
   * @param treinador1 primeiro competidor.
   * @param treinador2 segundo competidor.
   * @param terreno arena selecionada para o confronto.
   * @return instancia finalizada da batalha contendo vencedor e turnos.
   */
  Batalha executar(Treinador treinador1, Treinador treinador2, Terreno terreno);
}
