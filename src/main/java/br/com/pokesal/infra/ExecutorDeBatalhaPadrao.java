package br.com.pokesal.infra;

import br.com.pokesal.model.Antidote;
import br.com.pokesal.model.Golpe;
import br.com.pokesal.model.ItemDeBatalha;
import br.com.pokesal.model.Potion;
import br.com.pokesal.model.SuperPotion;
import br.com.pokesal.model.Terreno;
import br.com.pokesal.model.Treinador;
import br.com.pokesal.service.Batalha;
import br.com.pokesal.service.ExecutorDeBatalha;
import br.com.pokesal.service.ProcessadorDreno;
import br.com.pokesal.service.RegistroBatalha;
import br.com.pokesal.service.RepositorioResultados;
import br.com.pokesal.service.ServicoTerreno;
import br.com.pokesal.service.ServicoTorcida;
import br.com.pokesal.service.SimuladorDeBatalha;
import java.time.Clock;
import java.util.List;

/**
 * Implementacao padrao de {@link ExecutorDeBatalha} que conduz rodadas completas ate o desfecho.
 */
public class ExecutorDeBatalhaPadrao implements ExecutorDeBatalha {
  private static final int TURNOS_POR_RODADA = 2;
  private static final double LIMIAR_HP_USO_POCAO = 0.35;
  private static final double LIMIAR_HP_USO_DRENO = 0.70;
  private static final int INDICE_ITEM_INICIAL = -1;
  private static final int INDICE_GOLPE_PADRAO = 0;
  private static final int INDICE_GOLPE_STATUS = 1;
  private static final int INDICE_GOLPE_TERCIARIO = 2;

  private final SimuladorDeBatalha simulador;
  private final ServicoTerreno servicoTerreno;
  private final Clock clock;
  private final RepositorioResultados repositorioResultados;
  private final RegistroBatalha registroBatalha;
  private final ServicoTorcida servicoTorcida;
  private final ProcessadorDreno processadorDreno;

  /**
   * Constroi o executor com todos os colaboradores necessarios para iniciar e mediar batalhas.
   *
   * @param simulador servico de simulacao responsavel pelo contador e criacao da batalha.
   * @param servicoTerreno servico dinamico de avaliacao de terreno.
   * @param clock relogio de referencia temporal.
   * @param repositorioResultados repositorio de persistencia dos resultados.
   * @param registroBatalha registrador sequencial dos eventos de turno.
   * @param servicoTorcida servico da torcida universitaria (RA-F03-01).
   * @param processadorDreno processador de cura de golpes vampiricos (RA-F03-02).
   */
  public ExecutorDeBatalhaPadrao(SimuladorDeBatalha simulador,
                                 ServicoTerreno servicoTerreno,
                                 Clock clock,
                                 RepositorioResultados repositorioResultados,
                                 RegistroBatalha registroBatalha,
                                 ServicoTorcida servicoTorcida,
                                 ProcessadorDreno processadorDreno) {
    this.simulador = simulador;
    this.servicoTerreno = servicoTerreno;
    this.clock = clock;
    this.repositorioResultados = repositorioResultados;
    this.registroBatalha = registroBatalha;
    this.servicoTorcida = servicoTorcida;
    this.processadorDreno = processadorDreno;
  }

  @Override
  public Batalha executar(Treinador treinador1, Treinador treinador2, Terreno terreno) {
    Batalha batalha = simulador.iniciarBatalha(
        treinador1,
        treinador2,
        terreno,
        servicoTerreno,
        clock,
        repositorioResultados,
        registroBatalha,
        servicoTorcida,
        processadorDreno);

    while (!batalha.isEncerrada()) {
      int rodada = (batalha.getTurnoDaBatalha() / TURNOS_POR_RODADA) + 1;
      System.out.printf("%n--- RODADA %d (Turno %d de %d) ---%n",
          rodada, batalha.getTurnoDaBatalha() + 1, Batalha.LIMITE_MAXIMO_TURNOS);

      Treinador primeiro = batalha.determinarPrimeiroAtacante();
      Treinador segundo = (primeiro == treinador1) ? treinador2 : treinador1;

      executarAcaoCompetidor(batalha, primeiro, segundo);
      if (!batalha.isEncerrada() && !segundo.getPokesal().estaDesmaiado()) {
        executarAcaoCompetidor(batalha, segundo, primeiro);
      }

      batalha.finalizarTurno();
      System.out.printf("Status Fim da Rodada %d: %s HP: %d/%d | %s HP: %d/%d%n",
          rodada,
          treinador1.getPokesal().getNome(), treinador1.getPokesal().getHpAtual(),
          treinador1.getPokesal().getHpMax(),
          treinador2.getPokesal().getNome(), treinador2.getPokesal().getHpAtual(),
          treinador2.getPokesal().getHpMax());
    }
    return batalha;
  }

  private void executarAcaoCompetidor(Batalha batalha, Treinador ativo, Treinador oponente) {
    boolean podeUsarItem = ativo.getContItensUsados() < Treinador.LIMITE_MAXIMO_ITENS
        && !ativo.getMochila().isEmpty();

    if (ativo.getPokesal().temStatus() && podeUsarItem) {
      int idxAntidoto = localizarItem(ativo.getMochila(), Antidote.class);
      if (idxAntidoto >= 0) {
        ItemDeBatalha item = ativo.getMochila().get(idxAntidoto);
        System.out.printf("  [ITEM] %s usou %s para curar status de %s!%n",
            ativo.getNome(), item.getNome(), ativo.getPokesal().getNome());
        batalha.consumirItemNoTurno(ativo, idxAntidoto);
        return;
      }
    }

    boolean precisaCurarHp = ativo.getPokesal().getHpAtual()
        <= (ativo.getPokesal().getHpMax() * LIMIAR_HP_USO_POCAO);
    if (precisaCurarHp && podeUsarItem) {
      int idxPocao = localizarPocao(ativo.getMochila());
      if (idxPocao >= 0) {
        ItemDeBatalha item = ativo.getMochila().get(idxPocao);
        System.out.printf("  [ITEM] %s usou %s!%n", ativo.getNome(), item.getNome());
        batalha.consumirItemNoTurno(ativo, idxPocao);
        return;
      }
    }

    Golpe golpeEscolhido = selecionarGolpeEstrategico(ativo, oponente);
    System.out.printf("  %s ordenou %s usando %s!%n",
        ativo.getNome(), ativo.getPokesal().getNome(), golpeEscolhido.getNome());
    int dano = batalha.executarAcaoDeAtaque(ativo, oponente, golpeEscolhido);
    System.out.printf("  -> Dano causado: %d%n", dano);
  }

  private Golpe selecionarGolpeEstrategico(Treinador ativo, Treinador oponente) {
    List<Golpe> golpes = ativo.getPokesal().getGolpes();
    if (!oponente.getPokesal().temStatus()
        && golpes.size() > INDICE_GOLPE_STATUS
        && golpes.get(INDICE_GOLPE_STATUS).getEfeitoDeStatus() != null) {
      return golpes.get(INDICE_GOLPE_STATUS);
    }
    if (golpes.size() > INDICE_GOLPE_TERCIARIO
        && golpes.get(INDICE_GOLPE_TERCIARIO).isGolpeDreno()
        && ativo.getPokesal().getHpAtual()
        <= (ativo.getPokesal().getHpMax() * LIMIAR_HP_USO_DRENO)) {
      return golpes.get(INDICE_GOLPE_TERCIARIO);
    }
    return golpes.get(INDICE_GOLPE_PADRAO);
  }

  private int localizarItem(List<ItemDeBatalha> mochila, Class<?> tipo) {
    for (int i = 0; i < mochila.size(); i++) {
      if (tipo.isInstance(mochila.get(i))) {
        return i;
      }
    }
    return INDICE_ITEM_INICIAL;
  }

  private int localizarPocao(List<ItemDeBatalha> mochila) {
    for (int i = 0; i < mochila.size(); i++) {
      ItemDeBatalha item = mochila.get(i);
      if (item instanceof Potion || item instanceof SuperPotion) {
        return i;
      }
    }
    return INDICE_ITEM_INICIAL;
  }
}
