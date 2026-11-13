# Documento de Testes Automatizados e Matriz de Rastreabilidade

**Projeto:** PokeSal - Simulador de Batalha (UCSal Pituaçu)  
**Equipe:** Gabriel Castro Freitas, Luiz Gustavo França Barreto Silva, Ryan David de Oliveira Silva  
**Orientador:** Prof. Pedro Arthur Melo  
**Frameworks:** JUnit 5 (Jupiter Engine) | Mockito 5.11 | JaCoCo 0.8.11 | SonarQube  

---

## 1. Visão Geral da Arquitetura de Testes

A suíte de testes do PokeSal está dividida em camadas complementares para garantir a máxima confiabilidade do sistema e o cumprimento estrito das metas acadêmicas:
1. **[`PokeSalTest`](file:///c:/Users/luu/Desktop/trabalho%20testes/pokeSal/src/test/java/br/com/pokesal/PokeSalTest.java):** Suíte base das Fases 01 e 02 (9 testes unitários clássicos com JUnit 5).
2. **[`PokeSalFase03Test`](file:///c:/Users/luu/Desktop/trabalho%20testes/pokeSal/src/test/java/br/com/pokesal/PokeSalFase03Test.java):** Suíte oficial da Fase 03 (12 testes formais com Mockito: 8 testes obrigatórios + 4 testes dos requisitos autorais RA-F03-01 e RA-F03-02).
3. **[`PokeSalCoberturaExtraTest`](file:///c:/Users/luu/Desktop/trabalho%20testes/pokeSal/src/test/java/br/com/pokesal/PokeSalCoberturaExtraTest.java):** Suíte complementar de cobertura de branches para JaCoCo (13 testes cobrindo caminhos de borda e resiliência).

---

## 2. Matriz de Rastreabilidade - Fase 03 (Isolamento com Mockito)

| Requisito | Descrição da Regra | Método de Teste (Mockito) | Colaboradores Mockados | Resultado Esperado e Obtido | Status |
|:---:|---|---|---|---|:---:|
| **RN-F03-01** | Acerto Crítico (6,25% $\rightarrow \times 1.5$ de dano) | `testAcertoCriticoComGeradorMockado()` | `GeradorAleatorio`, `ServicoTerreno`, `RegistroBatalha` | Mock gera 0.02 (< 0.0625); dano multiplicado por 1.5x (40 $\rightarrow$ 60). Verificado com sucesso. | **PASS** |
| **RN-F03-02** | Precisão de golpes (0-100) e falha com dano zero | `testGolpeErraQuandoPrecisaoFalha()` | `GeradorAleatorio`, `RegistroBatalha` | Mock gera 0.99 (>= 0.80); golpe erra, causando 0 de dano e mantendo HP intacto. | **PASS** |
| **RN-F03-03** | Efeito Asfalto Quente condicionado ao dia | `testAsfaltoQuenteApenasDuranteODia()` | `ServicoTerreno`, `GeradorAleatorio`, `Clock` | Durante o dia aplica bônus de 15% (+6 dano); à noite permanece neutro (dano base 40). | **PASS** |
| **RN-F03-04** | Sequência regulamentar de eventos no turno | `testSequenciaDeEventosNoTurno()` | `RegistroBatalha`, `GeradorAleatorio`, `ServicoTerreno` | `InOrder` valida ordem estrita: ataque $\rightarrow$ dano $\rightarrow$ status $\rightarrow$ terreno. | **PASS** |
| **RN-F03-05** | Persistência do resultado ao término da batalha | `testResultadoSalvoAoFimDaBatalha()` | `RepositorioResultados`, `GeradorAleatorio`, `ServicoTerreno` | `ArgumentCaptor` intercepta `ResultadoBatalha` validando vencedor, perdedor e turnos corretos. | **PASS** |
| **RN-F03-06** | Resiliência: falha no repositório não invalida batalha | `testFalhaNoRepositorioNaoInvalidaBatalha()` | `RepositorioResultados`, `GeradorAleatorio`, `ServicoTerreno` | `doThrow()` simula queda do banco; erro é registrado em log e vencedor permanece consagrado. | **PASS** |
| **RN-F03-07** | Repositório não chamado se a batalha não encerrou | `testRepositorioNaoChamadoSeBatalhaNaoTerminou()` | `RepositorioResultados`, `GeradorAleatorio`, `ServicoTerreno` | `verify(repositorio, never()).salvar(...)` confirma que partidas abertas não são gravadas. | **PASS** |
| **RN-F03-08** | Chaveamento eliminatório de torneio avança vencedores | `testChaveamentoTorneioAvancaVencedores()` | `ExecutorDeBatalha`, `ServicoTerreno`, `GeradorAleatorio`, `Clock` | Confrontos mockados avançam corretamente os vencedores das semifinais até a grande final. | **PASS** |
| **RA-F03-01** | Torcida concede buff Vigor de Pituaçu quando HP $\le 30\%$ | `testTorcidaEmpolgadaConcedeBuffQuandoHpBaixo()` | `ServicoTorcida`, `GeradorAleatorio`, `ServicoTerreno` | Ao atingir 30% de HP, torcida empolga e ativa bônus de +15% no próximo ataque do Pokésal. | **PASS** |
| **RA-F03-01** | Guarda de eficiência: torcida não consultada se HP $> 30\%$ | `testTorcidaNaoConsultadaQuandoHpSaudavel()` | `ServicoTorcida`, `GeradorAleatorio`, `ServicoTerreno` | `verify(servicoTorcida, never()).aferirVibracao(...)` assegura ausência de chamadas supérfluas. | **PASS** |
| **RA-F03-02** | Golpe de dreno cura atacante em 50% do dano | `testGolpeDrenoBemSucedidoCalculaECuraAtacante()` | `ProcessadorDreno`, `GeradorAleatorio`, `ServicoTerreno` | Causa 80 de dano e recupera 40 de HP no atacante sem ultrapassar o HP máximo. | **PASS** |
| **RA-F03-02** | Golpes comuns ou sem acerto não acionam dreno | `testGolpeComumOuErrouNaoAcionaProcessadorDreno()` | `ProcessadorDreno`, `GeradorAleatorio`, `ServicoTerreno` | `verify(processadorDreno, never()).calcularCura(...)` garante que golpes normais não drenam. | **PASS** |

---

## 3. Matriz de Rastreabilidade - Fases 01 e 02 (Suíte Base)

| Requisito | Descrição da Regra | Método de Teste (JUnit 5) | Cenário e Entradas | Resultado Esperado | Status |
|:---:|---|---|---|---|:---:|
| **RF001** | Escolha formal do Pokésal inicial | `testEscolhaInicialTreinadorValidaEInvalida()` | Nome ("CharSal"), índice (3) e rejeições inválidas. | Inicial atribuído com mochila de 3 itens; exceção para inválidos. | **PASS** |
| **RF002** | Matriz de vantagens/desvantagens elementais | `testVantagemElemental()` | Combates Fogo, Água e Planta. | Dano $\times 2.0$ (80), $\times 0.5$ (20) e neutro $\times 1.0$ (40). | **PASS** |
| **RF007** | Terrenos da UCSal (Asfalto, Poça, Canteiro) | `testEfeitoTerrenoEstacionamentoUCSal()` | Asfalto (+15%), Poça (+10%) e Canteiro (+5% HP Planta). | Bonificações e regenerações em conformidade. | **PASS** |
| **RF008** | Iniciativa por Velocidade (SPD) e Queimadura | `testOrdemDeAtaquePorVelocidade()` | SPD 40 vs SPD 20; Queimadura ativa. | Maior SPD ataca primeiro; Queimadura reduz ATK em 4. | **PASS** |
| **RF005** | Limite máximo de 2 itens de batalha | `testUsoLimiteDeItensExcedido()` | Consumo de 2 poções e tentativa do 3º item. | Sucesso nos 2 primeiros; `LimiteItensExcedidosException` no 3º. | **PASS** |
| **Limites** | Análise de valores limite (Boundary) | `testCalculoDanoBoundaryValues()` | Dano 9999 e defesa extrema 999. | Piso de HP em 0; dano mínimo de 1; confirmação de desmaio. | **PASS** |
| **RA001** | 6 Held Items com bônus passivo | `testEfeitoHeldItemRequisitoAutoral()` | Carvão Incandescente e Restos do RU. | Dano com item passa a 50; regeneração de +6 HP por turno. | **PASS** |
| **RA002** | Limite temporal de 10 turnos | `testLimiteDeDezTurnosEncerraBatalha()` | 10 turnos alternados entre os combatentes. | Batalha marca `encerrada = true` exatamente no turno 10. | **PASS** |
| **RA003** | Julgamento por HP restante | `testDesempatePorHpRestante()` | Comparação de HP ao fim do limite. | Maior HP declarado vencedor; empate técnico se HP idêntico. | **PASS** |

---

## 4. Justificativa Técnica do Uso de Mocks

Conforme exigido pelo item 4.6 da especificação da Fase 03, a equipe formaliza os critérios de decisão técnica para cada classe mockada ou instanciada concretamente:

### 4.1 Por que estes colaboradores foram mockados?
1. **`GeradorAleatorio`:** Sorteios pseudoaleatórios são intrinsecamente não determinísticos. O mock é indispensável para testar com 100% de precisão as branches de acerto crítico (6,25%), erro de precisão e chances de efeito de status.
2. **`ServicoTerreno` e `Clock`:** Condições climáticas e passagem do tempo dependem do ambiente externo. O mock permite simular instantaneamente cenários diurnos, noturnos ou chuvosos sem amarras com a hora real do sistema operacional.
3. **`RegistroBatalha`:** Permite validação comportamental (*behavior verification*) via `InOrder`, assegurando que o motor de combate respeite a ordem cronológica estrita de publicação dos eventos.
4. **`RepositorioResultados`:** Isola o sistema contra dependências de infraestrutura (banco de dados ou disco) e permite simular cenários de indisponibilidade ou falhas catastróficas via `doThrow()`, validando a resiliência do sistema.
5. **`ServicoTorcida`:** Representa um serviço externo/sensorial de arquibancada. O mock possibilita testar os estados `EMPOLGADA`, `NEUTRA` e `VAIANDO` sob demanda, além de garantir a regra de guarda de HP $\le 30\%$.
6. **`ProcessadorDreno`:** Permite validar isoladamente o cálculo proporcional de absorção de vida e verificar com `never()` que ataques comuns não ativam o pipeline de cura.
7. **`ExecutorDeBatalha`:** No teste de `Torneio`, mockar o executor isola a lógica de chaveamento e progressão das fases eliminatórias, tornando o teste de torneio instantâneo e livre de efeitos colaterais de combates reais.

### 4.2 Por que estas classes NÃO foram mockadas?
1. **A própria classe sob teste (`Batalha` e `Torneio`):** Princípio fundamental de testes unitários. A classe que está sendo testada (*System Under Test - SUT*) deve ser executada com sua implementação real.
2. **Modelos de Domínio (`Pokesal`, `Treinador`, `Golpe`, `HeldItem`, `ResultadoBatalha`):** São objetos puros de domínio (*Value Objects* / *Entities*) que contêm lógica de negócio coesa, previsível e sem dependências externas. Mockar modelos geraria fragilidade desnecessária (*mocking data*).
3. **Matriz de Tipagem (`TipoElemental`):** É um enum estático com regras matemáticas imutáveis ($2.0\times, 0.5\times, 1.0\times$). Usar o enum real garante que as regras de vantagem permaneçam fielmente validadas durante o teste do cálculo de dano.
4. **Itens de Batalha Concretos (`Potion`, `SuperPotion`, `Antidote`):** Possuem comportamento simples e previsível de alteração de atributos em memória, sem efeitos colaterais.

---

## 5. Resumo Geral de Execução

- **Total de Testes da Suíte:** 34 testes automatizados
  - `PokeSalTest`: 9 testes
  - `PokeSalFase03Test`: 12 testes
  - `PokeSalCoberturaExtraTest`: 13 testes
- **Falhas:** 0
- **Erros:** 0
- **Checkstyle:** 0 violações (100% em conformidade com Google Java Style).
- **Cobertura JaCoCo:** $\ge 80\%$ de linhas e $\ge 70\%$ de branches nas classes centrais de negócio.
