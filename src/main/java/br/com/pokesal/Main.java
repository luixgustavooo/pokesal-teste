package br.com.pokesal;

import br.com.pokesal.infra.ExecutorDeBatalhaPadrao;
import br.com.pokesal.infra.GeradorAleatorioPadrao;
import br.com.pokesal.infra.ProcessadorDrenoPadrao;
import br.com.pokesal.infra.RegistroBatalhaConsole;
import br.com.pokesal.infra.RepositorioResultadosEmMemoria;
import br.com.pokesal.infra.ServicoTerrenoPadrao;
import br.com.pokesal.infra.ServicoTorcidaPadrao;
import br.com.pokesal.model.Pokesal;
import br.com.pokesal.model.ResultadoBatalha;
import br.com.pokesal.model.Treinador;
import br.com.pokesal.service.ExecutorDeBatalha;
import br.com.pokesal.service.GeradorAleatorio;
import br.com.pokesal.service.ProcessadorDreno;
import br.com.pokesal.service.RepositorioResultados;
import br.com.pokesal.service.ServicoTerreno;
import br.com.pokesal.service.ServicoTorcida;
import br.com.pokesal.service.SimuladorDeBatalha;
import br.com.pokesal.service.Torneio;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Scanner;

/**
 * Ponto de entrada do sistema e raiz de composicao do Torneio Oficial UCSAL (4 ou 8 treinadores).
 */
public class Main {
  /** Indice do treinador controlado pelo jogador principal. */
  private static final int INDICE_JOGADOR_PRINCIPAL = 0;

  /**
   * Metodo principal responsavel por compor as dependencias e conduzir o torneio eliminatorio.
   *
   * @param args argumentos de linha de comando.
   */
  public static void main(String[] args) {
    System.out.println("====================================================");
    System.out.println("    BEM-VINDO AO TORNEIO OFICIAL POKESAL - UCSAL    ");
    System.out.println("====================================================");

    final GeradorAleatorio geradorAleatorio = new GeradorAleatorioPadrao();
    SimuladorDeBatalha simulador = new SimuladorDeBatalha(geradorAleatorio);
    Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());

    int tamanhoTorneio = interagirTamanhoTorneio(scanner);
    simulador.cadastrarTreinadoresTorneio(tamanhoTorneio, "Ash Sal");

    List<Treinador> participantes = simulador.getTreinadores();
    Treinador jogador = participantes.get(INDICE_JOGADOR_PRINCIPAL);
    interagirEscolhaInicial(simulador, jogador, scanner);
    simulador.sortearHeldItem(jogador);

    for (int i = 1; i < participantes.size(); i++) {
      Treinador adversario = participantes.get(i);
      simulador.sortearPokesal(adversario);
      simulador.sortearHeldItem(adversario);
      System.out.printf("-> %s definiu seu inicial: %s (Held Item: %s)%n",
          adversario.getNome(),
          adversario.getPokesal().getNome(),
          adversario.getPokesal().getHeldItem().getNome());
    }

    Clock clock = Clock.systemDefaultZone();
    ServicoTerreno servicoTerreno = new ServicoTerrenoPadrao(geradorAleatorio);
    RepositorioResultados repositorioResultados = new RepositorioResultadosEmMemoria();
    RegistroBatalhaConsole registroBatalha = new RegistroBatalhaConsole();
    ServicoTorcida servicoTorcida = new ServicoTorcidaPadrao(geradorAleatorio);
    ProcessadorDreno processadorDreno = new ProcessadorDrenoPadrao();

    ExecutorDeBatalha executorDeBatalha = new ExecutorDeBatalhaPadrao(
        simulador,
        servicoTerreno,
        clock,
        repositorioResultados,
        registroBatalha,
        servicoTorcida,
        processadorDreno);

    Torneio torneio = new Torneio(
        executorDeBatalha,
        servicoTerreno,
        geradorAleatorio,
        clock);

    Treinador campeao = torneio.executarTorneio(participantes);
    exibirResumoFinal(campeao, repositorioResultados);
  }

  private static int interagirTamanhoTorneio(Scanner scanner) {
    System.out.print("Selecione o formato do torneio (4 ou 8 treinadores) [padrao 4]: ");
    try {
      if (scanner != null && scanner.hasNextLine()) {
        String linha = scanner.nextLine().trim();
        if ("8".equals(linha)) {
          return Torneio.QUANTIDADE_OITO_TREINADORES;
        }
      }
    } catch (Exception e) {
      System.out.println("Entrada invalida. Usando formato padrao de 4 treinadores.");
    }
    return Torneio.QUANTIDADE_QUATRO_TREINADORES;
  }

  private static void interagirEscolhaInicial(SimuladorDeBatalha simulador,
                                              Treinador treinador,
                                              Scanner scanner) {
    System.out.printf("%n>>> Selecao de Pokesal Inicial para o Treinador: %s <<<%n",
        treinador.getNome());
    System.out.println("Opcoes homologadas no Estacionamento da UCSal:");
    List<Pokesal> catalogo = SimuladorDeBatalha.obterCatalogoPokesal();
    for (int i = 0; i < catalogo.size(); i++) {
      Pokesal p = catalogo.get(i);
      System.out.printf("  [%d] %-9s | Tipo: %-6s | HP: %3d | ATK: %2d | DEF: %2d | SPD: %2d%n",
          i + 1, p.getNome(), p.getTipoElemental(), p.getHpMax(),
          p.getAtk(), p.getDef(), p.getSpd());
    }

    boolean escolhido = false;
    if (scanner != null) {
      System.out.print("Digite o numero (1 a 6) ou nome do Pokesal desejado: ");
      try {
        if (scanner.hasNextLine()) {
          String linha = scanner.nextLine().trim();
          if (!linha.isEmpty()) {
            try {
              int opcao = Integer.parseInt(linha);
              simulador.escolherPokesal(treinador, opcao);
              escolhido = true;
            } catch (NumberFormatException e) {
              simulador.escolherPokesal(treinador, linha);
              escolhido = true;
            }
          }
        }
      } catch (Exception e) {
        System.out.println("Entrada nao reconhecida. Aplicando selecao padrao.");
      }
    }

    if (!escolhido) {
      System.out.println("Nenhuma entrada. Selecionando BulbaSal por padrao regulamentar.");
      simulador.escolherPokesal(treinador, 1);
    }

    System.out.printf("-> %s escolheu com sucesso: %s!%n",
        treinador.getNome(), treinador.getPokesal().getNome());
  }

  private static void exibirResumoFinal(Treinador campeao,
                                        RepositorioResultados repositorio) {
    System.out.println("\n--- RESUMO DE RESULTADOS REGISTRADOS NO REPOSITORIO ---");
    List<ResultadoBatalha> historico = repositorio.listarTodos();
    for (int i = 0; i < historico.size(); i++) {
      ResultadoBatalha res = historico.get(i);
      String nomeVencedor = (res.getVencedor() != null) ? res.getVencedor().getNome() : "Empate";
      String nomePerdedor = (res.getPerdedor() != null) ? res.getPerdedor().getNome() : "Empate";
      System.out.printf("  Batalha %d: %s derrotou %s em %d turnos.%n",
          i + 1, nomeVencedor, nomePerdedor, res.getNumeroTurnos());
    }
    System.out.printf("Grande Campeao: %s (%d vitorias no torneio)%n",
        campeao.getNome(), campeao.getBatalhasVencidas());
  }
}
