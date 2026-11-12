package br.com.pokesal.service;

import br.com.pokesal.model.Terreno;
import br.com.pokesal.model.TipoElemental;
import java.time.LocalDateTime;

/**
 * Contrato para consulta dinamica das condicoes de terreno considerando clima e horario.
 */
public interface ServicoTerreno {

  /**
   * Obtem o terreno ativo para a batalha de acordo com a data e horario informados.
   *
   * @param dataHora instante atual de referencia.
   * @return instancia de terreno aplicavel ao confronto.
   */
  Terreno obterTerrenoAtual(LocalDateTime dataHora);

  /**
   * Calcula o modificador de dano concedido pelo terreno ao tipo de golpe no horario informado.
   *
   * @param terreno arena do confronto.
   * @param tipoGolpe tipo elemental do golpe desferido.
   * @param dataHora instante atual de referencia.
   * @return multiplicador de dano aplicavel (1.0 se neutro ou inativo).
   */
  double obterModificadorDano(Terreno terreno, TipoElemental tipoGolpe, LocalDateTime dataHora);

  /**
   * Verifica se o efeito do terreno informado esta ativo no horario especificado.
   *
   * @param terreno arena avaliada.
   * @param dataHora instante atual de referencia.
   * @return true se o efeito ambiental estiver ativo, false caso contrario.
   */
  boolean isEfeitoAtivo(Terreno terreno, LocalDateTime dataHora);
}
