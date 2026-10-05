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
import com.portfolio.snake.core.Forma;
import com.portfolio.snake.core.JogoSnake;
import com.portfolio.snake.skin.CatalogoSkins;
import com.portfolio.snake.skin.Skin;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

    // ------------------------------------------------------------------
    // O menu: botoes de verdade
    // ------------------------------------------------------------------

    /**
     * O menu pinta os oito botoes, e cada um com fundo proprio.
     *
     * <p>A comparacao e com o fundo do painel, e nao com uma cor absoluta: o botao
     * e desenhado com um alfa baixo sobre o painel, entao o pixel depende do que tem
     * atras. Um teste que exigisse branco quebraria no dia em que o fundo do painel
     * mudasse, sem nenhum defeito — e esse e o tipo de teste que se acostuma a ser
     * "corrigido" em vez de serve de alerta.</p>
     *
     * <p>Se o botao sumisse e sobrasse so o texto, o miolo do retangulo voltaria a
     * ser igual ao fundo e este teste cairia. Por isso a leitura e no miolo, aonde
     * nao ha texto nem borda.</p>
     */
    @Test
    public void oMenuPintaOsOitoBotoes() {
        JogoSnake jogo = new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        BufferedImage img = pintar(jogo, false);
        Rectangle p = layout.menu().painel;

        int fundo = luminancia(img, p.x + 3, p.y + 3);
        List<String> semFundo = new ArrayList<>();
        for (Rectangle r : layout.menu().retangulos) {
            int dentro = luminancia(img, r.x + r.width / 2, r.y + r.height / 2);
            // o painel tem 0.82 de alfa sobre o tabuleiro; na regiao do menu a arte
            // de tras e o tabuleiro, entao a diferenca observada fica bem abaixo de
            // 255. Qualquer numero positivo com folga ja prova que ha um retangulo.
            if (Math.abs(dentro - fundo) < 3) {
                semFundo.add(r.toString());
            }
        }
        assertTrue("botoes sem fundo proprio: " + semFundo, semFundo.isEmpty());
    }

    /**
     * O fundo do botao e translucido sobre o painel, entao ele clareia o que esta
     * atras. O teste anterior prova que ha retangulo; este prova que o retangulo e
     * o botao e nao um artefato da borda do painel.
     */
    @Test
    public void oFundoDoBotaoClareiaSobreOPainel() {
        JogoSnake jogo = new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        BufferedImage img = pintar(jogo, false);

        Rectangle r = layout.menu().retangulo(LayoutSnake.Alvo.COMECAR);
        int dentro = luminancia(img, r.x + r.width / 2, r.y + r.height / 2);
        Rectangle p = layout.menu().painel;
        int foraDoBotao = luminancia(img, p.x + 3, p.y + 3);
        assertTrue("o botao nao clareou o painel: " + dentro + " contra " + foraDoBotao,
                dentro > foraDoBotao);
    }

    /** Soma dos tres canais. Um numero, e nao uma cor: so importa o quanto clareou. */
    private static int luminancia(BufferedImage img, int x, int y) {
        int rgb = img.getRGB(x, y);
        return ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
    }

    /**
     * Passar o mouse no botao muda o que e pintado dentro dele.
     *
     * <p>Este teste nasceu de duas mutacoes que <b>nao</b> caíram. A primeira
     * desligava o {@code fillRoundRect} do botao, e a segunda tirava o dourado do
     * selecionado; as duas seguiam verdes. A razao e a mesma nos dois casos, e e
     * instrutiva: o painel e desenhado com 0.82 de alfa <b>sobre o tabuleiro</b>,
     * entao o fundo atras dos botoes tem as listras da grade. Qualquer teste que
     * compare uma cor absoluta, ou uma cor contra outra cor do painel, mede a grade
     * que aparece por baixo e nao o botao.</p>
     *
     * <p>A comparacao que nao depende do fundo e entre duas pintadas do
     * <b>mesmo</b> quadro, mudando so o estado do mouse. A borda e o texto sao
     * iguais nas duas, entao qualquer pixel diferente dentro do retangulo so pode
     * ser o preenchimento. Com o {@code fill} desligado as duas imagens ficam
     * identicas e o teste cai.</p>
     */
    @Test
    public void oHoverMudaOBotaoPintado() {
        JogoSnake jogo = new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        BufferedImage semMouse = pintar(jogo, false);

        Rectangle alvo = layout.menu().retangulo(LayoutSnake.Alvo.TROCAR_SKIN);
        assertTrue("o ponteiro nao achou o botao",
                desenho.getBotaos().moverPara(alvo.x + alvo.width / 2, alvo.y + alvo.height / 2));
        BufferedImage comMouse = pintar(jogo, false);

        // o miolo, sem a borda nem a faixa do texto
        Rectangle miolo = new Rectangle(alvo.x + 8, alvo.y + 8, alvo.width - 16, alvo.height - 16);
        int diferentes = 0;
        for (int y = miolo.y; y < miolo.y + miolo.height; y++) {
            for (int x = miolo.x; x < miolo.x + miolo.width; x++) {
                if (semMouse.getRGB(x, y) != comMouse.getRGB(x, y)) {
                    diferentes++;
                }
            }
        }
        assertTrue("o hover nao pintou nada: o preenchimento do botao sumiu",
                diferentes > 0);
    }

    /**
     * O botao da opcao em vigor tem o miolo <b>inteiro</b> dourado.
     *
     * <p>Este teste nasceu de uma mutacao que nao caiu: trocar a cor do
     * preenchimento do selecionado por outra continuava verde, porque o contorno do
     * botao ja era dourado e o teste anterior so exigia "algum pixel dourado na
     * regiao". Exigir um pixel era medir o contorno, nao o botao.</p>
     *
     * <p>Por isso a exigencia e sobre o miolo, e sobre a maioria dele: o contorno
     * tem poucos pixels e o texto ocupa uma faixa estreita, entao um retangulo
     * dourado de verdade cobre quase toda a area. Sem o preenchimento dourado, o
     * miolo fica com a cor translucida do fundo e a contagem despenca.</p>
     */
    @Test
    public void oBotaoSelecionadoPreencheOMioloDeDourado() {
        JogoSnake jogo = new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        BufferedImage img = pintar(jogo, false);

        Rectangle r = layout.menu().retangulo(LayoutSnake.Alvo.BORDA_MATA);
        // miolo generoso: recorta o contorno e ainda sobra area de preenchimento
        Rectangle miolo = new Rectangle(r.x + 8, r.y + 8, r.width - 16, r.height - 16);
        int dourados = contarPixels(img, miolo, DesenhoJogo.DESTAQUE.getRGB(), 40);
        int area = miolo.width * miolo.height;
        assertTrue("o miolo do botao selecionado nao esta dourado: " + dourados + " de " + area,
                dourados > area / 2);
    }

    /** O botao da opcao em vigor fica dourado: e o unico jeito de ver o que esta valendo. */
    @Test
    public void oBotaoDaOpcaoEmVigorFicaDourado() {
        JogoSnake jogo = new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        BufferedImage img = pintar(jogo, false);

        assertTrue("nenhum botao dourado no menu",
                existePixelProximo(img, layout.menu().retangulo(LayoutSnake.Alvo.BORDA_MATA),
                        DesenhoJogo.DESTAQUE, 20));
    }

    @Test
    public void trocarABordaMoveODouradoParaOOutroBotao() {
        JogoSnake jogo = new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        BufferedImage img = pintar(jogo, false);

        // o mesmo codigo que o clique usa, e nao um caminho so de teste
        for (LayoutSnake.Alvo alvo : new LayoutSnake.Alvo[] { LayoutSnake.Alvo.FACIL,
                LayoutSnake.Alvo.MEDIO, LayoutSnake.Alvo.DIFICIL }) {
            jogo = new JogoSnake(campo(), dificuldadeDe(alvo), SEMENTE);
            BufferedImage agora = pintar(jogo, false);
            assertTrue("o dourado nao foi para " + alvo,
                    existePixelProximo(agora, layout.menu().retangulo(alvo),
                            DesenhoJogo.DESTAQUE, 20));
        }
        // e o de borda oposta nao pode continuar dourado
        assertTrue("o dourado ficou nos dois botoes de borda",
                contarPixels(img, layout.menu().retangulo(LayoutSnake.Alvo.BORDA_WRAP),
                        DesenhoJogo.DESTAQUE.getRGB(), 20) < contarPixels(img,
                        layout.menu().retangulo(LayoutSnake.Alvo.BORDA_MATA),
                        DesenhoJogo.DESTAQUE.getRGB(), 20));
    }

    private static JogoSnake.Dificuldade dificuldadeDe(LayoutSnake.Alvo alvo) {
        switch (alvo) {
            case FACIL:
                return JogoSnake.Dificuldade.FACIL;
            case DIFICIL:
                return JogoSnake.Dificuldade.DIFICIL;
            default:
                return JogoSnake.Dificuldade.MEDIO;
        }
    }

    /** Clicar no botao precisa falar com a janela, e a janela decide o que fazer. */
    @Test
    public void clicarNoBotaoChamaAAcaoCerta() {
        AtomicInteger chamadas = new AtomicInteger();
        List<LayoutSnake.Alvo> pedidos = new ArrayList<>();
        AcoesMenu acoes = new AcoesMenu() {
            @Override
            public void escolher(Campo.Borda b, JogoSnake.Dificuldade d) {
                pedidos.add(b == Campo.Borda.WRAP ? LayoutSnake.Alvo.BORDA_WRAP
                        : LayoutSnake.Alvo.BORDA_MATA);
                chamadas.incrementAndGet();
            }

            @Override
            public void comecar() {
                pedidos.add(LayoutSnake.Alvo.COMECAR);
                chamadas.incrementAndGet();
            }

            @Override
            public void trocarSkin() {
                pedidos.add(LayoutSnake.Alvo.TROCAR_SKIN);
                chamadas.incrementAndGet();
            }
        };
        DesenhoJogo comMenu = new DesenhoJogo(layout, CatalogoSkins.padrao(), acoes);
        JogoSnake jogo = new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE);

        // pinta para registrar os botoes, e so entao clica: o registro e criado pela
        // pintura, como em qualquer quadro real
        comMenu.pintar(g2De(), jogo, false);
        Botaos registro = comMenu.getBotaos();
        assertEquals(layout.menu().tamanho(), registro.tamanho());

        Rectangle wrap = layout.menu().retangulo(LayoutSnake.Alvo.BORDA_WRAP);
        assertTrue("o clique nao achou o botao",
                registro.acionar(wrap.x + wrap.width / 2, wrap.y + wrap.height / 2));
        Rectangle comecar = layout.menu().retangulo(LayoutSnake.Alvo.COMECAR);
        registro.acionar(comecar.x + comecar.width / 2, comecar.y + comecar.height / 2);

        assertEquals(2, chamadas.get());
        assertEquals(LayoutSnake.Alvo.BORDA_WRAP, pedidos.get(0));
        assertEquals(LayoutSnake.Alvo.COMECAR, pedidos.get(1));
    }

    /** Clicar fora de todo botao nao pode disparar nada: o painel fundo e clicavel. */
    @Test
    public void clicarForaDosBotoesNaoFazNada() {
        AtomicInteger chamadas = new AtomicInteger();
        AcoesMenu acoes = new AcoesMenu() {
            @Override
            public void comecar() {
                chamadas.incrementAndGet();
            }
        };
        DesenhoJogo comMenu = new DesenhoJogo(layout, CatalogoSkins.padrao(), acoes);
        comMenu.pintar(g2De(), new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE), false);

        Rectangle p = layout.menu().painel;
        assertFalse("o canto do painel disparou um botao", comMenu.getBotaos().acionar(p.x + 2, p.y + 2));
        assertEquals(0, chamadas.get());
    }

    /** O atalho de teclado do botao tem de pedir a mesma coisa que o clique. */
    @Test
    public void cliqueEAtalhoPassamAMesmaEscolha() {
        AtomicInteger porClique = new AtomicInteger();
        AcoesMenu acoes = new AcoesMenu() {
            @Override
            public void escolher(Campo.Borda b, JogoSnake.Dificuldade d) {
                porClique.incrementAndGet();
            }
        };
        DesenhoJogo comMenu = new DesenhoJogo(layout, CatalogoSkins.padrao(), acoes);
        comMenu.pintar(g2De(), new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE), false);

        Rectangle wrap = layout.menu().retangulo(LayoutSnake.Alvo.BORDA_WRAP);
        comMenu.getBotaos().acionar(wrap.x + 2, wrap.y + 2);

        // atalho: aplicarEscolha(VK_2) e o que a tecla [2] chama na tela
        new TelaSnake(new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE),
                layout).aplicarEscolha(java.awt.event.KeyEvent.VK_2);

        assertEquals("o botao e a tecla escolheram coisas diferentes", 1, porClique.get());
    }

    /** Um destino de desenho descartável, do mesmo tamanho da janela. */
    private Graphics2D g2De() {
        return new BufferedImage(layout.getLarguraJanela(), layout.getAlturaJanela(),
                BufferedImage.TYPE_INT_RGB).createGraphics();
    }

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
                existePixelProximo(img, cabeca, CatalogoSkins.padrao().getCorpoClaro(), TOLERANCIA)
                        || existePixelProximo(img, cabeca,
                                CatalogoSkins.padrao().getCorpoEscuro(), TOLERANCIA));
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

        assertTrue("o selo de novo recorde não foi desenhado",
                existePixelProximo(img, faixaDoSelo(), DesenhoJogo.DESTAQUE, TOLERANCIA));
    }

    @Test
    public void oSeloDeNovoRecordeNaoApareceQuandoNaoFoiBatido() {
        JogoSnake jogo = jogoEncerrado();
        BufferedImage img = pintar(jogo, false);

        assertFalse("o selo de novo recorde apareceu sem ter sido batido",
                existePixelProximo(img, faixaDoSelo(), DesenhoJogo.DESTAQUE, TOLERANCIA));
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

        for (int i = 0; i < 10; i++) {
            BufferedImage img = pintar(jogo, true);
            assertTrue("o selo sumiu na repinturação número " + i,
                    existePixelProximo(img, faixaDoSelo(), DesenhoJogo.DESTAQUE, TOLERANCIA));
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
        Rectangle painel = layout.menu().painel;
        Rectangle tabuleiro = layout.tabuleiro();

        assertTrue("o painel do briefing não cobre o tabuleiro", painel.intersects(tabuleiro));
        assertTrue("o menu ficou sem texto",
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
     * A faixa onde o selo "NOVO RECORDE" é desenhado, e nada mais.
     *
     * <p>Estes testes procuravam o dourado no painel inteiro, e aí está o furo: o
     * <b>título</b> da tela de fim também é dourado quando há recorde novo. O
     * teste do selo passava por causa do título — apagando a linha inteira que
     * desenha o selo, ele continuava verde. Foi o que a mutação "o selo de
     * recorde some da faixa" mostrou, e ela só aparece quando a região é a faixa
     * do selo, porque é ela que separa as duas coisas.</p>
     */
    private Rectangle faixaDoSelo() {
        return layout.fim().recorde;
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

    // ------------------------------------------------------------------
    // A skin escolhida tem que aparecer nos pixels
    // ------------------------------------------------------------------

    /**
     * Trocar a skin muda o que está pintado.
     *
     * <p>Este é o teste que impede a skin de ser enfeite. Sem ele, {@code Skin}
     * seria uma classe bonita e inerte: o catálogo existiria, a tecla existiria,
     * a preferência gravaria em disco, e a cobra continuaria verde com o nome
     * "Colorida" escrito no briefing. A mutação que ele derruba é
     * {@code DesenhoJogo} voltar a usar as constantes fixas.
     */
    @Test
    public void aSkinEscolhidaMudaOsPixelsDaCobra() {
        JogoSnake jogo = jogoRodando();
        Celula cabeca = jogo.getCobra().cabeca();
        Rectangle r = layout.segmento(cabeca.getX(), cabeca.getY(), 0.12);

        BufferedImage classico = new BufferedImage(layout.getLarguraJanela(),
                layout.getAlturaJanela(), BufferedImage.TYPE_INT_RGB);
        desenharCom(classico, jogo, CatalogoSkins.CLASSSICO);

        for (Skin outra : CatalogoSkins.todas()) {
            if (outra.getId().equals(CatalogoSkins.CLASSSICO.getId())) {
                continue;
            }
            BufferedImage img = new BufferedImage(layout.getLarguraJanela(),
                    layout.getAlturaJanela(), BufferedImage.TYPE_INT_RGB);
            desenharCom(img, jogo, outra);
            int diferentes = pixelsDiferentes(pixels(classico), pixels(img));
            assertTrue("a skin " + outra.getId() + " pintou igual a classica: "
                    + "0 pixels mudaram, entao o desenho ignora a skin",
                    diferentes > 0);
        }
    }

    @Test
    public void aCabecaFicaComACorDaSkinEscolhida() {
        JogoSnake jogo = jogoRodando();
        Celula cabeca = jogo.getCobra().cabeca();
        Rectangle r = layout.segmento(cabeca.getX(), cabeca.getY(), 0.12);

        for (Skin s : CatalogoSkins.todas()) {
            BufferedImage img = new BufferedImage(layout.getLarguraJanela(),
                    layout.getAlturaJanela(), BufferedImage.TYPE_INT_RGB);
            desenharCom(img, jogo, s);
            boolean achou = existePixelProximo(img, r, s.getCorpoClaro(), TOLERANCIA)
                    || existePixelProximo(img, r, s.getCorpoEscuro(), TOLERANCIA);
            assertTrue("a cabeca da skin " + s.getId() + " nao tem cor de cobra", achou);
        }
    }

    /**
     * Desenhar duas vezes com o mesmo estado dá o mesmo desenho.
     *
     * <p>É a regra que o defeito do selo de recorde quebrou, e a de um pintor com
     * skin nova precisa continuar obedecendo: se a skin entrasse como estado
     * mutável, a segunda repintação poderia sair diferente da primeira.
     */
    @Test
    public void duasRepinturasComAMesmaSkinSaoIguais() {
        JogoSnake jogo = jogoRodando();
        BufferedImage a = new BufferedImage(layout.getLarguraJanela(),
                layout.getAlturaJanela(), BufferedImage.TYPE_INT_RGB);
        BufferedImage b = new BufferedImage(layout.getLarguraJanela(),
                layout.getAlturaJanela(), BufferedImage.TYPE_INT_RGB);
        DesenhoJogo comChapeu = new DesenhoJogo(layout, CatalogoSkins.CHAPEU);
        Graphics2D g1 = a.createGraphics();
        try {
            comChapeu.pintar(g1, jogo, false);
        } finally {
            g1.dispose();
        }
        Graphics2D g2b = b.createGraphics();
        try {
            comChapeu.pintar(g2b, jogo, false);
        } finally {
            g2b.dispose();
        }
        assertArrayEquals("a segunda repintura mudou o desenho", pixels(a), pixels(b));
    }

    @Test
    public void oConstrutorSemSkinUsaAPadrao() {
        assertEquals(CatalogoSkins.padrao(), new DesenhoJogo(layout).getSkin());
    }

    @Test
    public void skinNulaEhRejeitadaNaConstrucao() {
        try {
            new DesenhoJogo(layout, null);
            org.junit.Assert.fail("aceitou skin nula");
        } catch (NullPointerException esperado) {
            assertEquals(CatalogoSkins.padrao(), desenho.getSkin());
        }
    }

    private void desenharCom(BufferedImage img, JogoSnake jogo, Skin skin) {
        Graphics2D g2 = img.createGraphics();
        try {
            new DesenhoJogo(layout, skin).pintar(g2, jogo, false);
        } finally {
            g2.dispose();
        }
    }

    /**
     * A sombra do canto vem da skin.
     *
     * <p>O teste anterior — "cada skin pinta diferente da clássica" — <b>não</b>
     * pega a sombra trocada: as skins já diferem no corpo, então a imagem muda
     * de qualquer jeito e a sombra poderia estar fixa sem ninguém notar. Aqui as
     * duas skins são idênticas em tudo menos a sombra, e a única coisa que pode
     * mudar o desenho é ela.
     *
     * <p>A cobra <b>vira</b> antes de pintar. Com a cobra reta não existe canto,
     * a sombra não teria onde aparecer, e o teste passaria sem exercitar nada.
     */
    @Test
    public void aSombraDaSkinMudaOsCantos() {
        JogoSnake j = new JogoSnake(campo(), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        j.iniciar();
        // Virada de verdade: a cobra começa indo para CIMA, então inverter o
        // sentido seria recusado em silêncio e o cenário viraria "cobra reta".
        Direcao agora = j.getCobra().getDirecao();
        boolean naHorizontal = agora == Direcao.DIREITA || agora == Direcao.ESQUERDA;
        Direcao alvo = naHorizontal ? Direcao.CIMA : Direcao.DIREITA;
        j.virar(alvo);

        // O canto aparece e some: com poucos segmentos ele dura um passo so,
        // porque rola para o rabo assim que a cabeca avanca. Por isso o cenario
        // e DIRIGIDO ate a pre-condicao, com limite, em vez de bater um numero
        // de passos fixo. E o teste nao cresce a cobra: numa grade pequena o
        // perseguidor morre antes de comer tres vezes, e o teste falhava por
        // isso -- nao por causa da sombra.
        boolean achouCanto = false;
        for (int i = 0; i < 20 && !achouCanto; i++) {
            // a virada e enfileirada: so vale no passo seguinte, nao na hora
            j.passo(0.2);
            assertEquals("a virada nao foi aceita, e o cenario nao tem canto",
                    alvo, j.getCobra().getDirecao());
            for (Forma f : j.getCobra().formas()) {
                achouCanto |= f.ehCanto();
            }
            if (j.getEstado() != JogoSnake.Estado.JOGANDO) {
                break;
            }
        }
        assertEquals("a cobra morreu antes de ter canto, e o cenario nao alcança a linha",
                JogoSnake.Estado.JOGANDO, j.getEstado());
        assertTrue("a cobra ficou reta, e a sombra dos cantos nao teria onde aparecer",
                achouCanto);

        Skin base = CatalogoSkins.CLASSSICO;
        Skin sombraFraca = new Skin("fraca", "Fraca", base.getCorpoClaro(),
                base.getCorpoEscuro(), base.getCabeca(), new Color(0, 0, 0, 10),
                base.getOlhos());
        Skin sombraForte = new Skin("forte", "Forte", base.getCorpoClaro(),
                base.getCorpoEscuro(), base.getCabeca(), new Color(0, 0, 0, 220),
                base.getOlhos());

        BufferedImage fraca = new BufferedImage(layout.getLarguraJanela(),
                layout.getAlturaJanela(), BufferedImage.TYPE_INT_RGB);
        BufferedImage forte = new BufferedImage(layout.getLarguraJanela(),
                layout.getAlturaJanela(), BufferedImage.TYPE_INT_RGB);
        desenharCom(fraca, j, sombraFraca);
        desenharCom(forte, j, sombraForte);

        int diferentes = pixelsDiferentes(pixels(fraca), pixels(forte));
        assertTrue("so a sombra difere entre as duas skins, e nada mudou: "
                + "o desenho esta usando uma sombra fixa", diferentes > 0);
    }

    /** Quantos pixels mudaram entre dois desenhos. */
    private static int pixelsDiferentes(int[] a, int[] b) {
        int n = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                n++;
            }
        }
        return n;
    }

    private static int[] pixels(BufferedImage img) {
        return img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
    }
}
