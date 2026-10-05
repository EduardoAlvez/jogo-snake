package com.portfolio.snake.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * Testes do registro de botões e do highlight do mouse.
 *
 * <p>Rodam sem display: {@link Botaos} e {@link Botao} não dependem de Swing,
 * que é a razão de terem sido extraídos de {@code TelaPong}. O Pong tem os seus
 * próprios; estes são a mesma cobertura sobre a cópia do Snake, porque a cópia
 * pode divergir — e um arquivo duplicado diverge.</p>
 *
 * <p><b>O teste que importa é {@link #highlightSobreviveARepintura}.</b> Ele
 * reproduz o defeito real: o ponteiro marca o botão, a pintura recria a lista
 * com instâncias novas, e o destaque precisa continuar lá. Com o highlight
 * guardado dentro de cada instância de {@code Botao} este teste falha, porque a
 * instância nova nasce com o estado zerado.</p>
 *
 * @author Eduardo Alvez
 */
public class BotaosTest {

    private final Botaos botoes = new Botaos();

    // ---------------------------------------------------------------- identidade

    @Test
    public void chaveDependeDaGeometria() {
        Botao b = new Botao("A", 10, 20, 30, 40, () -> { });
        assertEquals("10:20:30:40", b.chave());
    }

    @Test
    public void chaveMudaComAPosicao() {
        String antes = new Botao("A", 10, 20, 30, 40, () -> { }).chave();
        String depois = new Botao("A", 11, 20, 30, 40, () -> { }).chave();
        assertFalse(antes.equals(depois));
    }

    /**
     * O texto fica fora da chave: o botão de skin muda de rótulo sem mudar de
     * lugar, e uma chave com o texto dentro perderia o highlight no clique.
     */
    @Test
    public void chaveIgnoraOTexto() {
        String antes = new Botao("Trocar skin", 10, 20, 30, 40, () -> { }).chave();
        String depois = new Botao("Dedinho", 10, 20, 30, 40, () -> { }).chave();
        assertEquals(antes, depois);
    }

    // ---------------------------------------------------------------- hit test

    @Test
    public void contemPontoDentro() {
        assertTrue(new Botao("A", 100, 100, 50, 20, () -> { }).contem(120, 110));
    }

    @Test
    public void contemAsBordas() {
        assertTrue(new Botao("A", 100, 100, 50, 20, () -> { }).contem(100, 100));
        assertTrue(new Botao("A", 100, 100, 50, 20, () -> { }).contem(150, 120));
    }

    @Test
    public void naoContemPontoFora() {
        assertFalse(new Botao("A", 100, 100, 50, 20, () -> { }).contem(99, 110));
        assertFalse(new Botao("A", 100, 100, 50, 20, () -> { }).contem(151, 110));
    }

    @Test
    public void botaoInativoNuncaEhEncontrado() {
        Botao b = new Botao("A", 100, 100, 50, 20, () -> { });
        b.ativo = false;
        assertFalse(b.contem(120, 110));
        botoes.adicionar(b);
        botoes.moverPara(120, 110);
        assertFalse(botoes.temHighlight(b));
    }

    // ---------------------------------------------------------------- hover

    @Test
    public void moverParaMarcaOBotaoSobOPonteiro() {
        Botao b = botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        botoes.moverPara(120, 110);
        assertTrue(botoes.temHighlight(b));
    }

    @Test
    public void moverParaForaDeTodosDesmarca() {
        Botao b = botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        botoes.moverPara(120, 110);
        botoes.moverPara(0, 0);
        assertFalse(botoes.temHighlight(b));
        assertNull(botoes.chaveHighlight());
    }

    /**
     * Mover dentro do mesmo botão não muda o highlight, e é por isso que
     * {@code moverPara} devolve {@code false}: sem isso, cada pixel de mouse
     * dentro do botão dispararia uma repintura.
     */
    @Test
    public void moverDentroDoMesmoBotaoNaoMudaOEstado() {
        Botao b = botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        assertTrue(botoes.moverPara(120, 110));
        assertFalse(botoes.moverPara(121, 111));
        assertFalse(botoes.moverPara(130, 115));
    }

    @Test
    public void nenhumBotaoNaoTemHighlight() {
        Botao b = botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        assertFalse(botoes.temHighlight(b));
    }

    // ---------------------------------------------------------------- reversão

    @Test
    public void highlightDesapareceQuandoOMouseSai() {
        Botao b = botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        botoes.moverPara(120, 110);
        botoes.moverPara(500, 500);
        assertFalse(botoes.temHighlight(b));
    }

    @Test
    public void highlightVoltaQuandoOMouseVolta() {
        Botao b = botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        botoes.moverPara(120, 110);
        botoes.moverPara(500, 500);
        botoes.moverPara(120, 110);
        assertTrue(botoes.temHighlight(b));
    }

    // ---------------------------------------------------------------- repinturação

    /**
     * O teste que importa. O ponteiro marca o botão, a pintura recria a lista com
     * instâncias novas, e o destaque precisa continuar lá.
     */
    @Test
    public void highlightSobreviveARepintura() {
        Botao antes = botoes.adicionar(new Botao("JOGAR", 40, 400, 150, 34, () -> { }));
        botoes.moverPara(100, 410);
        assertTrue(botoes.temHighlight(antes));

        // O que a pintura faz a cada quadro: limpa a lista e recria os botões
        // como instâncias novas. É aqui que o highlight morria.
        botoes.limpar();
        Botao depois = botoes.adicionar(new Botao("JOGAR", 40, 400, 150, 34, () -> { }));

        assertNotNull(depois);
        assertTrue("o highlight tem de sobreviver à repinturação", botoes.temHighlight(depois));
        assertFalse(antes == depois); // instância nova, de fato
    }

    @Test
    public void highlightNaoVazaParaOVizinho() {
        botoes.moverPara(0, 0);
        botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));

        botoes.moverPara(120, 110);
        botoes.limpar();
        Botao naRepintura = botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        Botao vizinho = botoes.adicionar(new Botao("B", 160, 100, 50, 20, () -> { }));

        assertTrue(botoes.temHighlight(naRepintura));
        assertFalse(botoes.temHighlight(vizinho));
    }

    @Test
    public void repinturacaoSemMouseNaoDeixaHighlight() {
        botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        assertNull(botoes.chaveHighlight());
        botoes.limpar();
        Botao novo = botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        assertFalse(botoes.temHighlight(novo));
    }

    // ---------------------------------------------------------------- clique

    @Test
    public void acionarExecutaABacao() {
        AtomicInteger vezes = new AtomicInteger();
        botoes.adicionar(new Botao("A", 100, 100, 50, 20, vezes::incrementAndGet));
        assertTrue(botoes.acionar(120, 110));
        assertEquals(1, vezes.get());
    }

    @Test
    public void acionarForaDeTodosNaoFazNada() {
        AtomicInteger vezes = new AtomicInteger();
        botoes.adicionar(new Botao("A", 100, 100, 50, 20, vezes::incrementAndGet));
        assertFalse(botoes.acionar(500, 500));
        assertEquals(0, vezes.get());
    }

    /** Um clique é um clique: arrastar o mouse não pode disparar duas vezes. */
    @Test
    public void acionarExecutaUmaVezPorClique() {
        AtomicInteger vezes = new AtomicInteger();
        botoes.adicionar(new Botao("A", 100, 100, 50, 20, vezes::incrementAndGet));
        botoes.acionar(120, 110);
        assertEquals(1, vezes.get());
    }

    @Test
    public void acionarNaoMoveOHover() {
        // Clicar não pode alterar o estado do mouse: o ponteiro continua onde está.
        Botao b = botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        botoes.moverPara(120, 110);
        botoes.acionar(120, 110);
        assertTrue("o clique não pode limpar o highlight que o mouse desenhou",
                botoes.temHighlight(b));
    }

    @Test
    public void acionarEmBotaoInativoNaoFazNada() {
        AtomicInteger vezes = new AtomicInteger();
        Botao b = new Botao("A", 100, 100, 50, 20, vezes::incrementAndGet);
        b.ativo = false;
        botoes.adicionar(b);
        assertFalse(botoes.acionar(120, 110));
        assertEquals(0, vezes.get());
    }

    // ---------------------------------------------------------------- lista

    @Test
    public void limparEsvaziaARegistro() {
        botoes.adicionar(new Botao("A", 100, 100, 50, 20, () -> { }));
        assertEquals(1, botoes.tamanho());
        botoes.limpar();
        assertEquals(0, botoes.tamanho());
    }

    @Test
    public void adicionarDevolveOMesmoBotao() {
        Botao b = new Botao("A", 100, 100, 50, 20, () -> { });
        assertTrue(b == botoes.adicionar(b));
    }

    @Test
    public void listaVaziaNaoQuebraMoverPara() {
        assertFalse(botoes.moverPara(10, 10));
        assertNull(botoes.sob(10, 10));
        assertFalse(botoes.acionar(10, 10));
    }

    /**
     * O menu do Snake tem oito botões e é recriado a cada quadro. Com hover
     * guardado na instância, nenhum deles jamais apareceria destacado.
     */
    @Test
    public void menuInteiroRepintadoMantemHighlightNoBotaoCerto() {
        Botaos registro = new Botaos();
        AtomicInteger cliques = new AtomicInteger();
        for (int repintura = 0; repintura < 3; repintura++) {
            registro.limpar();
            registro.adicionar(new Botao("BORDA", 16, 96, 240, 34, cliques::incrementAndGet));
            registro.adicionar(new Botao("DIFICILADE", 16, 144, 160, 34,
                    cliques::incrementAndGet));
            if (repintura == 0) {
                registro.moverPara(200, 110); // sobre "BORDA"
            }
        }
        Botao borda = registro.sob(200, 110);
        assertNotNull(borda);
        assertTrue(registro.temHighlight(borda));
        assertEquals("BORDA", borda.texto);
        assertTrue(registro.acionar(200, 110));
        assertEquals(1, cliques.get());
    }
}