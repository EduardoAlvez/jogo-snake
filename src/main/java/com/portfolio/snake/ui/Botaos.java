package com.portfolio.snake.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Registro de botões da tela e da posição do ponteiro sobre eles.
 *
 * <p><b>A lista é efêmera; o highlight não.</b> Quem reconstrói os botões é a
 * própria pintura — {@code DesenhoJogo.pintar} limpa e redesenha os overlays a
 * cada quadro. Guardar o hover em cada instância de {@link Botao} faz o destaque
 * sumir na repinturação seguinte, porque o paint substitui os objetos por outros
 * com o estado zerado.</p>
 *
 * <p>A saída é guardar aqui, fora da pintura e fora dos botões, a <em>chave
 * geométrica</em> do botão sob o mouse. A chave é derivada do retângulo, então a
 * instância recriada pelo paint reencontra o highlight sem precisar carregar
 * estado.</p>
 *
 * <p>Classe pura, sem Swing: {@link #moverPara}, {@link #sob} e
 * {@link #temHighlight} são decidíveis sem display, e é o que permite testar o
 * defeito sem abrir janela.</p>
 *
 * @author Eduardo Alvez
 */
public final class Botaos {

    private final List<Botao> lista = new ArrayList<>();

    /** Chave do botão sob o ponteiro, ou {@code null} se o mouse está fora de todos. */
    private String chaveHighlight;

    /**
     * Esvazia o registro. Chamado pela pintura no início de cada quadro, antes de
     * redesenhar os overlays.
     *
     * <p>Repinturar <b>não</b> descarta o highlight: {@link #chaveHighlight} é
     * preservado de propósito. Esse é o ponto da classe.</p>
     */
    public void limpar() {
        lista.clear();
    }

    /**
     * Adiciona um botão ao registro.
     *
     * @param b botão criado pela pintura
     * @return o mesmo botão, para encadear chamadas
     */
    public Botao adicionar(Botao b) {
        lista.add(b);
        return b;
    }

    /**
     * Move o ponteiro e recalcula o highlight.
     *
     * <p>Se o novo botão sob o mouse for o mesmo que já estava em destaque, o
     * estado não muda e {@code false} é devolvido — evita repintar a tela a cada
     * pixel de movimento dentro do mesmo botão.</p>
     *
     * @param px coordenada X do ponteiro
     * @param py coordenada Y do ponteiro
     * @return {@code true} se o highlight mudou de um botão para outro
     */
    public boolean moverPara(int px, int py) {
        String anterior = chaveHighlight;
        chaveHighlight = null;
        for (Botao b : lista) {
            if (b.contem(px, py)) {
                chaveHighlight = b.chave();
                // Sem break: o último da lista vence, que é a ordem de pintura.
                // Botões desenhados depois ficam por cima dos anteriores, e o
                // clique tem de acertar o que o jogador vê.
            }
        }
        return !igual(anterior, chaveHighlight);
    }

    /**
     * Diz se um botão está em destaque.
     *
     * <p>Compara por {@link Botao#chave()}, e não por identidade: é isso que faz
     * o destaque sobreviver à repinturação, já que o paint entrega uma instância
     * nova no mesmo lugar.</p>
     *
     * @param b botão a consultar
     * @return {@code true} se o ponteiro está sobre este botão
     */
    public boolean temHighlight(Botao b) {
        return chaveHighlight != null && chaveHighlight.equals(b.chave());
    }

    /**
     * Procura o botão sob um ponto, usando a geometria.
     *
     * @param px coordenada X do ponteiro
     * @param py coordenada Y do ponteiro
     * @return o botão encontrado, ou {@code null} se o ponto não pertence a nenhum
     */
    public Botao sob(int px, int py) {
        Botao achado = null;
        for (Botao b : lista) {
            if (b.contem(px, py)) {
                achado = b;
            }
        }
        return achado;
    }

    /**
     * Executa a ação do botão sob um ponto, se houver.
     *
     * @param px coordenada X do ponteiro
     * @param py coordenada Y do ponteiro
     * @return {@code true} se algum botão foi acionado
     */
    public boolean acionar(int px, int py) {
        Botao b = sob(px, py);
        if (b == null) {
            return false;
        }
        b.acao.run();
        return true;
    }

    /**
     * Chave do botão em destaque, exposta para teste.
     *
     * @return a chave, ou {@code null} se nenhum botão está em destaque
     */
    public String chaveHighlight() {
        return chaveHighlight;
    }

    /** @return o número de botões registrados */
    public int tamanho() {
        return lista.size();
    }

    private static boolean igual(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}