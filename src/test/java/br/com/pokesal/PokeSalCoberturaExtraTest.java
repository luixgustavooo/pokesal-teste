package br.com.pokesal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.pokesal.model.Golpe;
import br.com.pokesal.model.Pokesal;
import br.com.pokesal.model.ResultadoBatalha;
import br.com.pokesal.model.StatusTorcida;
import br.com.pokesal.model.Terreno;
import br.com.pokesal.model.TipoElemental;
import br.com.pokesal.model.TipoStatus;
import br.com.pokesal.model.Treinador;
import br.com.pokesal.service.Batalha;
import br.com.pokesal.service.EfeitoDeStatusGolpe;
import br.com.pokesal.service.ExecutorDeBatalha;
import br.com.pokesal.service.GeradorAleatorio;
import br.com.pokesal.service.ProcessadorDreno;
import br.com.pokesal.service.RegistroBatalha;
import br.com.pokesal.service.RepositorioResultados;
import br.com.pokesal.service.ServicoTerreno;
import br.com.pokesal.service.ServicoTorcida;
import br.com.pokesal.service.SimuladorDeBatalha;
import br.com.pokesal.service.Torneio;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Suite complementar de testes unitarios de cobertura (JaCoCo).
 * Exercita casos de borda, ramificacoes if/else alternativas e robustez
 * das classes Batalha, Torneio, SimuladorDeBatalha e modelos.
 */
@ExtendWith(MockitoExtension.class)
public class PokeSalCoberturaExtraTest {

  private static final int HP_BASE = 100;
  private static final int ATK_BASE = 30;
  private static final int DEF_BASE = 20;
  private static final int SPD_ALTO = 40;
  private static final int SPD_BAIXO = 20;
  private static final int PODER_GOLPE = 30;

  private static final double ROLAGEM_ACERTO_GARANTIDO = 0.10;
  private static final double ROLAGEM_SEM_CRITICO = 0.50;

  private static final Instant INSTANTE_DIA = Instant.parse("2026-11-12T12:00:00Z");

  @Mock
  private GeradorAleatorio geradorAleatorio;

  @Mock
  private ServicoTerreno servicoTerreno;

  @Mock
  private RepositorioResultados repositorioResultados;

  @Mock
  private RegistroBatalha registroBatalha;

  @Mock
  private ServicoTorcida servicoTorcida;

  @Mock
  private ProcessadorDreno processadorDreno;

  @Mock
  private ExecutorDeBatalha executorDeBatalha;

  private Clock clock;
  private Treinador treinador1;
  private Treinador treinador2;
  private Terreno terrenoNeutro;

  /**
   * Inicializa o cenario de teste antes de cada metodo.
   */
  @BeforeEach
  public void setUp() {
    clock = Clock.fixed(INSTANTE_DIA, ZoneOffset.UTC);
    treinador1 = new Treinador("Treinador Alfa");
    treinador2 = new Treinador("Treinador Beta");
    terrenoNeutro = new Terreno("Arena Neutra UCSal", null);
  }

  private Pokesal criarPokesalPadrao(String nome, TipoElemental tipo, int spd) {
    Golpe g1 = new Golpe("Ataque Basico", PODER_GOLPE, tipo, null, Golpe.PRECISAO_MAXIMA);
    Golpe g2 = new Golpe("Golpe Status", 20, tipo,
        new EfeitoDeStatusGolpe(TipoStatus.ENVENENADO, 0.8), 90);
    Golpe g3 = new Golpe("Golpe Auxiliar", 25, tipo, null, Golpe.PRECISAO_MAXIMA);
    return new Pokesal(nome, HP_BASE, ATK_BASE, DEF_BASE, spd, tipo, Arrays.asList(g1, g2, g3));
  }

  private Batalha instanciarBatalhaPadrao(Terreno terreno) {
    return new Batalha(
        treinador1,
        treinador2,
        terreno,
        geradorAleatorio,
        servicoTerreno,
        clock,
        repositorioResultados,
        registroBatalha,
        servicoTorcida,
        processadorDreno);
  }

  /**
   * Valida resiliencia quando ServicoTorcida lanca excecao:
   * o erro e capturado, registrado em log e o combate prossegue normalmente.
   */
  @Test
  @DisplayName("Extra 01 - Falha no ServicoTorcida nao interrompe o combate (Resiliencia)")
  public void testFalhaNoServicoTorcidaNaoTravaCombate() {
    Pokesal atacante = criarPokesalPadrao("CharSal", TipoElemental.FOGO, SPD_ALTO);
    Pokesal defensor = criarPokesalPadrao("CyndaSal", TipoElemental.FOGO, SPD_BAIXO);
    treinador1.setPokesal(atacante);
    treinador2.setPokesal(defensor);

    Golpe golpePesado = new Golpe("Super Pancada", 60, TipoElemental.FOGO,
        null, Golpe.PRECISAO_MAXIMA);

    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);
    doThrow(new RuntimeException("Microservico de torcida indisponivel"))
        .when(servicoTorcida).aferirVibracao(any());

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    int dano = batalha.executarAcaoDeAtaque(treinador1, treinador2, golpePesado);

    assertEquals(70, dano);
    assertFalse(batalha.isEncerrada());
    assertFalse(batalha.getErrosRegistrados().isEmpty(),
        "Erro de conexao com a torcida deve constar na lista de erros registrados.");
  }

  /**
   * Valida que o buff Vigor de Pituacu amplifica o proximo golpe em 15%
   * e e consumido imediatamente apos o ataque.
   */
  @Test
  @DisplayName("Extra 02 - Consumo do Buff Vigor de Pituacu no proximo golpe ofensivo")
  public void testConsumoDoBuffVigorDePituacuNoProximoGolpe() {
    Pokesal atacante = criarPokesalPadrao("CharSal", TipoElemental.FOGO, SPD_ALTO);
    Pokesal defensor = criarPokesalPadrao("CyndaSal", TipoElemental.FOGO, SPD_BAIXO);
    treinador1.setPokesal(atacante);
    treinador2.setPokesal(defensor);

    atacante.ativarVigorDePituacu();
    assertTrue(atacante.isVigorDePituacuAtivo());

    Golpe golpe = atacante.getGolpes().get(0);
    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    int danoComBuff = batalha.executarAcaoDeAtaque(treinador1, treinador2, golpe);

    assertEquals(46, danoComBuff, "Golpe sob Vigor de Pituacu deve receber 15% de dano extra.");
    assertFalse(atacante.isVigorDePituacuAtivo(), "O buff deve ser consumido apos o ataque.");
  }

  /**
   * Valida metodos de rodada, turno individual e ataque direto na Batalha.
   */
  @Test
  @DisplayName("Extra 03 - Fluxos de rodada, turno individual e ataque direto")
  public void testExecutarRodadaETurnoEAtacar() {
    Pokesal rapido = criarPokesalPadrao("Rapido", TipoElemental.FOGO, SPD_ALTO);
    Pokesal lento = criarPokesalPadrao("Lento", TipoElemental.AGUA, SPD_BAIXO);
    treinador1.setPokesal(rapido);
    treinador2.setPokesal(lento);

    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);

    batalha.atacar(rapido, lento, rapido.getGolpes().get(0));
    assertTrue(lento.getHpAtual() < HP_BASE);

    batalha.executarTurno(treinador1, treinador2, rapido.getGolpes().get(0));

    batalha.executarRodada(rapido.getGolpes().get(0), lento.getGolpes().get(0));
    assertTrue(batalha.getTurnoDaBatalha() > 0);
  }

  /**
   * Valida os caminhos de decisao de vencedor por HP, empate e nocaute mutuo.
   */
  @Test
  @DisplayName("Extra 04 - Verificacao de HP com vitoria, empate e nocaute mutuo")
  public void testDesfechosDeVitoriaEmpateENocauteMutuo() {
    Pokesal p1 = criarPokesalPadrao("P1", TipoElemental.FOGO, SPD_ALTO);
    Pokesal p2 = criarPokesalPadrao("P2", TipoElemental.AGUA, SPD_BAIXO);
    treinador1.setPokesal(p1);
    treinador2.setPokesal(p2);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);

    p1.receberDano(999);
    batalha.verificarHp(treinador1, treinador2);
    assertTrue(batalha.isEncerrada());
    assertEquals(treinador2, batalha.getVencedor());

    p2.receberDano(999);
    Batalha batalhaMutua = instanciarBatalhaPadrao(terrenoNeutro);
    batalhaMutua.verificarHp(treinador1, treinador2);
    assertTrue(batalhaMutua.isEncerrada());
    assertNull(batalhaMutua.getVencedor());

    p1.restaurarParaNovaBatalha();
    p2.restaurarParaNovaBatalha();
    p1.receberDano(40);
    Batalha batalhaT2 = instanciarBatalhaPadrao(terrenoNeutro);
    batalhaT2.pontuarVencedor();
    assertEquals(treinador2, batalhaT2.getVencedor());
  }

  /**
   * Valida o chaveamento de torneio com 8 treinadores (quartas, semifinal e final).
   */
  @Test
  @DisplayName("Extra 05 - Chaveamento completo de Torneio com 8 participantes")
  public void testTorneioOitoParticipantes() {
    List<Treinador> oito = Arrays.asList(
        new Treinador("T1"), new Treinador("T2"), new Treinador("T3"), new Treinador("T4"),
        new Treinador("T5"), new Treinador("T6"), new Treinador("T7"), new Treinador("T8")
    );
    for (int i = 0; i < oito.size(); i++) {
      oito.get(i).setPokesal(criarPokesalPadrao("P" + i, TipoElemental.FOGO, 20 + i));
    }

    Batalha batalhaVitoria = instanciarBatalhaPadrao(terrenoNeutro);
    batalhaVitoria.verificarHp(oito.get(0), oito.get(1));
    oito.get(1).getPokesal().receberDano(999);
    batalhaVitoria.verificarHp(oito.get(0), oito.get(1));

    when(executorDeBatalha.executar(any(), any(), any())).thenReturn(batalhaVitoria);

    Torneio torneio = new Torneio(executorDeBatalha, servicoTerreno, geradorAleatorio, clock);
    Treinador campeao = torneio.executarTorneio(oito);

    assertNotNull(campeao);
    verify(executorDeBatalha, times(7)).executar(any(), any(), any());
  }

  /**
   * Valida validacoes de lista e quantidade no Torneio (regras defensivas).
   */
  @Test
  @DisplayName("Extra 06 - Validacoes defensivas de participantes no Torneio")
  public void testValidacoesDeParticipantesTorneio() {
    Torneio torneio = new Torneio(executorDeBatalha, servicoTerreno, geradorAleatorio, clock);

    List<Treinador> tres = Arrays.asList(
        new Treinador("A"), new Treinador("B"), new Treinador("C"));
    assertThrows(IllegalArgumentException.class, () -> torneio.executarTorneio(tres));

    List<Treinador> quatroInvalido = Arrays.asList(
        new Treinador("A"), new Treinador("B"), new Treinador("C"), new Treinador("D"));
    assertThrows(IllegalArgumentException.class, () -> torneio.executarTorneio(quatroInvalido));
  }

  /**
   * Valida cadastros de torneio e sorteios em SimuladorDeBatalha.
   */
  @Test
  @DisplayName("Extra 07 - Cadastros de torneio e sorteios no SimuladorDeBatalha")
  public void testSimuladorDeBatalhaTorneioECatalogos() {
    SimuladorDeBatalha sim = new SimuladorDeBatalha(geradorAleatorio);

    sim.cadastrarTreinadoresTorneio(4, "Ash Sal");
    assertEquals(4, sim.getTreinadores().size());

    sim.cadastrarTreinadoresTorneio(8, "Ash Sal");
    assertEquals(8, sim.getTreinadores().size());

    assertThrows(IllegalArgumentException.class, () -> sim.cadastrarTreinadoresTorneio(6, "Ash"));

    when(geradorAleatorio.gerarInteiro(anyInt())).thenReturn(0);
    Treinador t = new Treinador("Teste");
    sim.sortearPokesal(t);
    assertNotNull(t.getPokesal());

    sim.sortearHeldItem(t);
    assertNotNull(t.getPokesal().getHeldItem());

    assertNotNull(sim.sortearTerreno());
    assertNotNull(SimuladorDeBatalha.obterCatalogoHeldItems());
    assertNotNull(SimuladorDeBatalha.obterCatalogoTerrenos());
  }

  /**
   * Valida metodos auxiliares de modelos Pokesal, Treinador e Golpe.
   */
  @Test
  @DisplayName("Extra 08 - Comportamentos auxiliares de Pokesal e Treinador")
  public void testModelosPokesalETreinadorAuxiliares() {
    Pokesal p = criarPokesalPadrao("Modelo", TipoElemental.FOGO, 30);
    p.curar(0);
    p.curarStatus();
    p.aplicarCondicaoStatus(TipoStatus.NENHUM);
    p.aplicarCondicaoStatus(TipoStatus.PARALISADO);
    p.aplicarCondicaoStatus(TipoStatus.QUEIMADO);
    assertEquals(TipoStatus.PARALISADO, p.getStatusAtual());
    assertTrue(p.temStatus());
    p.curarStatus();
    assertFalse(p.temStatus());

    Treinador t = new Treinador("T");
    t.incrementarContadorItensUtilizados();
    assertEquals(1, t.getItensUtilizados());
    t.reiniciarParaNovaBatalha();
    assertEquals(0, t.getItensUtilizados());
  }

  /**
   * Valida branches de combate na Batalha (limites, golpes elementais, itens e status).
   */
  @Test
  @DisplayName("Extra 09 - Branches especificos de Batalha (precisao zero, itens e NPE guard)")
  public void testBatalhaCoberturaDeBranches() {
    Pokesal atacante = criarPokesalPadrao("Atacante", TipoElemental.FOGO, SPD_ALTO);
    Pokesal defensor = criarPokesalPadrao("Defensor", TipoElemental.AGUA, SPD_BAIXO);
    treinador1.setPokesal(atacante);
    treinador2.setPokesal(defensor);

    Golpe golpePrecisaoZero = new Golpe("Golpe Nulo", 30, TipoElemental.FOGO, null, 0);
    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    assertEquals(Batalha.DANO_NULO, batalha.executarAcaoDeAtaque(
        treinador1, treinador2, golpePrecisaoZero));

    defensor.receberDano(999);
    batalha.verificarHp(treinador1, treinador2);
    assertTrue(batalha.isEncerrada());
    assertEquals(Batalha.DANO_NULO, batalha.executarAcaoDeAtaque(
        treinador1, treinador2, atacante.getGolpes().get(0)));
    batalha.atacar(atacante, defensor, atacante.getGolpes().get(0));
    batalha.consumirItemNoTurno(treinador1, 0);
    batalha.executarRodada(atacante.getGolpes().get(0), defensor.getGolpes().get(0));
    batalha.executarTurno(treinador1, treinador2, atacante.getGolpes().get(0));

    Batalha batalhaSemColaboradores = new Batalha(
        treinador1, treinador2, null, null, null, null, null, null, null, null);
    Pokesal vivo = criarPokesalPadrao("Vivo", TipoElemental.FOGO, 30);
    Pokesal alvo = criarPokesalPadrao("Alvo", TipoElemental.FOGO, 30);
    treinador1.setPokesal(vivo);
    treinador2.setPokesal(alvo);
    assertEquals(treinador1, batalhaSemColaboradores.determinarPrimeiroAtacante());
    assertTrue(batalhaSemColaboradores.calcularDano(vivo, alvo, vivo.getGolpes().get(0)) > 0);
    batalhaSemColaboradores.finalizarTurno();

    Batalha batalhaItem = instanciarBatalhaPadrao(terrenoNeutro);
    treinador1.adicionarItem(new br.com.pokesal.model.Potion());
    batalhaItem.usarItemdeBatalha(treinador1);
    assertEquals(1, treinador1.getContItensUsados());
  }

  /**
   * Valida branches de desempate e chaveamento no Torneio.
   */
  @Test
  @DisplayName("Extra 10 - Desempates e avanco em chaveamento no Torneio")
  public void testTorneioDesempatesEChaveamentoBranches() {
    Treinador t1 = new Treinador("T1");
    Treinador t2 = new Treinador("T2");
    Treinador t3 = new Treinador("T3");
    Treinador t4 = new Treinador("T4");

    t1.setPokesal(criarPokesalPadrao("P1", TipoElemental.FOGO, 50));
    t2.setPokesal(criarPokesalPadrao("P2", TipoElemental.AGUA, 20));
    t3.setPokesal(criarPokesalPadrao("P3", TipoElemental.PLANTA, 20));
    t4.setPokesal(criarPokesalPadrao("P4", TipoElemental.FOGO, 50));

    Batalha empateSemi1 = instanciarBatalhaPadrao(terrenoNeutro);
    Batalha empateSemi2 = instanciarBatalhaPadrao(terrenoNeutro);
    Batalha empateFinal = instanciarBatalhaPadrao(terrenoNeutro);

    when(executorDeBatalha.executar(any(), any(), any()))
        .thenReturn(empateSemi1, empateSemi2, empateFinal);

    Torneio torneio = new Torneio(executorDeBatalha, null, null, null);
    Treinador campeao = torneio.executarTorneio(Arrays.asList(t1, t2, t3, t4));
    assertNotNull(campeao);
  }

  /**
   * Valida cobertura de branches e casos extremos de Pokesal e Treinador.
   */
  @Test
  @DisplayName("Extra 11 - Casos extremos de dano, cura, status e bolsa do treinador")
  public void testPokesalETreinadorCasosExtremos() {
    Pokesal p = criarPokesalPadrao("Robusto", TipoElemental.PLANTA, 30);

    p.receberDano(0);
    p.receberDano(-10);
    assertEquals(HP_BASE, p.getHpAtual());

    p.receberDano(999);
    assertTrue(p.estaDesmaiado());
    p.curar(50);
    assertEquals(0, p.getHpAtual());

    p.restaurarParaNovaBatalha();
    p.aplicarCondicaoStatus(TipoStatus.ENVENENADO);
    p.processarEfeitosFimDeTurno();
    p.processarEfeitosFimDeTurno();
    assertTrue(p.getTurnosEnvenenado() > 1);

    p.restaurarParaNovaBatalha();
    p.aplicarCondicaoStatus(TipoStatus.QUEIMADO);
    p.processarEfeitosFimDeTurno();
    assertTrue(p.getHpAtual() < HP_BASE);

    p.setHeldItem(new br.com.pokesal.model.HeldItem("Leftovers",
        new br.com.pokesal.service.EfeitoDeStatusHeld(0, 5, null, 1.0)));
    int hpAntes = p.getHpAtual();
    p.processarEfeitosFimDeTurno();
    assertEquals(hpAntes - 1, p.getHpAtual());

    Treinador t = new Treinador("Treinador X");
    t.adicionarItem(null);
    t.usarItem(-1);
    t.usarItem(10);
    t.adicionarItem(new br.com.pokesal.model.Potion());
    t.usarItem(0);
    assertEquals(1, t.getContItensUsados());
  }

  /**
   * Valida branches adicionais em Batalha para atingir meta estrita de JaCoCo.
   */
  @Test
  @DisplayName("Extra 12 - Cobertura profunda de branches de iniciativa, limite e torcida")
  public void testBatalhaBranchesAdicionaisJaCoCo() {
    Pokesal p1 = criarPokesalPadrao("P1", TipoElemental.FOGO, 30);
    Pokesal p2 = criarPokesalPadrao("P2", TipoElemental.AGUA, 30);
    treinador1.setPokesal(p1);
    treinador2.setPokesal(p2);

    when(geradorAleatorio.gerarBooleano()).thenReturn(false);
    Batalha b = instanciarBatalhaPadrao(terrenoNeutro);
    assertEquals(treinador2, b.determinarPrimeiroAtacante());

    b.pontuarVencedor();
    assertTrue(b.isEncerrada());
    assertNull(b.getVencedor());

    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO, 0.95);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);
    Batalha bStatusFalha = instanciarBatalhaPadrao(terrenoNeutro);
    bStatusFalha.executarAcaoDeAtaque(treinador1, treinador2, p1.getGolpes().get(1));
    assertFalse(p2.temStatus());

    Batalha bTurno10 = instanciarBatalhaPadrao(terrenoNeutro);
    treinador1.adicionarItem(new br.com.pokesal.model.Potion());
    bTurno10.consumirItemNoTurno(treinador1, 99);
    for (int i = 0; i < 8; i++) {
      bTurno10.executarAcaoDeAtaque(treinador1, treinador2, p1.getGolpes().get(0));
    }
    bTurno10.consumirItemNoTurno(treinador1, 0);
    assertTrue(bTurno10.isEncerrada());

    Pokesal neutroAlvo = criarPokesalPadrao("NeutroAlvo", TipoElemental.FOGO, SPD_BAIXO);
    treinador2.setPokesal(neutroAlvo);
    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);
    when(servicoTorcida.aferirVibracao(any())).thenReturn(StatusTorcida.NEUTRA);
    Batalha bTorcidaNeutra = instanciarBatalhaPadrao(terrenoNeutro);
    bTorcidaNeutra.executarAcaoDeAtaque(treinador1, treinador2,
        new Golpe("Pancada", 60, TipoElemental.FOGO, null, Golpe.PRECISAO_MAXIMA));
    assertFalse(neutroAlvo.isVigorDePituacuAtivo());

    p1.setHeldItem(new br.com.pokesal.model.HeldItem("Gota de Agua",
        new br.com.pokesal.service.EfeitoDeStatusHeld(6, 0, TipoElemental.AGUA, 1.1)));
    Batalha bHeldDiferente = instanciarBatalhaPadrao(terrenoNeutro);
    assertTrue(bHeldDiferente.calcularDano(p1, p2, p1.getGolpes().get(0)) > 0);

    Pokesal fatal = new Pokesal("Fatal", HP_BASE, 999, DEF_BASE, SPD_ALTO, TipoElemental.FOGO,
        Arrays.asList(
            new Golpe("Fatal", 999, TipoElemental.FOGO, null, Golpe.PRECISAO_MAXIMA),
            new Golpe("G2", 10, TipoElemental.FOGO, null, Golpe.PRECISAO_MAXIMA),
            new Golpe("G3", 10, TipoElemental.FOGO, null, Golpe.PRECISAO_MAXIMA)));
    treinador2.setPokesal(fatal);
    Batalha bT2Vence = instanciarBatalhaPadrao(terrenoNeutro);
    bT2Vence.executarAcaoDeAtaque(treinador2, treinador1, fatal.getGolpes().get(0));
    assertTrue(bT2Vence.isEncerrada());
    assertEquals(treinador2, bT2Vence.getVencedor());

    assertNotNull(b.getTreinador1());
    assertNotNull(b.getTreinador2());
    assertNotNull(b.getTerreno());
    assertNotNull(b.getTerredoDaBatalha());
    assertNotNull(b.getTerrenoDaBatalha());
    assertEquals(b.getTurnoDaBatalha(), b.getTurnoAtual());
  }

  /**
   * Valida branches de SimuladorDeBatalha e ServicoTerrenoPadrao.
   */
  @Test
  @DisplayName("Extra 13 - Branches de cadastros nulos e fallback em servicos")
  public void testSimuladorEServicoTerrenoBranches() {
    SimuladorDeBatalha simSemGerador = new SimuladorDeBatalha(null);
    simSemGerador.adicionarTreinador(null);
    simSemGerador.associarItensMochila(null);

    Treinador t = new Treinador("SemPokesal");
    simSemGerador.sortearHeldItem(t);
    simSemGerador.sortearPokesal(t);
    assertNotNull(t.getPokesal());
    simSemGerador.sortearHeldItem(t);
    assertNotNull(t.getPokesal().getHeldItem());
    assertNotNull(simSemGerador.sortearTerreno());

    assertEquals(0, simSemGerador.sortearPrimeiraPartida().size());

    simSemGerador.cadastrarTreinadores();
    assertEquals(2, simSemGerador.getTreinadores().size());
    simSemGerador.cadastrarTreinadoresTorneio(4, "");
    assertEquals(4, simSemGerador.getTreinadores().size());

    br.com.pokesal.infra.ServicoTerrenoPadrao servico =
        new br.com.pokesal.infra.ServicoTerrenoPadrao(null, false);
    assertNotNull(servico.obterTerrenoAtual(null));
    assertFalse(servico.isEfeitoAtivo(null, null));
    assertTrue(servico.isEfeitoAtivo(new br.com.pokesal.model.PocaDeChuva(), null));

    br.com.pokesal.infra.ServicoTerrenoPadrao servicoChuvoso =
        new br.com.pokesal.infra.ServicoTerrenoPadrao(null, true);
    assertTrue(servicoChuvoso.obterTerrenoAtual(null) instanceof br.com.pokesal.model.PocaDeChuva);
  }
}
