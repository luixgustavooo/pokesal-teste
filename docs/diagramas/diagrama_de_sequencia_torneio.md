# Diagrama de Sequência - Chaveamento e Registro de Torneio (Fase 03)

**Projeto:** PokeSal - Simulador de Batalha (UCSal Pituaçu)  
**Notação:** Mermaid Sequence Diagram  
**Objetivo:** Demonstrar o fluxo de execução do chaveamento eliminatório (4 ou 8 competidores), orquestração de partidas via `ExecutorDeBatalha`, resolução de confrontos, avanço de vencedores e persistência resiliente no `RepositorioResultados`.

---

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Main / Simulador
    participant Torneio as :Torneio
    participant ServTerreno as :ServicoTerreno
    participant Executor as :ExecutorDeBatalha
    participant Batalha as :Batalha
    participant Repo as :RepositorioResultados
    participant Gerador as :GeradorAleatorio

    Cliente->>Torneio: executarTorneio(participantes [4 ou 8])
    activate Torneio

    %% 1. Validação defensiva de entrada
    Note over Torneio: Valida se participantes == 4 ou 8<br/>e se todos possuem Pokésal válido

    %% 2. Loop de Fases Eliminatórias (Quartas -> Semis -> Final)
    loop Para cada fase (ex: Semifinais com 4 competidores)
        
        loop Para cada confronto da fase (Par de Treinadores: t1 vs t2)
            Torneio->>ServTerreno: obterTerrenoAtual(horario)
            ServTerreno-->>Torneio: terrenoSorteado

            Torneio->>Executor: executar(t1, t2, terrenoSorteado)
            activate Executor

            Executor->>Batalha: new Batalha(t1, t2, terrenoSorteado, colaboradores...)
            activate Batalha
            
            Note over Batalha: Executa rodadas de combate<br/>até nocaute ou limite de turnos
            
            %% Encerramento da Batalha e Persistência
            opt Fim da Batalha
                Batalha->>Repo: salvar(ResultadoBatalha)
                alt Falha no Repositório
                    Repo-->>Batalha: throw DataAccessException
                    Note over Batalha: Captura exceção e continua (Resiliência)
                else Sucesso
                    Repo-->>Batalha: salvo com sucesso
                end
            end

            Batalha-->>Executor: batalhaConcluida
            deactivate Batalha

            Executor-->>Torneio: batalhaConcluida
            deactivate Executor

            %% Apuração do Vencedor do Confronto
            alt batalha.getVencedor() != null
                Note over Torneio: Vencedor avança para a próxima fase:<br/>vencedor.incrementarBatalhasVencidas()
            else Empate (vencedor == null)
                Note over Torneio: Desempate por Velocidade (SPD) do Pokésal
                opt SPD idêntico
                    Torneio->>Gerador: gerarBooleano()
                    Gerador-->>Torneio: desempateAleatorio
                end
                Note over Torneio: Vencedor desempatado avança de fase
            end

            Note over Torneio: Restaura atributos do vencedor<br/>para o próximo confronto
        end

        Note over Torneio: Lista de competidores reduzida pela metade<br/>(8 -> 4 -> 2 -> Campeão)
    end

    Torneio-->>Cliente: Treinador Campeão do Torneio UCSal
    deactivate Torneio
```
