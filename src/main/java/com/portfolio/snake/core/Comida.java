package com.portfolio.snake.core;

import java.util.Random;

/**
 * A comida: onde está e quanto vale.
 *
 * <p>Repartida em célula cheia, o que é o padrão do Snake de grade.
 */
public final class Comida {

    private int x;
    private int y;
    private final int pontos;

    /**
     * @param x      coluna inicial
     * @param y      linha inicial
     * @param pontos valor ao comer
     */
    public Comida(int x, int y, int pontos) {
        this.x = x;
        this.y = y;
        this.pontos = pontos;
    }

    /** Cria a comida com o valor padrão de 10 pontos. */
    public static Comida padrao(int x, int y) {
        return new Comida(x, y, 10);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getPontos() {
        return pontos;
    }

    /** A célula onde a comida está. */
    public Celula celula() {
        return new Celula(x, y);
    }

    /** Move a comida para outra célula, sem alterar o valor. */
    public void moverPara(int novoX, int novoY) {
        this.x = novoX;
        this.y = novoY;
    }

    /**
     * Sorteia uma célula livre, fora do corpo da cobra e de outros obstáculos.
     *
     * <p><b>Este método nunca fica em laço infinito.</b> Tentar
     * "sorteia até achar uma célula livre" é o terceiro erro clássico: quando a
     * cobra cobre o campo inteiro, nenhuma tentativa sorteia livre e o jogo
     * trava. Aqui o número de tentativas é limitado, e se o campo estiver
     * lotado o método devolve {@code null} — o sinal de que o jogador venceu.
     *
     * @param campo    o tabuleiro
     * @param aleatorio fonte de sorteio, para os testes poderem fixar a semente
     * @param ocupadas células já ocupadas (corpo da cobra, poderes, obstáculos)
     * @return a célula sorteada, ou {@code null} se o campo está lotado
     */
    public static Celula sortearLivre(Campo campo, Random aleatorio,
            java.util.Set<Long> ocupadas) {
        int tentativas = campo.totalCelulas() * 2;
        for (int i = 0; i < tentativas; i++) {
            int x = aleatorio.nextInt(campo.getLargura());
            int y = aleatorio.nextInt(campo.getAltura());
            long chave = new Celula(x, y).chave();
            if (!ocupadas.contains(chave)) {
                return new Celula(x, y);
            }
        }
        // Campo lotado: nenhuma célula livre depois de duas voltas completas.
        return null;
    }
}
