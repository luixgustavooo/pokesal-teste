package br.com.pokesal.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Registro imutavel do resultado de uma batalha oficial para persistencia em repositorio.
 */
public class ResultadoBatalha {
  private final Treinador vencedor;
  private final Treinador perdedor;
  private final int numeroTurnos;
  private final Pokesal pokesalTreinador1;
  private final Pokesal pokesalTreinador2;
  private final boolean empate;

  /**
   * Constroi o registro consolidado do desfecho de uma batalha.
   *
   * @param vencedor treinador vencedor ou null em caso de empate.
   * @param perdedor treinador perdedor ou null em caso de empate.
   * @param numeroTurnos quantidade total de turnos executados na batalha.
   * @param pokesalTreinador1 Pokesal utilizado pelo primeiro treinador.
   * @param pokesalTreinador2 Pokesal utilizado pelo segundo treinador.
   */
  public ResultadoBatalha(Treinador vencedor,
                          Treinador perdedor,
                          int numeroTurnos,
                          Pokesal pokesalTreinador1,
                          Pokesal pokesalTreinador2) {
    this.vencedor = vencedor;
    this.perdedor = perdedor;
    this.numeroTurnos = numeroTurnos;
    this.pokesalTreinador1 = pokesalTreinador1;
    this.pokesalTreinador2 = pokesalTreinador2;
    this.empate = (vencedor == null);
  }

  /**
   * Retorna o treinador vencedor da batalha, ou null em caso de empate.
   *
   * @return treinador vencedor ou null.
   */
  public Treinador getVencedor() {
    return vencedor;
  }

  /**
   * Retorna o treinador perdedor da batalha, ou null em caso de empate.
   *
   * @return treinador perdedor ou null.
   */
  public Treinador getPerdedor() {
    return perdedor;
  }

  /**
   * Retorna o numero total de turnos executados na batalha.
   *
   * @return total de turnos.
   */
  public int getNumeroTurnos() {
    return numeroTurnos;
  }

  /**
   * Retorna o Pokesal utilizado pelo primeiro competidor.
   *
   * @return Pokesal do treinador 1.
   */
  public Pokesal getPokesalTreinador1() {
    return pokesalTreinador1;
  }

  /**
   * Retorna o Pokesal utilizado pelo segundo competidor.
   *
   * @return Pokesal do treinador 2.
   */
  public Pokesal getPokesalTreinador2() {
    return pokesalTreinador2;
  }

  /**
   * Retorna a lista imutavel dos Pokesal participantes da batalha.
   *
   * @return lista dos dois Pokesal utilizados.
   */
  public List<Pokesal> getPokesaisUtilizados() {
    return Collections.unmodifiableList(Arrays.asList(pokesalTreinador1, pokesalTreinador2));
  }

  /**
   * Indica se a batalha terminou em empate tecnico.
   *
   * @return true se empate, false caso haja vencedor definido.
   */
  public boolean isEmpate() {
    return empate;
  }
}
