package br.com.pokesal.infra;

import br.com.pokesal.model.Pokesal;
import br.com.pokesal.model.StatusTorcida;
import br.com.pokesal.service.GeradorAleatorio;
import br.com.pokesal.service.ServicoTorcida;

/**
 * Implementacao padrao de {@link ServicoTorcida} para o Estacionamento da UCSal (RA-F03-01).
 */
public class ServicoTorcidaPadrao implements ServicoTorcida {
  private final GeradorAleatorio geradorAleatorio;

  /**
   * Constroi o servico de torcida utilizando o gerador aleatorio injetado.
   *
   * @param geradorAleatorio fonte de aleatoriedade para aferir vibracao.
   */
  public ServicoTorcidaPadrao(GeradorAleatorio geradorAleatorio) {
    this.geradorAleatorio = geradorAleatorio;
  }

  @Override
  public StatusTorcida aferirVibracao(Pokesal pokesal) {
    if (geradorAleatorio == null) {
      return StatusTorcida.NEUTRA;
    }
    StatusTorcida[] estados = StatusTorcida.values();
    return estados[geradorAleatorio.gerarInteiro(estados.length)];
  }
}
