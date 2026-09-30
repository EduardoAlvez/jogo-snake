package com.portfolio.snake.core;

/**
 * Uma célula do campo, com coluna e linha.
 *
 * <p>Imutável de propósito: segmentos da cobra são recriados a cada passo, e
 * uma célula que muda por baixo de quem a segura é a forma mais comum de
 * corromper a lista de segmentos. O {@link #chave()} permite usar a célula como
 * chave de {@code Set} para checagem de colisão em tempo constante.
 */
public final class Celula {

    private final int x;
    private final int y;

    public Celula(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Coluna. */
    public int getX() {
        return x;
    }

    /** Linha. */
    public int getY() {
        return y;
    }

    /**
     * Identificador único da célula dentro de um campo, para usar em
     * {@code Set} ou {@code Map} sem alocar objetos.
     *
     * @return a chave, que só faz sentido comparada com células do mesmo campo
     */
    public long chave() {
        return (((long) x) << 32) ^ (y & 0xFFFFFFFFL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Celula)) {
            return false;
        }
        Celula outra = (Celula) o;
        return x == outra.x && y == outra.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
