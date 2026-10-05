package com.portfolio.snake.skin;

import java.awt.Color;
import java.util.Objects;

/**
 * As cores de uma skin.
 *
 * <p>Só dados: nenhuma decisão de desenho mora aqui. A mistura ao longo do corpo,
 * o raio do canto e a posição dos olhos continuam no
 * {@code DesenhoJogo}, que é quem pinta. Uma skin que calculasse pixels seria um
 * segundo pintor competindo com o primeiro.
 *
 * <p><b>Por que paleta, e não sprite.</b> São cinco cores. Um sprite exigiria 14
 * PNGs por skin — um para cada {@code Forma}, e são 14 — e 56 arquivos para as
 * quatro skins, com um renderizador por forma. A paleta entrega as três skins que
 * são só cor com uma fração do trabalho, e é 100% testável sem display.
 */
public final class Skin {

    private final String id;
    private final String nome;
    private final Color corpoClaro;
    private final Color corpoEscuro;
    private final Color cabeca;
    private final Color sombra;
    private final Color olhos;

    /**
     * @param id          identificador estável, gravado no disco; não pode ser vazio
     * @param nome        como aparece na tela
     * @param corpoClaro  tom do corpo junto à cabeça
     * @param corpoEscuro tom do corpo junto ao rabo
     * @param cabeca      tom da cabeça
     * @param sombra      sombra do vinco dos cantos, com alfa
     * @param olhos       cor dos olhos
     */
    public Skin(String id, String nome, Color corpoClaro, Color corpoEscuro,
            Color cabeca, Color sombra, Color olhos) {
        this.id = exigeTexto(id, "id");
        this.nome = exigeTexto(nome, "nome");
        this.corpoClaro = Objects.requireNonNull(corpoClaro, "corpoClaro");
        this.corpoEscuro = Objects.requireNonNull(corpoEscuro, "corpoEscuro");
        this.cabeca = Objects.requireNonNull(cabeca, "cabeca");
        this.sombra = Objects.requireNonNull(sombra, "sombra");
        this.olhos = Objects.requireNonNull(olhos, "olhos");
    }

    private static String exigeTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " nao pode ser vazio: " + valor);
        }
        return valor;
    }

    /** Identificador estável, o que vai para o disco. */
    public String getId() {
        return id;
    }

    /** Nome que aparece para o jogador. */
    public String getNome() {
        return nome;
    }

    /** Tom do corpo junto à cabeça. */
    public Color getCorpoClaro() {
        return corpoClaro;
    }

    /** Tom do corpo junto ao rabo. */
    public Color getCorpoEscuro() {
        return corpoEscuro;
    }

    /** Tom da cabeça. */
    public Color getCabeca() {
        return cabeca;
    }

    /** Sombra do vinco dos cantos, com alfa. */
    public Color getSombra() {
        return sombra;
    }

    /** Cor dos olhos. */
    public Color getOlhos() {
        return olhos;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Skin s)) {
            return false;
        }
        return id.equals(s.id) && nome.equals(s.nome)
                && corpoClaro.equals(s.corpoClaro) && corpoEscuro.equals(s.corpoEscuro)
                && cabeca.equals(s.cabeca) && sombra.equals(s.sombra)
                && olhos.equals(s.olhos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nome, corpoClaro, corpoEscuro, cabeca, sombra, olhos);
    }

    @Override
    public String toString() {
        return id;
    }
}