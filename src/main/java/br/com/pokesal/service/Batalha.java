package br.com.pokesal.service;

import br.com.pokesal.model.Golpe;
import br.com.pokesal.model.ItemDeBatalha;
import br.com.pokesal.model.Pokesal;
import br.com.pokesal.model.ResultadoBatalha;
import br.com.pokesal.model.StatusTorcida;
import br.com.pokesal.model.Terreno;
import br.com.pokesal.model.TipoElemental;
import br.com.pokesal.model.TipoStatus;
import br.com.pokesal.model.Treinador;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mediador e maquina de estados responsavel por executar as regras de combate entre treinadores.
 */
public class Batalha {
  /** Limite maximo regulamentar de turnos da batalha (5 acoes para cada treinador). */
  public static final int LIMITE_MAXIMO_TURNOS = 10;

  /** Dano minimo garantido ao desferir um golpe bem-sucedido. */
  public static final int DANO_MINIMO_FINAL = 1;

  /** Multiplicador do segundo golpe nos catalogos de golpes. */
  public static final double MULTIPLICADOR_SEGUNDO_GOLPE = 0.5;

  /** Probabilidade de ativacao de status para golpes secundarios. */
  public static final double CHANCE_STATUS_SEGUNDO_GOLPE = 0.8;

  /** Indice do primeiro item da mochila para acao padrao. */
  public static final int INDICE_PRIMEIRO_ITEM = 0;

  /** Limiar neutro de modificador de terreno. */
  public static final double MODIFICADOR_NEUTRO_LIMIAR = 1.0;

  /** Multiplicador de dano super efetivo. */
  public static final double MODIFICADOR_SUPER_EFETIVO = 2.0;

  /** Multiplicador de dano pouco efetivo. */
  public static final double MODIFICADOR_POUCO_EFETIVO = 0.5;

  /** Dano minimo inicial antes de aplicacao de multiplicadores. */
  public static final int DANO_MINIMO_BASE = 1;

  /** Dano nulo retornado quando uma acao nao e concretizada ou o golpe erra. */
  public static final int DANO_NULO = 0;

  /** Chance regulamentar de acerto critico (6,25%). */
  public static final double CHANCE_ACERTO_CRITICO = 0.0625;

  /** Multiplicador de dano aplicado quando ocorre acerto critico. */
  public static final double MULTIPLICADOR_CRITICO = 1.5;

  /** Escala percentual maxima para calculo de precisao dos golpes. */
  public static final double ESCALA_PRECISAO_MAXIMA = 100.0;

  /** Limiar de HP (30%) para acionamento da torcida universitaria (RA-F03-01). */
  public static final double LIMIAR_HP_TORCIDA = 0.30;

  /** Multiplicador de dano adicional do buff Vigor de Pituacu (+15%). */
  public static final double MULTIPLICADOR_VIGOR_PITUACU = 1.15;

  private final Treinador treinador1;
  private final Treinador treinador2;
  private final Terreno terrenoDaBatalha;
  private final GeradorAleatorio geradorAleatorio;
  private final ServicoTerreno servicoTerreno;
  private final Clock clock;
  private final RepositorioResultados repositorioResultados;
  private final RegistroBatalha registroBatalha;
  private final ServicoTorcida servicoTorcida;
  private final ProcessadorDreno processadorDreno;
  private final List<String> errosRegistrados;
  private int turnoDaBatalha;
  private Treinador vencedor;
  private boolean encerrada;
  private boolean resultadoSalvo;

  /**
   * Construtor completo da batalha com injecao de todos os colaboradores externos (DIP).
   *
   * @param treinador1 primeiro treinador competidor.
   * @param treinador2 segundo treinador competidor.
   * @param terreno arena selecionada para o confronto.
   * @param geradorAleatorio fonte de aleatoriedade para precisao, critico, status e desempate.
   * @param servicoTerreno servico dinamico de avaliacao ambiental do terreno.
   * @param clock relogio de referencia para regras dependentes de horario.
   * @param repositorioResultados repositorio para persistencia do resultado da batalha.
   * @param registroBatalha registrador sequencial de eventos de turno.
   * @param servicoTorcida servico da torcida universitaria da UCSal (RA-F03-01).
   * @param processadorDreno processador de cura por absorcao de vitalidade (RA-F03-02).
   */
  public Batalha(Treinador treinador1,
                 Treinador treinador2,
                 Terreno terreno,
                 GeradorAleatorio geradorAleatorio,
                 ServicoTerreno servicoTerreno,
                 Clock clock,
                 RepositorioResultados repositorioResultados,
                 RegistroBatalha registroBatalha,
                 ServicoTorcida servicoTorcida,
                 ProcessadorDreno processadorDreno) {
    this.treinador1 = treinador1;
    this.treinador2 = treinador2;
    this.terrenoDaBatalha = terreno;
    this.geradorAleatorio = geradorAleatorio;
    this.servicoTerreno = servicoTerreno;
    this.clock = clock;
    this.repositorioResultados = repositorioResultados;
    this.registroBatalha = registroBatalha;
    this.servicoTorcida = servicoTorcida;
    this.processadorDreno = processadorDreno;
    this.errosRegistrados = new ArrayList<>();
    this.turnoDaBatalha = 0;
    this.encerrada = false;
    this.resultadoSalvo = false;
  }

  /**
   * Executa um ataque direto entre duas criaturas (compatibilidade UML).
   *
   * @param atacante a criatura que desfere o ataque.
   * @param defensor a criatura que recebe o impacto.
   * @param golpe o movimento de combate utilizado.
   */
  public void atacar(Pokesal atacante, Pokesal defensor, Golpe golpe) {
    if (encerrada || atacante.estaDesmaiado()) {
      return;
    }
    if (!verificarAcerto(golpe)) {
      return;
    }
    boolean critico = verificarCritico();
    boolean comVigor = atacante.consumirVigorDePituacu();
    int dano = calcularDano(atacante, defensor, golpe, critico, comVigor);
    defensor.receberDano(dano);

    aplicarEfeitoStatusDoGolpe(defensor, golpe);
    processarDreno(atacante, golpe, dano);
    avaliarTorcidaEmPerigo(defensor, dano);
    verificarHp(treinador1, treinador2);
  }

  /**
   * Executa a acao de ataque de um competidor consumindo um turno de combate.
   *
   * @param atacante treinador que ordenou o ataque.
   * @param defensor treinador que recebe a ofensiva.
   * @param golpe movimento executado pelo Pokesal atacante.
   * @return quantidade de dano calculada e infligida (0 se o golpe errar).
   */
  public int executarAcaoDeAtaque(Treinador atacante, Treinador defensor, Golpe golpe) {
    if (encerrada || atacante.getPokesal().estaDesmaiado()) {
      return DANO_NULO;
    }
    this.turnoDaBatalha++;

    if (registroBatalha != null) {
      registroBatalha.registrarAtaque(atacante, defensor, golpe);
    }

    if (!verificarAcerto(golpe)) {
      if (registroBatalha != null) {
        registroBatalha.registrarDano(atacante, defensor, DANO_NULO, false);
      }
      System.out.printf("  [ERROU] %s errou o golpe %s!%n",
          atacante.getPokesal().getNome(), golpe.getNome());
      if (this.turnoDaBatalha >= LIMITE_MAXIMO_TURNOS && !encerrada) {
        pontuarVencedor();
      }
      return DANO_NULO;
    }

    boolean critico = verificarCritico();
    boolean comVigor = atacante.getPokesal().consumirVigorDePituacu();
    int dano = calcularDano(
        atacante.getPokesal(), defensor.getPokesal(), golpe, critico, comVigor);
    defensor.getPokesal().receberDano(dano);

    if (registroBatalha != null) {
      registroBatalha.registrarDano(atacante, defensor, dano, critico);
    }
    if (critico) {
      System.out.println("  [CRITICO] Acerto critico! Dano multiplicado por 1.5x!");
    }

    aplicarEfeitoStatusDoGolpe(defensor.getPokesal(), golpe);
    notificarEfeitosDeAtaque(atacante.getPokesal(), defensor.getPokesal(), golpe);
    processarDreno(atacante.getPokesal(), golpe, dano);
    avaliarTorcidaEmPerigo(defensor.getPokesal(), dano);

    verificarHp(treinador1, treinador2);
    if (this.turnoDaBatalha >= LIMITE_MAXIMO_TURNOS && !encerrada) {
      pontuarVencedor();
    }
    return dano;
  }

  /**
   * Executa uma rodada completa respeitando iniciativa SPD e encerrando se houver desmaio.
   *
   * @param golpeTreinador1 golpe escolhido pelo treinador 1.
   * @param golpeTreinador2 golpe escolhido pelo treinador 2.
   */
  public void executarRodada(Golpe golpeTreinador1, Golpe golpeTreinador2) {
    if (encerrada) {
      return;
    }

    Treinador primeiro = determinarPrimeiroAtacante();
    Treinador segundo = (primeiro == treinador1) ? treinador2 : treinador1;
    Golpe golpePrimeiro = (primeiro == treinador1) ? golpeTreinador1 : golpeTreinador2;
    Golpe golpeSegundo = (segundo == treinador1) ? golpeTreinador1 : golpeTreinador2;

    executarAcaoDeAtaque(primeiro, segundo, golpePrimeiro);

    if (!encerrada && !segundo.getPokesal().estaDesmaiado()) {
      executarAcaoDeAtaque(segundo, primeiro, golpeSegundo);
    }

    finalizarTurno();
  }

  /**
   * Executa um turno individual de combate com finalizacao de efeitos.
   *
   * @param atacante treinador em acao.
   * @param defensor treinador alvo.
   * @param golpe movimento executado.
   */
  public void executarTurno(Treinador atacante, Treinador defensor, Golpe golpe) {
    if (encerrada) {
      return;
    }
    executarAcaoDeAtaque(atacante, defensor, golpe);
    finalizarTurno();
  }

  /**
   * Consome o primeiro item disponivel da mochila do treinador (compatibilidade UML).
   *
   * @param treinador o competidor que utiliza o item.
   */
  public void usarItemdeBatalha(Treinador treinador) {
    consumirItemNoTurno(treinador, INDICE_PRIMEIRO_ITEM);
  }

  /**
   * Consome um item especifico da mochila, consumindo a acao do turno e validando limites.
   *
   * @param treinador o competidor que utiliza o item.
   * @param indiceItem o indice do item na mochila.
   * @throws LimiteItensExcedidosException se o treinador ja tiver usado 2 itens na batalha.
   */
  public void consumirItemNoTurno(Treinador treinador, int indiceItem) {
    if (encerrada) {
      return;
    }
    if (treinador.getContItensUsados() >= Treinador.LIMITE_MAXIMO_ITENS) {
      throw new LimiteItensExcedidosException(
          "Limite excedido! O treinador so pode usar ate 2 itens por batalha.");
    }
    ItemDeBatalha item = (indiceItem >= 0 && indiceItem < treinador.getMochila().size())
        ? treinador.getMochila().get(indiceItem) : null;
    this.turnoDaBatalha++;
    treinador.usarItem(indiceItem);
    if (item != null && registroBatalha != null) {
      registroBatalha.registrarUsoItem(treinador, item);
    }
    if (this.turnoDaBatalha >= LIMITE_MAXIMO_TURNOS && !encerrada) {
      pontuarVencedor();
    }
  }

  /**
   * Calcula o dano base final do golpe sem modificadores de critico ou vigor.
   *
   * @param atacante Pokesal que desfere o ataque.
   * @param defensor Pokesal alvo do ataque.
   * @param golpe movimento ofensivo utilizado.
   * @return dano inteiro final garantindo no minimo 1 de dano.
   */
  public int calcularDano(Pokesal atacante, Pokesal defensor, Golpe golpe) {
    return calcularDano(atacante, defensor, golpe, false, false);
  }

  /**
   * Calcula o dano final do golpe considerando defesa, tipos, terreno, item, critico e torcida.
   *
   * @param atacante Pokesal que desfere o ataque.
   * @param defensor Pokesal alvo do ataque.
   * @param golpe movimento ofensivo utilizado.
   * @param critico indica se o golpe foi um acerto critico (1.5x).
   * @param comVigorPituacu indica se o atacante possui o buff Vigor de Pituacu (+15%).
   * @return dano inteiro final garantindo no minimo 1 de dano.
   */
  public int calcularDano(Pokesal atacante, Pokesal defensor, Golpe golpe,
                          boolean critico, boolean comVigorPituacu) {
    int diferencaAtkDef = atacante.getAtk() - defensor.getDef();
    int baseDano = Math.max(DANO_MINIMO_BASE, diferencaAtkDef + golpe.getPoder());

    double modElemental = golpe.getTipo().obterMultiplicadorContra(defensor.getTipoElemental());
    double modTerreno = obterModificadorTerrenoAtual(golpe.getTipo());

    double danoCalculado = baseDano * modElemental * modTerreno;

    int efeitoHeldItem = 0;
    if (atacante.getHeldItem() != null && atacante.getHeldItem().getEfeitoDeStatus() != null) {
      EfeitoDeStatusHeld efeito = atacante.getHeldItem().getEfeitoDeStatus();
      if (efeito.getTipoBeneficiado() == null || efeito.getTipoBeneficiado() == golpe.getTipo()) {
        efeitoHeldItem = efeito.getBonusDanoAtaque();
        danoCalculado *= efeito.getMultiplicadorElemental();
      }
    }

    int danoBaseComItem = (int) Math.round(danoCalculado) + efeitoHeldItem;
    double danoTotal = danoBaseComItem;
    if (critico) {
      danoTotal *= MULTIPLICADOR_CRITICO;
    }
    if (comVigorPituacu) {
      danoTotal *= MULTIPLICADOR_VIGOR_PITUACU;
    }

    int danoFinal = (int) Math.round(danoTotal);
    return Math.max(DANO_MINIMO_FINAL, danoFinal);
  }

  /**
   * Determina qual treinador tem a prioridade de agir com base na velocidade SPD.
   *
   * @return o treinador cujo Pokesal possui maior velocidade (ou sorteio se empatado).
   */
  public Treinador determinarPrimeiroAtacante() {
    int spd1 = treinador1.getPokesal().getSpd();
    int spd2 = treinador2.getPokesal().getSpd();

    if (spd1 > spd2) {
      return treinador1;
    } else if (spd2 > spd1) {
      return treinador2;
    }
    if (geradorAleatorio != null) {
      return geradorAleatorio.gerarBooleano() ? treinador1 : treinador2;
    }
    return treinador1;
  }

  /**
   * Processa os efeitos de fim de turno (status, regeneracao de terreno e itens segurados).
   */
  public void finalizarTurno() {
    processarFimDeTurnoPokesal(treinador1.getPokesal());
    processarFimDeTurnoPokesal(treinador2.getPokesal());

    aplicarRegeneracaoTerrenoNoTurno(treinador1.getPokesal());
    aplicarRegeneracaoTerrenoNoTurno(treinador2.getPokesal());

    verificarHp(treinador1, treinador2);

    if (this.turnoDaBatalha >= LIMITE_MAXIMO_TURNOS && !encerrada) {
      pontuarVencedor();
    }
  }

  /**
   * Avalia a condicao de desmaio das criaturas e encerra o combate se houver nocaute.
   *
   * @param t1 primeiro treinador avaliado.
   * @param t2 segundo treinador avaliado.
   */
  public void verificarHp(Treinador t1, Treinador t2) {
    if (t1.getPokesal().estaDesmaiado() && t2.getPokesal().estaDesmaiado()) {
      encerrada = true;
      vencedor = null;
      persistirResultadoFinal();
    } else if (t1.getPokesal().estaDesmaiado()) {
      encerrada = true;
      vencedor = t2;
      persistirResultadoFinal();
    } else if (t2.getPokesal().estaDesmaiado()) {
      encerrada = true;
      vencedor = t1;
      persistirResultadoFinal();
    }
  }

  /**
   * Determina o vencedor por desempate de HP remanescente ao atingir o limite maximo de turnos.
   */
  public void pontuarVencedor() {
    encerrada = true;
    int hp1 = treinador1.getPokesal().getHpAtual();
    int hp2 = treinador2.getPokesal().getHpAtual();

    if (hp1 > hp2) {
      vencedor = treinador1;
    } else if (hp2 > hp1) {
      vencedor = treinador2;
    } else {
      vencedor = null;
    }
    persistirResultadoFinal();
  }

  private boolean verificarAcerto(Golpe golpe) {
    if (geradorAleatorio != null) {
      double rolagemPrecisao = geradorAleatorio.gerarDouble();
      if (golpe.getPrecisao() <= Golpe.PRECISAO_MINIMA) {
        return false;
      }
      if (golpe.getPrecisao() < Golpe.PRECISAO_MAXIMA) {
        return (rolagemPrecisao * ESCALA_PRECISAO_MAXIMA) < golpe.getPrecisao();
      }
      return true;
    }
    return golpe.getPrecisao() > Golpe.PRECISAO_MINIMA;
  }

  private boolean verificarCritico() {
    return geradorAleatorio != null
        && geradorAleatorio.gerarDouble() < CHANCE_ACERTO_CRITICO;
  }

  private void aplicarEfeitoStatusDoGolpe(Pokesal defensor, Golpe golpe) {
    if (golpe.getEfeitoDeStatus() == null || geradorAleatorio == null) {
      return;
    }
    double rolagem = geradorAleatorio.gerarDouble();
    if (rolagem <= golpe.getEfeitoDeStatus().getChanceDeAtivacao()) {
      TipoStatus statusAntes = defensor.getStatusAtual();
      golpe.getEfeitoDeStatus().aplicar(defensor);
      if (statusAntes == TipoStatus.NENHUM
          && defensor.getStatusAtual() != TipoStatus.NENHUM
          && registroBatalha != null) {
        registroBatalha.registrarEfeitoStatus(defensor, defensor.getStatusAtual());
      }
    }
  }

  private void notificarEfeitosDeAtaque(Pokesal atacante, Pokesal defensor, Golpe golpe) {
    double modTerreno = obterModificadorTerrenoAtual(golpe.getTipo());
    if (modTerreno > MODIFICADOR_NEUTRO_LIMIAR) {
      System.out.printf(
          "  [TERRENO] O terreno '%s' potencializou o golpe %s em %d%%!%n",
          terrenoDaBatalha.getNome(), golpe.getNome(), terrenoDaBatalha.aplicarEfeito());
      if (registroBatalha != null) {
        registroBatalha.registrarEfeitoTerreno(terrenoDaBatalha, atacante);
      }
    }

    double modElemental = golpe.getTipo().obterMultiplicadorContra(defensor.getTipoElemental());
    if (modElemental >= MODIFICADOR_SUPER_EFETIVO) {
      System.out.printf(
          "  [VANTAGEM ELEMENTAL] Golpe SUPER EFETIVO contra %s! (Multiplicador x2.0)%n",
          defensor.getNome());
    } else if (modElemental <= MODIFICADOR_POUCO_EFETIVO) {
      System.out.printf(
          "  [DESVANTAGEM ELEMENTAL] Golpe POUCO EFETIVO contra %s... (Multiplicador x0.5)%n",
          defensor.getNome());
    }
  }

  private void processarDreno(Pokesal atacante, Golpe golpe, int dano) {
    if (dano > DANO_NULO && golpe.isGolpeDreno() && processadorDreno != null) {
      int cura = processadorDreno.calcularCura(dano, golpe.getTaxaConversaoDreno());
      if (cura > 0) {
        int hpAntes = atacante.getHpAtual();
        atacante.curar(cura);
        int curado = atacante.getHpAtual() - hpAntes;
        if (curado > 0) {
          System.out.printf(
              "  [DRENO] %s absorveu +%d HP com %s! (HP atual: %d/%d)%n",
              atacante.getNome(), curado, golpe.getNome(),
              atacante.getHpAtual(), atacante.getHpMax());
        }
      }
    }
  }

  private void avaliarTorcidaEmPerigo(Pokesal alvo, int dano) {
    if (dano > DANO_NULO && !alvo.estaDesmaiado()
        && alvo.getHpAtual() <= (alvo.getHpMax() * LIMIAR_HP_TORCIDA)
        && servicoTorcida != null) {
      try {
        StatusTorcida statusTorcida = servicoTorcida.aferirVibracao(alvo);
        if (statusTorcida == StatusTorcida.EMPOLGADA) {
          alvo.ativarVigorDePituacu();
          System.out.printf(
              "  [TORCIDA] Arquibancada EMPOLGADA! %s recebeu Vigor de Pituacu (+15%% dano)!%n",
              alvo.getNome());
        }
      } catch (RuntimeException e) {
        registrarErroColaborador("Falha ao consultar ServicoTorcida: " + e.getMessage());
      }
    }
  }

  private void processarFimDeTurnoPokesal(Pokesal pokesal) {
    if (pokesal.estaDesmaiado()) {
      return;
    }
    TipoStatus status = pokesal.getStatusAtual();
    pokesal.processarEfeitosFimDeTurno();
    if ((status == TipoStatus.QUEIMADO || status == TipoStatus.ENVENENADO)
        && registroBatalha != null) {
      registroBatalha.registrarEfeitoStatus(pokesal, status);
    }
  }

  private void aplicarRegeneracaoTerrenoNoTurno(Pokesal pokesal) {
    if (terrenoDaBatalha == null || terrenoDaBatalha.getEfeitoDeStatus() == null) {
      return;
    }
    LocalDateTime agora = obterInstanteAtual();
    boolean ativo = (servicoTerreno == null)
        || servicoTerreno.isEfeitoAtivo(terrenoDaBatalha, agora);
    if (!ativo) {
      return;
    }
    int hpAntes = pokesal.getHpAtual();
    terrenoDaBatalha.getEfeitoDeStatus().aplicar(pokesal);
    int cura = pokesal.getHpAtual() - hpAntes;
    if (cura > 0) {
      System.out.printf(
          "  [TERRENO] O terreno '%s' regenerou +%d HP de %s (tipo %s)! (HP atual: %d/%d)%n",
          terrenoDaBatalha.getNome(), cura, pokesal.getNome(),
          pokesal.getTipoElemental(), pokesal.getHpAtual(), pokesal.getHpMax());
      if (registroBatalha != null) {
        registroBatalha.registrarEfeitoTerreno(terrenoDaBatalha, pokesal);
      }
    }
  }

  private double obterModificadorTerrenoAtual(TipoElemental tipoGolpe) {
    if (servicoTerreno != null) {
      return servicoTerreno.obterModificadorDano(
          terrenoDaBatalha, tipoGolpe, obterInstanteAtual());
    }
    if (terrenoDaBatalha != null) {
      return terrenoDaBatalha.obterModificadorDano(tipoGolpe);
    }
    return EfeitoDeStatusTerreno.MULTIPLICADOR_NEUTRO;
  }

  private LocalDateTime obterInstanteAtual() {
    return (clock != null) ? LocalDateTime.now(clock) : null;
  }

  private void persistirResultadoFinal() {
    if (resultadoSalvo || repositorioResultados == null) {
      return;
    }
    this.resultadoSalvo = true;
    Treinador perdedor = null;
    if (vencedor == treinador1) {
      perdedor = treinador2;
    } else if (vencedor == treinador2) {
      perdedor = treinador1;
    }
    ResultadoBatalha resultado = new ResultadoBatalha(
        vencedor,
        perdedor,
        turnoDaBatalha,
        treinador1.getPokesal(),
        treinador2.getPokesal());
    try {
      repositorioResultados.salvar(resultado);
    } catch (RuntimeException e) {
      registrarErroColaborador("Falha ao salvar resultado no repositorio: " + e.getMessage());
    }
  }

  private void registrarErroColaborador(String mensagem) {
    this.errosRegistrados.add(mensagem);
    System.err.println("  [ERRO COLABORADOR] " + mensagem);
  }

  /**
   * Retorna o turno atual da batalha.
   *
   * @return numero do turno da batalha.
   */
  public int getTurnoDaBatalha() {
    return turnoDaBatalha;
  }

  /**
   * Retorna o turno corrente da batalha.
   *
   * @return numero do turno.
   */
  public int getTurnoAtual() {
    return turnoDaBatalha;
  }

  /**
   * Retorna a referencia ao primeiro treinador da batalha.
   *
   * @return treinador 1.
   */
  public Treinador getTreinador1() {
    return treinador1;
  }

  /**
   * Retorna a referencia ao segundo treinador da batalha.
   *
   * @return treinador 2.
   */
  public Treinador getTreinador2() {
    return treinador2;
  }

  /**
   * Retorna o terreno associado a batalha.
   *
   * @return terreno da arena.
   */
  public Terreno getTerreno() {
    return terrenoDaBatalha;
  }

  /**
   * Retorna o terreno (nome mantido por compatibilidade com modelagem UML).
   *
   * @return terreno da arena.
   */
  public Terreno getTerredoDaBatalha() {
    return terrenoDaBatalha;
  }

  /**
   * Retorna o terreno da batalha.
   *
   * @return terreno da arena.
   */
  public Terreno getTerrenoDaBatalha() {
    return terrenoDaBatalha;
  }

  /**
   * Retorna o treinador vencedor do confronto, ou null em caso de empate.
   *
   * @return treinador vencedor ou null.
   */
  public Treinador getVencedor() {
    return vencedor;
  }

  /**
   * Indica se a batalha ja foi encerrada por desmaio ou limite de turnos.
   *
   * @return true se encerrada, false se ainda em andamento.
   */
  public boolean isEncerrada() {
    return encerrada;
  }

  /**
   * Retorna a lista imutavel de erros de infraestrutura registrados sem invalidar a batalha.
   *
   * @return lista de mensagens de erro registradas.
   */
  public List<String> getErrosRegistrados() {
    return Collections.unmodifiableList(errosRegistrados);
  }
}
