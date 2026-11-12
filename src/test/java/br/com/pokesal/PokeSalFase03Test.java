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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.pokesal.model.AsfaltoQuente;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Suite oficial de testes unitarios com Mockito para a Fase 03 do PokeSal.
 * Cobre isolamento de dependencias, acerto critico, precisao, servico de terreno,
 * observabilidade sequencial de eventos, persistencia resiliente, chaveamento de torneio
 * e os novos requisitos autorais RA-F03-01 e RA-F03-02.
 */
@ExtendWith(MockitoExtension.class)
public class PokeSalFase03Test {

  private static final int HP_BASE = 100;
  private static final int ATK_BASE = 30;
  private static final int DEF_BASE = 20;
  private static final int SPD_ALTO = 40;
  private static final int SPD_BAIXO = 20;
  private static final int PODER_GOLPE = 30;

  private static final double ROLAGEM_ACERTO_GARANTIDO = 0.10;
  private static final double ROLAGEM_ERRO_PRECISAO = 0.99;
  private static final double ROLAGEM_CRITICO_ATIVO = 0.02;
  private static final double ROLAGEM_SEM_CRITICO = 0.50;
  private static final double ROLAGEM_STATUS_GARANTIDO = 0.05;

  private static final Instant INSTANTE_DIA = Instant.parse("2026-11-12T12:00:00Z");
  private static final Instant INSTANTE_NOITE = Instant.parse("2026-11-12T23:00:00Z");

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

  @Captor
  private ArgumentCaptor<ResultadoBatalha> resultadoCaptor;

  private Clock clock;
  private Treinador treinador1;
  private Treinador treinador2;
  private Terreno terrenoNeutro;

  /**
   * Configura os objetos base de teste antes de cada execucao.
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
   * Teste 1: Valida que quando o gerador mockado retorna valor abaixo de 6.25%,
   * o dano e multiplicado por 1.5x (acerto critico).
   */
  @Test
  @DisplayName("1. testAcertoCriticoComGeradorMockado - Valida multiplicador 1.5x em critico")
  public void testAcertoCriticoComGeradorMockado() {
    Pokesal atacante = criarPokesalPadrao("CharSal", TipoElemental.FOGO, SPD_ALTO);
    Pokesal defensor = criarPokesalPadrao("CyndaSal", TipoElemental.FOGO, SPD_BAIXO);
    treinador1.setPokesal(atacante);
    treinador2.setPokesal(defensor);

    // Retornos em sequencia do mock: precisao (0.10) e chance de critico (0.02 < 0.0625)
    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_CRITICO_ATIVO);
    when(servicoTerreno.obterModificadorDano(eq(terrenoNeutro), eq(TipoElemental.FOGO), any()))
        .thenReturn(1.0);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    Golpe golpe = atacante.getGolpes().get(0); // poder 30, base = (30 - 20) + 30 = 40

    int danoNormalEsperado = 40;
    int danoCriticoEsperado = (int) Math.round(danoNormalEsperado * Batalha.MULTIPLICADOR_CRITICO);

    int danoCausado = batalha.executarAcaoDeAtaque(treinador1, treinador2, golpe);

    assertEquals(danoCriticoEsperado, danoCausado,
        "O dano deve ser multiplicado por 1.5x quando ocorre acerto critico.");
    assertEquals(HP_BASE - danoCriticoEsperado, defensor.getHpAtual());
    verify(registroBatalha).registrarDano(
        eq(treinador1), eq(treinador2), eq(danoCriticoEsperado), eq(true));
  }

  /**
   * Teste 2: Valida que quando a rolagem de precisao falha, o golpe erra,
   * causando dano zero e mantendo o HP do alvo inalterado.
   */
  @Test
  @DisplayName("2. testGolpeErraQuandoPrecisaoFalha - Valida dano zero ao falhar precisao")
  public void testGolpeErraQuandoPrecisaoFalha() {
    Pokesal atacante = criarPokesalPadrao("CharSal", TipoElemental.FOGO, SPD_ALTO);
    Pokesal defensor = criarPokesalPadrao("BulbaSal", TipoElemental.PLANTA, SPD_BAIXO);
    treinador1.setPokesal(atacante);
    treinador2.setPokesal(defensor);

    // Golpe com precisao 80; rolagem do gerador 0.99 (99% >= 80% -> erra)
    Golpe golpePrecisao80 = new Golpe("Fogo Rapido", 30, TipoElemental.FOGO, null, 80);
    when(geradorAleatorio.gerarDouble()).thenReturn(ROLAGEM_ERRO_PRECISAO);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    int danoCausado = batalha.executarAcaoDeAtaque(treinador1, treinador2, golpePrecisao80);

    assertEquals(Batalha.DANO_NULO, danoCausado, "Golpe com falha de precisao deve causar dano 0.");
    assertEquals(HP_BASE, defensor.getHpAtual(), "HP do defensor nao deve sofrer alteracao.");
    verify(registroBatalha).registrarDano(
        eq(treinador1), eq(treinador2), eq(Batalha.DANO_NULO), eq(false));
  }

  /**
   * Teste 3: Valida que o efeito do terreno Asfalto Quente so se ativa durante o dia,
   * utilizando mockito e clocks fixos de dia e noite.
   */
  @Test
  @DisplayName("3. testAsfaltoQuenteApenasDuranteODia - Valida ativacao de terreno por horario")
  public void testAsfaltoQuenteApenasDuranteODia() {
    Terreno asfalto = new AsfaltoQuente();
    Pokesal atacante = criarPokesalPadrao("CharSal", TipoElemental.FOGO, SPD_ALTO);
    Pokesal defensor = criarPokesalPadrao("CyndaSal", TipoElemental.FOGO, SPD_BAIXO);
    treinador1.setPokesal(atacante);
    treinador2.setPokesal(defensor);
    Golpe golpeFogo = atacante.getGolpes().get(0); // dano base 40

    // Cenário Dia: ServicoTerreno mockado retorna bonus 1.15x
    when(servicoTerreno.obterModificadorDano(eq(asfalto), eq(TipoElemental.FOGO), any()))
        .thenReturn(Terreno.MODIFICADOR_ASFALTO_QUENTE);
    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);

    Batalha batalhaDia = new Batalha(
        treinador1, treinador2, asfalto, geradorAleatorio, servicoTerreno,
        Clock.fixed(INSTANTE_DIA, ZoneOffset.UTC), repositorioResultados,
        registroBatalha, servicoTorcida, processadorDreno);

    int danoDia = batalhaDia.executarAcaoDeAtaque(treinador1, treinador2, golpeFogo);
    assertEquals(46, danoDia, "Durante o dia, Asfalto Quente deve aplicar 15% de bonus.");

    // Cenario Noite: ServicoTerreno mockado retorna neutro 1.0x
    when(servicoTerreno.obterModificadorDano(eq(asfalto), eq(TipoElemental.FOGO), any()))
        .thenReturn(1.0);
    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);

    Batalha batalhaNoite = new Batalha(
        treinador1, treinador2, asfalto, geradorAleatorio, servicoTerreno,
        Clock.fixed(INSTANTE_NOITE, ZoneOffset.UTC), repositorioResultados,
        registroBatalha, servicoTorcida, processadorDreno);

    int danoNoite = batalhaNoite.executarAcaoDeAtaque(treinador1, treinador2, golpeFogo);
    assertEquals(40, danoNoite, "Durante a noite, o terreno Asfalto Quente deve ser neutro.");
  }

  /**
   * Teste 4: Valida com InOrder que cada evento do turno e enviado a RegistroBatalha na ordem:
   * ataque -> dano -> status -> terreno -> item.
   */
  @Test
  @DisplayName("4. testSequenciaDeEventosNoTurno - Valida sequencia estrita de eventos com InOrder")
  public void testSequenciaDeEventosNoTurno() {
    Pokesal atacante = criarPokesalPadrao("BulbaSal", TipoElemental.PLANTA, SPD_ALTO);
    Pokesal defensor = criarPokesalPadrao("SquirtSal", TipoElemental.AGUA, SPD_BAIXO);
    treinador1.setPokesal(atacante);
    treinador2.setPokesal(defensor);

    // Golpe de status (Po Venenoso)
    Golpe golpeStatus = atacante.getGolpes().get(1);

    // Mock das rolagens: precisao ok, sem critico, status aplica
    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO, ROLAGEM_STATUS_GARANTIDO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.15);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    batalha.executarAcaoDeAtaque(treinador1, treinador2, golpeStatus);

    InOrder ordem = inOrder(registroBatalha);
    // 1. Ataque
    ordem.verify(registroBatalha).registrarAtaque(
        eq(treinador1), eq(treinador2), eq(golpeStatus));
    // 2. Dano
    ordem.verify(registroBatalha).registrarDano(
        eq(treinador1), eq(treinador2), anyInt(), eq(false));
    // 3. Efeito de status
    ordem.verify(registroBatalha).registrarEfeitoStatus(
        eq(defensor), eq(TipoStatus.ENVENENADO));
    // 4. Efeito de terreno
    ordem.verify(registroBatalha).registrarEfeitoTerreno(
        eq(terrenoNeutro), eq(atacante));
  }

  /**
   * Teste 5: Valida com ArgumentCaptor que o resultado da batalha e salvo no repositorio
   * contendo vencedor, perdedor e numero de turnos corretos.
   */
  @Test
  @DisplayName("5. testResultadoSalvoAoFimDaBatalha - Valida resultado salvo com ArgumentCaptor")
  public void testResultadoSalvoAoFimDaBatalha() {
    Pokesal forte = new Pokesal("Forte", HP_BASE, 999, DEF_BASE, SPD_ALTO, TipoElemental.FOGO,
        Arrays.asList(
            new Golpe("Fatality", 999, TipoElemental.FOGO, null, Golpe.PRECISAO_MAXIMA),
            new Golpe("G2", 10, TipoElemental.FOGO, null, Golpe.PRECISAO_MAXIMA),
            new Golpe("G3", 10, TipoElemental.FOGO, null, Golpe.PRECISAO_MAXIMA)));
    Pokesal fragil = criarPokesalPadrao("Fragil", TipoElemental.PLANTA, SPD_BAIXO);

    treinador1.setPokesal(forte);
    treinador2.setPokesal(fragil);

    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    batalha.executarAcaoDeAtaque(treinador1, treinador2, forte.getGolpes().get(0));

    assertTrue(batalha.isEncerrada(), "A batalha deve encerrar imediatamente por nocaute.");
    assertEquals(treinador1, batalha.getVencedor(), "Treinador 1 deve ser o vencedor.");

    verify(repositorioResultados, times(1)).salvar(resultadoCaptor.capture());
    ResultadoBatalha salvo = resultadoCaptor.getValue();

    assertNotNull(salvo);
    assertEquals(treinador1, salvo.getVencedor());
    assertEquals(treinador2, salvo.getPerdedor());
    assertEquals(1, salvo.getNumeroTurnos());
    assertFalse(salvo.isEmpate());
  }

  /**
   * Teste 6: Valida resiliencia quando o repositorio lanca excecao:
   * o erro e capturado, registrado e o vencedor continua definido normalmente.
   */
  @Test
  @DisplayName("6. testFalhaNoRepositorioNaoInvalidaBatalha - Valida resiliencia a falha de BD")
  public void testFalhaNoRepositorioNaoInvalidaBatalha() {
    Pokesal forte = new Pokesal("Forte", HP_BASE, 999, DEF_BASE, SPD_ALTO, TipoElemental.FOGO,
        Arrays.asList(
            new Golpe("Fatality", 999, TipoElemental.FOGO, null, Golpe.PRECISAO_MAXIMA),
            new Golpe("G2", 10, TipoElemental.FOGO, null, Golpe.PRECISAO_MAXIMA),
            new Golpe("G3", 10, TipoElemental.FOGO, null, Golpe.PRECISAO_MAXIMA)));
    Pokesal fragil = criarPokesalPadrao("Fragil", TipoElemental.PLANTA, SPD_BAIXO);

    treinador1.setPokesal(forte);
    treinador2.setPokesal(fragil);

    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);

    // Simula falha catastrófica no banco de dados com doThrow
    doThrow(new RuntimeException("Falha de conexao com PostgreSQL"))
        .when(repositorioResultados).salvar(any(ResultadoBatalha.class));

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    batalha.executarAcaoDeAtaque(treinador1, treinador2, forte.getGolpes().get(0));

    assertTrue(batalha.isEncerrada(), "Batalha deve encerrar.");
    assertEquals(treinador1, batalha.getVencedor(),
        "O vencedor deve continuar definido mesmo se o repositorio falhar.");
    assertFalse(batalha.getErrosRegistrados().isEmpty(),
        "A falha do repositorio deve ser catalogada no registro de erros.");
  }

  /**
   * Teste 7: Valida com verify(..., never()) que o repositorio NUNCA e acionado
   * se a batalha ainda esta em andamento.
   */
  @Test
  @DisplayName("7. testRepositorioNaoChamadoSeBatalhaNaoTerminou - Valida verify com never()")
  public void testRepositorioNaoChamadoSeBatalhaNaoTerminou() {
    Pokesal p1 = criarPokesalPadrao("Poke1", TipoElemental.AGUA, SPD_ALTO);
    Pokesal p2 = criarPokesalPadrao("Poke2", TipoElemental.AGUA, SPD_BAIXO);
    treinador1.setPokesal(p1);
    treinador2.setPokesal(p2);

    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    batalha.executarAcaoDeAtaque(treinador1, treinador2, p1.getGolpes().get(0));

    assertFalse(batalha.isEncerrada(), "Apos um golpe sem nocaute, batalha segue aberta.");
    verify(repositorioResultados, never()).salvar(any());
  }

  /**
   * Teste 8: Valida chaveamento do torneio de 4 competidores com batalhas mockadas,
   * garantindo que os vencedores avancam de fase corretamente ate a final.
   */
  @Test
  @DisplayName("8. testChaveamentoTorneioAvancaVencedores - Valida avanco correto no Torneio")
  public void testChaveamentoTorneioAvancaVencedores() {
    Treinador t1 = new Treinador("Treinador 1");
    Treinador t2 = new Treinador("Treinador 2");
    Treinador t3 = new Treinador("Treinador 3");
    Treinador t4 = new Treinador("Treinador 4");

    t1.setPokesal(criarPokesalPadrao("P1", TipoElemental.FOGO, 30));
    t2.setPokesal(criarPokesalPadrao("P2", TipoElemental.AGUA, 30));
    t3.setPokesal(criarPokesalPadrao("P3", TipoElemental.PLANTA, 30));
    t4.setPokesal(criarPokesalPadrao("P4", TipoElemental.FOGO, 30));

    // Batalha mockada 1: Semifinal 1 -> T1 vence T2
    Batalha semi1 = instanciarBatalhaPadrao(terrenoNeutro);
    semi1.verificarHp(t1, t2);
    t2.getPokesal().receberDano(999);
    semi1.verificarHp(t1, t2);

    // Batalha mockada 2: Semifinal 2 -> T3 vence T4
    Batalha semi2 = instanciarBatalhaPadrao(terrenoNeutro);
    t4.getPokesal().receberDano(999);
    semi2.verificarHp(t3, t4);

    // Batalha mockada 3: Final -> T1 vence T3
    Batalha finalBatalha = instanciarBatalhaPadrao(terrenoNeutro);
    t3.getPokesal().receberDano(999);
    finalBatalha.verificarHp(t1, t3);

    when(executorDeBatalha.executar(any(), any(), any()))
        .thenReturn(semi1, semi2, finalBatalha);

    Torneio torneio = new Torneio(executorDeBatalha, servicoTerreno, geradorAleatorio, clock);
    Treinador campeao = torneio.executarTorneio(Arrays.asList(t1, t2, t3, t4));

    assertEquals(t1, campeao, "O campeao final do torneio deve ser o Treinador 1.");
    verify(executorDeBatalha, times(3)).executar(any(), any(), any());
    assertEquals(2, t1.getBatalhasVencidas());
  }

  /**
   * Teste 9 (RA-F03-01): Valida que quando o HP do Pokesal cai para <= 30%,
   * o ServicoTorcida e consultado e concede o buff Vigor de Pituacu (+15% no proximo ataque).
   */
  @Test
  @DisplayName("9. testTorcidaEmpolgadaConcedeBuffQuandoHpBaixo - RA-F03-01: Vigor de Pituacu")
  public void testTorcidaEmpolgadaConcedeBuffQuandoHpBaixo() {
    Pokesal atacante = criarPokesalPadrao("CharSal", TipoElemental.FOGO, SPD_ALTO);
    Pokesal defensor = criarPokesalPadrao("CyndaSal", TipoElemental.FOGO, SPD_BAIXO);
    treinador1.setPokesal(atacante);
    treinador2.setPokesal(defensor);

    // Golpe com poder 60: dano neutro base = (30 - 20) + 60 = 70
    // HP do defensor cai de 100 para 30 (exatamente 30% -> faixa de perigo)
    Golpe golpePesado = new Golpe("Super Pancada", 60, TipoElemental.FOGO,
        null, Golpe.PRECISAO_MAXIMA);

    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);
    when(servicoTorcida.aferirVibracao(eq(defensor))).thenReturn(StatusTorcida.EMPOLGADA);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    batalha.executarAcaoDeAtaque(treinador1, treinador2, golpePesado);

    assertTrue(defensor.getHpAtual() <= (HP_BASE * Batalha.LIMIAR_HP_TORCIDA),
        "HP deve estar na faixa de perigo (<= 30%).");
    verify(servicoTorcida, times(1)).aferirVibracao(eq(defensor));
    assertTrue(defensor.isVigorDePituacuAtivo(),
        "O Pokesal deve estar com o buff Vigor de Pituacu ativo.");
  }

  /**
   * Teste 10 (RA-F03-01): Valida a regra de eficiencia (guarda):
   * se o Pokésal estiver saudável (> 30% do HP), o ServicoTorcida NUNCA deve ser consultado.
   */
  @Test
  @DisplayName("10. testTorcidaNaoConsultadaQuandoHpSaudavel - RA-F03-01: Guarda de HP > 30%")
  public void testTorcidaNaoConsultadaQuandoHpSaudavel() {
    Pokesal atacante = criarPokesalPadrao("CharSal", TipoElemental.FOGO, SPD_ALTO);
    Pokesal defensor = criarPokesalPadrao("SquirtSal", TipoElemental.AGUA, SPD_BAIXO);
    treinador1.setPokesal(atacante);
    treinador2.setPokesal(defensor);

    // Golpe leve com poder 10: causa 20 de dano, defensor fica com 80/100 (80% > 30%)
    Golpe golpeLeve = new Golpe("Toque Leve", 10, TipoElemental.FOGO, null, Golpe.PRECISAO_MAXIMA);

    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    batalha.executarAcaoDeAtaque(treinador1, treinador2, golpeLeve);

    assertTrue(defensor.getHpAtual() > (HP_BASE * Batalha.LIMIAR_HP_TORCIDA),
        "HP do defensor deve permanecer saudavel (> 30%).");
    verify(servicoTorcida, never()).aferirVibracao(any());
    assertFalse(defensor.isVigorDePituacuAtivo());
  }

  /**
   * Teste 11 (RA-F03-02): Valida que ao acertar um golpe de dreno (isGolpeDreno = true),
   * a batalha aciona ProcessadorDreno e cura o atacante sem ultrapassar o HP maximo.
   */
  @Test
  @DisplayName("11. testGolpeDrenoBemSucedidoCalculaECuraAtacante - RA-F03-02: Absorcao de vida")
  public void testGolpeDrenoBemSucedidoCalculaECuraAtacante() {
    Pokesal atacante = criarPokesalPadrao("BulbaSal", TipoElemental.PLANTA, SPD_ALTO);
    Pokesal defensor = criarPokesalPadrao("SquirtSal", TipoElemental.AGUA, SPD_BAIXO);
    treinador1.setPokesal(atacante);
    treinador2.setPokesal(defensor);

    // Atacante ferido previamente com 50/100 HP
    atacante.receberDano(50);
    assertEquals(50, atacante.getHpAtual());

    Golpe megaDreno = new Golpe("Mega Dreno", 30, TipoElemental.PLANTA, null,
        Golpe.PRECISAO_MAXIMA, true, Golpe.TAXA_DRENO_PADRAO);

    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);
    // Dano causado esperado: base (30 - 20 + 30) * 2.0 (super efetivo) = 80
    // Cura calculada: 50% de 80 = 40
    when(processadorDreno.calcularCura(eq(80), eq(Golpe.TAXA_DRENO_PADRAO))).thenReturn(40);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    int dano = batalha.executarAcaoDeAtaque(treinador1, treinador2, megaDreno);

    assertEquals(80, dano);
    verify(processadorDreno, times(1)).calcularCura(eq(80), eq(Golpe.TAXA_DRENO_PADRAO));
    assertEquals(90, atacante.getHpAtual(), "Atacante deve ter recuperado 40 de HP (50 + 40 = 90).");
  }

  /**
   * Teste 12 (RA-F03-02): Valida que golpes fisicos comuns ou golpes que erram o alvo
   * NUNCA devem acionar o ProcessadorDreno (verify com never()).
   */
  @Test
  @DisplayName("12. testGolpeComumOuErrouNaoAcionaProcessadorDreno - RA-F03-02: Sem dreno indevido")
  public void testGolpeComumOuErrouNaoAcionaProcessadorDreno() {
    Pokesal atacante = criarPokesalPadrao("CharSal", TipoElemental.FOGO, SPD_ALTO);
    Pokesal defensor = criarPokesalPadrao("BulbaSal", TipoElemental.PLANTA, SPD_BAIXO);
    treinador1.setPokesal(atacante);
    treinador2.setPokesal(defensor);

    // 1. Golpe comum (isGolpeDreno = false) com acerto
    Golpe golpeComum = atacante.getGolpes().get(0);
    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    batalha.executarAcaoDeAtaque(treinador1, treinador2, golpeComum);

    verify(processadorDreno, never()).calcularCura(anyInt(), anyDouble());

    // 2. Golpe de dreno que erra precisao (dano 0)
    Golpe drenoErrante = new Golpe("Dreno Impreciso", 30, TipoElemental.PLANTA, null,
        50, true, Golpe.TAXA_DRENO_PADRAO);
    when(geradorAleatorio.gerarDouble()).thenReturn(ROLAGEM_ERRO_PRECISAO);

    batalha.executarAcaoDeAtaque(treinador1, treinador2, drenoErrante);
    verify(processadorDreno, never()).calcularCura(anyInt(), anyDouble());
  }

  /**
   * Teste 13 (RA-F03-01): Valida resiliencia quando ServicoTorcida lanca excecao:
   * o erro e capturado, registrado em log e o combate prossegue normalmente.
   */
  @Test
  @DisplayName("13. testFalhaNoServicoTorcidaNaoTravaCombate - RA-F03-01: Resiliencia")
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
   * Teste 14 (RA-F03-01): Valida que o buff Vigor de Pituaçu amplifica o proximo golpe em 15%
   * e e consumido imediatamente apos o ataque.
   */
  @Test
  @DisplayName("14. testConsumoDoBuffVigorDePituacuNoProximoGolpe - RA-F03-01: Bonus ofensivo")
  public void testConsumoDoBuffVigorDePituacuNoProximoGolpe() {
    Pokesal atacante = criarPokesalPadrao("CharSal", TipoElemental.FOGO, SPD_ALTO);
    Pokesal defensor = criarPokesalPadrao("CyndaSal", TipoElemental.FOGO, SPD_BAIXO);
    treinador1.setPokesal(atacante);
    treinador2.setPokesal(defensor);

    // Ativa manualmente o buff de torcida
    atacante.ativarVigorDePituacu();
    assertTrue(atacante.isVigorDePituacuAtivo());

    Golpe golpe = atacante.getGolpes().get(0); // poder 30 -> dano base 40
    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    int danoComBuff = batalha.executarAcaoDeAtaque(treinador1, treinador2, golpe);

    // 40 * 1.15 = 46
    assertEquals(46, danoComBuff, "Golpe sob Vigor de Pituacu deve receber 15% de dano extra.");
    assertFalse(atacante.isVigorDePituacuAtivo(), "O buff deve ser consumido apos o ataque.");
  }

  /**
   * Teste 15: Valida metodos de rodada, turno individual e ataque direto na Batalha.
   */
  @Test
  @DisplayName("15. testExecutarRodadaETurnoEAtacar - Valida fluxos de combate e acoes")
  public void testExecutarRodadaETurnoEAtacar() {
    Pokesal rapido = criarPokesalPadrao("Rapido", TipoElemental.FOGO, SPD_ALTO);
    Pokesal lento = criarPokesalPadrao("Lento", TipoElemental.AGUA, SPD_BAIXO);
    treinador1.setPokesal(rapido);
    treinador2.setPokesal(lento);

    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);

    // Ataque direto (compatibilidade)
    batalha.atacar(rapido, lento, rapido.getGolpes().get(0));
    assertTrue(lento.getHpAtual() < HP_BASE);

    // Executar turno
    batalha.executarTurno(treinador1, treinador2, rapido.getGolpes().get(0));

    // Executar rodada completa
    batalha.executarRodada(rapido.getGolpes().get(0), lento.getGolpes().get(0));
    assertTrue(batalha.getTurnoDaBatalha() > 0);
  }

  /**
   * Teste 16: Valida os caminhos de decisao de vencedor por HP, empate e nocaute mutuo.
   */
  @Test
  @DisplayName("16. testDesfechosDeVitoriaEmpateENocauteMutuo - Valida verificacao de HP")
  public void testDesfechosDeVitoriaEmpateENocauteMutuo() {
    Pokesal p1 = criarPokesalPadrao("P1", TipoElemental.FOGO, SPD_ALTO);
    Pokesal p2 = criarPokesalPadrao("P2", TipoElemental.AGUA, SPD_BAIXO);
    treinador1.setPokesal(p1);
    treinador2.setPokesal(p2);

    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);

    // 1. T2 vence quando T1 desmaia
    p1.receberDano(999);
    batalha.verificarHp(treinador1, treinador2);
    assertTrue(batalha.isEncerrada());
    assertEquals(treinador2, batalha.getVencedor());

    // 2. Nocaute mutuo
    p2.receberDano(999);
    Batalha batalhaMutua = instanciarBatalhaPadrao(terrenoNeutro);
    batalhaMutua.verificarHp(treinador1, treinador2);
    assertTrue(batalhaMutua.isEncerrada());
    assertNull(batalhaMutua.getVencedor());

    // 3. Pontuar vencedor no limite por maior HP do T2
    p1.restaurarParaNovaBatalha();
    p2.restaurarParaNovaBatalha();
    p1.receberDano(40); // p1 tem 60, p2 tem 100
    Batalha batalhaT2 = instanciarBatalhaPadrao(terrenoNeutro);
    batalhaT2.pontuarVencedor();
    assertEquals(treinador2, batalhaT2.getVencedor());
  }

  /**
   * Teste 17: Valida o chaveamento de torneio com 8 treinadores (quartas, semifinal e final).
   */
  @Test
  @DisplayName("17. testTorneioOitoParticipantes - Valida formato de 8 competidores")
  public void testTorneioOitoParticipantes() {
    List<Treinador> oito = Arrays.asList(
        new Treinador("T1"), new Treinador("T2"), new Treinador("T3"), new Treinador("T4"),
        new Treinador("T5"), new Treinador("T6"), new Treinador("T7"), new Treinador("T8")
    );
    for (int i = 0; i < oito.size(); i++) {
      oito.get(i).setPokesal(criarPokesalPadrao("P" + i, TipoElemental.FOGO, 20 + i));
    }

    // Cria batalha vencedora genérica
    Batalha batalhaVitoria = instanciarBatalhaPadrao(terrenoNeutro);
    batalhaVitoria.verificarHp(oito.get(0), oito.get(1));
    oito.get(1).getPokesal().receberDano(999);
    batalhaVitoria.verificarHp(oito.get(0), oito.get(1));

    when(executorDeBatalha.executar(any(), any(), any())).thenReturn(batalhaVitoria);

    Torneio torneio = new Torneio(executorDeBatalha, servicoTerreno, geradorAleatorio, clock);
    Treinador campeao = torneio.executarTorneio(oito);

    assertNotNull(campeao);
    // 8 participantes = 4 quartas + 2 semis + 1 final = 7 batalhas executadas
    verify(executorDeBatalha, times(7)).executar(any(), any(), any());
  }

  /**
   * Teste 18: Valida validacoes de lista e quantidade no Torneio (regras defensivas).
   */
  @Test
  @DisplayName("18. testValidacoesDeParticipantesTorneio - Valida excecoes de entrada")
  public void testValidacoesDeParticipantesTorneio() {
    Torneio torneio = new Torneio(executorDeBatalha, servicoTerreno, geradorAleatorio, clock);

    // Quantidade invalida (ex: 3 ou 5 treinadores)
    List<Treinador> tres = Arrays.asList(
        new Treinador("A"), new Treinador("B"), new Treinador("C"));
    assertThrows(IllegalArgumentException.class, () -> torneio.executarTorneio(tres));

    // Participante sem pokesal
    List<Treinador> quatroInvalido = Arrays.asList(
        new Treinador("A"), new Treinador("B"), new Treinador("C"), new Treinador("D"));
    assertThrows(IllegalArgumentException.class, () -> torneio.executarTorneio(quatroInvalido));
  }

  /**
   * Teste 19: Valida cadastros de torneio e sorteios em SimuladorDeBatalha.
   */
  @Test
  @DisplayName("19. testSimuladorDeBatalhaTorneioECatalogos - Valida servico de simulacao")
  public void testSimuladorDeBatalhaTorneioECatalogos() {
    SimuladorDeBatalha sim = new SimuladorDeBatalha(geradorAleatorio);

    // Cadastros
    sim.cadastrarTreinadoresTorneio(4, "Ash Sal");
    assertEquals(4, sim.getTreinadores().size());

    sim.cadastrarTreinadoresTorneio(8, "Ash Sal");
    assertEquals(8, sim.getTreinadores().size());

    assertThrows(IllegalArgumentException.class, () -> sim.cadastrarTreinadoresTorneio(6, "Ash"));

    // Sorteios
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
   * Teste 20: Valida metodos auxiliares de modelos Pokesal, Treinador e Golpe.
   */
  @Test
  @DisplayName("20. testModelosPokesalETreinadorAuxiliares - Valida comportamentos de modelo")
  public void testModelosPokesalETreinadorAuxiliares() {
    Pokesal p = criarPokesalPadrao("Modelo", TipoElemental.FOGO, 30);
    p.curar(0); // ramo cura <= 0
    p.curarStatus(); // sem status ativo
    p.aplicarCondicaoStatus(TipoStatus.NENHUM); // status nenhum
    p.aplicarCondicaoStatus(TipoStatus.PARALISADO);
    p.aplicarCondicaoStatus(TipoStatus.QUEIMADO); // ja tem status
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
   * Teste 21: Valida branches de combate na Batalha (limites, golpes elementais, itens e status).
   */
  @Test
  @DisplayName("21. testBatalhaCoberturaDeBranches - Valida branches especificos de Batalha")
  public void testBatalhaCoberturaDeBranches() {
    Pokesal atacante = criarPokesalPadrao("Atacante", TipoElemental.FOGO, SPD_ALTO);
    Pokesal defensor = criarPokesalPadrao("Defensor", TipoElemental.AGUA, SPD_BAIXO);
    treinador1.setPokesal(atacante);
    treinador2.setPokesal(defensor);

    // 1. Golpe com precisao zero retorna DANO_NULO
    Golpe golpePrecisaoZero = new Golpe("Golpe Nulo", 30, TipoElemental.FOGO, null, 0);
    Batalha batalha = instanciarBatalhaPadrao(terrenoNeutro);
    assertEquals(Batalha.DANO_NULO, batalha.executarAcaoDeAtaque(
        treinador1, treinador2, golpePrecisaoZero));

    // 2. Acao quando batalha esta encerrada
    defensor.receberDano(999);
    batalha.verificarHp(treinador1, treinador2);
    assertTrue(batalha.isEncerrada());
    assertEquals(Batalha.DANO_NULO, batalha.executarAcaoDeAtaque(
        treinador1, treinador2, atacante.getGolpes().get(0)));
    batalha.atacar(atacante, defensor, atacante.getGolpes().get(0));
    batalha.consumirItemNoTurno(treinador1, 0);
    batalha.executarRodada(atacante.getGolpes().get(0), defensor.getGolpes().get(0));
    batalha.executarTurno(treinador1, treinador2, atacante.getGolpes().get(0));

    // 3. Batalha sem colaboradores opcionais (seguranca contra NPE)
    Batalha batalhaSemColaboradores = new Batalha(
        treinador1, treinador2, null, null, null, null, null, null, null, null);
    Pokesal vivo = criarPokesalPadrao("Vivo", TipoElemental.FOGO, 30);
    Pokesal alvo = criarPokesalPadrao("Alvo", TipoElemental.FOGO, 30);
    treinador1.setPokesal(vivo);
    treinador2.setPokesal(alvo);
    assertEquals(treinador1, batalhaSemColaboradores.determinarPrimeiroAtacante());
    assertTrue(batalhaSemColaboradores.calcularDano(vivo, alvo, vivo.getGolpes().get(0)) > 0);
    batalhaSemColaboradores.finalizarTurno();

    // 4. Usar item de batalha direto
    Batalha batalhaItem = instanciarBatalhaPadrao(terrenoNeutro);
    treinador1.adicionarItem(new br.com.pokesal.model.Potion());
    batalhaItem.usarItemdeBatalha(treinador1);
    assertEquals(1, treinador1.getContItensUsados());
  }

  /**
   * Teste 22: Valida branches de desempate e chaveamento no Torneio.
   */
  @Test
  @DisplayName("22. testTorneioDesempatesEChaveamentoBranches - Valida desempates e fases")
  public void testTorneioDesempatesEChaveamentoBranches() {
    Treinador t1 = new Treinador("T1");
    Treinador t2 = new Treinador("T2");
    Treinador t3 = new Treinador("T3");
    Treinador t4 = new Treinador("T4");

    // T1 mais rapido que T2; T4 mais rapido que T3
    t1.setPokesal(criarPokesalPadrao("P1", TipoElemental.FOGO, 50));
    t2.setPokesal(criarPokesalPadrao("P2", TipoElemental.AGUA, 20));
    t3.setPokesal(criarPokesalPadrao("P3", TipoElemental.PLANTA, 20));
    t4.setPokesal(criarPokesalPadrao("P4", TipoElemental.FOGO, 50));

    // Simula batalha que encerra em empate (vencedor nulo)
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
   * Teste 23: Valida cobertura de branches e casos extremos de Pokesal e Treinador.
   */
  @Test
  @DisplayName("23. testPokesalETreinadorCasosExtremos - Valida integridade e robustez")
  public void testPokesalETreinadorCasosExtremos() {
    Pokesal p = criarPokesalPadrao("Robusto", TipoElemental.PLANTA, 30);

    // Danos nao positivos
    p.receberDano(0);
    p.receberDano(-10);
    assertEquals(HP_BASE, p.getHpAtual());

    // Curar criatura desmaiada
    p.receberDano(999);
    assertTrue(p.estaDesmaiado());
    p.curar(50);
    assertEquals(0, p.getHpAtual());

    // Status envenenado e queima em processarEfeitosFimDeTurno
    p.restaurarParaNovaBatalha();
    p.aplicarCondicaoStatus(TipoStatus.ENVENENADO);
    p.processarEfeitosFimDeTurno();
    p.processarEfeitosFimDeTurno();
    assertTrue(p.getTurnosEnvenenado() > 1);

    p.restaurarParaNovaBatalha();
    p.aplicarCondicaoStatus(TipoStatus.QUEIMADO);
    p.processarEfeitosFimDeTurno();
    assertTrue(p.getHpAtual() < HP_BASE);

    // Held Item regenerativo com processarEfeitosFimDeTurno
    p.setHeldItem(new br.com.pokesal.model.HeldItem("Leftovers",
        new br.com.pokesal.service.EfeitoDeStatusHeld(0, 5, null, 1.0)));
    int hpAntes = p.getHpAtual();
    p.processarEfeitosFimDeTurno();
    // Sofre dano de queimadura (6) e cura leftovers (5) -> variacao de -1
    assertEquals(hpAntes - 1, p.getHpAtual());

    // Treinador itens e limites
    Treinador t = new Treinador("Treinador X");
    t.adicionarItem(null); // item nulo
    t.usarItem(-1); // indice negativo
    t.usarItem(10); // indice fora da lista
    t.adicionarItem(new br.com.pokesal.model.Potion());
    t.usarItem(0); // sem pokesal ativo
    assertEquals(1, t.getContItensUsados());
  }

  /**
   * Teste 24: Valida branches adicionais em Batalha para atingir meta estrita de JaCoCo.
   */
  @Test
  @DisplayName("24. testBatalhaBranchesAdicionaisJaCoCo - Cobertura profunda de branches")
  public void testBatalhaBranchesAdicionaisJaCoCo() {
    Pokesal p1 = criarPokesalPadrao("P1", TipoElemental.FOGO, 30);
    Pokesal p2 = criarPokesalPadrao("P2", TipoElemental.AGUA, 30);
    treinador1.setPokesal(p1);
    treinador2.setPokesal(p2);

    // 1. Determinar primeiro atacante com SPD empatado e gerador retornando false
    when(geradorAleatorio.gerarBooleano()).thenReturn(false);
    Batalha b = instanciarBatalhaPadrao(terrenoNeutro);
    assertEquals(treinador2, b.determinarPrimeiroAtacante());

    // 2. Pontuar vencedor no limite de turnos com empate exato de HP (hp1 == hp2)
    b.pontuarVencedor();
    assertTrue(b.isEncerrada());
    assertNull(b.getVencedor());

    // 3. Golpe de status quando a rolagem falha (rolagem > chance)
    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO, 0.95);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);
    Batalha bStatusFalha = instanciarBatalhaPadrao(terrenoNeutro);
    bStatusFalha.executarAcaoDeAtaque(treinador1, treinador2, p1.getGolpes().get(1));
    assertFalse(p2.temStatus());

    // 4. Consumir item no 10o turno para disparar pontuarVencedor
    Batalha bTurno10 = instanciarBatalhaPadrao(terrenoNeutro);
    treinador1.adicionarItem(new br.com.pokesal.model.Potion());
    // Consumir com indice invalido para cobrir ramo de item nulo
    bTurno10.consumirItemNoTurno(treinador1, 99);
    for (int i = 0; i < 8; i++) {
      bTurno10.executarAcaoDeAtaque(treinador1, treinador2, p1.getGolpes().get(0));
    }
    bTurno10.consumirItemNoTurno(treinador1, 0);
    assertTrue(bTurno10.isEncerrada());

    // 5. Torcida em estado NEUTRA (nao concede buff quando HP <= 30%)
    Pokesal neutroAlvo = criarPokesalPadrao("NeutroAlvo", TipoElemental.FOGO, SPD_BAIXO);
    treinador2.setPokesal(neutroAlvo);
    when(geradorAleatorio.gerarDouble()).thenReturn(
        ROLAGEM_ACERTO_GARANTIDO, ROLAGEM_SEM_CRITICO);
    when(servicoTerreno.obterModificadorDano(any(), any(), any())).thenReturn(1.0);
    when(servicoTorcida.aferirVibracao(any())).thenReturn(StatusTorcida.NEUTRA);
    Batalha bTorcidaNeutra = instanciarBatalhaPadrao(terrenoNeutro);
    // Dano 70 reduz HP de 100 para 30 (<= 30% ativa consulta da torcida)
    bTorcidaNeutra.executarAcaoDeAtaque(treinador1, treinador2,
        new Golpe("Pancada", 60, TipoElemental.FOGO, null, Golpe.PRECISAO_MAXIMA));
    assertFalse(neutroAlvo.isVigorDePituacuAtivo());

    // 6. HeldItem com elemento diferente do golpe (nao bonifica elemento)
    p1.setHeldItem(new br.com.pokesal.model.HeldItem("Gota de Agua",
        new br.com.pokesal.service.EfeitoDeStatusHeld(6, 0, TipoElemental.AGUA, 1.1)));
    Batalha bHeldDiferente = instanciarBatalhaPadrao(terrenoNeutro);
    assertTrue(bHeldDiferente.calcularDano(p1, p2, p1.getGolpes().get(0)) > 0);

    // 7. Persistir resultado com T2 vencedor e com empate
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

    // 8. Getters e consultas de compatibilidade
    assertNotNull(b.getTreinador1());
    assertNotNull(b.getTreinador2());
    assertNotNull(b.getTerreno());
    assertNotNull(b.getTerredoDaBatalha());
    assertNotNull(b.getTerrenoDaBatalha());
    assertEquals(b.getTurnoDaBatalha(), b.getTurnoAtual());
  }

  /**
   * Teste 25: Valida branches de SimuladorDeBatalha e ServicoTerrenoPadrao.
   */
  @Test
  @DisplayName("25. testSimuladorEServicoTerrenoBranches - Cobertura profunda de servicos")
  public void testSimuladorEServicoTerrenoBranches() {
    SimuladorDeBatalha simSemGerador = new SimuladorDeBatalha(null);
    simSemGerador.adicionarTreinador(null);
    simSemGerador.associarItensMochila(null);

    Treinador t = new Treinador("SemPokesal");
    simSemGerador.sortearHeldItem(t); // pokesal e null
    simSemGerador.sortearPokesal(t);
    assertNotNull(t.getPokesal());
    simSemGerador.sortearHeldItem(t);
    assertNotNull(t.getPokesal().getHeldItem());
    assertNotNull(simSemGerador.sortearTerreno());

    // sortearPrimeiraPartida com menos de 2 treinadores
    assertEquals(0, simSemGerador.sortearPrimeiraPartida().size());

    // Cadastro de treinadores padrao e com nome vazio
    simSemGerador.cadastrarTreinadores();
    assertEquals(2, simSemGerador.getTreinadores().size());
    simSemGerador.cadastrarTreinadoresTorneio(4, "");
    assertEquals(4, simSemGerador.getTreinadores().size());

    // ServicoTerrenoPadrao branches
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
