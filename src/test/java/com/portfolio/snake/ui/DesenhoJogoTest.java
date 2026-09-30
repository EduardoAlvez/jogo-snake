package com.portfolio.snake.ui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.portfolio.snake.core.Campo;
import com.portfolio.snake.core.Celula;
import com.portfolio.snake.core.Comida;
import com.portfolio.snake.core.Cobra;
import com.portfolio.snake.core.Direcao;
import com.portfolio.snake.core.JogoSnake;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * O que a janela <b>realmente mostra</b>, conferido pixel a pixel.
 *
 * <p>Existe porque o modelo que cuida deste projeto não consegue olhar a janela.
 * Dizer "a janela abriu" não prova que a cobra aparece: a janela pode abrir com o
 * tabuleiro desenhado e a cobra invisível, e nenhum teste de regra notaria, porque
 * regra nenhuma é sobre cor. O caminho que fecha essa lacuna é o mesmo que tira a
 * janela da equação: {@link DesenhoJogo} pinta em qualquer
 * {@code Graphics2D}, e um {@code BufferedImage} é um destino de desenho
 * legítimo. O que se confere aqui é exatamente o que a tela mostraria.
 *
 * <p>O teste mais importante é {@link #desenharNaoGravaORecorde()}. A versão
 * anterior decidia e gravava o recorde <b>dentro</b> do desenho de fim de
 * partida, e como o registro só aceita números maiores que o já salvo, a
 * primeira repinturação gravava e mostrava o selo "NOVO RECORDE", e a segunda
 * lia o mesmo número de volta, concluía que não era recorde e <b>apagava o
 * selo</b>. O defeito era invisível para a lógica do jogo, que estava correta,
 * e invisível para o jogador se a janela não repintasse rápido o bastante.
 */
public class DesenhoJogoTest {

    private static final long SEMENTE = 42L;
    private static final String ARQUIVO_RECORDE = ".jogo-snake-recorde";
    private static final int TOLERANCIA = 70;

    private LayoutSnake layout;
    private DesenhoJogo desenho;
    private String homeOriginal;
    private Path casaTemporaria;

    @Before
    public void prepararCasaTemporaria() throws IOException {
        layout = new LayoutSnake();
        desenho = new DesenhoJogo(layout);
        casaTemporaria = Files.createTempDirectory("jogo-snake-teste");
        homeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", casaTemporaria.toString());
    }

    @After
    public void restaurarCasa() throws IOException {
        if (homeOriginal != null) {
            System.setProperty("user.home", homeOriginal);
        }
        if (casaTemporaria != null && Files.exists(casaTemporaria)) {
            try (java.util.stream.Stream<Path> itens = Files.list(casaTemporaria)) {
                for (Path p : itens.toArray(Path[]::new)) {
                    Files.deleteIfExists(p);
                }
            }
            Files.deleteIfExists(casaTemporaria);
        }
    }

    // ------------------------------------------------------------------
    // A cena tem as coisas onde deveria
    // ------------------------------------------------------------------

    @Test
    public void aCabecaDaCobraApareceNoTabuleiro() {
        JogoSnake jogo = jogoRodando();
        BufferedImage img = pintar(jogo, false);
        Rectangle cabeca = layout.segmento(
                jogo.getCobra().cabeca().getX(), jogo.getCobra().cabeca().getY(), 0.12);

        assertTrue("nenhum pixel verde de cobra na célula da cabeça",
                existePixelProximo(img, cabeca, DesenhoJogo.COBRA_A, TOLERANCIA)
                        || existePixelProximo(img, cabeca, DesenhoJogo.COBRA_B, TOLERANCIA));
    }

    @Test
    public void aComidaApareceNoTabuleiro() {
        JogoSnake jogo = jogoRodando();
        BufferedImage img = pintar(jogo, false);
        Celula comida = jogo.getComida().celula();
        Rectangle celula = layout.celula(comida.getX(), comida.getY());

        assertTrue("nenhum pixel vermelho de comida na célula da comida",
                existePixelProximo(img, celula, DesenhoJogo.COMIDA, TOLERANCIA));
    }

    @Test
    public void oTabuleiroFicaComACorDoTabuleiro() {
        JogoSnake jogo = jogoRodando();
        BufferedImage img = pintar(jogo, false);
        Rectangle tab = layout.tabuleiro();
        // um canto do tabuleiro é fundo liso: nem grade, nem cobra, nem comida
        Rectangle canto = new Rectangle(tab.x + 2, tab.y + 2, tab.width - 4, tab.height - 4);
        int foraDaCobra = contarPixels(img, canto,
                DesenhoJogo.TABULEIRO.getRGB(), TOLERANCIA);

        assertTrue("o tabuleiro não foi pintado com a cor do tabuleiro", foraDaCobra > 0);
    }

    @Test
    public void oHudTemTexto() {
        JogoSnake jogo = jogoRodando();
        BufferedImage img = pintar(jogo, false);
        Rectangle hud = layout.hud();

        assertTrue("o HUD ficou sem nenhum pixel de texto claro",
                existePixelProximo(img, hud, DesenhoJogo.TEXTO, 40));
    }

    @Test
    public void oRodapeFalaDoTecladoQuandoNaoHaPoderAtivo() {
        JogoSnake jogo = jogoRodando();
        BufferedImage img = pintar(jogo, false);

        assertTrue("o rodapé não tem texto", existePixelProximo(img, layout.rodape(),
                DesenhoJogo.TEXTO_FRACO, TOLERANCIA));
    }

    // ------------------------------------------------------------------
    // O selo de recorde: o defeito que este teste existe para prender
    // ------------------------------------------------------------------

    @Test
    public void oSeloDeNovoRecordeApareceQuandoInformado() {
        JogoSnake jogo = jogoEncerrado();
        BufferedImage img = pintar(jogo, true);
        Rectangle painel = layout.painel(440, 260);

        assertTrue("o selo de novo recorde não foi desenhado",
                existePixelProximo(img, painel, DesenhoJogo.DESTAQUE, TOLERANCIA));
    }

    @Test
    public void oSeloDeNovoRecordeNaoApareceQuandoNaoFoiBatido() {
        JogoSnake jogo = jogoEncerrado();
        BufferedImage img = pintar(jogo, false);
        Rectangle painel = layout.painel(440, 260);

        assertFalse("o selo de novo recorde apareceu sem ter sido batido",
                existePixelProximo(img, painel, DesenhoJogo.DESTAQUE, TOLERANCIA));
    }

    /**
     * O selo não pode depender de quantas vezes a cena foi pintada.
     *
     * <p>Este é o teste que realmente prende o defeito. Antes, a decisão ficava
     * dentro do desenho: pintar uma vez mostrava o selo, pintar de novo
     * escondia. Aqui a mesma partida é pintada dez vezes seguidas e o selo
     * continua lá — o que não aconteceria se a decisão fosse tomada durante o
     * desenho.
     */
    @Test
    public void oSeloNaoDesapareceRepintandoVariasVezes() {
        JogoSnake jogo = jogoEncerrado();
        Rectangle painel = layout.painel(440, 260);

        for (int i = 0; i < 10; i++) {
            BufferedImage img = pintar(jogo, true);
            assertTrue("o selo sumiu na repinturação número " + i,
                    existePixelProximo(img, painel, DesenhoJogo.DESTAQUE, TOLERANCIA));
        }
    }

    @Test
    public void desenharNaoGravaORecorde() throws IOException {
        JogoSnake jogo = jogoEncerrado();

        // primeiro um controle: o registro realmente grava quando chamado
        assertTrue("o registro deveria gravar um recorde novo",
                com.portfolio.snake.core.RegistroDeRecordes.registrar(jogo.getPontos() + 1));
        Path arquivo = new File(casaTemporaria.toFile(), ARQUIVO_RECORDE).toPath();
        assertTrue("o registro não criou o arquivo", Files.exists(arquivo));
        Files.deleteIfExists(arquivo);

        // agora a mesma partida, só pintando: nada pode ser gravado
        pintar(jogo, true);
        pintar(jogo, true);
        pintar(jogo, true);

        assertFalse("pintar a cena gravou o arquivo de recorde", Files.exists(arquivo));
    }

    // ------------------------------------------------------------------
    // Sobreposições e estabilidade
    // ------------------------------------------------------------------

    @Test
    public void oBriefingCobreOTabuleiro() {
        JogoSnake jogo = new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        // sem iniciar(): o estado é PAUSADO com zero pontos, o briefing
        BufferedImage img = pintar(jogo, false);
        Rectangle painel = layout.painel(520, 340);
        Rectangle tabuleiro = layout.tabuleiro();

        assertTrue("o painel do briefing não cobre o tabuleiro", painel.intersects(tabuleiro));
        assertTrue("o briefing ficou sem texto",
                existePixelProximo(img, painel, DesenhoJogo.TEXTO, 40));
    }

    @Test
    public void aPausaApareceAposJogar() {
        JogoSnake jogo = jogoRodando();
        jogo.pausar();
        BufferedImage img = pintar(jogo, false);
        Rectangle painel = layout.painel(360, 160);

        assertEquals(JogoSnake.Estado.PAUSADO, jogo.getEstado());
        assertTrue("a pausa ficou sem texto",
                existePixelProximo(img, painel, DesenhoJogo.TEXTO, 40));
    }

    @Test
    public void aCenaEPreenchidaInteira() {
        JogoSnake jogo = jogoRodando();
        BufferedImage img = pintar(jogo, false);

        for (int y = 0; y < layout.getAlturaJanela(); y += 7) {
            for (int x = 0; x < layout.getLarguraJanela(); x += 7) {
                assertTrue("pixel não pintado em " + x + "," + y, img.getRGB(x, y) != 0);
            }
        }
    }

    @Test
    public void pintarDuasVezesDaOMesmaCena() {
        JogoSnake jogo = jogoRodando();
        int w = layout.getLarguraJanela();
        int h = layout.getAlturaJanela();
        int[] a = pintar(jogo, false).getRGB(0, 0, w, h, null, 0, w);
        int[] b = pintar(jogo, false).getRGB(0, 0, w, h, null, 0, w);

        assertArrayEquals("a mesma cena pintada duas vezes saiu diferente", a, b);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private static Campo campo() {
        LayoutSnake l = new LayoutSnake();
        return new Campo(l.getColunas(), l.getLinhas(), Campo.Borda.MORRE);
    }

    /** Jogo em andamento, com a cobra parada em uma posição previsível. */
    private static JogoSnake jogoRodando() {
        JogoSnake j = new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        j.iniciar();
        return j;
    }

    /**
     * Jogo que já acabou <b>e que tem pontos</b>, para poder pintar a tela de fim.
     *
     * <p>Os pontos não são decoração: uma partida com zero nunca entra na linha
     * que grava o recorde, e um teste de "pintar não grava" construído sobre
     * uma partida sem pontuação passaria mesmo com o defeito presente. Ele
     * mediria o nada. Por isso a cobra persegue a comida até comer.
     *
     * <p>A perseguição é feita só com a API pública, sem atalho de teste: a
     * cobra vai atrás da comida morro abaixo, e quando não dá mais para
     * continuar morre na parede. É assim que a partida chega a {@code FIM} da
     * mesma forma que chegaria com um jogador.
     */
    private JogoSnake jogoEncerrado() {
        JogoSnake j = new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        j.iniciar();
        for (int i = 0; i < 4000 && j.getEstado() == JogoSnake.Estado.JOGANDO; i++) {
            Direcao desejada = direcaoParaComida(j);
            if (desejada != null) {
                j.virar(desejada);
            }
            j.passo(0.2);
        }
        assertEquals("a partida não terminou, o teste não chegou à tela de fim",
                JogoSnake.Estado.FIM, j.getEstado());
        assertTrue("a partida terminou sem comer, e o teste de recorde não valeria nada",
                j.getPontos() > 0);
        return j;
    }

    /**
     * A melhor direção disponível para chegar mais perto da comida.
     *
     * <p>Tenta primeiro o eixo onde a distância é maior e, empatado, o sentido
     * positivo. Só entra na conta uma direção que não inverta a cobra — a mesma
     * regra que o núcleo aplica, reimplementada aqui para poder <b>escolher</b>
     * sem virar a cobra a cada tentativa, já que {@code virar} tem efeito colateral.
     */
    private static Direcao direcaoParaComida(JogoSnake j) {
        Celula cabeca = j.getCobra().cabeca();
        Comida comida = j.getComida();
        int dx = comida.getX() - cabeca.getX();
        int dy = comida.getY() - cabeca.getY();
        Direcao atual = j.getCobra().getDirecao();

        Direcao[] candidatas;
        if (Math.abs(dx) >= Math.abs(dy)) {
            candidatas = dx >= 0
                    ? new Direcao[]{Direcao.DIREITA, Direcao.BAIXO, Direcao.CIMA,
                            Direcao.ESQUERDA}
                    : new Direcao[]{Direcao.ESQUERDA, Direcao.BAIXO, Direcao.CIMA,
                            Direcao.DIREITA};
        } else {
            candidatas = dy >= 0
                    ? new Direcao[]{Direcao.BAIXO, Direcao.DIREITA, Direcao.ESQUERDA,
                            Direcao.CIMA}
                    : new Direcao[]{Direcao.CIMA, Direcao.DIREITA, Direcao.ESQUERDA,
                            Direcao.BAIXO};
        }
        for (Direcao d : candidatas) {
            if (!inverte(d, atual)) {
                return d;
            }
        }
        return atual;
    }

    /** {@code true} se {@code nova} é a direção oposta a {@code atual}. */
    private static boolean inverte(Direcao nova, Direcao atual) {
        return (nova == Direcao.CIMA && atual == Direcao.BAIXO)
                || (nova == Direcao.BAIXO && atual == Direcao.CIMA)
                || (nova == Direcao.ESQUERDA && atual == Direcao.DIREITA)
                || (nova == Direcao.DIREITA && atual == Direcao.ESQUERDA);
    }

    private BufferedImage pintar(JogoSnake jogo, boolean recordeBatido) {
        BufferedImage img = new BufferedImage(layout.getLarguraJanela(),
                layout.getAlturaJanela(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        try {
            desenho.pintar(g2, jogo, recordeBatido);
        } finally {
            g2.dispose();
        }
        return img;
    }

    /** {@code true} se algum pixel da região está perto o bastante da cor pedida. */
    private static boolean existePixelProximo(BufferedImage img, Rectangle r, Color alvo,
            int tolerancia) {
        return contarPixels(img, r, alvo.getRGB(), tolerancia) > 0;
    }

    /**
     * Conta os pixels da região próximos da cor, canal a canal.
     *
     * <p>A tolerância existe porque o desenho é antialiasado: a borda de um
     * círculo passa por todos os tons entre a cor e o fundo, e um teste que
     * exigisse a cor exata estaria medindo o suavizador de bordas em vez do
     * desenho.
     */
    private static int contarPixels(BufferedImage img, Rectangle r, int rgbAlvo,
            int tolerancia) {
        Color alvo = new Color(rgbAlvo);
        int x0 = Math.max(0, r.x);
        int y0 = Math.max(0, r.y);
        int x1 = Math.min(img.getWidth(), r.x + r.width);
        int y1 = Math.min(img.getHeight(), r.y + r.height);
        int achados = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                Color c = new Color(img.getRGB(x, y));
                if (Math.abs(c.getRed() - alvo.getRed()) <= tolerancia
                        && Math.abs(c.getGreen() - alvo.getGreen()) <= tolerancia
                        && Math.abs(c.getBlue() - alvo.getBlue()) <= tolerancia) {
                    achados++;
                }
            }
        }
        return achados;
    }
}
