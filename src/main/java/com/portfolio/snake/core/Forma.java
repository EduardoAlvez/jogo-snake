package com.portfolio.snake.core;

/**
 * A forma de um segmento da cobra, derivada de por onde ele entrou e para onde
 * sai.
 *
 * <p>É o que separa uma cobra de uma centipede: sem saber se o segmento faz
 * curva, o desenho só pode repetir a mesma peça e a cobra vira uma fileira de
 * quadrados. Este enum é lógica pura, testável, e serve tanto para skins
 * vetoriais quanto para tiles de sprite (que precisam dos 14 casos).
 */
public enum Forma {

    /** Segmento do meio, indo e vindo na mesma direção. */
    RETO_H,

    /** Segmento do meio, vertical. */
    RETO_V,

    /** Canto que vira para baixo à direita. */
    CANTO_SE,

    /** Canto que vira para baixo à esquerda. */
    CANTO_SO,

    /** Canto que vira para cima à direita. */
    CANTO_NE,

    /** Canto que vira para cima à esquerda. */
    CANTO_NO,

    /** Primeiro segmento, com a direção de saída. */
    CABECA_CIMA,

    /** Cabeça apontando para a direita. */
    CABECA_DIREITA,

    /** Cabeça apontando para baixo. */
    CABECA_BAIXO,

    /** Cabeça apontando para a esquerda. */
    CABECA_ESQUERDA,

    /** Último segmento, com a direção de entrada. */
    RABO_CIMA,

    /** Rabo que vem da direita. */
    RABO_DIREITA,

    /** Rabo que vem de baixo. */
    RABO_BAIXO,

    /** Rabo que vem da esquerda. */
    RABO_ESQUERDA;

    /** A forma de um segmento, a partir de onde ele veio e para onde vai. */
    public static Forma de(Direcao entrada, Direcao saida) {
        if (entrada == null) {
            return deCabeca(saida);
        }
        if (saida == null) {
            return deRabo(entrada);
        }
        if (entrada == saida) {
            // Reto na mesma orientação do movimento.
            return (entrada == Direcao.CIMA || entrada == Direcao.BAIXO) ? RETO_V : RETO_H;
        }
        return deCanto(entrada, saida);
    }

    private static Forma deCabeca(Direcao saida) {
        if (saida == null) {
            return CABECA_DIREITA; // cobra parada: aponta para onde já andava
        }
        switch (saida) {
            case CIMA:
                return CABECA_CIMA;
            case BAIXO:
                return CABECA_BAIXO;
            case ESQUERDA:
                return CABECA_ESQUERDA;
            case DIREITA:
            default:
                return CABECA_DIREITA;
        }
    }

    private static Forma deRabo(Direcao entrada) {
        // O rabo aponta de volta: veio do lado oposto de onde "entrada" aponta.
        switch (entrada) {
            case CIMA:
                return RABO_BAIXO;
            case BAIXO:
                return RABO_CIMA;
            case ESQUERDA:
                return RABO_DIREITA;
            case DIREITA:
            default:
                return RABO_ESQUERDA;
        }
    }

    private static Forma deCanto(Direcao entrada, Direcao saida) {
        // O canto é nomeado pela orientação visual: onde ele abre
        // (norte/sul) e para onde o corpo se estende (leste/oeste).
        boolean abreParaCima = entrada == Direcao.BAIXO || saida == Direcao.CIMA;
        boolean abreParaBaixo = entrada == Direcao.CIMA || saida == Direcao.BAIXO;
        boolean abreParaDireita = entrada == Direcao.ESQUERDA || saida == Direcao.DIREITA;
        boolean abreParaEsquerda = entrada == Direcao.DIREITA || saida == Direcao.ESQUERDA;

        if (abreParaCima && abreParaEsquerda) {
            return CANTO_NO;
        }
        if (abreParaCima && abreParaDireita) {
            return CANTO_NE;
        }
        if (abreParaBaixo && abreParaEsquerda) {
            return CANTO_SO;
        }
        return CANTO_SE;
    }

    /** {@code true} se a forma é uma das cabeças. */
    public boolean ehCabeca() {
        return this == CABECA_CIMA || this == CABECA_DIREITA || this == CABECA_BAIXO
                || this == CABECA_ESQUERDA;
    }

    /** {@code true} se a forma é uma das caudas. */
    public boolean ehRabo() {
        return this == RABO_CIMA || this == RABO_DIREITA || this == RABO_BAIXO
                || this == RABO_ESQUERDA;
    }

    /** {@code true} se a forma é um dos cantos. */
    public boolean ehCanto() {
        return this == CANTO_SE || this == CANTO_SO || this == CANTO_NE || this == CANTO_NO;
    }

    /** {@code true} se a forma é horizontal (cabeça, rabo ou corpo reto na horizontal). */
    public boolean horizontal() {
        switch (this) {
            case RETO_H:
            case CANTO_SE:
            case CANTO_SO:
            case CABECA_CIMA:
            case CABECA_BAIXO:
            case RABO_CIMA:
            case RABO_BAIXO:
                return true;
            default:
                return false;
        }
    }
}
