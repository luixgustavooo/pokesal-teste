package br.com.pokesal.infra;

import br.com.pokesal.model.AsfaltoQuente;
import br.com.pokesal.model.PocaDeChuva;
import br.com.pokesal.model.Terreno;
import br.com.pokesal.model.TipoElemental;
import br.com.pokesal.service.EfeitoDeStatusTerreno;
import br.com.pokesal.service.GeradorAleatorio;
import br.com.pokesal.service.ServicoTerreno;
import br.com.pokesal.service.SimuladorDeBatalha;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementacao de {@link ServicoTerreno} que avalia condicoes de horario e clima para os terrenos.
 */
public class ServicoTerrenoPadrao implements ServicoTerreno {
  /** Hora inicial do periodo diurno (06:00 inclusivo). */
  public static final int HORA_INICIO_DIA = 6;

  /** Hora final do periodo diurno (18:00 exclusivo). */
  public static final int HORA_FIM_DIA = 18;

  private final GeradorAleatorio geradorAleatorio;
  private final boolean climaChuvoso;

  /**
   * Construtor padrao do servico de terreno com clima parametrizado.
   *
   * @param geradorAleatorio gerador utilizado para sorteio de arenas.
   * @param climaChuvoso indica se o clima atual no estacionamento esta chuvoso.
   */
  public ServicoTerrenoPadrao(GeradorAleatorio geradorAleatorio, boolean climaChuvoso) {
    this.geradorAleatorio = geradorAleatorio;
    this.climaChuvoso = climaChuvoso;
  }

  /**
   * Construtor simplificado considerando clima seco por padrao.
   *
   * @param geradorAleatorio gerador utilizado para sorteio de arenas.
   */
  public ServicoTerrenoPadrao(GeradorAleatorio geradorAleatorio) {
    this(geradorAleatorio, false);
  }

  @Override
  public Terreno obterTerrenoAtual(LocalDateTime dataHora) {
    if (climaChuvoso) {
      return new PocaDeChuva();
    }
    List<Terreno> catalogo = SimuladorDeBatalha.obterCatalogoTerrenos();
    if (geradorAleatorio != null) {
      return catalogo.get(geradorAleatorio.gerarInteiro(catalogo.size()));
    }
    return isHorarioDiurno(dataHora) ? new AsfaltoQuente() : new PocaDeChuva();
  }

  @Override
  public double obterModificadorDano(Terreno terreno,
                                      TipoElemental tipoGolpe,
                                      LocalDateTime dataHora) {
    if (terreno == null || !isEfeitoAtivo(terreno, dataHora)) {
      return EfeitoDeStatusTerreno.MULTIPLICADOR_NEUTRO;
    }
    return terreno.obterModificadorDano(tipoGolpe);
  }

  @Override
  public boolean isEfeitoAtivo(Terreno terreno, LocalDateTime dataHora) {
    if (terreno == null) {
      return false;
    }
    if (terreno instanceof AsfaltoQuente) {
      return isHorarioDiurno(dataHora);
    }
    return true;
  }

  private boolean isHorarioDiurno(LocalDateTime dataHora) {
    if (dataHora == null) {
      return true;
    }
    int hora = dataHora.getHour();
    return hora >= HORA_INICIO_DIA && hora < HORA_FIM_DIA;
  }
}
