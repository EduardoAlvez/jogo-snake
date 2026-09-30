package com.portfolio.snake.core;

/**
 * As quatro direções de movimento da cobra.
 *
 * <p>Cada constante sabe o próprio oposto ({@link #oposto()}). Existe para
 * centralizar a única comparação que importa na hora de virar — o teste
 * "essa direção é a reversa da atual?" — que é exatamente onde nasce o
 * primeiro erro clássico do Snake: virar 180° sobre o próprio corpo e morrer
 * sem motivo aparente.
 *
 * <p>Com {@link #oposto()} disponível, esse teste é uma chamada e não uma
 * comparação repetida em quatro lugares, o que faz o erro de inverter a
 * comparação (comparar com a direção <em>atual</em> em vez da <em>enfileirada</em>)
 * muito menos provável.
 */
public enum Direcao {

    /** Para cima: o Y do campo cresce para baixo, então "cima" subtrai. */
    CIMA(0, -1),

    /** Para a direita: soma no X. */
    DIREITA(1, 0),

    /** Para baixo: soma no Y. */
    BAIXO(0, 1),

    /** Para a esquerda: subtrai no X. */
    ESQUERDA(-1, 0);

    private final int dx;
    private final int dy;

    Direcao(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** Deslocamento no X (coluna) por um passo. */
    public int dx() {
        return dx;
    }

    /** Deslocamento no Y (linha) por um passo. */
    public int dy() {
        return dy;
    }

    /**
     * A direção exatamente oposta a esta, ou seja, a que representaria uma
     * inversão de 180°.
     *
     * @return a direção contrária a esta
     */
    public Direcao oposto() {
        switch (this) {
            case CIMA:
                return BAIXO;
            case BAIXO:
                return CIMA;
            case ESQUERDA:
                return DIREITA;
            case DIREITA:
            default:
                return ESQUERDA;
        }
    }

    /**
     * Verifica se {@code outra} é a reversa desta, isto é, se virar para ela
     * agora faria a cabeça dar de cara com o segundo segmento.
     *
     * @param outra direção a comparar
     * @return {@code true} se as duas são opostas
     */
    public boolean eOposta(Direcao outra) {
        return outra == oposto();
    }

    /**
     * Verifica se {@code outra} muda o eixo em relação a esta.
     *
     * <p>É o teste de "vou virar de verdade": horizontal contra vertical. Serve
     * para a cobra poder virar em uma direção que não seja a reversa, e para a
     * UI saber se o desenho do segmento é reto ou canto.
     *
     * @param outra direção a comparar
     * @return {@code true} se uma é horizontal e a outra vertical
     */
    public boolean mudaEixo(Direcao outra) {
        return outra != null && outra != this && (dx != 0) != (outra.dx != 0);
    }
}
