package com.portfolio.snake.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.portfolio.snake.audio.Sons.Efeito;
import com.portfolio.snake.core.JogoSnake;
import com.portfolio.snake.core.Poder;

/**
 * Decide quais sons tocar, a partir do que mudou entre dois tiques.
 *
 * <p>Fica fora da {@code JFrame} pelo mesmo motivo que {@code DesenhoJogo} e
 * {@code LayoutSnake} ficam: dentro da janela não há como testar, e uma regra de
 * jogo sem teste é uma regra que alguém vai "ajustar" sem querer. Aqui
 * a regra é uma função pura, de entrada e saída visíveis.
 *
 * <p><b>Por que um retrato, e não o jogo.</b> A comparação é "o que mudou entre
 * antes e depois do passo", mas {@code JogoSnake} é o mesmo objeto mutável nos
 * dois momentos: não dá para guardar o estado de antes sem copiá-lo. O retrato é
 * essa cópia — e por ser explícito, não há como ler o valor depois e chamar aquilo
 * de "antes".
 */
public final class Trilha {

    /** O que se observa da partida em um instante. */
    public static final class Retrato {

        final int comidas;
        final boolean poderAtivo;
        final JogoSnake.Estado estado;
        final JogoSnake.Motivo motivo;

        Retrato(int comidas, boolean poderAtivo, JogoSnake.Estado estado, JogoSnake.Motivo motivo) {
            this.comidas = comidas;
            this.poderAtivo = poderAtivo;
            this.estado = estado;
            this.motivo = motivo;
        }

        /** Comidas já comidas até aqui. Só comida normal conta, não poder. */
        public int getComidas() {
            return comidas;
        }

        /** {@code true} se algum poder estiver valendo. */
        public boolean isPoderAtivo() {
            return poderAtivo;
        }

        /** Estado da partida neste instante. */
        public JogoSnake.Estado getEstado() {
            return estado;
        }

        /** Motivo da derrota, se já houve uma. */
        public JogoSnake.Motivo getMotivo() {
            return motivo;
        }
    }

    private Trilha() {
    }

    /** Fotografa a partida. */
    public static Retrato tira(JogoSnake jogo) {
        return new Retrato(jogo.getComidas(),
                jogo.temPoder(Poder.FANTASMA) || jogo.temPoder(Poder.PONTOS_X2),
                jogo.getEstado(),
                jogo.getMotivo());
    }

    /**
     * Os efeitos que o passo de {@code antes} para {@code depois} produziram.
     *
     * <p>A ordem é fixa e não é arbitrária: a morte vem antes da comida, porque
     * o passo que come é também o passo que pode acabar a partida, e uma partida
     * que acabou não deve sair tocando o bip de comida junto do som de morte.
     *
     * <p>Só há som quando <b>alguma coisa mudou</b>: um tique que não comeu, não
     * pegou poder e não terminou a partida não devolve lista nenhuma.
     *
     * @param antes  o retrato de antes do passo
     * @param depois o retrato de depois do passo
     * @return os efeitos a tocar, na ordem em que devem soar
     */
    public static List<Efeito> dePara(Retrato antes, Retrato depois) {
        List<Efeito> sons = new ArrayList<>(2);
        if (antes == null || depois == null) {
            return sons;
        }
        boolean terminou = antes.getEstado() == JogoSnake.Estado.JOGANDO
                && depois.getEstado() != JogoSnake.Estado.JOGANDO;
        if (terminou) {
            if (depois.getMotivo() == JogoSnake.Motivo.PAREDE) {
                sons.add(Efeito.PAREDE);
            }
            sons.add(Efeito.FIM);
            // a partida acabou: comer e poder não fazem sentido depois disso
            return Collections.unmodifiableList(sons);
        }
        if (depois.isPoderAtivo() && !antes.isPoderAtivo()) {
            sons.add(Efeito.PODER);
        }
        if (depois.getComidas() > antes.getComidas()) {
            sons.add(Efeito.COMER);
        }
        return Collections.unmodifiableList(sons);
    }
}