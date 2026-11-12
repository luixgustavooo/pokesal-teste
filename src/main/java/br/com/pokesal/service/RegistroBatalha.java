package br.com.pokesal.service;

import br.com.pokesal.model.Golpe;
import br.com.pokesal.model.ItemDeBatalha;
import br.com.pokesal.model.Pokesal;
import br.com.pokesal.model.Terreno;
import br.com.pokesal.model.TipoStatus;
import br.com.pokesal.model.Treinador;

/**
 * Contrato para registro sequencial dos eventos ocorridos em cada turno de batalha.
 */
public interface RegistroBatalha {

  /**
   * Registra o inicio da acao de ataque de um competidor.
   *
   * @param atacante treinador que ordenou o ataque.
   * @param defensor treinador alvo.
   * @param golpe golpe selecionado.
   */
  void registrarAtaque(Treinador atacante, Treinador defensor, Golpe golpe);

  /**
   * Registra o dano efetivo causado pelo golpe e se houve acerto critico.
   *
   * @param atacante treinador atacante.
   * @param defensor treinador defensor.
   * @param dano valor de dano infligido (0 se o golpe errou).
   * @param critico true se houve acerto critico.
   */
  void registrarDano(Treinador atacante, Treinador defensor, int dano, boolean critico);

  /**
   * Registra a aplicacao ou tick de efeito de status em um Pokesal.
   *
   * @param alvo Pokesal afetado pelo status.
   * @param status tipo de condicao de status aplicada.
   */
  void registrarEfeitoStatus(Pokesal alvo, TipoStatus status);

  /**
   * Registra a ativacao do efeito ambiental do terreno durante o turno.
   *
   * @param terreno arena responsavel pelo efeito.
   * @param alvo Pokesal impactado pelo terreno.
   */
  void registrarEfeitoTerreno(Terreno terreno, Pokesal alvo);

  /**
   * Registra o consumo de um item da mochila por um treinador.
   *
   * @param treinador treinador que utilizou o item.
   * @param item item consumido.
   */
  void registrarUsoItem(Treinador treinador, ItemDeBatalha item);
}
