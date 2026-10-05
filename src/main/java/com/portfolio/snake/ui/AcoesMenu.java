package com.portfolio.snake.ui;

import com.portfolio.snake.core.Campo;
import com.portfolio.snake.core.JogoSnake;

/**
 * O que o menu pode pedir à janela.
 *
 * <p>Existe porque quem desenha os botões ({@code DesenhoJogo}) e quem sabe
 * reiniciar a partida ({@code TelaSnake}) são classes diferentes, e o desenho não
 * pode conhecer a janela: um pintor que guarda a {@code JFrame} não é testável sem
 * abrir display, e este projeto testou 163 coisas sem abrir nenhuma.</p>
 *
 * <p>Todos os métodos têm corpo vazio. A implementação de teste — e o desenho, num
 * futuro quadro sem menu — não precisa escrever nenhum, e um método novo neste
 * contrato não quebra quem já implementou.</p>
 *
 * @author Eduardo Alvez
 */
public interface AcoesMenu {

    /**
     * Troca a regra da borda e a dificuldade, recomeçando.
     *
     * @param borda a nova regra da borda
     * @param dif a nova dificuldade
     */
    default void escolher(Campo.Borda borda, JogoSnake.Dificuldade dif) {
    }

    /** Entra na partida. */
    default void comecar() {
    }

    /** Passa para a próxima skin. */
    default void trocarSkin() {
    }

    /** Volta ao estado inicial. */
    default void recomecar() {
    }

    /** Uma implementação que não faz nada, para teste e para quem só quer desenhar. */
    AcoesMenu NENHUMA = new AcoesMenu() {
    };
}
