package br.com.pokesal.service;

import br.com.pokesal.model.Pokesal;
import br.com.pokesal.model.StatusTorcida;

/**
 * Contrato para consulta da vibracao da torcida universitaria da UCSal (RA-F03-01).
 */
public interface ServicoTorcida {

  /**
   * Afere o estado de animacao da torcida em favor do Pokesal em situacao de perigo.
   *
   * @param pokesal criatura que entrou na faixa critica de HP.
   * @return estado atual da torcida (EMPOLGADA, NEUTRA ou DISPERSA).
   */
  StatusTorcida aferirVibracao(Pokesal pokesal);
}
