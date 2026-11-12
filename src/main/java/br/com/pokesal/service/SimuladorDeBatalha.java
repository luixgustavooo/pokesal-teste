package br.com.pokesal.service;

import br.com.pokesal.model.Antidote;
import br.com.pokesal.model.AsfaltoQuente;
import br.com.pokesal.model.CanteiroCentral;
import br.com.pokesal.model.Golpe;
import br.com.pokesal.model.HeldItem;
import br.com.pokesal.model.PocaDeChuva;
import br.com.pokesal.model.Pokesal;
import br.com.pokesal.model.Potion;
import br.com.pokesal.model.SuperPotion;
import br.com.pokesal.model.Terreno;
import br.com.pokesal.model.TipoElemental;
import br.com.pokesal.model.TipoStatus;
import br.com.pokesal.model.Treinador;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Servico responsavel pelo catalogo oficial, sorteios de equipes, arenas e gestao de batalhas.
 */
public class SimuladorDeBatalha {
  /** Quantidade minima de competidores para realizacao do sorteio da primeira partida. */
  public static final int QUANTIDADE_MINIMA_TREINADORES = 2;

  /** Indice do primeiro competidor sorteado para a partida. */
  public static final int INDICE_PRIMEIRO_CONFRONTO = 0;

  /** Indice do segundo competidor sorteado para a partida. */
  public static final int INDICE_SEGUNDO_CONFRONTO = 1;

  /** Precisao intermediaria (90%) para golpes secundarios de status. */
  public static final int PRECISAO_GOLPE_STATUS = 90;

  /** Precisao alta (95%) para golpes ofensivos potentes. */
  public static final int PRECISAO_GOLPE_FORTE = 95;

  /** Precisao de golpes de status com maior risco (85%). */
  public static final int PRECISAO_GOLPE_ARRISCADO = 85;

  private static final List<String> NOMES_ADVERSARIOS_OFICIAIS = Arrays.asList(
      "Gary Sal", "Misty Sal", "Brock Sal", "Erika Sal",
      "Lt. Surge Sal", "Sabrina Sal", "Mestre Red"
  );

  private final List<Treinador> treinadores;
  private final GeradorAleatorio geradorAleatorio;
  private int contadorBatalha;

  /**
   * Construtor com injecao de dependencia do gerador aleatorio (DIP).
   *
   * @param geradorAleatorio fonte de aleatoriedade desacoplada.
   */
  public SimuladorDeBatalha(GeradorAleatorio geradorAleatorio) {
    this.treinadores = new ArrayList<>();
    this.contadorBatalha = 0;
    this.geradorAleatorio = geradorAleatorio;
  }

  /**
   * Adiciona um treinador a lista oficial do torneio.
   *
   * @param treinador treinador a ser cadastrado.
   */
  public void adicionarTreinador(Treinador treinador) {
    if (treinador != null) {
      this.treinadores.add(treinador);
    }
  }

  /**
   * Cadastra os competidores iniciais padrao do torneio oficial UCSAL.
   */
  public void cadastrarTreinadores() {
    adicionarTreinador(new Treinador("Ash Sal"));
    adicionarTreinador(new Treinador("Gary Sal"));
  }

  /**
   * Cadastra os competidores para o torneio eliminatorio de 4 ou 8 treinadores.
   *
   * @param quantidadeTotal numero total de competidores (4 ou 8).
   * @param nomeJogadorPrincipal nome do treinador controlado pelo usuario.
   * @throws IllegalArgumentException se a quantidade nao for 4 ou 8.
   */
  public void cadastrarTreinadoresTorneio(int quantidadeTotal, String nomeJogadorPrincipal) {
    if (quantidadeTotal != Torneio.QUANTIDADE_QUATRO_TREINADORES
        && quantidadeTotal != Torneio.QUANTIDADE_OITO_TREINADORES) {
      throw new IllegalArgumentException(
          "O torneio oficial suporta apenas 4 ou 8 treinadores.");
    }
    this.treinadores.clear();
    String nomePrincipal = (nomeJogadorPrincipal != null && !nomeJogadorPrincipal.trim().isEmpty())
        ? nomeJogadorPrincipal.trim() : "Ash Sal";
    adicionarTreinador(new Treinador(nomePrincipal));
    for (int i = 0; i < quantidadeTotal - 1; i++) {
      adicionarTreinador(new Treinador(NOMES_ADVERSARIOS_OFICIAIS.get(i)));
    }
  }

  /**
   * Permite ao treinador escolher formalmente seu Pokesal inicial entre as 6 opcoes oficiais.
   * Atende estritamente a Regra 1 de Negocio do sistema.
   *
   * @param treinador competidor que recebera a criatura escolhida.
   * @param nomePokesal nome da especie selecionada.
   * @throws IllegalArgumentException se o nome informado nao corresponder aos iniciais permitidos.
   */
  public void escolherPokesal(Treinador treinador, String nomePokesal) {
    if (treinador == null || nomePokesal == null) {
      throw new IllegalArgumentException("Treinador e nome do Pokesal nao podem ser nulos.");
    }
    List<Pokesal> catalogo = obterCatalogoPokesal();
    Pokesal selecionado = catalogo.stream()
        .filter(p -> p.getNome().equalsIgnoreCase(nomePokesal.trim()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Pokesal invalido: '" + nomePokesal + "'. Opcoes permitidas: BulbaSal, CharSal, "
                + "SquirtSal, ChikoSal, CyndaSal ou TotoSal."));

    treinador.setPokesal(clonarPokesal(selecionado));
    associarItensMochila(treinador);
  }

  /**
   * Permite ao treinador escolher seu Pokesal inicial pelo indice numerico do catalogo (1 a 6).
   *
   * @param treinador competidor que recebera a criatura escolhida.
   * @param indiceOpcao indice baseado em 1 (1 para a primeira opcao ate 6).
   * @throws IllegalArgumentException se o indice estiver fora da faixa 1..6.
   */
  public void escolherPokesal(Treinador treinador, int indiceOpcao) {
    List<Pokesal> catalogo = obterCatalogoPokesal();
    if (indiceOpcao < 1 || indiceOpcao > catalogo.size()) {
      throw new IllegalArgumentException(
          "Indice de Pokesal invalido. Escolha entre 1 e " + catalogo.size());
    }
    Pokesal selecionado = catalogo.get(indiceOpcao - 1);
    treinador.setPokesal(clonarPokesal(selecionado));
    associarItensMochila(treinador);
  }

  /**
   * Sorteia um Pokesal do catalogo oficial e o associa ao competidor por clonagem profunda.
   *
   * @param treinador competidor que recebera a criatura.
   */
  public void sortearPokesal(Treinador treinador) {
    List<Pokesal> opcoes = obterCatalogoPokesal();
    int indice = (geradorAleatorio != null)
        ? geradorAleatorio.gerarInteiro(opcoes.size()) : 0;
    Pokesal modelo = opcoes.get(indice);
    Pokesal sorteado = clonarPokesal(modelo);
    treinador.setPokesal(sorteado);

    associarItensMochila(treinador);
  }

  /**
   * Equipa a mochila do treinador com o kit oficial regulamentar de 3 itens de batalha.
   *
   * @param treinador competidor cuja mochila sera abastecida.
   */
  public void associarItensMochila(Treinador treinador) {
    if (treinador != null) {
      treinador.adicionarItem(new Potion());
      treinador.adicionarItem(new SuperPotion());
      treinador.adicionarItem(new Antidote());
    }
  }

  /**
   * Cria uma nova instancia independente de Pokesal baseada em um modelo existente.
   *
   * @param base modelo original de referencia.
   * @return nova instancia clonada isolada em memoria.
   */
  public static Pokesal clonarPokesal(Pokesal base) {
    return new Pokesal(base.getNome(), base.getHpMax(), base.getAtk(), base.getDef(),
        base.getSpd(), base.getTipoElemental(), base.getGolpes());
  }

  /**
   * Sorteia um item segurado do catalogo e equipa no Pokesal ativo do treinador.
   *
   * @param treinador treinador portador do Pokesal.
   */
  public void sortearHeldItem(Treinador treinador) {
    List<HeldItem> opcoes = obterCatalogoHeldItems();
    int indice = (geradorAleatorio != null)
        ? geradorAleatorio.gerarInteiro(opcoes.size()) : 0;
    HeldItem sorteado = opcoes.get(indice);
    if (treinador.getPokesal() != null) {
      treinador.getPokesal().setHeldItem(sorteado);
    }
  }

  /**
   * Sorteia uma das 3 arenas oficiais cadastradas para o combate.
   *
   * @return instancia de terreno sorteada.
   */
  public Terreno sortearTerreno() {
    List<Terreno> opcoes = obterCatalogoTerrenos();
    int indice = (geradorAleatorio != null)
        ? geradorAleatorio.gerarInteiro(opcoes.size()) : 0;
    return opcoes.get(indice);
  }

  /**
   * Realiza o sorteio da ordem de chaveamento para o confronto inicial.
   *
   * @return lista com os 2 competidores sorteados para o confronto.
   */
  public List<Treinador> sortearPrimeiraPartida() {
    if (treinadores.size() < QUANTIDADE_MINIMA_TREINADORES) {
      return new ArrayList<>(treinadores);
    }
    List<Treinador> copia = new ArrayList<>(treinadores);
    if (geradorAleatorio != null) {
      for (int i = copia.size() - 1; i > 0; i--) {
        int j = geradorAleatorio.gerarInteiro(i + 1);
        Collections.swap(copia, i, j);
      }
    }
    return Arrays.asList(
        copia.get(INDICE_PRIMEIRO_CONFRONTO),
        copia.get(INDICE_SEGUNDO_CONFRONTO));
  }

  /**
   * Inicia e registra uma nova batalha oficial com injecao completa de colaboradores.
   *
   * @param treinador1 primeiro participante.
   * @param treinador2 segundo participante.
   * @param terrenoEscolhido arena de combate.
   * @param servicoTerreno servico dinamico de avaliacao do terreno.
   * @param clock relogio de referencia temporal.
   * @param repositorioResultados repositorio de persistencia dos resultados.
   * @param registroBatalha registrador sequencial de eventos do turno.
   * @param servicoTorcida servico da torcida universitaria (RA-F03-01).
   * @param processadorDreno processador de cura de golpes vampiricos (RA-F03-02).
   * @return instancia de batalha inicializada.
   */
  public Batalha iniciarBatalha(Treinador treinador1,
                                Treinador treinador2,
                                Terreno terrenoEscolhido,
                                ServicoTerreno servicoTerreno,
                                Clock clock,
                                RepositorioResultados repositorioResultados,
                                RegistroBatalha registroBatalha,
                                ServicoTorcida servicoTorcida,
                                ProcessadorDreno processadorDreno) {
    this.contadorBatalha++;
    return new Batalha(
        treinador1,
        treinador2,
        terrenoEscolhido,
        geradorAleatorio,
        servicoTerreno,
        clock,
        repositorioResultados,
        registroBatalha,
        servicoTorcida,
        processadorDreno);
  }

  /**
   * Retorna a quantidade total de batalhas iniciadas neste simulador.
   *
   * @return contagem acumulada de batalhas.
   */
  public int getContadorBatalha() {
    return contadorBatalha;
  }

  /**
   * Retorna o catalogo oficial com os 6 Pokesal homologados para a competicao.
   *
   * @return lista das 6 especies de Pokesal com seus golpes (incluindo precisao e golpes de dreno).
   */
  public static List<Pokesal> obterCatalogoPokesal() {
    List<Pokesal> lista = new ArrayList<>();

    Golpe b1 = new Golpe("Chicote de Vinha", 40, TipoElemental.PLANTA,
        null, Golpe.PRECISAO_MAXIMA);
    Golpe b2 = new Golpe("Pó Venenoso", 20, TipoElemental.PLANTA,
        new EfeitoDeStatusGolpe(TipoStatus.ENVENENADO, Batalha.CHANCE_STATUS_SEGUNDO_GOLPE),
        PRECISAO_GOLPE_STATUS);
    Golpe b3 = new Golpe("Mega Dreno", 30, TipoElemental.PLANTA,
        null, Golpe.PRECISAO_MAXIMA, true, Golpe.TAXA_DRENO_PADRAO);
    lista.add(new Pokesal("BulbaSal", 120, 35, 30, 25,
        TipoElemental.PLANTA, Arrays.asList(b1, b2, b3)));

    Golpe c1 = new Golpe("Lança Chamas", 42, TipoElemental.FOGO,
        null, PRECISAO_GOLPE_FORTE);
    Golpe c2 = new Golpe("Brasas Queimantes", 21, TipoElemental.FOGO,
        new EfeitoDeStatusGolpe(TipoStatus.QUEIMADO, Batalha.CHANCE_STATUS_SEGUNDO_GOLPE),
        PRECISAO_GOLPE_STATUS);
    Golpe c3 = new Golpe("Arranhão", 28, TipoElemental.FOGO,
        null, Golpe.PRECISAO_MAXIMA);
    lista.add(new Pokesal("CharSal", 110, 42, 25, 35,
        TipoElemental.FOGO, Arrays.asList(c1, c2, c3)));

    Golpe s1 = new Golpe("Jato de Água", 38, TipoElemental.AGUA,
        null, Golpe.PRECISAO_MAXIMA);
    Golpe s2 = new Golpe("Onda Estática", 19, TipoElemental.AGUA,
        new EfeitoDeStatusGolpe(TipoStatus.PARALISADO, Batalha.CHANCE_STATUS_SEGUNDO_GOLPE),
        PRECISAO_GOLPE_STATUS);
    Golpe s3 = new Golpe("Cabeçada", 30, TipoElemental.AGUA,
        null, PRECISAO_GOLPE_FORTE);
    lista.add(new Pokesal("SquirtSal", 125, 32, 38, 22,
        TipoElemental.AGUA, Arrays.asList(s1, s2, s3)));

    Golpe ck1 = new Golpe("Folha Navalha", 39, TipoElemental.PLANTA,
        null, PRECISAO_GOLPE_FORTE);
    Golpe ck2 = new Golpe("Esporo Paralisante", 19, TipoElemental.PLANTA,
        new EfeitoDeStatusGolpe(TipoStatus.PARALISADO, Batalha.CHANCE_STATUS_SEGUNDO_GOLPE),
        PRECISAO_GOLPE_ARRISCADO);
    Golpe ck3 = new Golpe("Pancada Suave", 27, TipoElemental.PLANTA,
        null, Golpe.PRECISAO_MAXIMA);
    lista.add(new Pokesal("ChikoSal", 115, 33, 35, 28,
        TipoElemental.PLANTA, Arrays.asList(ck1, ck2, ck3)));

    Golpe cy1 = new Golpe("Roda de Fogo", 41, TipoElemental.FOGO,
        null, PRECISAO_GOLPE_FORTE);
    Golpe cy2 = new Golpe("Fumaça Tóxica", 20, TipoElemental.FOGO,
        new EfeitoDeStatusGolpe(TipoStatus.ENVENENADO, Batalha.CHANCE_STATUS_SEGUNDO_GOLPE),
        PRECISAO_GOLPE_STATUS);
    Golpe cy3 = new Golpe("Ataque Rápido", 30, TipoElemental.FOGO,
        null, Golpe.PRECISAO_MAXIMA);
    lista.add(new Pokesal("CyndaSal", 108, 40, 26, 36,
        TipoElemental.FOGO, Arrays.asList(cy1, cy2, cy3)));

    Golpe t1 = new Golpe("Pistola d'Água", 39, TipoElemental.AGUA,
        null, Golpe.PRECISAO_MAXIMA);
    Golpe t2 = new Golpe("Mordida Térmica", 19, TipoElemental.AGUA,
        new EfeitoDeStatusGolpe(TipoStatus.QUEIMADO, Batalha.CHANCE_STATUS_SEGUNDO_GOLPE),
        PRECISAO_GOLPE_STATUS);
    Golpe t3 = new Golpe("Mordida Sanguessuga", 29, TipoElemental.AGUA,
        null, Golpe.PRECISAO_MAXIMA, true, Golpe.TAXA_DRENO_PADRAO);
    lista.add(new Pokesal("TotoSal", 122, 36, 34, 26,
        TipoElemental.AGUA, Arrays.asList(t1, t2, t3)));

    return lista;
  }

  /**
   * Retorna o catalogo oficial com os 6 itens segurados (held items) homologados.
   *
   * @return lista dos 6 held items com suas bonificacoes passivas.
   */
  public static List<HeldItem> obterCatalogoHeldItems() {
    return Arrays.asList(
        new HeldItem("Carvão Incandescente",
            new EfeitoDeStatusHeld(6, 0, TipoElemental.FOGO, 1.1)),
        new HeldItem("Gota d'Água Sagrada",
            new EfeitoDeStatusHeld(6, 0, TipoElemental.AGUA, 1.1)),
        new HeldItem("Semente de Salto",
            new EfeitoDeStatusHeld(6, 0, TipoElemental.PLANTA, 1.1)),
        new HeldItem("Faixa de Foco UCSal",
            new EfeitoDeStatusHeld(5, 0, null, 1.0)),
        new HeldItem("Restos do RU (Leftovers)",
            new EfeitoDeStatusHeld(0, 6, null, 1.0)),
        new HeldItem("Colete de Pituaçu",
            new EfeitoDeStatusHeld(4, 3, null, 1.0))
    );
  }

  /**
   * Retorna o catalogo com as 3 arenas regulamentares de combate.
   *
   * @return lista dos 3 terrenos cadastrados.
   */
  public static List<Terreno> obterCatalogoTerrenos() {
    return Arrays.asList(
        new AsfaltoQuente(),
        new PocaDeChuva(),
        new CanteiroCentral()
    );
  }

  /**
   * Retorna uma visao imutavel da lista de competidores inscritos.
   *
   * @return lista imutavel de treinadores.
   */
  public List<Treinador> getTreinadores() {
    return Collections.unmodifiableList(treinadores);
  }
}
