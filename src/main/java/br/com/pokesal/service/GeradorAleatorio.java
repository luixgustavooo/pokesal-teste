package br.com.pokesal.service;

/**
 * Contrato para geracao de valores pseudoaleatorios desacoplada das regras de negocio.
 */
public interface GeradorAleatorio {

  /**
   * Gera um numero decimal pseudoaleatorio no intervalo [0.0, 1.0).
   *
   * @return valor decimal entre 0.0 (inclusivo) e 1.0 (exclusivo).
   */
  double gerarDouble();

  /**
   * Gera um numero inteiro pseudoaleatorio entre 0 (inclusivo) e o limite (exclusivo).
   *
   * @param limite valor maximo exclusivo (deve ser positivo).
   * @return inteiro sorteado no intervalo [0, limite).
   */
  int gerarInteiro(int limite);

  /**
   * Gera um valor booleano pseudoaleatorio.
   *
   * @return true ou false.
   */
  boolean gerarBooleano();
}
