package br.com.pokesal.service;

import br.com.pokesal.model.Terreno;
import br.com.pokesal.model.Treinador;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gerencia torneios eliminatorios oficiais com suporte a 4 ou 8 treinadores (DIP).
 */
public class Torneio {
  /** Quantidade minima de competidores (formato Semifinais + Final). */
  public static final int QUANTIDADE_QUATRO_TREINADORES = 4;

  /** Quantidade maxima de competidores (formato Quartas + Semifinais + Final). */
  public static final int QUANTIDADE_OITO_TREINADORES = 8;

  /** Passo de pareamento de confrontos eliminatorios (2 competidores por batalha). */
  private static final int PASSO_CONFRONTO = 2;

  private final ExecutorDeBatalha executorDeBatalha;
  private final ServicoTerreno servicoTerreno;
  private final GeradorAleatorio geradorAleatorio;
  private final Clock clock;

  /**
   * Constroi o gestor de torneio com injecao completa de dependencias.
   *
   * @param executorDeBatalha colaborador responsavel por conduzir cada batalha do chaveamento.
   * @param servicoTerreno servico dinamico para determinacao das arenas.
   * @param geradorAleatorio fonte de aleatoriedade para sorteio de chaves e desempates.
   * @param clock relogio de referencia para consulta ao servico de terreno.
   */
  public Torneio(ExecutorDeBatalha executorDeBatalha,
                 ServicoTerreno servicoTerreno,
                 GeradorAleatorio geradorAleatorio,
                 Clock clock) {
    this.executorDeBatalha = executorDeBatalha;
    this.servicoTerreno = servicoTerreno;
    this.geradorAleatorio = geradorAleatorio;
    this.clock = clock;
  }

  /**
   * Executa o torneio eliminatorio completo ate definir o campeao.
   *
   * @param participantes lista contendo exatamente 4 ou 8 treinadores com Pokesal definido.
   * @return treinador campeao do torneio.
   * @throws IllegalArgumentException se a quantidade nao for 4 ou 8,
   *     ou houver participante invalido.
   */
  public Treinador executarTorneio(List<Treinador> participantes) {
    validarParticipantes(participantes);

    List<Treinador> chaveAtual = new ArrayList<>(participantes);
    embaralharParticipantes(chaveAtual);

    int numeroFase = 1;
    while (chaveAtual.size() > 1) {
      String nomeFase = obterNomeFase(chaveAtual.size(), numeroFase);
      System.out.printf("%n====================================================%n");
      System.out.printf("             %s (%d COMPETIDORES)%n", nomeFase, chaveAtual.size());
      System.out.printf("====================================================%n");

      List<Treinador> vencedoresDaFase = new ArrayList<>();
      for (int i = 0; i < chaveAtual.size(); i += PASSO_CONFRONTO) {
        Treinador t1 = chaveAtual.get(i);
        Treinador t2 = chaveAtual.get(i + 1);

        prepararCompetidoresParaConfronto(t1, t2);
        Terreno terreno = obterTerrenoDoConfronto();

        System.out.printf("%n[Confronto %d] %s (%s) vs %s (%s)%n",
            (i / PASSO_CONFRONTO) + 1,
            t1.getNome(), t1.getPokesal().getNome(),
            t2.getNome(), t2.getPokesal().getNome());

        Batalha batalha = executorDeBatalha.executar(t1, t2, terreno);
        Treinador vencedor = definirVencedorConfronto(batalha, t1, t2);
        vencedor.incrementarBatalhasVencidas();
        vencedoresDaFase.add(vencedor);

        System.out.printf("-> %s avanca de fase!%n", vencedor.getNome());
      }
      chaveAtual = vencedoresDaFase;
      numeroFase++;
    }

    Treinador campeao = chaveAtual.get(0);
    System.out.printf("%n====================================================%n");
    System.out.printf("  CAMPEAO DO TORNEIO UCSAL: %s (%s)!%n",
        campeao.getNome(), campeao.getPokesal().getNome());
    System.out.printf("====================================================%n");
    return campeao;
  }

  private void validarParticipantes(List<Treinador> participantes) {
    if (participantes == null
        || (participantes.size() != QUANTIDADE_QUATRO_TREINADORES
        && participantes.size() != QUANTIDADE_OITO_TREINADORES)) {
      throw new IllegalArgumentException(
          "O torneio eliminatorio exige exatamente 4 ou 8 treinadores participantes.");
    }
    for (Treinador treinador : participantes) {
      if (treinador == null || treinador.getPokesal() == null) {
        throw new IllegalArgumentException(
            "Todos os participantes do torneio devem ser validos e possuir um Pokesal.");
      }
    }
  }

  private void embaralharParticipantes(List<Treinador> lista) {
    if (geradorAleatorio == null) {
      return;
    }
    for (int i = lista.size() - 1; i > 0; i--) {
      int j = geradorAleatorio.gerarInteiro(i + 1);
      if (j >= 0 && j <= i) {
        Collections.swap(lista, i, j);
      }
    }
  }

  private void prepararCompetidoresParaConfronto(Treinador t1, Treinador t2) {
    t1.getPokesal().restaurarParaNovaBatalha();
    t1.reiniciarParaNovaBatalha();
    t2.getPokesal().restaurarParaNovaBatalha();
    t2.reiniciarParaNovaBatalha();
  }

  private Terreno obterTerrenoDoConfronto() {
    if (servicoTerreno == null) {
      return null;
    }
    LocalDateTime agora = (clock != null) ? LocalDateTime.now(clock) : null;
    return servicoTerreno.obterTerrenoAtual(agora);
  }

  private Treinador definirVencedorConfronto(Batalha batalha, Treinador t1, Treinador t2) {
    Treinador vencedor = (batalha != null) ? batalha.getVencedor() : null;
    if (vencedor != null) {
      return vencedor;
    }
    int spd1 = t1.getPokesal().getSpd();
    int spd2 = t2.getPokesal().getSpd();
    if (spd1 > spd2) {
      return t1;
    }
    if (spd2 > spd1) {
      return t2;
    }
    if (geradorAleatorio != null) {
      return geradorAleatorio.gerarBooleano() ? t1 : t2;
    }
    return t1;
  }

  private String obterNomeFase(int tamanhoChave, int numeroFase) {
    if (tamanhoChave == QUANTIDADE_OITO_TREINADORES) {
      return "QUARTAS DE FINAL";
    }
    if (tamanhoChave == QUANTIDADE_QUATRO_TREINADORES) {
      return "SEMIFINAIS";
    }
    if (tamanhoChave == PASSO_CONFRONTO) {
      return "GRANDE FINAL";
    }
    return "FASE " + numeroFase;
  }
}
