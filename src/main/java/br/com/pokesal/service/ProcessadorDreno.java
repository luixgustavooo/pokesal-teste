package br.com.pokesal.service;

/**
 * Contrato para calculo de absorcao de vitalidade em golpes de dreno (RA-F03-02).
 */
public interface ProcessadorDreno {

  /**
   * Calcula a quantidade de HP recuperada pelo atacante com base no dano causado.
   *
   * @param danoCausado dano efetivamente infligido ao alvo.
   * @param taxaConversao proporcao de conversao do dano em cura (ex: 0.5 para 50%).
   * @return quantidade inteira de pontos de vida a restaurar.
   */
  int calcularCura(int danoCausado, double taxaConversao);
}
