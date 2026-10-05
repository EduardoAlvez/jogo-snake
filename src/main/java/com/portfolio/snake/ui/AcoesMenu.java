package com.portfolio.snake.ui;

import com.portfolio.snake.core.Campo;
import com.portfolio.snake.core.JogoSnake;

/**
 * O que o menu pode pedir à janela.
 *
 * <p>Existe porque quem desenha os botões ({@code DesenhoJogo}) e quem sabe
 * reiniciar a partida ({@code TelaSnake}) são classes diferentes, e o desenho não
 * pode conhecer a janela: um pintor que guarda a {@code JFrame} não é testável sem
 * abrir display, e este projeto testou 231 coisas sem abrir nenhuma.</p>
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

    /**
     * Zera a partida e entra nela, sem passar pelo menu.
     *
     * <p>Existe separado de {@link #comecar} porque na tela de fim a partida está
     * morta: chamar só {@code comecar} ali não ressuscita nada, porque
     * {@code iniciar} só sai do estado pausado. São dois passos, e são sempre os
     * dois juntos.</p>
     */
    default void jogarDeNovo() {
    }

    /**
     * Volta ao menu inicial, mantendo a borda e a dificuldade já escolhidas.
     *
     * <p>Difere de {@link #recomecar} de propósito: recomeçar monta a partida de
     * novo e começa na hora, enquanto isto apenas devolve o jogador às escolhas.
     * São dois verbos diferentes e o jogador escolhe qual quer.</p>
     */
    default void voltarAoMenu() {
    }

    /** Uma implementação que não faz nada, para teste e para quem só quer desenhar. */
    AcoesMenu NENHUMA = new AcoesMenu() {
    };
}
