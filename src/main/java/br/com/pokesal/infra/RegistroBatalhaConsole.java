package br.com.pokesal.infra;

import br.com.pokesal.model.Golpe;
import br.com.pokesal.model.ItemDeBatalha;
import br.com.pokesal.model.Pokesal;
import br.com.pokesal.model.Terreno;
import br.com.pokesal.model.TipoStatus;
import br.com.pokesal.model.Treinador;
import br.com.pokesal.service.RegistroBatalha;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementacao de {@link RegistroBatalha} que registra eventos sequenciais em memoria e log.
 */
public class RegistroBatalhaConsole implements RegistroBatalha {
  private final List<String> historicoEventos = new ArrayList<>();

  @Override
  public void registrarAtaque(Treinador atacante, Treinador defensor, Golpe golpe) {
    String evento = String.format("[ATAQUE] %s (%s) usou %s contra %s.",
        atacante.getNome(), atacante.getPokesal().getNome(),
        golpe.getNome(), defensor.getNome());
    historicoEventos.add(evento);
  }

  @Override
  public void registrarDano(Treinador atacante, Treinador defensor, int dano, boolean critico) {
    String evento = String.format("[DANO] %s causou %d de dano em %s%s.",
        atacante.getPokesal().getNome(), dano, defensor.getPokesal().getNome(),
        critico ? " (CRITICO)" : "");
    historicoEventos.add(evento);
  }

  @Override
  public void registrarEfeitoStatus(Pokesal alvo, TipoStatus status) {
    String evento = String.format("[STATUS] %s sofreu efeito de status: %s.",
        alvo.getNome(), status);
    historicoEventos.add(evento);
  }

  @Override
  public void registrarEfeitoTerreno(Terreno terreno, Pokesal alvo) {
    String evento = String.format("[TERRENO] %s atuou sobre %s.",
        terreno.getNome(), alvo.getNome());
    historicoEventos.add(evento);
  }

  @Override
  public void registrarUsoItem(Treinador treinador, ItemDeBatalha item) {
    String evento = String.format("[ITEM] %s utilizou o item %s.",
        treinador.getNome(), item.getNome());
    historicoEventos.add(evento);
  }

  /**
   * Retorna a lista imutavel dos eventos registrados em ordem cronologica.
   *
   * @return historico textual de eventos da batalha.
   */
  public List<String> getHistoricoEventos() {
    return Collections.unmodifiableList(historicoEventos);
  }
}
