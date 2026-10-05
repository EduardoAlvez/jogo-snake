package com.portfolio.snake.core;

import static com.portfolio.snake.core.JogoSnake.Estado.JOGANDO;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Soak da partida inteira: muitos passos, com o jogo real e nenhum atalho.
 *
 * <p>Existe por causa de uma classe de bug que nenhum teste de caso fixo
 * encontra. Os outros testes colocam a cobra numa situação e batem numa tecla.
 * Aqui a cobra anda sozinha por milhares de passos, a comida é sorteada a cada
 *vez que é comida, os poderes nascem e expiram, o nível sobe e a partida às vezes
 * termina — a sequência de estados que só aparece quando nada está preparado.
 *
 * <p><b>A semente é fixa</b>, então o soak é reprodutível: quando ele quebra,
 * quebro no mesmo lugar. O que não é garantido é que a primeira falha seja a
 * interessante.
 *
 * <p><b>O piloto é descartável.</b> Ele existe só para a cobra não morrer no
 * terceiro passo e o soak ter o que percorrer. Se o piloto fica melhor, o soak
 * fica mais fundo; se o piloto morre mais, o soak encurta. Nenhuma asserção
 * aqui verifica comportamento do piloto — só do jogo.
 */
public class JogoSnakeSoakTest {

    /** Passos totais, somando todas as partidas que o soak encadeia. */
    private static final int PASSOS = 20000;

    private static final long SEMENTE_INICIAL = 7L;

    /** Uma partida só, com uma semente, e os números que o soak mede. */
    private static final class Relatorio {
        int partidas;
        int passos;
        int maiorNivel;
        int maiorComprimento;
        int poderesVistos;
        boolean algumaPartidaLonga;
    }

    @Test
    public void aPartidaInteiraNaoQuebraNaParedeQueMata() {
        Relatorio r = mergulha(Campo.Borda.MORRE, PASSOS);
        confereQueOSoakChegouLonge(r, "parede que mata");
    }

    @Test
    public void aPartidaInteiraNaoQuebraNaParedeQueDaVolta() {
        Relatorio r = mergulha(Campo.Borda.WRAP, PASSOS);
        confereQueOSoakChegouLonge(r, "parede que da volta");
    }

    /**
     * As asserções de alcance.
     *
     * <p>Sem elas o soak pode passar por não fazer nada: mil tentativas morrendo
     * no passo um também não quebram nada, e um teste que só passa porque o jogo
     * nunca chegou a lugar nenhum não testemunha partida nenhuma. Por isso o
     * alcance também é verificado — e é medido, não Torcido.
     */
    private static void confereQueOSoakChegouLonge(Relatorio r, String modo) {
        String msg = "no soak da " + modo + ": ";
        assertTrue(msg + "o loop nao cobriu os " + PASSOS + " passos", r.passos == PASSOS);
        assertTrue(msg + "so houve " + r.partidas + " partida(s)", r.partidas > 1);
        assertTrue(msg + "nenhum poder chegou a existir em " + r.passos + " passos",
                r.poderesVistos > 0);
        assertTrue(msg + "o nivel mal passou de 1 (maior visto: " + r.maiorNivel + ")",
                r.maiorNivel >= 3);
        assertTrue(msg + "a cobra nunca cresceu (maior: " + r.maiorComprimento + ")",
                r.maiorComprimento >= 6);
        assertTrue(msg + "nenhuma partida passou de 200 passos",
                r.algumaPartidaLonga);
    }

    /**
     * Encadeia partidas até somar {@code passos} passos, conferindo os invariantes
     * a cada um.
     *
     * <p>Quando a partida acaba, outra começa com a semente seguinte. Isso não é
     * detalhe: é o que faz o soak cobrir várias distribuições de comida, vários
     * tamanhos de cobra e vários momentos do nível, em vez de repetir uma única
     * situação dez mil vezes.
     */
    private static Relatorio mergulha(Campo.Borda borda, int passos) {
        Relatorio r = new Relatorio();
        long semente = SEMENTE_INICIAL;
        int passosNestaPartida = 0;
        JogoSnake j = novaPartida(borda, semente);

        while (r.passos < passos) {
            if (j.getEstado() != JOGANDO) {
                // a partida terminou: guarda o tamanho dela e com a próxima
                if (passosNestaPartida >= 200) {
                    r.algumaPartidaLonga = true;
                }
                r.partidas++;
                j = novaPartida(borda, ++semente);
                passosNestaPartida = 0;
                confereInvariantes(j, r.passos);
            }

            pilota(j);
            confereInvariantes(j, r.passos);

            j.passo(0.15);
            passosNestaPartida++;
            r.passos++;
            r.maiorNivel = Math.max(r.maiorNivel, j.getNivel());
            r.maiorComprimento = Math.max(r.maiorComprimento, j.getCobra().tamanho());
            if (j.temPoder(Poder.FANTASMA) || j.temPoder(Poder.PONTOS_X2)) {
                r.poderesVistos++;
            }
        }
        return r;
    }

    private static JogoSnake novaPartida(Campo.Borda borda, long semente) {
        JogoSnake j = new JogoSnake(new Campo(30, 30, borda),
                JogoSnake.Dificuldade.DIFICIL, semente);
        j.iniciar();
        return j;
    }

    /**
     * Os invariantes do soak, conferidos antes e depois de cada passo.
     *
     * <p>São afirmações que precisam valer <b>sempre</b>, inclusive no passo em
     * que a partida termina — por isso a checagem de colisão é condicionada ao
     * estado {@code JOGANDO}: no passo da morte a cabeça está sobre o corpo de
     * propósito, e cobrá-lo ali seria cobrar o bug.
     */
    private static void confereInvariantes(JogoSnake j, int passo) {
        Campo campo = j.getCampo();
        Cobra cobra = j.getCobra();
        String onde = " no passo " + passo + ": ";

        Celula cabeca = cobra.cabeca();
        assertTrue("a cabeca saiu do campo" + onde,
                campo.dentro(cabeca.getX(), cabeca.getY()));

        List<Celula> corpo = cobra.segmentos();
        assertTrue("a cobra encolheu abaixo do minimo" + onde, cobra.tamanho() >= 2);
        assertTrue("a cobra passou do tamanho do campo" + onde,
                cobra.tamanho() <= campo.totalCelulas());
        for (Celula s : corpo) {
            assertTrue("um segmento ficou fora do campo" + onde,
                    campo.dentro(s.getX(), s.getY()));
        }

        assertTrue("o nivel saiu de faixa" + onde,
                j.getNivel() >= 1 && j.getNivel() <= JogoSnake.Dificuldade.DIFICIL.nivelMaximo());
        assertTrue("o intervalo caiu abaixo do piso" + onde, j.intervaloMs() >= 60);

        Celula comida = j.getComida().celula();
        assertTrue("a comida nasceu fora do campo" + onde,
                campo.dentro(comida.getX(), comida.getY()));

        if (j.getEstado() == JOGANDO) {
            assertTrue("a cabeca esta sobre o corpo e a partida continua" + onde,
                    !cobra.colidiuComSi());
        }
    }

    private static final Direcao[] TODAS_AS_DIRECOES = {
            Direcao.CIMA, Direcao.DIREITA, Direcao.BAIXO, Direcao.ESQUERDA };

    /**
     * Escolhe a direção mais segura que ainda aproxime da comida.
     *
     * <p>Não é uma inteligência de jogo, é um piloto descartável. O que ele
     * evita é o óbvio: inversão de 180° (que a própria cobra já recusa), parede
     * que mata e o próprio corpo. O que ele persegue é a comida, porque é assim
     * que o soak chega a nível alto, cobra comprida e poder — sem isso ele dá
     * umas dezenas de passos e reinicia, e o soak não vê nada.
     */
    private static void pilota(JogoSnake j) {
        Cobra cobra = j.getCobra();
        if (cobra.tamanhoFila() > 0) {
            return; // já há virada pendente; enfileirar outra atropelaria a fila
        }
        Direcao atual = cobra.getDirecao();
        Celula cabeca = cobra.cabeca();
        Celula comida = j.getComida().celula();
        List<Celula> corpo = cobra.segmentos();
        Celula rabo = cobra.rabo();

        Direcao melhor = null;
        int menorDistancia = Integer.MAX_VALUE;
        int[] corrida = new int[2];
        for (Direcao d : TODAS_AS_DIRECOES) {
            if (d.eOposta(atual)) {
                continue;
            }
            // traduzir já resolve os dois modos de borda: na parede que mata
            // devolve false, na que dá volta devolve a célula de destino
            if (!j.getCampo().traduzir(cabeca.getX() + d.dx(), cabeca.getY() + d.dy(), corrida)) {
                continue;
            }
            Celula destino = new Celula(corrida[0], corrida[1]);
            // o rabo sai antes da checagem, então entrar na célula dele é legal
            if (!destino.equals(rabo) && corpo.contains(destino)) {
                continue;
            }
            int distancia = Math.abs(destino.getX() - comida.getX())
                    + Math.abs(destino.getY() - comida.getY());
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                melhor = d;
            }
        }
        if (melhor != null) {
            cobra.enfileirar(melhor);
        }
    }
}