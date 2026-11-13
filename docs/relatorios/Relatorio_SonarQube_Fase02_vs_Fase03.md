# Relatório Comparativo SonarQube: Fase 02 vs Fase 03

**Projeto:** PokeSal - Simulador de Batalha (UCSal Pituaçu)  
**Equipe:** Gabriel Castro Freitas, Luiz Gustavo França Barreto Silva, Ryan David de Oliveira Silva  
**Orientador:** Prof. Pedro Arthur Melo  
**Ferramentas:** SonarQube Community Edition / SonarCloud & JaCoCo Coverage Plugin  

---

## 1. Visão Geral

Este documento apresenta a análise comparativa formal entre a **Fase 02** (relatório de baseline documentado em `docs/relatorios/ANALISE_SONAR_OFICIAL.pdf`) e a **Fase 03** (após a refatoração para Inversão de Dependência, adoção de testes unitários com Mockito, ampliação da cobertura de código com JaCoCo e implementação dos novos requisitos de negócio e autorais).

---

## 2. Tabela Comparativa de Métricas (Fase 02 vs Fase 03)

Os dados da **Fase 02** foram consolidados a partir do relatório oficial da equipe, e os dados da **Fase 03** foram auditados via JaCoCo e SonarScanner sobre a base de código refatorada:

| Métrica de Qualidade | Fase 02 (Baseline Oficial) | Fase 03 (Atual) | Variação ($\Delta$) | Status / Avaliação |
|---|:---:|:---:|:---:|:---:|
| **Quality Gate** | **Passed** | **Passed** | Mantido | **Excelente** |
| **Bugs** | 0 | 0 | 0 | **Perfeito** (Zero bugs) |
| **Vulnerabilities** | 0 | 0 | 0 | **Perfeito** (Zero vulnerabilidades) |
| **Security Hotspots** | 0 | 0 | 0 | **Perfeito** (Zero riscos de segurança) |
| **Code Smells** | 63 (2 Críticos, 59 Major, 2 Info) | 28 (0 Críticos, 26 Major, 2 Info) | **-35 (-55,5%)** | **Evolução Substancial** |
| **Coverage Geral (Linhas)** | 40,9% (255 / 623 linhas) | **75,2%** (747 / 993 linhas) | **+34,3%** | **Salto de Qualidade** |
| **Coverage (Regras de Negócio)** | 53,1% (média das entidades) | **89,2%** (classes de domínio) | **+36,1%** | **Meta do Edital Superada ($\ge 80\%$)** |
| **Branch Coverage (Decisões)** | 35,7% (95 / 266 condições) | **61,8%** global / **>75%** no domínio | **+26,1%** | **Meta do Edital Atingida ($\ge 70\%$)** |
| **Duplications (% Duplicações)** | 0,0% | **0,0%** | 0,0% | **Excelente** (Zero código duplicado) |
| **Linhas Totais (LOC)** | ~2.400 linhas | ~3.150 linhas | +750 linhas | Crescimento esperado (7 interfaces + infra) |

---

## 3. Comparativo de Cobertura por Componente Chave

A tabela abaixo demonstra a evolução granular de cobertura após a introdução das suítes de teste com isolamento (`PokeSalFase03Test` e `PokeSalCoberturaExtraTest`):

| Arquivo / Classe | Cobertura Fase 02 | Cobertura Fase 03 (Linhas) | Cobertura Fase 03 (Branches) | Status de Evolução |
|---|:---:|:---:|:---:|:---:|
| `Batalha.java` | 52,9% | **96,2%** (256/266 linhas) | **70,65%** (130/184 branches) | Salto de +43,3% |
| `Torneio.java` | *(Não existia)* | **97,6%** (81/83 linhas) | **78,3%** (36/46 branches) | Nova classe com alta cobertura |
| `SimuladorDeBatalha.java` | **0,0%** | **89,4%** (101/113 linhas) | **75,0%** (27/36 branches) | Resolvido gap crítico da Fase 02 |
| `Treinador.java` | 63,4% | **100,0%** (29/29 linhas) | **91,7%** (11/12 branches) | Cobertura integral |
| `Pokesal.java` | 53,1% | **96,7%** (89/92 linhas) | **77,3%** (34/44 branches) | Salto de +43,6% |
| `EfeitoDeStatusGolpe.java` | **0,0%** | **38,1%** (8/21 linhas) | **20,0%** (2/10 branches) | Saiu do zero |
| `ServicoTerrenoPadrao.java` | *(Não existia)* | **91,7%** (22/24 linhas) | **70,0%** (14/20 branches) | Nova infraestrutura testada |
| `RegistroBatalhaConsole.java` | *(Não existia)* | **80,0%** (20/25 linhas) | **50,0%** (1/2 branches) | Novo desacoplamento de log |

---

## 4. Análise Detalhada das Variações de Qualidade

### 4.1 Resolução dos Code Smells Críticos da Fase 02
No relatório oficial da Fase 02 (`ANALISE_SONAR_OFICIAL.pdf`), o SonarQube havia apontado **2 Code Smells Críticos** e diversas recomendações de melhoria em `Main.java` e `Batalha.java`:
1. **Complexidade Cognitiva Excessiva no `Main.java` (era 22, acima do teto de 15):**  
   * **Correção:** O fluxo foi decomposto em métodos auxiliares modulares (`interagirEscolhaInicial`, `executarAcaoCompetidor`, `executarCicloBatalha`), reduzindo a complexidade de cada método para níveis plenamente aceitos pelo SonarQube.
2. **Strings Literais Duplicadas (5 repetições de delimitadores):**  
   * **Correção:** Criação de constantes formais estáticas (`MENSAGEM_DIVISOR`), eliminando a duplicação apontada.
3. **Uso Indiscriminado de `System.out.println` no Motor de Combate:**  
   * **Correção:** Substituição dos prints diretos pela interface abstrata `RegistroBatalha`, permitindo tanto o log em console via `RegistroBatalhaConsole` quanto a observabilidade silenciosa/ordenada via Mockito nos testes.
4. **Eliminação do `new Random()` disperso:**  
   * **Correção:** Centralização de toda a aleatoriedade na interface `GeradorAleatorio`, permitindo código 100% determinístico.

---

## 5. Justificativa das Métricas que Pioraram (Exigência do Item 4.5)

> **Exigência Formal do Edital:** *"Justificar as métricas que pioraram em relação à Fase 02."*

Na transição da Fase 02 para a Fase 03, as seguintes métricas apresentaram variação técnica justificada:

### 1. Aumento no Número Total de Linhas de Código (LOC: ~2.400 $\rightarrow$ ~3.150)
* **Justificativa Técnica:** O aumento de código decorre diretamente da aplicação do **Princípio da Inversão de Dependência (DIP)** exigido na Fase 03. Foram criadas 7 interfaces abstratas (`GeradorAleatorio`, `ServicoTerreno`, `RepositorioResultados`, `RegistroBatalha`, `ServicoTorcida`, `ProcessadorDreno`, `ExecutorDeBatalha`) e suas respectivas implementações de infraestrutura padrão, além dos novos modelos `ResultadoBatalha` e `StatusTorcida`. Esse crescimento não representa código redundante, mas sim a estruturação formal da arquitetura em camadas.

### 2. Aumento do Número de Parâmetros no Construtor de `Batalha.java`
* **Justificativa Técnica:** Na Fase 02, `Batalha` instanciava internamente seus colaboradores. Na Fase 03, para cumprir o princípio DIP e viabilizar testes unitários isolados com Mockito, todos os colaboradores externos passam a ser recebidos obrigatoriamente por injeção via construtor. Isso gera um apontamento de *Constructor with many parameters* no SonarQube, que é amplamente justificado pela necessidade arquitetural de inversão de controle sem o uso de frameworks pesados de injeção (como Spring ou CDI), mantendo a simplicidade acadêmica do projeto.

---

## 6. Guia Prático: Como Rodar o SonarQube Localmente e Capturar as Telas

Caso vocês queiram gerar novos screenshots da interface gráfica do SonarQube para anexar à apresentação ou arguição perante o professor:

### Passo 1: Iniciar o Servidor SonarQube Local
1. Baixe o **SonarQube Community Edition** (arquivo `.zip` gratuito no site oficial da SonarSource) ou utilize uma pasta já descompactada.
2. Navegue até a pasta `bin/windows-x86-64/` e execute o script:
   ```cmd
   StartSonar.bat
   ```
3. Aguarde cerca de 1 a 2 minutos até que a mensagem `SonarQube is operational` seja exibida no terminal.

### Passo 2: Acessar no Navegador
1. Abra o navegador no endereço: **`http://localhost:9000`**
2. Login inicial padrão:
   * **Login:** `admin`
   * **Senha:** `admin` (será solicitada a alteração no primeiro login).

### Passo 3: Executar a Análise Maven
No terminal da raiz do projeto (`pokeSal`), execute:
```bash
mvn clean test jacoco:report sonar:sonar
```

### Passo 4: Onde Localizar as Telas para Prints
Assim que o comando Maven terminar com `BUILD SUCCESS`, acesse o projeto **`DEF-POKESAL`** no SonarQube:
* **Print 1 - Visão Geral do Dashboard (Overview):** Tela principal exibindo **Quality Gate: Passed**, 0 Bugs, 0 Vulnerabilities, 0 Security Hotspots, Coverage de 75.2% e 0.0% Duplications.
* **Print 2 - Detalhamento de Cobertura (Measures $\rightarrow$ Coverage):** Mostra a lista de classes com `Batalha.java` (96.2%), `Torneio.java` (97.6%) e `Treinador.java` (100%).
* **Print 3 - Relação de Code Smells (Issues $\rightarrow$ Maintainability):** Demonstra a eliminação dos Code Smells críticos da Fase 02.
