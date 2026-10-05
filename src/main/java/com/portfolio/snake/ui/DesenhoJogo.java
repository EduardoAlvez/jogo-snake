package com.portfolio.snake.ui;

import com.portfolio.snake.core.Campo;
import com.portfolio.snake.core.Celula;
import com.portfolio.snake.core.Cobra;
import com.portfolio.snake.core.Direcao;
import com.portfolio.snake.core.Forma;
import com.portfolio.snake.core.JogoSnake;
import com.portfolio.snake.core.Poder;
import com.portfolio.snake.skin.CatalogoSkins;
import com.portfolio.snake.skin.Skin;
import com.portfolio.snake.skin.SkinSprites;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * O desenho do jogo, sem janela.
 *
 * <p>Mora fora da {@link TelaSnake} pelo mesmo motivo que {@link LayoutSnake}
 * existe: para que um teste sem display consiga exercitar o desenho. Um
 * {@code BufferedImage} com um {@code Graphics2D} em cima é uma superfície de
 * desenho de verdade, e dá para conferir pixel por pixel o que a janela
 * mostraria.
 *
 * <p>Isso não é preciosismo. A alternativa — desenhar dentro da {@code JFrame} —
 * foi exatamente o que deixou o defeito dos badges do Pong passar: o cálculo
 * estava no meio do método de pintura, e nenhum teste podia alcançá-lo sem abrir
 * uma janela, então ninguém abriu.
 *
 * <p>Nenhum método aqui decide regra. Se a cobra morreu, quem sabe é o
 * {@link JogoSnake}; esta classe só pergunta o estado e pinta o que for dito.
 */
public final class DesenhoJogo {

    /** Cores do tabuleiro, em um lugar só para a tela não ficar com números soltos. */
    static final Color FUNDO = new Color(0x0E1116);
    static final Color TABULEIRO = new Color(0x151A21);
    static final Color GRADE = new Color(0x1E252F);
    static final Color BORDA = new Color(0x2C3644);
    static final Color TEXTO = new Color(0xE8EDF4);
    static final Color TEXTO_FRACO = new Color(0x8B97A8);
    static final Color COMIDA = new Color(0xFF6B6B);
    static final Color DESTAQUE = new Color(0xFFD166);

    private final LayoutSnake layout;
    private final Skin skin;
    private final SkinSprites sprites;

    /** Desenha com a skin padrão. É o que a maioria dos testes quer. */
    public DesenhoJogo(LayoutSnake layout) {
        this(layout, CatalogoSkins.padrao());
    }

    /**
     * Desenha com a skin escolhida.
     *
     * <p>A skin entra por aqui, na construção, e não por setter: um pintor com
     * estado que muda é o caminho mais curto para "a tela mostra uma coisa e o
     * teste da outra". A tela troca de skin construindo outro pintor — é barato,
     * o objeto não guarda nada.
     */
    public DesenhoJogo(LayoutSnake layout, Skin skin) {
        this.layout = java.util.Objects.requireNonNull(layout, "layout");
        this.skin = java.util.Objects.requireNonNull(skin, "skin");
        // Carrega uma vez, na construção. Desenhar roda a 60Hz e abrir 14 PNGs por
        // quadro é o tipo de lentidão que só apareceria com o jogo rodando.
        this.sprites = SkinSprites.carregar(skin.getId());
    }

    /**
     * Os PNGs desta skin, para quem quiser saber se a arte entrou.
     *
     * <p>Existe para o diagnóstico e para o teste. Uma skin pela metade é o caso
     * perigoso — a cobra sai com dedo no corpo e quadrado na cauda — e saber
     * {@code cobreTodasAsFormas()} antes de abrir o jogo é melhor do que
     * descobrir olhando.
     */
    public SkinSprites getSprites() {
        return sprites;
    }

    /** A skin com que este pintor desenha. */
    public Skin getSkin() {
        return skin;
    }

    /** O layout usado por este desenho. */
    public LayoutSnake getLayout() {
        return layout;
    }

    /**
     * Pinta a cena inteira.
     *
     * @param g2           destino do desenho
     * @param jogo         a partida a mostrar
     * @param recordeBatido se a partida que terminou superou o recorde gravado
     */
    public void pintar(Graphics2D g2, JogoSnake jogo, boolean recordeBatido) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        try {
            desenharFundo(g2);
            desenharTabuleiro(g2);
            desenharGrade(g2);
            desenharComida(g2, jogo);
            desenharPoderesNoChao(g2, jogo);
            desenharCobra(g2, jogo.getCobra());
            desenharHud(g2, jogo);
            desenharFaixaBorda(g2, jogo.getCampo().getBorda());
            desenharRodape(g2, jogo.poderesAtivos());

            switch (jogo.getEstado()) {
                case PAUSADO:
                    if (jogo.getPontos() == 0 && jogo.getPassos() == 0) {
                        desenharBriefing(g2);
                    } else {
                        desenharPausa(g2);
                    }
                    break;
                case FIM:
                    desenharFim(g2, jogo, "Bateu na parede",
                            "A cobra bateu ou se enroscou.", recordeBatido);
                    break;
                case VITORIA:
                    desenharFim(g2, jogo, "Campo lotado",
                            "A cobra cobriu o tabuleiro inteiro.", recordeBatido);
                    break;
                case JOGANDO:
                default:
                    break;
            }
        } finally {
            g2.setComposite(AlphaComposite.SrcOver);
        }
    }

    // ------------------------------------------------------------------
    // Fundo, tabuleiro e grade
    // ------------------------------------------------------------------

    private void desenharFundo(Graphics2D g2) {
        g2.setPaint(new GradientPaint(0, 0, new Color(0x0B0E13),
                0, layout.getAlturaJanela(), new Color(0x11161F)));
        g2.fillRect(0, 0, layout.getLarguraJanela(), layout.getAlturaJanela());
    }

    private void desenharTabuleiro(Graphics2D g2) {
        Rectangle t = layout.tabuleiro();
        g2.setColor(TABULEIRO);
        g2.fill(t);
        g2.setColor(BORDA);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(t);
    }

    private void desenharGrade(Graphics2D g2) {
        g2.setColor(GRADE);
        g2.setStroke(new BasicStroke(1f));
        for (int x = 1; x < layout.getColunas(); x++) {
            int px = layout.celula(x, 0).x;
            g2.drawLine(px, layout.tabuleiro().y, px,
                    layout.tabuleiro().y + layout.tabuleiro().height);
        }
        for (int y = 1; y < layout.getLinhas(); y++) {
            int py = layout.celula(0, y).y;
            g2.drawLine(layout.tabuleiro().x, py,
                    layout.tabuleiro().x + layout.tabuleiro().width, py);
        }
    }

    private void desenharComida(Graphics2D g2, JogoSnake jogo) {
        Rectangle c = layout.celula(jogo.getComida().getX(), jogo.getComida().getY());
        int d = (int) (c.width * 0.5);
        int x = c.x + (c.width - d) / 2;
        int y = c.y + (c.height - d) / 2;
        g2.setColor(new Color(COMIDA.getRed(), COMIDA.getGreen(), COMIDA.getBlue(), 60));
        g2.fill(new Ellipse2D.Double(x - d * 0.25, y - d * 0.25, d * 1.5, d * 1.5));
        g2.setColor(COMIDA);
        g2.fill(new Ellipse2D.Double(x, y, d, d));
        g2.setColor(new Color(255, 255, 255, 190));
        g2.fill(new Ellipse2D.Double(x + d * 0.22, y + d * 0.2, d * 0.26, d * 0.26));
    }

    private void desenharPoderesNoChao(Graphics2D g2, JogoSnake jogo) {
        for (JogoSnake.PoderNoChao p : jogo.poderesNoChao()) {
            Celula cel = p.getCelula();
            Rectangle c = layout.celula(cel.getX(), cel.getY());
            Color cor = new Color(p.getTipo().getCorRgb());
            int pad = (int) (c.width * 0.16);
            g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 70));
            g2.fill(new RoundRectangle2D.Double(c.x + pad, c.y + pad,
                    c.width - 2 * pad, c.height - 2 * pad, c.width * 0.3, c.height * 0.3));
            g2.setColor(cor);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Double(c.x + pad, c.y + pad,
                    c.width - 2 * pad, c.height - 2 * pad, c.width * 0.3, c.height * 0.3));
            centralizar(g2, p.getTipo().getRotulo(), c, fonte(14, Font.BOLD), Color.WHITE);
        }
    }

    // ------------------------------------------------------------------
    // A cobra
    // ------------------------------------------------------------------

    /**
     * Desenha a cobra segmento a segmento, usando a {@link Forma} de cada um.
     *
     * <p>É a parte que separa a cobra de uma fileira de quadrados: cada segmento
     * sabe de onde veio e para onde vai, e por isso sabe se é reto ou canto. Com
     * a cabeça virando de uma linha para outra, quem fecha a curva é o canto.
     */
    private void desenharCobra(Graphics2D g2, Cobra cobra) {
        List<Celula> celulas = cobra.segmentos();
        List<Forma> formas = cobra.formas();
        int n = celulas.size();

        for (int i = 0; i < n; i++) {
            Celula cel = celulas.get(i);
            Forma forma = formas.get(i);
            BufferedImage sprite = sprites.para(forma);

            // O recuo é do desenho, não da célula -- e só existe no vetorial.
            //
            // As formas do desenho por código são blocos simples, e o espaço entre
            // eles é o que faz a cobra ser lida como segmentos e não como um
            // retângulo único. Num PNG o recuo é ERRADO: a arte já vem com a
            // própria margem desenhada dentro dela, então recuar de novo abre uma
            // fresta entre duas peças que se deveriam encostar. Foi exatamente o
            // que apareceu no Dedinho -- o dedo saía picotado, com um vão visível a
            // cada segmento.
            //
            // Por isso a escolha é por origem da arte, e não uma constante única:
            // sprite preenche a célula, vetorial mantém o respiro.
            Rectangle seg = layout.segmento(cel.getX(), cel.getY(), sprite != null ? 0.0 : 0.12);

            if (sprite != null) {
                // A arte manda: gradiente e sombra de canto são do desenho
                // vetorial e não se aplicam a um PNG.
                g2.drawImage(sprite, seg.x, seg.y, seg.width, seg.height, null);
                continue;
            }

            // a cor clareia em direção à cabeça, o que dá noção de frente
            float t = n <= 1 ? 1f : 1f - (i / (float) (n - 1));
            g2.setColor(mesclar(skin.getCorpoEscuro(), skin.getCorpoClaro(), 0.25 + t * 0.75));

            int r = (int) Math.round(seg.width * (forma.ehCanto() ? 0.34 : 0.5));
            g2.fill(new RoundRectangle2D.Double(seg.x, seg.y, seg.width, seg.height, r, r));

            if (forma.ehCanto()) {
                g2.setColor(skin.getSombra());
                g2.fill(new RoundRectangle2D.Double(seg.x + seg.width * 0.28,
                        seg.y + seg.height * 0.28, seg.width * 0.44, seg.height * 0.44,
                        r, r));
            }
        }
        // Os olhos só são desenhados por código quando a cabeça NÃO é um PNG. Com
        // arte na cabeça, o vetor por cima desenharia um segundo par de olhos em
        // cima do dedo — e a regra é "existe PNG?", não uma flag na skin, porque
        // flag é estado que pode sair de sincronia com o disco.
        if (n > 0 && sprites.para(formas.get(0)) == null) {
            desenharOlhos(g2, cobra, formas.get(0));
        }
    }

    /** Os dois olhos, na frente da cabeça, segundo a direção em que ela anda. */
    private void desenharOlhos(Graphics2D g2, Cobra cobra, Forma forma) {
        if (forma == null || !forma.ehCabeca()) {
            return;
        }
        Celula cabeca = cobra.cabeca();
        Rectangle seg = layout.segmento(cabeca.getX(), cabeca.getY(), 0.12);
        double cx = seg.x + seg.width / 2.0;
        double cy = seg.y + seg.height / 2.0;
        double d = Math.max(2.0, seg.width * 0.12);
        double sep = seg.width * 0.17;

        double fx;
        double fy;
        switch (forma) {
            case CABECA_DIREITA:
                fx = 0.16;
                fy = 0.0;
                break;
            case CABECA_ESQUERDA:
                fx = -0.16;
                fy = 0.0;
                break;
            case CABECA_BAIXO:
                fx = 0.0;
                fy = 0.16;
                break;
            case CABECA_CIMA:
            default:
                fx = 0.0;
                fy = -0.16;
                break;
        }
        double px = -fy;
        double py = fx;

        g2.setColor(skin.getOlhos());
        g2.fill(new Ellipse2D.Double(cx + fx * seg.width + px * sep - d / 2,
                cy + fy * seg.height + py * sep - d / 2, d, d));
        g2.fill(new Ellipse2D.Double(cx + fx * seg.width - px * sep - d / 2,
                cy + fy * seg.height - py * sep - d / 2, d, d));
    }

    // ------------------------------------------------------------------
    // HUD, faixa e rodapé
    // ------------------------------------------------------------------

    private void desenharHud(Graphics2D g2, JogoSnake jogo) {
        Rectangle h = layout.hud();
        g2.setColor(TEXTO);
        centralizarEsquerda(g2, "SNAKE", new Rectangle(h.x, h.y, 200, 34),
                fonte(26, Font.BOLD));

        int linha = h.y + 40;
        int passo = h.width / 4;
        escreverValor(g2, "PONTOS", String.valueOf(jogo.getPontos()), h.x, linha, passo);
        escreverValor(g2, "RECORDE",
                String.valueOf(Math.max(jogo.getRecorde(), jogo.getPontos())),
                h.x + passo, linha, passo);
        escreverValor(g2, "NÍVEL", String.valueOf(jogo.getNivel()),
                h.x + passo * 2, linha, passo);
        escreverValor(g2, "TAMANHO", String.valueOf(jogo.getCobra().tamanho()),
                h.x + passo * 3, linha, passo);
    }

    private void escreverValor(Graphics2D g2, String rotulo, String valor,
            int x, int y, int largura) {
        centralizar(g2, rotulo, new Rectangle(x, y, largura, 18), fonte(11, Font.PLAIN),
                TEXTO_FRACO);
        centralizar(g2, valor, new Rectangle(x, y + 16, largura, 26), fonte(20, Font.BOLD),
                TEXTO);
    }

    private void desenharFaixaBorda(Graphics2D g2, Campo.Borda borda) {
        Rectangle f = layout.faixaBorda();
        boolean wrap = borda == Campo.Borda.WRAP;
        String texto = wrap
                ? "BORDA: DÁ A VOLTA — a cobra atravessa e reaparece do outro lado"
                : "BORDA: MATA — encostar na parede encerra a partida";
        g2.setColor(new Color(DESTAQUE.getRed(), DESTAQUE.getGreen(),
                DESTAQUE.getBlue(), 26));
        g2.fillRoundRect(f.x, f.y, f.width, f.height, 10, 10);
        centralizar(g2, texto, f, fonte(12, Font.PLAIN), wrap ? DESTAQUE : TEXTO_FRACO);
    }

    private void desenharRodape(Graphics2D g2, List<Poder.Ativo> ativos) {
        if (ativos.isEmpty()) {
            centralizar(g2, "Espaço: pausar    R: recomeçar    Esc: sair",
                    layout.rodape(), fonte(12, Font.PLAIN), TEXTO_FRACO);
            return;
        }
        for (int i = 0; i < ativos.size(); i++) {
            Poder.Ativo a = ativos.get(i);
            Rectangle c = layout.cartaoPoder(i, ativos.size());
            Color cor = new Color(a.getTipo().getCorRgb());

            g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 40));
            g2.fillRoundRect(c.x, c.y, c.width, c.height, 10, 10);
            g2.setColor(cor);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(c.x, c.y, c.width, c.height, 10, 10);
            centralizar(g2, a.getTipo().getRotulo(),
                    new Rectangle(c.x, c.y + 2, c.width, c.height - 16),
                    fonte(16, Font.BOLD), cor);
            centralizar(g2, a.segundosRestantes() + "s",
                    new Rectangle(c.x, c.y + c.height - 17, c.width, 14),
                    fonte(11, Font.PLAIN), TEXTO_FRACO);
        }
    }

    // ------------------------------------------------------------------
    // Sobreposições
    // ------------------------------------------------------------------

    private void desenharBriefing(Graphics2D g2) {
        Rectangle p = layout.painel(520, 340);
        painelFundo(g2, p);
        centralizar(g2, "SNAKE", new Rectangle(p.x, p.y + 22, p.width, 40),
                fonte(30, Font.BOLD), TEXTO);
        centralizar(g2, "Setas ou W A S D para virar", new Rectangle(p.x, p.y + 66, p.width, 20),
                fonte(13, Font.PLAIN), TEXTO_FRACO);

        int y = p.y + 108;
        centralizar(g2, "BORDA      [1] mata      [2] dá a volta",
                new Rectangle(p.x, y, p.width, 22), fonte(14, Font.PLAIN), TEXTO);
        y += 30;
        centralizar(g2, "DIFICULDADE      [3] fácil      [4] médio      [5] difícil",
                new Rectangle(p.x, y, p.width, 22), fonte(14, Font.PLAIN), TEXTO);
        y += 40;
        centralizar(g2, "Começa no médio, com a parede matando. [Enter] para jogar",
                new Rectangle(p.x, y, p.width, 20), fonte(12, Font.PLAIN), TEXTO_FRACO);
        y += 26;
        // a skin é lida do pintor, e não perguntada à tela: quem está pintando já
        // sabe, e perguntar criaria dois lugares gratuitos discordando
        centralizar(g2, "Skin " + skin.getNome() + "   —   [N] troca",
                new Rectangle(p.x, y, p.width, 20), fonte(12, Font.PLAIN), TEXTO_FRACO);
    }

    private void desenharPausa(Graphics2D g2) {
        Rectangle p = layout.painel(360, 160);
        painelFundo(g2, p);
        centralizar(g2, "PAUSADO", new Rectangle(p.x, p.y + 34, p.width, 34),
                fonte(24, Font.BOLD), TEXTO);
        centralizar(g2, "Espaço continua    R recomeça", new Rectangle(p.x, p.y + 80, p.width, 20),
                fonte(13, Font.PLAIN), TEXTO_FRACO);
    }

    /**
     * A tela de fim de partida.
     *
     * <p>Este método só <b>mostra</b> o selo de novo recorde; ele não decide e
     * não grava nada. Quem sabe se foi recorde é a janela, uma única vez, quando
     * a partida acaba. Gravar aqui faria o arquivo ser reescrito a cada
     * repintura — e como o registro só aceita números maiores, a primeira
     * gravação mostraria o selo e todas as seguintes leria o mesmo número de
     * volta e o apagariam, deixando o selo piscar.
     *
     * @param novoRecorde se esta partida bateu o recorde
     */
    private void desenharFim(Graphics2D g2, JogoSnake jogo, String titulo, String subtitulo,
            boolean novoRecorde) {
        Rectangle p = layout.painel(440, 260);
        painelFundo(g2, p);
        centralizar(g2, titulo, new Rectangle(p.x, p.y + 26, p.width, 34),
                fonte(24, Font.BOLD), novoRecorde ? DESTAQUE : TEXTO);
        centralizar(g2, subtitulo, new Rectangle(p.x, p.y + 64, p.width, 20),
                fonte(13, Font.PLAIN), TEXTO_FRACO);
        centralizar(g2, "Pontos: " + jogo.getPontos() + "      Recorde: "
                        + Math.max(jogo.getRecorde(), jogo.getPontos()),
                new Rectangle(p.x, p.y + 100, p.width, 28), fonte(18, Font.BOLD), TEXTO);
        centralizar(g2, "Tamanho máximo: " + jogo.getMaiorComprimento()
                        + "      Comidas: " + jogo.getComidas(),
                new Rectangle(p.x, p.y + 130, p.width, 20), fonte(12, Font.PLAIN), TEXTO_FRACO);
        if (novoRecorde) {
            centralizar(g2, "NOVO RECORDE", new Rectangle(p.x, p.y + 160, p.width, 26),
                    fonte(15, Font.BOLD), DESTAQUE);
        }
        centralizar(g2, "[R] jogar de novo      [Esc] sair",
                new Rectangle(p.x, p.y + 200, p.width, 20), fonte(12, Font.PLAIN), TEXTO_FRACO);
    }

    private void painelFundo(Graphics2D g2, Rectangle p) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
        g2.setColor(FUNDO);
        g2.fillRoundRect(p.x, p.y, p.width, p.height, 16, 16);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(BORDA);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(p.x, p.y, p.width, p.height, 16, 16);
    }

    // ------------------------------------------------------------------
    // Ajuda de texto
    // ------------------------------------------------------------------

    static Font fonte(int tamanho, int estilo) {
        return new Font(Font.SANS_SERIF, estilo, tamanho);
    }

    private void centralizar(Graphics2D g2, String texto, Rectangle r, Font f, Color cor) {
        g2.setFont(f);
        g2.setColor(cor);
        FontMetrics fm = g2.getFontMetrics(f);
        int x = r.x + (r.width - fm.stringWidth(texto)) / 2;
        int y = r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(texto, Math.max(r.x, x), y);
    }

    private void centralizarEsquerda(Graphics2D g2, String texto, Rectangle r, Font f) {
        g2.setFont(f);
        g2.setColor(TEXTO);
        FontMetrics fm = g2.getFontMetrics(f);
        g2.drawString(texto, r.x, r.y + fm.getAscent());
    }

    static Color mesclar(Color a, Color b, double t) {
        double k = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * k),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k));
    }
}
