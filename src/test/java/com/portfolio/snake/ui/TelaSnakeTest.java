package com.portfolio.snake.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.portfolio.snake.core.Campo;
import com.portfolio.snake.core.JogoSnake;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * A janela, e não o desenho.
 *
 * <p>O que mora aqui é a ligação entre a tecla e a partida, que o teste do
 * desenho não alcança: o desenho sabe pintar um botão, mas quem decide se a
 * tecla começa o jogo é a janela. E foi exatamente aí que os três defeitos
 * apareceram — todos os testes anteriores passavam verdes com eles.</p>
 */
public class TelaSnakeTest {

    private LayoutSnake layout;
    private TelaSnake tela;
    private String homeOriginal;
    private Path casa;

    @Before
    public void apontarCasaParaPastaTemporaria() throws IOException {
        // a janela carrega e salva a skin em ~/.jogo-snake-skin.properties; sem
        // isto o teste da troca de skin escreveria na preferencia do usuario
        homeOriginal = System.getProperty("user.home");
        casa = Files.createTempDirectory("jogo-snake-tela");
        System.setProperty("user.home", casa.toString());

        layout = new LayoutSnake();
        tela = new TelaSnake(partida(), layout);
    }

    @After
    public void fecharJanela() throws IOException {
        tela.pararRelogio();
        tela.dispose();
        System.setProperty("user.home", homeOriginal);
        apagarRecursivamente(casa);
    }

    // ------------------------------------------------------------------
    // Escolher nao e comecar
    // ------------------------------------------------------------------

    @Test
    public void escolherDificuldadeNaoComecaAPartida() {
        tela.aplicarEscolha(KeyEvent.VK_5);

        assertEquals("a dificuldade escolhida tem que valer",
                JogoSnake.Dificuldade.DIFICIL, tela.getJogo().getDificuldade());
        assertEquals("escolher dificuldade nao pode comecar a partida",
                JogoSnake.Estado.PAUSADO, tela.getJogo().getEstado());
        assertTrue("a partida tem que continuar no menu", tela.getJogo().noInicio());
    }

    @Test
    public void escolherBordaNaoComecaAPartida() {
        tela.aplicarEscolha(KeyEvent.VK_2);

        assertEquals("a borda escolhida tem que valer",
                Campo.Borda.WRAP, tela.getJogo().getCampo().getBorda());
        assertEquals("escolher borda nao pode comecar a partida",
                JogoSnake.Estado.PAUSADO, tela.getJogo().getEstado());
    }

    /**
     * A reclamacao que motivou estes testes: escolher dificuldade e depois
     * escolher borda nao pode trocar uma coisa e devolver a outra.
     */
    @Test
    public void bordaEDificuldadeSeEscolhemSemSeAnularem() {
        tela.aplicarEscolha(KeyEvent.VK_5);
        tela.aplicarEscolha(KeyEvent.VK_2);

        JogoSnake depois = tela.getJogo();
        assertEquals("escolher a borda depois da dificuldade nao pode voltar ao medio",
                JogoSnake.Dificuldade.DIFICIL, depois.getDificuldade());
        assertEquals("a segunda escolha tem que valer",
                Campo.Borda.WRAP, depois.getCampo().getBorda());
        assertTrue("as duas escolhas deixam o jogo no menu", depois.noInicio());
    }

    /**
     * O botao do menu faz o mesmo que a tecla. So assim o teste prova que o
     * botao esta ligado a alguma coisa: contar que a tecla mudou o estado nao
     * diz nada sobre o clique.
     */
    @Test
    public void oBotaoDoMenuTemOMesmoEfeitoQueATecla() {
        tela.aplicarEscolha(KeyEvent.VK_5);
        JogoSnake pelaTecla = tela.getJogo();

        TelaSnake outra = new TelaSnake(partida(), layout);
        try {
            DesenhoJogo desenho = outra.getDesenho();
            pintar(desenho, outra.getJogo());
            clicar(desenho, LayoutSnake.Alvo.DIFICIL);

            assertEquals("o botao Dificil tem que deixar a partida dificil",
                    JogoSnake.Dificuldade.DIFICIL, outra.getJogo().getDificuldade());
            assertTrue("o botao tambem nao pode comecar a partida",
                    outra.getJogo().noInicio());
            assertEquals("tecla e botao precisam chegar ao mesmo estado",
                    pelaTecla.getDificuldade(), outra.getJogo().getDificuldade());
        } finally {
            outra.pararRelogio();
            outra.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Tecla no meio do jogo
    // ------------------------------------------------------------------

    @Test
    public void teclasDeOpcaoNaoReiniciamAPartidaEmAndamento() {
        // um passo so: ja nao e mais o menu, e a partida vale alguma coisa
        JogoSnake jogo = partida();
        jogo.iniciar();
        jogo.passo(0.5);
        assertFalse("a partida deste teste tem que estar em andamento", jogo.noInicio());

        TelaSnake emJogo = new TelaSnake(jogo, layout);
        try {
            emJogo.aplicarEscolha(KeyEvent.VK_3);
            emJogo.aplicarEscolha(KeyEvent.VK_5);
            emJogo.aplicarEscolha(KeyEvent.VK_2);

            assertSame("aperto de numero no meio do jogo nao pode trocar a partida",
                    jogo, emJogo.getJogo());
            assertEquals("nem desfazer o que ja tinha acontecido",
                    1, emJogo.getJogo().getPassos());
        } finally {
            emJogo.pararRelogio();
            emJogo.dispose();
        }
    }

    // ------------------------------------------------------------------
    // O menu continua ligado
    // ------------------------------------------------------------------

    @Test
    public void osBotoesDoMenuSobrevivemATrocaDeSkin() {
        tela.tratarTecla(KeyEvent.VK_N);

        // pinta o quadro do pintor novo: e o paint que registra os botoes
        DesenhoJogo desenho = tela.getDesenho();
        pintar(desenho, tela.getJogo());
        clicar(desenho, LayoutSnake.Alvo.COMECAR);

        assertEquals("depois de trocar a skin os botoes do menu precisam continuar valendo",
                JogoSnake.Estado.JOGANDO, tela.getJogo().getEstado());
    }

    @Test
    public void botoesDoMenuSaoLimposQuandoAPartidaComeca() {
        DesenhoJogo desenho = tela.getDesenho();
        pintar(desenho, tela.getJogo());
        int noMenu = desenho.getBotaos().tamanho();
        assertEquals("o menu tem oito botoes registrados", 8, noMenu);

        // comeca a partida e pinta de novo
        tela.comecarPartida();
        assertEquals("a partida tem que estar comecada",
                JogoSnake.Estado.JOGANDO, tela.getJogo().getEstado());
        pintar(desenho, tela.getJogo());

        int noJogo = desenho.getBotaos().tamanho();
        assertEquals("com a partida em andamento nao pode sobrar botao do menu para clicar",
                0, noJogo);
    }

    // ------------------------------------------------------------------
// A tela de fim tambem tem botao
// ------------------------------------------------------------------

    /**
     * A reclamacao que motivou estes testes: no fim da partida a tela ficava sem
     * botão nenhum, e o jogador só tinha o teclado.
     *
     * <p>Antes desta Mudança o registro ficava zerado em todo quadro, e como a
     * tela de fim não desenhava botão nenhum o clique não tinha onde cair.</p>
     */
    @Test
    public void aTelaDeFimRegistraOsDoisBotoes() {
        TelaSnake emFim = new TelaSnake(partidaEncerrada(), layout);
        try {
            DesenhoJogo desenho = emFim.getDesenho();
            pintar(desenho, emFim.getJogo());

            assertEquals("a tela de fim tem que registrar os dois botao",
                    2, desenho.getBotaos().tamanho());
        } finally {
            emFim.pararRelogio();
            emFim.dispose();
        }
    }

    /**
     * Jogar de novo tem que entrar na partida, e não só redesenhar.
     *
     * <p>Este é o teste que pega a armadilha do {@code iniciar}: ele só sai do
     * estado pausado, então um botão que só chamasse "começar" numa partida já
     * morta seria um botão que não faz nada — e passaria num teste que só
     * contasse que o clique chegou ao desenho.</p>
     */
    @Test
    public void jogarDeNovoNoFimComecaUmaPartidaCorrendo() {
        TelaSnake emFim = new TelaSnake(partidaEncerrada(), layout);
        try {
            DesenhoJogo desenho = emFim.getDesenho();
            pintar(desenho, emFim.getJogo());
            clicarNoFim(desenho, LayoutSnake.Alvo.JOGAR_DE_NOVO);

            assertEquals("jogar de novo tem que comecar a partida na hora",
                    JogoSnake.Estado.JOGANDO, emFim.getJogo().getEstado());
            assertEquals("e a pontuacao tem que estar zerada",
                    0, emFim.getJogo().getPontos());
            assertFalse("a partida nova nao pode nascer no menu",
                    emFim.getJogo().noInicio());
        } finally {
            emFim.pararRelogio();
            emFim.dispose();
        }
    }

    @Test
    public void voltarAoMenuNoFimDevolveAMenu() {
        TelaSnake emFim = new TelaSnake(partidaEncerrada(), layout);
        try {
            DesenhoJogo desenho = emFim.getDesenho();
            pintar(desenho, emFim.getJogo());
            clicarNoFim(desenho, LayoutSnake.Alvo.VOLTAR_AO_MENU);

            assertTrue("o botao de menu inicial tem que devolver o jogador ao menu",
                    emFim.getJogo().noInicio());
            assertEquals("e sem comecar a partida",
                    JogoSnake.Estado.PAUSADO, emFim.getJogo().getEstado());
        } finally {
            emFim.pararRelogio();
            emFim.dispose();
        }
    }

    /**
     * A decisão que separou os dois botões.
     *
     * <p>Existe um caminho curto para os dois virarem a mesma coisa: tanto
     * recomeçar quanto ir para o menu terminam em {@code reiniciar()}, porque
     * reiniciar deixa a partida pausada com zero ponto — que é o estado do menu.
     * Um botão de cada lado da tela fazendo a mesma coisa é pior do que um botão
     * só, então este teste existe para travar a diferença.</p>
     */
    @Test
    public void osDoisBotoesDaTelaDeFimFazemCoisasDiferentes() {
        TelaSnake jogando = new TelaSnake(partidaEncerrada(), layout);
        TelaSnake noMenu = new TelaSnake(partidaEncerrada(), layout);
        try {
            DesenhoJogo dJogando = jogando.getDesenho();
            pintar(dJogando, jogando.getJogo());
            clicarNoFim(dJogando, LayoutSnake.Alvo.JOGAR_DE_NOVO);

            DesenhoJogo dMenu = noMenu.getDesenho();
            pintar(dMenu, noMenu.getJogo());
            clicarNoFim(dMenu, LayoutSnake.Alvo.VOLTAR_AO_MENU);

            assertEquals("jogar de novo tem que estar correndo",
                    JogoSnake.Estado.JOGANDO, jogando.getJogo().getEstado());
            assertTrue("voltar ao menu tem que estar no menu",
                    noMenu.getJogo().noInicio());
        } finally {
            jogando.pararRelogio();
            jogando.dispose();
            noMenu.pararRelogio();
            noMenu.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private JogoSnake partida() {
        return new JogoSnake(
                new Campo(layout.getColunas(), layout.getLinhas(), Campo.Borda.MORRE),
                JogoSnake.Dificuldade.MEDIO, 42L);
    }

    /**
     * Uma partida que já acabou, do jeito mais simples possível.
     *
     * <p>Basta não virar: com a borda que mata a cobra segue em frente e morre na
     * parede. Não há perseguição à comida aqui porque esta partida só precisa
     * chegar ao estado {@code FIM} — quem precisa de pontos é o teste do recorde,
     * e esse mora no desenho.</p>
     */
    private JogoSnake partidaEncerrada() {
        JogoSnake j = partida();
        j.iniciar();
        for (int i = 0; i < 500 && j.getEstado() == JogoSnake.Estado.JOGANDO; i++) {
            j.passo(0.2);
        }
        assertEquals("a partida deste teste tem que ter acabado",
                JogoSnake.Estado.FIM, j.getEstado());
        return j;
    }

    private void pintar(DesenhoJogo desenho, JogoSnake jogo) {
        BufferedImage img = new BufferedImage(layout.getLarguraJanela(),
                layout.getAlturaJanela(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            desenho.pintar(g, jogo, false);
        } finally {
            g.dispose();
        }
    }

    private void clicar(DesenhoJogo desenho, LayoutSnake.Alvo botao) {
        Rectangle r = layout.menu().retangulo(botao);
        desenho.getBotaos().acionar(r.x + r.width / 2, r.y + r.height / 2);
    }

    private void clicarNoFim(DesenhoJogo desenho, LayoutSnake.Alvo botao) {
        Rectangle r = layout.fim().retangulo(botao);
        desenho.getBotaos().acionar(r.x + r.width / 2, r.y + r.height / 2);
    }

    private static void apagarRecursivamente(Path p) throws IOException {
        if (!Files.exists(p)) {
            return;
        }
        if (Files.isDirectory(p)) {
            try (java.util.stream.Stream<Path> itens = Files.list(p)) {
                for (Path item : (Iterable<Path>) itens::iterator) {
                    apagarRecursivamente(item);
                }
            }
        }
        Files.deleteIfExists(p);
    }
}
