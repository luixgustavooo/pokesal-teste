# Registro Oficial de Bugs Encontrados e Corrigidos

**Projeto:** PokeSal - Simulador de Batalha (UCSal Pituaçu)  
**Equipe:** Gabriel Castro Freitas, Luiz Gustavo França Barreto Silva, Ryan David de Oliveira Silva  
**Orientador:** Prof. Pedro Arthur Melo  
**Fases:** 01, 02 e 03  

---

## 1. Visão Geral

Este documento formaliza todos os bugs e não conformidades identificados durante os testes estáticos manuais, a análise de regras de negócio, a refatoração para a Fase 03 e a execução das suítes de testes automatizados (JUnit 5 e Mockito), detalhando a causa raiz, a severidade, o commit associado e as correções implementadas.

---

## 2. Tabela Consolidada de Registro de Bugs

| ID | Título do Bug | Severidade | Fase Detectada | Componente Afetado | Commit de Correção | Status |
|:---:|---|:---:|:---:|:---:|:---:|:---:|
| **BUG-001** | Aborto precoce do torneio caso o segundo treinador vença a semifinal | **Crítica** | Devolutiva / Teste Estático | `Main.java` | `d0e63cd` | **Resolvido** |
| **BUG-002** | Ausência de seleção interativa do Pokésal inicial (sorteio forçado) | **Alta** | Devolutiva / Inspeção | `SimuladorDeBatalha.java`, `Main.java` | `d0e63cd` | **Resolvido** |
| **BUG-003** | Sobreposição semântica entre RA002 (Limite de Turnos) e RA003 (Desempate por HP) | **Média** | Devolutiva / Especificação | Documentação & `Batalha.java` | `d0e63cd` | **Resolvido** |
| **BUG-004** | Ações de combate permitidas após a batalha atingir estado de encerramento | **Média** | Teste Automatizado | `Batalha.java` | `d0e63cd` | **Resolvido** |
| **BUG-005** | Dano negativo em cenários de alta disparidade de defesa | **Baixa** | Boundary Values (JUnit) | `Batalha.java` | `d0e63cd` | **Resolvido** |
| **BUG-006** | Desvantagem elemental mascarando limiar de dano no teste de Torcida (RA-F03-01) | **Média** | Testes Mockito (Fase 03) | `PokeSalFase03Test.java` | `0b9818b` | **Resolvido** |
| **BUG-007** | Falha de `UnnecessaryStubbingException` por stubbing em fluxo de guarda de HP | **Média** | Testes Mockito (Fase 03) | `PokeSalFase03Test.java` | `0b9818b` | **Resolvido** |
| **BUG-008** | Quebra da partida por falha não tratada de colaboradores externos (Repositório e Torcida) | **Alta** | Refatoração Fase 03 | `Batalha.java` | `b640979` | **Resolvido** |

---

## 3. Detalhamento Técnico das Ocorrências

### BUG-001: Encerramento Prematuro do Torneio na Semifinal
- **Descrição:** Na versão inicial da condução do torneio (`Main.java`), a estrutura condicional verificava apenas se o Treinador 1 (`t1`) vencia a semifinal (`if (batalha1.getVencedor() == t1)`). Caso o Treinador 2 (`t2`) vencesse, o bloco `else` imprimia a mensagem de derrota e executava `return;`, finalizando o programa sem permitir que o vencedor disputasse a Grande Final.
- **Causa Raiz:** Suposição incorreta de que apenas o protagonista avançaria no chaveamento.
- **Correção Implementada:** Foi criada a variável `Treinador finalista`, que armazena legitimamente o vencedor da Batalha 1 (seja `t1` ou `t2`). O competidor vencedor tem seus atributos restaurados (`restaurarParaNovaBatalha()`) e é encaminhado para disputar a Grande Final.

---

### BUG-002: Violação da Regra de Negócio 1 (Seleção do Inicial)
- **Descrição:** A Regra 1 estipula expressamente: *"O treinador deve escolher apenas 1 Pokésal inicial entre as opções permitidas: BulbaSal, CharSal, SquirtSal, ChikoSal, CyndaSal ou TotoSal"*. O código anterior realizava sorteio aleatório direto para todos os competidores.
- **Causa Raiz:** Ausência de método no serviço para aceitar a escolha informada pelo treinador/usuário.
- **Correção Implementada:** 
  1. Criação dos métodos `escolherPokesal(Treinador, String)` e `escolherPokesal(Treinador, int)` em `SimuladorDeBatalha.java`, validando estritamente os 6 nomes permitidos.
  2. Implementação de interação via console (`Scanner`) no `Main.java`, exibindo menu detalhado de catálogo e capturando a escolha do treinador.
  3. Adição do teste unitário `testEscolhaInicialTreinadorValidaEInvalida()` em `PokeSalTest.java`.

---

### BUG-003: Sobreposição Conceitual entre RA002 e RA003
- **Descrição:** O requisito RA002 definia a finalização da batalha em 10 turnos e o RA003 definia a vitória por maior HP residual em caso de término por tempo. O professor apontou que ambas constituíam a mesma regra de desempate.
- **Causa Raiz:** Falta de dissociação formal entre o mecanismo temporal de corte do loop de combate e a política arbitral de julgamento.
- **Correção Implementada:**
  - **RA002 (Limite Temporal Estrito):** Trata exclusivamente da máquina de estados do combate, travando a contagem em 10 turnos (5 ações por combatente) e bloqueando dano para quaisquer ações posteriores.
  - **RA003 (Critério Arbitral de Julgamento por Vitalidade Residual):** Define formalmente a regra de decisão: maior percentual/total de HP em campo é declarado vencedor; se houver paridade exata de HP, declara-se empate técnico (vencedor nulo).

---

### BUG-004: Ações Após Término de Combate
- **Descrição:** Caso um método invocasse `executarAcaoDeAtaque` ou `consumirItemNoTurno` com a flag `encerrada == true`, o turno ainda podia ser incrementado indevidamente.
- **Causa Raiz:** Falta de checagem defensiva de estado no início dos métodos públicos de ação.
- **Correção Implementada:** Inclusão de guarda antecipada `if (isEncerrada()) return DANO_NULO;`, garantindo a imutabilidade do resultado após o fim do combate.

---

### BUG-005: Dano Negativo ou Nulo em Cenários de Alta Defesa (Boundary Test)
- **Descrição:** Quando o valor de defesa do oponente superava a soma do ataque com o poder do golpe (`ATK + Poder < DEF`), a fórmula base gerava um valor negativo.
- **Causa Raiz:** Ausência de piso mínimo antes da multiplicação de terreno e tipo.
- **Correção Implementada:** Inserção de `Math.max(DANO_MINIMO_BASE, diferencaAtkDef + golpe.getPoder())` e garantia de `DANO_MINIMO_FINAL = 1` no retorno final.

---

### BUG-006: Desvantagem Elemental Mascarando Limiar de HP da Torcida (Fase 03)
- **Descrição:** Durante a execução inicial de `testTorcidaEmpolgadaConcedeBuffQuandoHpBaixo`, o Pokésal atacante (CharSal - Fogo) utilizava um golpe de poder 60 contra o defensor (SquirtSal - Água). Com a desvantagem elemental ($0.5\times$), o dano final foi reduzido para 35, deixando o defensor com 65 de HP (65% > 30%), o que não ativava a consulta ao `ServicoTorcida`.
- **Causa Raiz:** Desconsideração do multiplicador elemental no cálculo esperado para atingir a zona de perigo ($\le 30\%$).
- **Correção Implementada:** Ajustou-se o defensor para uma criatura de elemento neutro (`CyndaSal` - Fogo), resultando em dano neutro $1.0\times$ de exatamente 70 pontos (reduzindo o HP de 100 para 30, exatamente no limiar regulamentar $\le 30\%$). Commit `0b9818b`.

---

### BUG-007: Falha de UnnecessaryStubbingException com Mockito Strict Mode
- **Descrição:** Ao executar a suíte com `@ExtendWith(MockitoExtension.class)`, o Mockito abortava o teste `testBatalhaBranchesAdicionaisJaCoCo` com `UnnecessaryStubbingException: Unnecessary stubbings detected in test class: servicoTorcida.aferirVibracao(...)`.
- **Causa Raiz:** O teste stubbava o método `aferirVibracao`, porém o golpe aplicado no cenário não reduzia o HP da criatura para a faixa de perigo ($\le 30\%$), fazendo com que a cláusula de guarda de `Batalha.java` evitasse a chamada e deixasse o stub ocioso.
- **Correção Implementada:** O golpe de teste foi configurado com dano suficiente para atingir o limiar de 30% de HP, exercitando legitimamente a chamada stubbada e garantindo 100% de conformidade com o modo estrito do Mockito. Commit `0b9818b`.

---

### BUG-008: Quebra de Combate por Falha em Colaboradores de Infraestrutura
- **Descrição:** Caso o repositório de dados lançasse uma falha de conexão (ex: banco de dados inacessível) ou o serviço de torcida estivesse indisponível, a exceção subia na pilha de execução, quebrando a batalha e impedindo a determinação do campeão do torneio.
- **Causa Raiz:** Ausência de tratamento defensivo em torno das chamadas aos colaboradores de infraestrutura na classe `Batalha.java`.
- **Correção Implementada:** Implementação de blocos `try/catch` defensivos envolvendo `repositorioResultados.salvar(...)` e `servicoTorcida.aferirVibracao(...)`. As exceções são capturadas, registradas na lista `errosRegistrados` para auditoria, e o combate segue seu fluxo normal, preservando o vencedor e o torneio. Commit `b640979`.
