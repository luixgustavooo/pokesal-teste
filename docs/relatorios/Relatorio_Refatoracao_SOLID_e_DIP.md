# Relatório Técnico de Refatoração: Testabilidade, Acoplamento e Inversão de Dependência (SOLID)

**Projeto:** PokeSal - Simulador de Batalha (UCSal Pituaçu)  
**Equipe:** Gabriel Castro Freitas, Luiz Gustavo França Barreto Silva, Ryan David de Oliveira Silva  
**Orientador:** Prof. Pedro Arthur Melo  
**Fase:** 03 - Isolamento com Mockito e Inversão de Dependência  

---

## 1. Contexto e Motivação da Refatoração

Nas Fases 01 e 02, o sistema possuía alto acoplamento com dependências concretas e não determinísticas, tais como:
1. Geração direta de números pseudoaleatórios via `new Random()` ou `Math.random()`;
2. Avaliação de terrenos e modificadores diretamente no fluxo interno de combate;
3. Saída direta de logs via console sem possibilidade de auditoria ou inspeção sequencial;
4. Ausência de persistência estruturada e desacoplada dos resultados das partidas;
5. Torneio conduzido de forma estática no `Main.java`, sem chaveamento flexível e sem injeção de motor de combate.

Essas características impediam a escrita de testes unitários previsíveis e repetíveis. Para atender às exigências da Fase 03, aplicamos o **Princípio da Inversão de Dependência (DIP - SOLID)** e o isolamento de colaboradores com Mockito.

---

## 2. Aplicação do Princípio da Inversão de Dependência (DIP)

> *"Módulos de alto nível não devem depender de módulos de baixo nível. Ambos devem depender de abstrações. Abstrações não devem depender de detalhes. Detalhes devem depender de abstrações."* (Robert C. Martin)

### Antes (Alto Acoplamento):
```text
[Batalha] ──> new Random()
[Batalha] ──> System.out.println()
[Torneio] ──> Batalha instanciada diretamente no loop
```

### Depois (Inversão de Dependência - DIP):
```text
[Batalha] ──> <<interface>> GeradorAleatorio <──.. GeradorAleatorioPadrao
[Batalha] ──> <<interface>> ServicoTerreno   <──.. ServicoTerrenoPadrao
[Batalha] ──> <<interface>> RegistroBatalha  <──.. RegistroBatalhaConsole
[Batalha] ──> <<interface>> RepositorioResultados <──.. RepositorioResultadosEmMemoria
[Batalha] ──> <<interface>> ServicoTorcida   <──.. ServicoTorcidaPadrao
[Batalha] ──> <<interface>> ProcessadorDreno <──.. ProcessadorDrenoPadrao
[Torneio] ──> <<interface>> ExecutorDeBatalha <──.. ExecutorDeBatalhaPadrao
```

---

## 3. Mapeamento de Classes Refatoradas e Justificativas

| Classe | Alterações Realizadas | Motivação & Benefício Arquitetural |
|---|---|---|
| **`Batalha.java`** | Injeção via construtor de `GeradorAleatorio`, `ServicoTerreno`, `Clock`, `RegistroBatalha`, `RepositorioResultados`, `ServicoTorcida` e `ProcessadorDreno`. Implementação de acerto crítico (6,25%), verificação de precisão (0-100) e persistência resiliente com tratamento de exceções (`try/catch`). | Redução radical do acoplamento. A classe de combate passa a focar puramente nas regras de negócio, delegando sorteios, auditoria e persistência a contratos abstratos mockáveis. |
| **`Torneio.java`** | Criação de serviço de orquestração com injeção de `ExecutorDeBatalha`, `ServicoTerreno`, `GeradorAleatorio` e `Clock`. Suporte dinâmico a 4 ou 8 participantes em chaveamento eliminatório. | Permite testar o chaveamento em isolamento absoluto, mockando desfechos de partidas sem necessidade de simular turnos reais de combate. |
| **`Golpe.java`** | Adição dos atributos `precisao` (0 a 100), `isGolpeDreno` (boolean) e `taxaDreno` (double). | Permite a implementação das regras de precisão da Fase 03 e o suporte nativo ao requisito autoral RA-F03-02 (Golpe de Absorção de Vida). |
| **`Pokesal.java`** | Adição do estado e métodos de controle do buff *Vigor de Pituaçu* (`isVigorDePituacuAtivo()`, `ativarVigorDePituacu()`, `consumirVigorDePituacu()`). | Atende ao requisito autoral RA-F03-01 de forma desacoplada, permitindo que a criatura retenha o bônus ofensivo até que seu próximo ataque seja deferido. |
| **`SimuladorDeBatalha.java`** | Injeção de `GeradorAleatorio` no construtor e criação do método `cadastrarTreinadoresTorneio(int, String)`. | Remove chamadas diretas a `java.util.Random`, possibilitando testes unitários determinísticos dos sorteios de Pokésal, itens e terrenos. |

---

## 4. Ganhos de Testabilidade e Resiliência

1. **Determinismo:** Em vez de depender da sorte do processador, os testes agora utilizam `when(gerador.gerarDouble()).thenReturn(...)` para forçar acertos críticos (1.5x) ou erros de precisão.
2. **Observabilidade Ordenada:** O colaborador `RegistroBatalha` permite validar via `InOrder` a sequência regulamentar de eventos de cada turno (ataque $\rightarrow$ dano $\rightarrow$ status $\rightarrow$ terreno $\rightarrow$ item).
3. **Resiliência a Falhas:** A persistência em `RepositorioResultados` é protegida: se o banco falhar (simulado via `doThrow()`), a partida não é abortada e o vencedor permanece coroado, registrando o erro defensivamente.
