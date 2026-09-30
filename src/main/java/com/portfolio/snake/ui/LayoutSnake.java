package com.portfolio.snake.ui;

import java.awt.Rectangle;

/**
 * Onde cada coisa é desenhada, calculado sem Swing e sem abrir janela.
 *
 * <p>Esta classe existe por causa de um defeito do Pong: os badges saíam do
 * campo quando a raquete encostava no topo, e <b>nenhum teste pegou</b>, porque
 * as posições eram calculadas dentro do método de desenho e nada além da tela
 * conseguia inspecioná-las. O conserto não foi no cálculo — foi tirar o cálculo
 * de dentro da tela.
 *
 * <p>Por isso aqui nenhuma posição é calculada durante o desenho. A
 * {@link TelaSnake} pergunta a esta classe onde cada coisa fica, e a resposta é
 * um {@link Rectangle} que um teste sem display consegue conferir. Se um badge,
 * um cartão de poder ou o painel de fim de partida saírem da janela, existe um
 * teste que falha — não um relato de bug depois de rodar o jogo.
 *
 * <p>A grade é sempre um número inteiro de células quadradas: a largura do
 * tabuleiro é {@code celula * colunas}, e não "a largura menos a margem
 * dividida por colunas". Arredondar por célula é o que produz a emenda visível
 * entre dois segmentos da cobra, e a sobra de meio pixel vira uma fresta que
 * aparece e some conforme a cobra vira.
 */
public final class LayoutSnake {

    private final int larguraJanela;
    private final int alturaJanela;
    private final int colunas;
    private final int linhas;
    private final int margem;

    private final int alturaHud;
    private final int alturaRodape;
    private final int alturaFaixaTopo;

    private final int celula;
    private final int larguraTabuleiro;
    private final int alturaTabuleiro;
    private final int tabuleiroX;
    private final int tabuleiroY;

    private static final int LARGURA_PADRAO = 720;
    private static final int ALTURA_PADRAO = 700;
    private static final int COLUNAS_PADRAO = 20;
    private static final int LINHAS_PADRAO = 20;
    private static final int MARGEM_PADRAO = 16;
    private static final int HUD_PADRAO = 96;
    private static final int RODAPE_PADRAO = 72;
    private static final int FAIXA_TOPO_PADRAO = 40;

    /** Cria o layout padrão: 720x700 com grade 20x20. */
    public LayoutSnake() {
        this(LARGURA_PADRAO, ALTURA_PADRAO, COLUNAS_PADRAO, LINHAS_PADRAO,
                MARGEM_PADRAO, HUD_PADRAO, RODAPE_PADRAO, FAIXA_TOPO_PADRAO);
    }

    /**
     * Cria um layout com as medidas dadas.
     *
     * @param larguraJanela  largura da janela em pixels
     * @param alturaJanela   altura da janela em pixels
     * @param colunas        células na horizontal
     * @param linhas         células na vertical
     * @param margem         respiro entre a janela e o conteúdo
     * @param alturaHud      altura da faixa de placar no topo
     * @param alturaRodape   altura da faixa de poderes embaixo
     * @param alturaFaixaTopo altura reservada acima do tabuleiro, para o modo de borda
     */
    public LayoutSnake(int larguraJanela, int alturaJanela, int colunas, int linhas,
            int margem, int alturaHud, int alturaRodape, int alturaFaixaTopo) {
        if (larguraJanela < 120 || alturaJanela < 120) {
            throw new IllegalArgumentException("janela pequena demais: "
                    + larguraJanela + "x" + alturaJanela);
        }
        if (colunas < 4 || linhas < 4) {
            throw new IllegalArgumentException("grade pequena demais: "
                    + colunas + "x" + linhas);
        }
        this.larguraJanela = larguraJanela;
        this.alturaJanela = alturaJanela;
        this.colunas = colunas;
        this.linhas = linhas;
        this.margem = margem;
        this.alturaHud = alturaHud;
        this.alturaRodape = alturaRodape;
        this.alturaFaixaTopo = alturaFaixaTopo;

        // A célula é o maior quadrado inteiro que cabe na área disponível.
        // Arredondar para baixo é o que garante largura exata: tabuleiro = celula * colunas.
        //
        // A altura disponível desconta a margem DUAS vezes, uma embaixo e outra
        // em cima. Descontar uma só faz o tabuleiro descer até dentro do rodapé:
        // o rodapé começa em alturaJanela - margem - alturaRodape, e um tabuleiro
        // 16px mais alto alcança os 8 pixels do rodapé. Foi o que o teste de
        // sobreposição pegou.
        int larguraUtil = larguraJanela - 2 * margem;
        int alturaUtil = alturaJanela - 2 * margem
                - alturaHud - alturaFaixaTopo - alturaRodape;
        this.celula = Math.max(4, Math.min(larguraUtil / colunas, alturaUtil / linhas));
        this.larguraTabuleiro = celula * colunas;
        this.alturaTabuleiro = celula * linhas;
        this.tabuleiroX = margem + (larguraUtil - larguraTabuleiro) / 2;
        this.tabuleiroY = margem + alturaHud + alturaFaixaTopo
                + (alturaUtil - alturaTabuleiro) / 2;
    }

    // ------------------------------------------------------------------
    // As áreas
    // ------------------------------------------------------------------

    /** A janela inteira, que é a fronteira que nada pode atravessar. */
    public Rectangle janela() {
        return new Rectangle(0, 0, larguraJanela, alturaJanela);
    }

    /** A faixa de placar no topo. */
    public Rectangle hud() {
        return new Rectangle(margem, margem, larguraJanela - 2 * margem, alturaHud);
    }

    /**
     * A faixa que explica a regra da borda, logo acima do tabuleiro.
     *
     * <p>Existe porque "a parede mata" e "a cobra dá a volta" são regras
     * invisíveis: o mesmo comando em teclas diferentes produzresults opostos, e
     * o jogador só descobre batendo. Dizer qual das duas está valendo é mais
     * barato do que deixar a surpresa acontecer.
     */
    public Rectangle faixaBorda() {
        return new Rectangle(margem, margem + alturaHud,
                larguraJanela - 2 * margem, alturaFaixaTopo);
    }

    /** A área jogável, já sem borda desenhada. */
    public Rectangle tabuleiro() {
        return new Rectangle(tabuleiroX, tabuleiroY, larguraTabuleiro, alturaTabuleiro);
    }

    /** A faixa de poderes embaixo. */
    public Rectangle rodape() {
        return new Rectangle(margem, alturaJanela - margem - alturaRodape,
                larguraJanela - 2 * margem, alturaRodape);
    }

    // ------------------------------------------------------------------
    // Itens dentro do tabuleiro
    // ------------------------------------------------------------------

    /**
     * A área de uma célula da grade.
     *
     * @param x coluna
     * @param y linha
     * @return o retângulo exato daquela célula
     */
    public Rectangle celula(int x, int y) {
        return new Rectangle(tabuleiroX + x * celula, tabuleiroY + y * celula,
                celula, celula);
    }

    /**
     * A área de um segmento, com folga para o corpo não encostar na grade.
     *
     * <p>A folga é proporcional e nunca zera: com a folga igual à célula o
     * segmento some, e sem folga os vizinhos ficam colados num bloco só, que é
     * o outro jeito de a cobra virar um quadrado sem forma.
     *
     * @param x     coluna
     * @param y     linha
     * @param folga fração da célula deixada vazia, de 0 a 0.4
     * @return o retângulo do segmento
     */
    public Rectangle segmento(int x, int y, double folga) {
        int recuo = (int) Math.round(celula * Math.min(0.4, Math.max(0, folga)));
        return new Rectangle(tabuleiroX + x * celula + recuo,
                tabuleiroY + y * celula + recuo,
                celula - 2 * recuo, celula - 2 * recuo);
    }

    /**
     * Onde um cartão de poder vai no rodapé.
     *
     * <p>Os cartões são centralizados como um conjunto e a largura encolhe se
     * não couberem, em vez de transbordar. A última defesa é o clamp do lado
     * direito: um cartão quepassa da janela é o defeito do Pong repetido, e ele
     * não pode depender de a conta anterior estar certa.
     *
     * @param indice posição do cartão, começando em 0
     * @param total  quantos cartões vão ser desenhados
     * @return o retângulo do cartão, sempre dentro do rodapé e da janela
     */
    public Rectangle cartaoPoder(int indice, int total) {
        Rectangle rodape = rodape();
        if (total <= 0) {
            return new Rectangle(rodape.x, rodape.y, 0, 0);
        }
        int gap = 10;
        int alturaCartao = Math.min(44, rodape.height);
        int larguraMax = (rodape.width - gap * (total - 1)) / total;
        int largura = Math.max(36, Math.min(150, larguraMax));
        int larguraTotal = largura * total + gap * (total - 1);
        int x = rodape.x + (rodape.width - larguraTotal) / 2 + indice * (largura + gap);
        int y = rodape.y + (rodape.height - alturaCartao) / 2;

        // clamp final: nada sai da janela, aconteça o que acontecer na conta
        int maxX = rodape.x + rodape.width - largura;
        if (x > maxX) {
            x = maxX;
        }
        if (x < rodape.x) {
            x = rodape.x;
        }
        return new Rectangle(x, y, largura, alturaCartao);
    }

    /**
     * O painel de sobreposição, centrado na janela.
     *
     * <p>Usado no briefing, na pausa e no fim de partida. Nasce dentro da janela
     * por construção, com uma margem, e é verificado por teste para as três
     * situações — inclusive quando o texto é maior do que a janela.
     *
     * @param larguraDesejada largura do painel
     * @param alturaDesejada  altura do painel
     * @return o painel já LIMITED à janela
     */
    public Rectangle painel(int larguraDesejada, int alturaDesejada) {
        int largura = Math.min(larguraDesejada, larguraJanela - 2 * margem);
        int altura = Math.min(alturaDesejada, alturaJanela - 2 * margem);
        return new Rectangle(margem + (larguraJanela - 2 * margem - largura) / 2,
                margem + (alturaJanela - 2 * margem - altura) / 2,
                largura, altura);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public int getLarguraJanela() {
        return larguraJanela;
    }

    public int getAlturaJanela() {
        return alturaJanela;
    }

    public int getCelula() {
        return celula;
    }

    public int getColunas() {
        return colunas;
    }

    public int getLinhas() {
        return linhas;
    }

    /**
     * Um retângulo está inteiramente dentro da janela?
     *
     * <p>É o método que os testes usam para dizer "isto saiu do campo", e é
     * público justamente para que qualquer teste possa usá-lo em vez de
     * reescrever a conta.
     *
     * @param r o retângulo a conferir
     * @return {@code true} se está contido na janela
     */
    public boolean dentroDaJanela(Rectangle r) {
        return r.x >= 0 && r.y >= 0
                && r.x + r.width <= larguraJanela
                && r.y + r.height <= alturaJanela;
    }
}
