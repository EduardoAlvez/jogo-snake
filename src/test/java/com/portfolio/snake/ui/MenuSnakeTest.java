package com.portfolio.snake.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * Testes da geometria do menu, decididos sem display.
 *
 * <p>A razão de o menu ter ganhado testes é a mesma dos badges no Pong: a posição
 * nascia dentro da pintura e nenhum teste conseguia vê-la. Aqui a geometria é
 * perguntada a {@link LayoutSnake} e conferida direto, inclusive numa janela menor
 * do que o menu pede.</p>
 */
public class MenuSnakeTest {

    private static final LayoutSnake PADRAO = new LayoutSnake();

    private static LayoutSnake janela(int largura, int altura) {
        return new LayoutSnake(largura, altura, 16, 16, 16, 96, 72, 40);
    }

    // ------------------------------------------------------------------
    // O menu tem os botoes que o jogador precisa
    // ------------------------------------------------------------------

    /**
     * Oito, não sete: dois de borda, três de dificuldade, começar, trocar skin e
     * recomeçar. A contagem está escrita à mão porque é a lista de botões que o
     * jogador vê, e um botão que sumisse da geometria tem de quebrar um teste.
     */
    @Test
    public void oMenuTemOitoBotoes() {
        assertEquals(8, PADRAO.menu().tamanho());
    }

    @Test
    public void oMenuTemBotaoParaCadaAlvo() {
        LayoutSnake.Menu m = PADRAO.menu();
        assertNotNull(m.retangulo(LayoutSnake.Alvo.BORDA_MATA));
        assertNotNull(m.retangulo(LayoutSnake.Alvo.BORDA_WRAP));
        assertNotNull(m.retangulo(LayoutSnake.Alvo.FACIL));
        assertNotNull(m.retangulo(LayoutSnake.Alvo.MEDIO));
        assertNotNull(m.retangulo(LayoutSnake.Alvo.DIFICIL));
        assertNotNull(m.retangulo(LayoutSnake.Alvo.COMECAR));
        assertNotNull(m.retangulo(LayoutSnake.Alvo.TROCAR_SKIN));
        assertNotNull(m.retangulo(LayoutSnake.Alvo.RECOMEÇAR));
    }

    @Test
    public void nenhumAlvoApareceDuasVezes() {
        List<LayoutSnake.Alvo> alvos = PADRAO.menu().alvos;
        Set<LayoutSnake.Alvo> vistos = new HashSet<>();
        for (LayoutSnake.Alvo a : alvos) {
            assertTrue("alvo repetido: " + a, vistos.add(a));
        }
        assertEquals(alvos.size(), vistos.size());
    }

    @Test
    public void alvoDesconhecidoNaoTemRetangulo() {
        assertNull(PADRAO.menu().retangulo(null));
    }

    // ------------------------------------------------------------------
    // Os rótulos têm faixa própria, acima do seu grupo
    // ------------------------------------------------------------------

    /**
     * O botão é pintado depois do rótulo, então um rótulo na mesma faixa do botão
     * não sai "um pouco acima": some atrás do preenchimento. Este teste existe
     * porque foi exatamente assim que "BORDA" e "DIFICULDADE" apareceram.
     */
    @Test
    public void nenhumRotuloFicaEmCimaDeUmBotao() {
        LayoutSnake.Menu m = PADRAO.menu();
        Rectangle[] rotulos = {m.titulo, m.subtitulo, m.rotuloBorda,
            m.rotuloDificuldade, m.dica};
        for (Rectangle r : rotulos) {
            for (Rectangle b : m.retangulos) {
                assertFalse("rotulo " + r + " invade o botao " + b, r.intersects(b));
            }
        }
    }

    @Test
    public void oRotuloDaBordaFicaAcimaDosDoisBotoesDeBorda() {
        LayoutSnake.Menu m = PADRAO.menu();
        int base = m.rotuloBorda.y + m.rotuloBorda.height;
        for (LayoutSnake.Alvo a : new LayoutSnake.Alvo[] {LayoutSnake.Alvo.BORDA_MATA,
            LayoutSnake.Alvo.BORDA_WRAP}) {
            assertTrue("rotulo precisa ficar acima de " + a, base <= m.retangulo(a).y);
        }
    }

    @Test
    public void oRotuloDaDificuldadeFicaAcimaDosTresBotoes() {
        LayoutSnake.Menu m = PADRAO.menu();
        int base = m.rotuloDificuldade.y + m.rotuloDificuldade.height;
        for (LayoutSnake.Alvo a : new LayoutSnake.Alvo[] {LayoutSnake.Alvo.FACIL,
            LayoutSnake.Alvo.MEDIO, LayoutSnake.Alvo.DIFICIL}) {
            assertTrue("rotulo precisa ficar acima de " + a, base <= m.retangulo(a).y);
        }
    }

    @Test
    public void osRotulosTambemFicamDentroDoPainel() {
        LayoutSnake.Menu m = PADRAO.menu();
        Rectangle[] rotulos = {m.titulo, m.subtitulo, m.rotuloBorda,
            m.rotuloDificuldade, m.dica};
        for (Rectangle r : rotulos) {
            assertTrue("rotulo fora do painel: " + r, r.y >= m.painel.y);
            assertTrue("rotulo fora do painel: " + r,
                    r.y + r.height <= m.painel.y + m.painel.height);
        }
    }

    // ------------------------------------------------------------------
    // Nada sai do painel
    // ------------------------------------------------------------------

    @Test
    public void todoBotaoEstaDentroDoPainel() {
        LayoutSnake.Menu m = PADRAO.menu();
        for (Rectangle r : m.retangulos) {
            assertTrue("botao acima do painel: " + r, r.y >= m.painel.y);
            assertTrue("botao abaixo do painel: " + r,
                    r.y + r.height <= m.painel.y + m.painel.height);
            assertTrue("botao a esquerda: " + r, r.x >= m.painel.x);
            assertTrue("botao a direita: " + r,
                    r.x + r.width <= m.painel.x + m.painel.width);
        }
    }

    /**
     * O painel é limitado à janela, então numa janela pequena os botões têm de
     * encolher junto. Sem o clamp eles escapariam do painel — e o clique do mouse
     * acertaria o vazio.
     */
    @Test
    public void botaoNaoEscapaDoPainelEmJanelaPequena() {
        LayoutSnake.Menu m = janela(360, 300).menu();
        for (Rectangle r : m.retangulos) {
            assertTrue("escapou em janela pequena: " + r,
                    r.x >= m.painel.x && r.x + r.width <= m.painel.x + m.painel.width);
            assertTrue("escapou em janela pequena: " + r,
                    r.y >= m.painel.y && r.y + r.height <= m.painel.y + m.painel.height);
        }
    }

    @Test
    public void botaoTemTamanhoUtilEmJanelaPequena() {
        for (Rectangle r : janela(360, 300).menu().retangulos) {
            assertTrue("botao achatado demais: " + r, r.width >= 24 && r.height >= 24);
        }
    }

    // ------------------------------------------------------------------
    // Botoes nao se sobrepoem: dois retangulos que se cruzam roubariam o clique
    // ------------------------------------------------------------------

    @Test
    public void nenhumBotaoSeSobrepoeAOutro() {
        LayoutSnake.Menu m = PADRAO.menu();
        for (int i = 0; i < m.retangulos.size(); i++) {
            for (int j = i + 1; j < m.retangulos.size(); j++) {
                Rectangle a = m.retangulos.get(i);
                Rectangle b = m.retangulos.get(j);
                assertFalse(m.alvos.get(i) + " e " + m.alvos.get(j) + " se cruzam",
                        a.intersects(b));
            }
        }
    }

    @Test
    public void asDuasBordasFicamNaMesmaLinha() {
        LayoutSnake.Menu m = PADRAO.menu();
        assertEquals(m.retangulo(LayoutSnake.Alvo.BORDA_MATA).y,
                m.retangulo(LayoutSnake.Alvo.BORDA_WRAP).y);
    }

    @Test
    public void asTresDificuldadesFicamNaMesmaLinha() {
        LayoutSnake.Menu m = PADRAO.menu();
        assertEquals(m.retangulo(LayoutSnake.Alvo.FACIL).y,
                m.retangulo(LayoutSnake.Alvo.MEDIO).y);
        assertEquals(m.retangulo(LayoutSnake.Alvo.MEDIO).y,
                m.retangulo(LayoutSnake.Alvo.DIFICIL).y);
    }

    /** As linhas não podem encostar: uma borda de 1px seria indistinguível do fundo. */
    @Test
    public void asLinhasDoMenuTemSeparacao() {
        LayoutSnake.Menu m = PADRAO.menu();
        int y1 = m.retangulo(LayoutSnake.Alvo.BORDA_MATA).y
                + m.retangulo(LayoutSnake.Alvo.BORDA_MATA).height;
        int y2 = m.retangulo(LayoutSnake.Alvo.FACIL).y;
        int y3 = m.retangulo(LayoutSnake.Alvo.COMECAR).y;
        int y4 = m.retangulo(LayoutSnake.Alvo.RECOMEÇAR).y;
        assertTrue("linhas de borda e dificuldade coladas", y2 - y1 >= 8);
        assertTrue("linhas de dificuldade e acao coladas", y3 - y2 >= 12);
        assertTrue("linhas de acao e recomecar coladas",
                y4 - y3 - m.retangulo(LayoutSnake.Alvo.COMECAR).height >= 8);
    }

    @Test
    public void osBotoesNaoSaoTaoLargosQueSeTocam() {
        LayoutSnake.Menu m = PADRAO.menu();
        Rectangle mata = m.retangulo(LayoutSnake.Alvo.BORDA_MATA);
        Rectangle wrap = m.retangulo(LayoutSnake.Alvo.BORDA_WRAP);
        assertTrue("bordas coladas", wrap.x - (mata.x + mata.width) >= 4);
    }

    /** Um botão largo demais para a linha encolhe; o teste trava a largura real. */
    @Test
    public void aLinhaDeDificuldadeUsaATotalidadeDaLargura() {
        Rectangle painel = PADRAO.menu().painel;
        Rectangle dif = PADRAO.menu().retangulo(LayoutSnake.Alvo.FACIL);
        int total = dif.width * 3 + 8 * 2;
        assertTrue("a linha nao preenche o painel: " + total + " de " + painel.width,
                total >= painel.width - 40);
    }
}