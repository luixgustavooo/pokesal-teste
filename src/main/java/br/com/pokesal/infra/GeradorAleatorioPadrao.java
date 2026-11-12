package br.com.pokesal.infra;

import br.com.pokesal.service.GeradorAleatorio;
import java.util.Random;

/**
 * Implementacao padrao de {@link GeradorAleatorio} baseada em {@link java.util.Random}.
 */
public class GeradorAleatorioPadrao implements GeradorAleatorio {
  private final Random random;

  /**
   * Construtor padrao utilizando instancia padrao de Random.
   */
  public GeradorAleatorioPadrao() {
    this(new Random());
  }

  /**
   * Construtor com semente ou instancia controlada de Random.
   *
   * @param random instancia de Random utilizada na geracao.
   */
  public GeradorAleatorioPadrao(Random random) {
    this.random = (random != null) ? random : new Random();
  }

  @Override
  public double gerarDouble() {
    return random.nextDouble();
  }

  @Override
  public int gerarInteiro(int limite) {
    return random.nextInt(limite);
  }

  @Override
  public boolean gerarBooleano() {
    return random.nextBoolean();
  }
}
