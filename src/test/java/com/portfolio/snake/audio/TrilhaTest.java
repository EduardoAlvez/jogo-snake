package com.portfolio.snake.audio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.portfolio.snake.audio.Sons.Efeito;
import com.portfolio.snake.audio.Trilha.Retrato;
import com.portfolio.snake.core.Campo;
import com.portfolio.snake.core.Direcao;
import com.portfolio.snake.core.JogoSnake;

/**
 * A regra que decide o que soa, testada sem janela e sem placa de som.
 *
 * <p>Estes testes nunca chamam {@link Sons}: eles verificam a <b>decisão</b>,
 * que é o que dá para testar. Se tocarem, o teste passa a depender da máquina
 * que roda, que é o mesmo caminho que fez o `.exe` do Pong sair mudo.
 */
public class TrilhaTest {

    private static Retrato de(JogoSnake.Estado estado, int comidas, boolean poder) {
        return new Retrato(comidas, poder, estado, null);
    }

    private static Retrato de(JogoSnake.Estado estado, int comidas, boolean poder,
            JogoSnake.Motivo motivo) {
        return new Retrato(comidas, poder, estado, motivo);
    }

    @Test
    public void umTiqueQueNaoMudaNadaNaoTocaNada() {
        List<Efeito> sons = Trilha.dePara(
                de(JogoSnake.Estado.JOGANDO, 3, false),
                de(JogoSnake.Estado.JOGANDO, 3, false));
        assertTrue("um tique ocioso nao pode tocar som nenhum: " + sons, sons.isEmpty());
    }

    @Test
    public void comerTocaComer() {
        List<Efeito> sons = Trilha.dePara(
                de(JogoSnake.Estado.JOGANDO, 7, false),
                de(JogoSnake.Estado.JOGANDO, 8, false));
        assertEquals(List.of(Efeito.COMER), sons);
    }

    @Test
    public void pegarPoderTocaPoder() {
        List<Efeito> sons = Trilha.dePara(
                de(JogoSnake.Estado.JOGANDO, 7, false),
                de(JogoSnake.Estado.JOGANDO, 7, true));
        assertEquals(List.of(Efeito.PODER), sons);
    }

    @Test
    public void umPoderQueContinuaAtivoNaoTocaDeNovo() {
        // o poder dura varios passos; so a virada de false para true e evento
        List<Efeito> sons = Trilha.dePara(
                de(JogoSnake.Estado.JOGANDO, 9, true),
                de(JogoSnake.Estado.JOGANDO, 10, true));
        assertEquals("o segundo alimento so devia soar 'comer'", List.of(Efeito.COMER), sons);
    }

    @Test
    public void morrerNaParedeTocaParedeEFim() {
        List<Efeito> sons = Trilha.dePara(
                de(JogoSnake.Estado.JOGANDO, 12, false),
                de(JogoSnake.Estado.FIM, 12, false, JogoSnake.Motivo.PAREDE));
        assertEquals(List.of(Efeito.PAREDE, Efeito.FIM), sons);
    }

    @Test
    public void morrerNoCorpoTocaSoFim() {
        List<Efeito> sons = Trilha.dePara(
                de(JogoSnake.Estado.JOGANDO, 12, false),
                de(JogoSnake.Estado.FIM, 12, false, JogoSnake.Motivo.CORPO));
        assertEquals(List.of(Efeito.FIM), sons);
    }

    @Test
    public void vencerTocaFimMasNaoParede() {
        // a vitoria nao tem motivo de parede: quem died na parede foi a derrota
        List<Efeito> sons = Trilha.dePara(
                de(JogoSnake.Estado.JOGANDO, 99, false),
                de(JogoSnake.Estado.VITORIA, 99, false, null));
        assertEquals(List.of(Efeito.FIM), sons);
    }

    @Test
    public void oPassoQueComeEMataTocaASomDeMorteEODaComida() {
        // o tique que come e o mesmo que bate na parede. Tocar os dois seria
        // dizer duas coisas verdadeiras sobre o mesmo instante.
        List<Efeito> sons = Trilha.dePara(
                de(JogoSnake.Estado.JOGANDO, 20, false),
                de(JogoSnake.Estado.FIM, 21, false, JogoSnake.Motivo.PAREDE));
        assertEquals("a partida acabou, entao 'comer' nao entra", List.of(Efeito.PAREDE, Efeito.FIM), sons);
    }

    @Test
    public void umPoderQueAcabouNaoTocaNada() {
        List<Efeito> sons = Trilha.dePara(
                de(JogoSnake.Estado.JOGANDO, 7, true),
                de(JogoSnake.Estado.JOGANDO, 7, false));
        assertTrue("perder um poder nao e evento: " + sons, sons.isEmpty());
    }

    @Test
    public void umTiqueComAPartidaJaTerminadaNaoTocaNada() {
        // depois do FIM o timer ainda pode dar um tique; sem esta regra a tela
        // redesenhada tocaria o som de morte a cada repintura seguinte
        List<Efeito> sons = Trilha.dePara(
                de(JogoSnake.Estado.FIM, 12, false, JogoSnake.Motivo.PAREDE),
                de(JogoSnake.Estado.FIM, 12, false, JogoSnake.Motivo.PAREDE));
        assertTrue("partida ja terminada nao repete o som: " + sons, sons.isEmpty());
    }

    @Test
    public void oRetratoDoJogoRealRefleteOMotivo() {
        // liga o retrato ao jogo de verdade: e o retrato que a tela usa
        JogoSnake jogo = new JogoSnake(new Campo(20, Campo.Borda.WRAP));
        jogo.iniciar();
        jogo.reiniciar();
        assertEquals(JogoSnake.Estado.PAUSADO, Trilha.tira(jogo).getEstado());
        assertEquals(0, Trilha.tira(jogo).getComidas());
    }

    @Test
    public void oRetratoDoJogoRealEnxergaOMotivoDepoisDeMorrer() {
        Campo campo = new Campo(20, Campo.Borda.MORRE);
        JogoSnake jogo = new JogoSnake(campo);
        jogo.iniciar();
        while (jogo.getEstado() == JogoSnake.Estado.JOGANDO) {
            jogo.passo(0.15);
        }
        assertEquals(JogoSnake.Motivo.PAREDE, Trilha.tira(jogo).getMotivo());
    }

    @Test
    public void retratoNuloNaoQuebra() {
        // o primeiro tique depois de abrir a janela nao tem "antes"
        assertTrue(Trilha.dePara(null, de(JogoSnake.Estado.JOGANDO, 0, false)).isEmpty());
        assertTrue(Trilha.dePara(de(JogoSnake.Estado.JOGANDO, 0, false), null).isEmpty());
    }
}