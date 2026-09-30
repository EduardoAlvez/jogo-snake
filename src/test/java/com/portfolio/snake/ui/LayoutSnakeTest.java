package com.portfolio.snake.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import org.junit.Test;

/**
 * O desenho não pode sair da janela.
 *
 * <p>Esta suíte existe por causa do defeito do Pong, em que os badges subiam a
 * cada desenho e com cinco efeitos ativos o topo da pilha chegava a
 * {@code y = −104}: nada aparecia, e <b>nenhum teste falhou</b>, porque as
 * posições eram calculadas dentro do método de desenho, fora do alcance de
 * qualquer teste. O conserto foi tirar o cálculo de dentro da tela, e é o que
 * {@link LayoutSnake} faz.
 *
 * <p>Todos os testes rodam em headless, sem abrir janela: {@link Rectangle} é
 * geometria pura e não precisa de display.
 */
public class LayoutSnakeTest {

    private static LayoutSnake padrao() {
        return new LayoutSnake();
    }

    // ------------------------------------------------------------------
    // As áreas não podem se sobrepor nem sair
    // ------------------------------------------------------------------

    @Test
    public void hudTabuleiroERodapeNaoSeSobrepoem() {
        LayoutSnake l = padrao();
        Rectangle hud = l.hud();
        Rectangle tab = l.tabuleiro();
        Rectangle rod = l.rodape();
        assertTrue("hud e tabuleiro se sobrepoem", !hud.intersects(tab));
        assertTrue("tabuleiro e rodape se sobrepoem", !tab.intersects(rod));
        assertTrue("hud e rodape se sobrepoem", !hud.intersects(rod));
    }

    @Test
    public void aFaixaDaBordaEstaEntreOHudEOtabuleiro() {
        LayoutSnake l = padrao();
        Rectangle faixa = l.faixaBorda();
        assertTrue("a faixa da borda invade o tabuleiro", !faixa.intersects(l.tabuleiro()));
        assertTrue("a faixa da borda invade o hud", !faixa.intersects(l.hud()));
    }

    @Test
    public void tudoCabeNaJanela() {
        LayoutSnake l = padrao();
        assertTrue("hud", l.dentroDaJanela(l.hud()));
        assertTrue("faixa da borda", l.dentroDaJanela(l.faixaBorda()));
        assertTrue("tabuleiro", l.dentroDaJanela(l.tabuleiro()));
        assertTrue("rodape", l.dentroDaJanela(l.rodape()));
    }

    // ------------------------------------------------------------------
    // A grade
    // ------------------------------------------------------------------

    @Test
    public void oTabuleiroTemNumeroInteiroDeCelulas() {
        LayoutSnake l = padrao();
        Rectangle t = l.tabuleiro();
        // se isto não bater, sobra uma fração de pixel e aparece uma fresta
        // entre os segmentos da cobra
        assertEquals("largura nao e multiplo exato", 0, t.width % l.getCelula());
        assertEquals("altura nao e multiplo exato", 0, t.height % l.getCelula());
        assertEquals(l.getColunas() * l.getCelula(), t.width);
        assertEquals(l.getLinhas() * l.getCelula(), t.height);
    }

    @Test
    public void aCelulaZeroECantoECorrespondeAoTabuleiro() {
        LayoutSnake l = padrao();
        Rectangle c = l.celula(0, 0);
        Rectangle t = l.tabuleiro();
        assertEquals(t.x, c.x);
        assertEquals(t.y, c.y);
    }

    @Test
    public void aUltimaCelulaTerminaExatamenteNaBorda() {
        LayoutSnake l = padrao();
        Rectangle ultima = l.celula(l.getColunas() - 1, l.getLinhas() - 1);
        Rectangle t = l.tabuleiro();
        assertEquals(t.x + t.width, ultima.x + ultima.width);
        assertEquals(t.y + t.height, ultima.y + ultima.height);
    }

    @Test
    public void cadaCelulaEstaDentroDoTabuleiro() {
        LayoutSnake l = padrao();
        Rectangle t = l.tabuleiro();
        for (int x = 0; x < l.getColunas(); x++) {
            for (int y = 0; y < l.getLinhas(); y++) {
                Rectangle c = l.celula(x, y);
                assertTrue("celula " + x + "," + y + " fora do tabuleiro", t.contains(c));
            }
        }
    }

    @Test
    public void celulasVizinhasNaoSeSobrepoem() {
        LayoutSnake l = padrao();
        Rectangle a = l.celula(5, 5);
        Rectangle b = l.celula(6, 5);
        Rectangle c = l.celula(5, 6);
        assertTrue("vizinho na horizontal se sobrepoe", !a.intersects(b));
        assertTrue("vizinho na vertical se sobrepoe", !a.intersects(c));
    }

    // ------------------------------------------------------------------
    // O segmento: a folga tem que caber
    // ------------------------------------------------------------------

    @Test
    public void oSegmentoTemFolgaEContinuaDentroDaCelula() {
        LayoutSnake l = padrao();
        Rectangle cel = l.celula(4, 4);
        Rectangle seg = l.segmento(4, 4, 0.12);
        assertTrue("o segmento saiu da celula", cel.contains(seg));
        assertTrue("o segmento nao tem folga nenhuma",
                seg.width < cel.width && seg.height < cel.height);
    }

    @Test
    public void folgaAbsurdaNaoFazOSegmentoSumir() {
        LayoutSnake l = padrao();
        Rectangle seg = l.segmento(4, 4, 5.0);
        assertTrue("o segmento sumiu com folga grande demais", seg.width > 0);
        assertTrue("o segmento ficou degenerado", seg.height > 0);
    }

    @Test
    public void folgaZeroNaoCobreACelulaInteira() {
        // folga zero é o pior caso visual: sem respiro nenhum a cobra vira um
        // bloco só, que é o outro jeito de ela não parecer uma cobra
        LayoutSnake l = padrao();
        Rectangle seg = l.segmento(4, 4, 0.0);
        assertEquals(l.getCelula(), seg.width);
    }

    // ------------------------------------------------------------------
    // Os cartões de poder: o defeito do Pong, aqui
    // ------------------------------------------------------------------

    @Test
    public void nenhumCartaoSaiDaJanelaComQualquerQuantidade() {
        LayoutSnake l = padrao();
        // o caso que quebrou o Pong era a pilha com tudo empilhado; aqui a
        // lista de poderes é curta, mas o teste varre bem além do que o jogo
        // permite, porque a defesa tem de ser do layout e não da regra
        for (int total = 0; total <= 24; total++) {
            for (int i = 0; i < total; i++) {
                Rectangle c = l.cartaoPoder(i, total);
                assertTrue("cartao " + i + " de " + total + " saiu da janela",
                        l.dentroDaJanela(c));
                assertTrue("cartao " + i + " de " + total + " saiu do rodape",
                        l.rodape().intersects(c) || c.width == 0);
            }
        }
    }

    @Test
    public void osCartoesNaoSeSobrepoem() {
        LayoutSnake l = padrao();
        for (int total = 1; total <= 8; total++) {
            Rectangle anterior = null;
            for (int i = 0; i < total; i++) {
                Rectangle c = l.cartaoPoder(i, total);
                if (anterior != null) {
                    assertTrue("cartoes " + (i - 1) + " e " + i + " de " + total
                            + " se sobrepoem", !anterior.intersects(c));
                }
                anterior = c;
            }
        }
    }

    @Test
    public void osCartoesFicamCentradosNoRodape() {
        LayoutSnake l = padrao();
        Rectangle rod = l.rodape();
        for (int total = 1; total <= 4; total++) {
            Rectangle primeiro = l.cartaoPoder(0, total);
            Rectangle ultimo = l.cartaoPoder(total - 1, total);
            int folgaEsq = primeiro.x - rod.x;
            int folgaDir = (rod.x + rod.width) - (ultimo.x + ultimo.width);
            assertTrue("cartoes de " + total + " nao estao centrais",
                    Math.abs(folgaEsq - folgaDir) <= 1);
        }
    }

    @Test
    public void nenhumCartaoTemTamanhoZeroOuNegativo() {
        LayoutSnake l = padrao();
        for (int total = 1; total <= 10; total++) {
            for (int i = 0; i < total; i++) {
                Rectangle c = l.cartaoPoder(i, total);
                assertTrue("cartao sem largura", c.width > 0);
                assertTrue("cartao sem altura", c.height > 0);
            }
        }
    }

    @Test
    public void zeroCartoesNaoDesenhaNada() {
        LayoutSnake l = padrao();
        Rectangle c = l.cartaoPoder(0, 0);
        assertEquals(0, c.width);
        assertEquals(0, c.height);
    }

    // ------------------------------------------------------------------
    // Os painéis de sobreposição
    // ------------------------------------------------------------------

    @Test
    public void osPaineisCabemNaJanelaNasTresSituacoes() {
        LayoutSnake l = padrao();
        // briefing, pausa e fim de partida
        int[][] pedidos = {{520, 340}, {360, 160}, {440, 260}};
        for (int[] p : pedidos) {
            Rectangle painel = l.painel(p[0], p[1]);
            assertTrue("painel " + p[0] + "x" + p[1] + " saiu da janela",
                    l.dentroDaJanela(painel));
        }
    }

    @Test
    public void umPainelMaiorQueAJanelaEIlimitado() {
        LayoutSnake l = padrao();
        Rectangle painel = l.painel(99999, 99999);
        assertTrue("painel gigante nao foi limitado", l.dentroDaJanela(painel));
    }

    @Test
    public void oPainelFicaCentralizado() {
        LayoutSnake l = padrao();
        Rectangle painel = l.painel(440, 260);
        int folgaEsq = painel.x;
        int folgaDir = (l.getLarguraJanela()) - (painel.x + painel.width);
        assertTrue("painel nao esta centralizado", Math.abs(folgaEsq - folgaDir) <= 1);
    }

    // ------------------------------------------------------------------
    // Respostas a janelas fora do padrão
    // ------------------------------------------------------------------

    @Test
    public void oLayoutSeAjustaAUmaJanelaEstreita() {
        LayoutSnake l = new LayoutSnake(520, 560, 20, 20, 16, 96, 72, 40);
        assertTrue("hud", l.dentroDaJanela(l.hud()));
        assertTrue("tabuleiro", l.dentroDaJanela(l.tabuleiro()));
        assertTrue("rodape", l.dentroDaJanela(l.rodape()));
        assertEquals(0, l.tabuleiro().width % l.getCelula());
    }

    @Test
    public void oLayoutSeAjustaAUmaJanelaLargaEAbaixa() {
        LayoutSnake l = new LayoutSnake(1200, 620, 24, 18, 20, 90, 70, 36);
        assertTrue("tabuleiro", l.dentroDaJanela(l.tabuleiro()));
        assertEquals(0, l.tabuleiro().height % l.getCelula());
        assertEquals(24, l.getColunas());
    }

    @Test
    public void aCelulaNuncaSomeEmNenhumaConfiguracao() {
        // se a janela for pequena demais para a grade, a célula tem um piso: uma
        // célula de tamanho zero faria o desenho sumir sem erro nenhum
        LayoutSnake l = new LayoutSnake(300, 300, 20, 20, 16, 96, 72, 40);
        assertTrue("a celula ficou pequena demais", l.getCelula() >= 4);
        assertTrue("o tabuleiro sumiu", l.tabuleiro().width > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void janelaMinusculaEhRecusada() {
        new LayoutSnake(10, 10, 20, 20, 16, 96, 72, 40);
    }

    @Test(expected = IllegalArgumentException.class)
    public void gradeMinusculaEhRecusada() {
        new LayoutSnake(720, 700, 2, 2, 16, 96, 72, 40);
    }
}
