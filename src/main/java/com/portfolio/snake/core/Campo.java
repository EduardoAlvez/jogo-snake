package com.portfolio.snake.core;

/**
 * A grade do tabuleiro e a regra de borda.
 *
 * <p>O Snake clássico tem dois comportamentos de borda incompatíveis, e
 * escolher um no meio da partida é o que confunde: ou a parede mata, ou a cobra
 * "dá a volta" e reaparece do outro lado. Aqui os dois existem como
 * {@link Borda} e ficam separados da lógica de movimento — o {@link JogoSnake}
 * consulta {@link #traduzir} e nunca precisa saber qual está em uso.
 */
public final class Campo {

    /** Comportamento da borda do campo. */
    public enum Borda {
        /** A cobra morre ao encostar na parede. É o Snake clássico. */
        MORRE,

        /** A cobra reaparece no lado oposto. Torna o jogo mais leve. */
        WRAP
    }

    private final int largura;
    private final int altura;
    private final Borda borda;

    /**
     * @param largura número de colunas, positivo
     * @param altura  número de linhas, positivo
     * @param borda   o que acontece ao encostar na parede
     */
    public Campo(int largura, int altura, Borda borda) {
        if (largura < 2 || altura < 2) {
            throw new IllegalArgumentException(
                    "O campo precisa de ao menos 2x2, veio " + largura + "x" + altura);
        }
        this.largura = largura;
        this.altura = altura;
        this.borda = borda;
    }

    /** Campo quadrado com a borda informada. */
    public Campo(int lado, Borda borda) {
        this(lado, lado, borda);
    }

    public int getLargura() {
        return largura;
    }

    public int getAltura() {
        return altura;
    }

    public Borda getBorda() {
        return borda;
    }

    /** Total de células do campo. */
    public int totalCelulas() {
        return largura * altura;
    }

    /**
     * Verifica se a célula está dentro do campo.
     *
     * @param x coluna
     * @param y linha
     * @return {@code true} se existe no tabuleiro
     */
    public boolean dentro(int x, int y) {
        return x >= 0 && x < largura && y >= 0 && y < altura;
    }

    /**
     * Aplica a regra de borda a uma célula e devolve a posição resultante.
     *
     * <p>Com {@link Borda#MORRE} a posição volta igual, e quem chamou descobre
     * a morte por {@link #dentro}. Com {@link Borda#WRAP} a posição é dobrada
     * de volta para dentro do campo.
     *
     * @param x    coluna desejada, possivelmente fora do campo
     * @param y    linha desejada, possivelmente fora do campo
     * @param saida array de tamanho 2 que recebe {@code [x, y]} corrigidos
     * @return {@code true} se a célula é jogável depois da correção
     */
    public boolean traduzir(int x, int y, int[] saida) {
        int nx = x;
        int ny = y;
        if (borda == Borda.WRAP) {
            nx = ((x % largura) + largura) % largura;
            ny = ((y % altura) + altura) % altura;
        }
        saida[0] = nx;
        saida[1] = ny;
        return dentro(nx, ny);
    }

    /**
     * Dobra a coordenada para dentro do campo, sempre.
     *
     * <p>É o que {@link #traduzir} só faz no modo {@link Borda#WRAP}. Existe
     * separado porque o poder do fantasma precisa dar a volta numa parede que,
     * no resto da partida, mata: a cabeça não pode ficar fora da grade, onde o
     * desenho não saberia onde colocá-la, e a cobra também não pode simplesmente
     * ignorar a parede e sumir.
     *
     * @param x     coluna desejada, possivelmente fora do campo
     * @param y     linha desejada, possivelmente fora do campo
     * @param saida array de tamanho 2 que recebe {@code [x, y]} dobrados
     */
    public void dobrarParaDentro(int x, int y, int[] saida) {
        saida[0] = ((x % largura) + largura) % largura;
        saida[1] = ((y % altura) + altura) % altura;
    }

    /**
     * Verifica se a parede mata nesta configuração, ou seja, se a borda mais
     * próxima está a menos de um passo.
     *
     * @param x   coluna da cabeça
     * @param y   linha da cabeça
     * @param dx  deslocamento em X do movimento
     * @param dy  deslocamento em Y do movimento
     * @return {@code true} se o próximo passo sai do campo e mata
     */
    public boolean paredeMata(int x, int y, int dx, int dy) {
        if (borda == Borda.WRAP) {
            return false;
        }
        return !dentro(x + dx, y + dy);
    }
}
