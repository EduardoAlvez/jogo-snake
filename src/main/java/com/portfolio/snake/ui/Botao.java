package com.portfolio.snake.ui;

/**
 * Botão desenhado à mão, usado no menu e nas sobreposições.
 *
 * <p>Classe pura de propósito: não depende de Swing, tem {@link #contem} e
 * {@link #chave} decidíveis sem janela. É o que permite testar o hover deste
 * pacote sem abrir display.</p>
 *
 * <p><b>Por que a identidade é geométrica.</b> A lista de botões é recriada a
 * cada repinturação, porque quem decide o desenho dos overlays é o próprio
 * desenho. Guardar o hover <em>nesta instância</em> significa que ele morre na
 * repinturação seguinte: o mouse marca o botão, o paint o substitui por uma
 * instância nova com {@code hover = false}, e o destaque — que já estava
 * pintando — nunca chega a aparecer. No Pong esse defeito existiu por um bom
 * tempo e nenhum teste o via, porque a linha que calculava a posição ficava
 * dentro da pintura.</p>
 *
 * <p>Por isso {@link #chave()} devolve a geometria, e não a identidade do
 * objeto. O botão da próxima repinturação tem o mesmo retângulo, então a mesma
 * chave, e o highlight sobrevive ao repaint. O texto fica <em>fora</em> da chave
 * de propósito: o botão de skin muda de nome entre "Trocar skin" e o nome
 * escolhido sem trocar de lugar, e uma chave com o texto dentro perderia o
 * highlight no clique.</p>
 *
 * @author Eduardo Alvez
 */
public final class Botao {

    /** Texto exibido no botão. */
    public final String texto;

    /** Posição e tamanho do botão, em pixels do painel. */
    public final int x, y, w, h;

    /** O que acontece quando o botão é clicado. */
    public final Runnable acao;

    /** Desenha o botão no estado de destaque dourado. */
    public boolean selecionado;

    /** Botão inativo não recebe hover nem clique. */
    public boolean ativo = true;

    /**
     * Cria um botão.
     *
     * @param texto rótulo desenhado no centro
     * @param x borda esquerda
     * @param y borda superior
     * @param w largura
     * @param h altura
     * @param acao executada no clique
     */
    public Botao(String texto, int x, int y, int w, int h, Runnable acao) {
        this.texto = texto;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.acao = acao;
    }

    /**
     * Identidade estável deste botão entre repinturas.
     *
     * @return a chave geométrica {@code x:y:w:h}
     */
    public String chave() {
        return x + ":" + y + ":" + w + ":" + h;
    }

    /**
     * Diz se o ponto está sobre este botão ativo.
     *
     * @param px coordenada X do ponteiro
     * @param py coordenada Y do ponteiro
     * @return {@code true} se o ponto pertence ao retângulo e o botão está ativo
     */
    public boolean contem(int px, int py) {
        return ativo && px >= x && px <= x + w && py >= y && py <= y + h;
    }
}