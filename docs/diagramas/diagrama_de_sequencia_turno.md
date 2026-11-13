# Diagrama de Sequência - Turno Completo de Batalha (Fase 03)

**Projeto:** PokeSal - Simulador de Batalha (UCSal Pituaçu)  
**Notação:** Mermaid Sequence Diagram  
**Objetivo:** Demonstrar a orquestração e a ordem cronológica estrita de eventos em um turno de combate, destacando a consulta aos colaboradores mockáveis (`GeradorAleatorio`, `ServicoTerreno`, `RegistroBatalha`, `ProcessadorDreno`, `ServicoTorcida` e `RepositorioResultados`).

---

```mermaid
sequenceDiagram
    autonumber
    actor TreinadorAtivo as Treinador Atacante
    participant Batalha as :Batalha
    participant Gerador as :GeradorAleatorio
    participant TerrenoSvc as :ServicoTerreno
    participant Registro as :RegistroBatalha
    participant DrenoSvc as :ProcessadorDreno
    participant TorcidaSvc as :ServicoTorcida
    participant Repo as :RepositorioResultados

    TreinadorAtivo->>Batalha: executarAcaoDeAtaque(atacante, defensor, golpe)
    activate Batalha

    %% 1. Verificação de Precisão
    Batalha->>Gerador: gerarDouble()
    Gerador-->>Batalha: rolagemPrecisao (ex: 0.10)
    
    alt rolagemPrecisao >= (golpe.precisao / 100.0) [Errou o Golpe]
        Batalha->>Registro: registrarDano(atacante, defensor, 0, false)
        Batalha-->>TreinadorAtivo: dano = 0
    else Golpe Acertou

        %% 2. Notificação de Início de Ataque
        Batalha->>Registro: registrarAtaque(atacante, defensor, golpe)

        %% 3. Consulta ao Terreno e Clima
        Batalha->>TerrenoSvc: obterModificadorDano(terreno, tipoGolpe, horario)
        TerrenoSvc-->>Batalha: modTerreno (ex: 1.15 se Dia no Asfalto)

        %% 4. Verificação de Acerto Crítico (6.25%)
        Batalha->>Gerador: gerarDouble()
        Gerador-->>Batalha: rolagemCritico (ex: 0.02 -> Crítico!)

        %% 5. Aplicação de Dano e Registro
        Note over Batalha: Calcula dano final:<br/>(Base * Elemental * Terreno * Crítico * BuffVigor)
        Batalha->>Registro: registrarDano(atacante, defensor, danoCausado, true)
        
        %% 6. Processamento de Golpe com Dreno (RA-F03-02)
        opt golpe.isGolpeDreno == true
            Batalha->>DrenoSvc: calcularCura(danoCausado, taxaDreno)
            DrenoSvc-->>Batalha: pontosDeCura
            Note over Batalha: Atacante regenera HP (sem estourar hpMax)
        end

        %% 7. Efeito de Status do Golpe
        opt golpe possui efeito de status
            Batalha->>Gerador: gerarDouble()
            Gerador-->>Batalha: rolagemStatus
            opt rolagemStatus < chance
                Batalha->>Registro: registrarEfeitoStatus(defensor, tipoStatus)
            end
        end

        %% 8. Registro de Terreno
        Batalha->>Registro: registrarEfeitoTerreno(terreno, atacante)

        %% 9. Aferição da Torcida (RA-F03-01)
        opt defensor.hpAtual <= 30% do hpMax (Faixa de Perigo)
            Batalha->>TorcidaSvc: aferirVibracao(defensor)
            TorcidaSvc-->>Batalha: StatusTorcida.EMPOLGADA
            Note over Batalha: Ativa buff 'Vigor de Pituaçu' (+15% prox ataque)
        end

        %% 10. Verificação de Encerramento e Persistência Resiliente
        opt defensor desmaiou (HP <= 0) ou Turno 10 atingido
            Note over Batalha: Define vencedor e encerra combate
            Batalha->>Registro: registrarFimDeBatalha(vencedor, perdedor, turnos)
            
            rect rgb(240, 248, 255)
                note right of Batalha: Persistência Resiliente (try/catch)
                Batalha->>Repo: salvar(ResultadoBatalha)
                alt Repositório Falha (ex: Banco Indisponível)
                    Repo-->>Batalha: throw RuntimeException
                    Note over Batalha: Registra erro em log sem invalidar a vitória
                else Repositório Sucesso
                    Repo-->>Batalha: void
                end
            end
        end

        Batalha-->>TreinadorAtivo: danoCausado
    end

    deactivate Batalha
```
