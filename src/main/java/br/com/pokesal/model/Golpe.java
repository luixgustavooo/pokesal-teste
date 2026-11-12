package br.com.pokesal.model;

import br.com.pokesal.service.EfeitoDeStatusGolpe;

/**
 * Representa um golpe que um Pokesal pode executar, incluindo precisao, dreno e efeito de status.
 */
public class Golpe {
  /** Precisao minima permitida para um golpe. */
  public static final int PRECISAO_MINIMA = 0;

  /** Precisao maxima (100%) para golpes que nao dependem de reducao de acuracia. */
  public static final int PRECISAO_MAXIMA = 100;

  /** Taxa padrao de conversao de dano em cura para golpes de dreno (50%). */
  public static final double TAXA_DRENO_PADRAO = 0.50;

  private final String nome;
  private final int poder;
  private final TipoElemental tipo;
  private final EfeitoDeStatusGolpe efeitoDeStatus;
  private final int precisao;
  private final boolean golpeDreno;
  private final double taxaConversaoDreno;

  /**
   * Cria um novo golpe com todos os atributos configuraveis, incluindo precisao e dreno.
   *
   * @param nome nome do golpe.
   * @param poder valor base de dano do golpe.
   * @param tipo tipo elemental do golpe.
   * @param efeitoDeStatus efeito de status opcional (pode ser null).
   * @param precisao probabilidade de acerto entre 0 e 100.
   * @param golpeDreno indica se o golpe absorve parte do dano causado em cura.
   * @param taxaConversaoDreno proporcao do dano convertido em cura (ex: 0.5 para 50%).
   * @throws IllegalArgumentException se a precisao estiver fora da faixa [0, 100].
   */
  public Golpe(String nome, int poder, TipoElemental tipo, EfeitoDeStatusGolpe efeitoDeStatus,
               int precisao, boolean golpeDreno, double taxaConversaoDreno) {
    if (precisao < PRECISAO_MINIMA || precisao > PRECISAO_MAXIMA) {
      throw new IllegalArgumentException("Precisao do golpe deve estar entre 0 e 100.");
    }
    this.nome = nome;
    this.poder = poder;
    this.tipo = tipo;
    this.efeitoDeStatus = efeitoDeStatus;
    this.precisao = precisao;
    this.golpeDreno = golpeDreno;
    this.taxaConversaoDreno = taxaConversaoDreno;
  }

  /**
   * Cria um novo golpe com precisao personalizada e sem absorcao de vida.
   *
   * @param nome nome do golpe.
   * @param poder valor base de dano do golpe.
   * @param tipo tipo elemental do golpe.
   * @param efeitoDeStatus efeito de status opcional (pode ser null).
   * @param precisao probabilidade de acerto entre 0 e 100.
   */
  public Golpe(String nome, int poder, TipoElemental tipo, EfeitoDeStatusGolpe efeitoDeStatus,
               int precisao) {
    this(nome, poder, tipo, efeitoDeStatus, precisao, false, 0.0);
  }

  /**
   * Cria um novo golpe com atributos e efeito de status secundario (precisao padrao de 100%).
   *
   * @param nome nome do golpe.
   * @param poder valor base de dano do golpe.
   * @param tipo tipo elemental do golpe.
   * @param efeitoDeStatus efeito de status opcional (pode ser null).
   */
  public Golpe(String nome, int poder, TipoElemental tipo, EfeitoDeStatusGolpe efeitoDeStatus) {
    this(nome, poder, tipo, efeitoDeStatus, PRECISAO_MAXIMA, false, 0.0);
  }

  /**
   * Cria um novo golpe sem efeito de status (precisao padrao de 100%).
   *
   * @param nome nome do golpe.
   * @param poder valor base de dano do golpe.
   * @param tipo tipo elemental do golpe.
   */
  public Golpe(String nome, int poder, TipoElemental tipo) {
    this(nome, poder, tipo, null, PRECISAO_MAXIMA, false, 0.0);
  }

  /**
   * Executa o golpe retornando o valor base de poder.
   *
   * @return valor de poder do golpe.
   */
  public int atacar() {
    return this.poder;
  }

  /**
   * Retorna o nome do golpe.
   *
   * @return nome do golpe.
   */
  public String getNome() {
    return nome;
  }

  /**
   * Retorna o poder base do golpe.
   *
   * @return poder base.
   */
  public int getPoder() {
    return poder;
  }

  /**
   * Retorna o tipo elemental do golpe.
   *
   * @return tipo elemental.
   */
  public TipoElemental getTipo() {
    return tipo;
  }

  /**
   * Retorna o efeito de status associado ao golpe, se houver.
   *
   * @return efeito de status ou null se nao houver.
   */
  public EfeitoDeStatusGolpe getEfeitoDeStatus() {
    return efeitoDeStatus;
  }

  /**
   * Retorna a precisao do golpe na escala de 0 a 100.
   *
   * @return precisao do golpe.
   */
  public int getPrecisao() {
    return precisao;
  }

  /**
   * Indica se o golpe possui propriedade vampirica de absorcao de vitalidade (RA-F03-02).
   *
   * @return true se for golpe de dreno, false caso contrario.
   */
  public boolean isGolpeDreno() {
    return golpeDreno;
  }

  /**
   * Retorna a taxa de conversao de dano em cura para golpes de dreno.
   *
   * @return taxa de conversao (ex: 0.5).
   */
  public double getTaxaConversaoDreno() {
    return taxaConversaoDreno;
  }
}
