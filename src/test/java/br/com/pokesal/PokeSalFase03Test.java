package br.com.pokesal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
import br.com.pokesal.service.Torneio;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
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
 * Contem os 12 testes especificados formalmente no documento da Fase 03:
 * 8 testes obrigatorios com Mockito + 4 testes dos requisitos autorais (RA-F03-01 e RA-F03-02).
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

    // Cenario Dia: ServicoTerreno mockado retorna bonus 1.15x
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

    // Simula falha catastrofica no banco de dados com doThrow
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
   * se o Pokésal estiver saudavel (> 30% do HP), o ServicoTorcida NUNCA deve ser consultado.
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
}
