package br.com.pokesal.infra;

import br.com.pokesal.service.ProcessadorDreno;

/**
 * Implementacao padrao de {@link ProcessadorDreno} para calculo de cura vampirica (RA-F03-02).
 */
public class ProcessadorDrenoPadrao implements ProcessadorDreno {

  @Override
  public int calcularCura(int danoCausado, double taxaConversao) {
    if (danoCausado <= 0 || taxaConversao <= 0.0) {
      return 0;
    }
    return (int) Math.max(1L, Math.round(danoCausado * taxaConversao));
  }
}
