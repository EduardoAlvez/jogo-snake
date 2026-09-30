package com.portfolio.snake.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A cobra: uma lista de células da cabeça ao rabo, e a direção em que anda.
 *
 * <p>A cabeça fica no índice 0 do deque e o rabo no fim. A cada passo a cabeça
 * entra pela frente e, se não comeu, a cauda sai pela retaguarda.
 *
 * <p><b>A ordem importa e é a origem do bug mais citado em implementações de
 * Snake.</b> Se a colisão com o próprio corpo for checada <em>antes</em> de a
 * cauda ser removida, o jogador perde a vida entrando exatamente na célula que
 * a cauda está liberando neste mesmo passo — algo que a regra permite. Por isso
 * {@link #moverPara} remove a cauda antes de devolver o corpo, e o
 * {@link JogoSnake} faz a checagem sobre o corpo já atualizado. O caminho
 * inverso é o que gera a morte que "não devia ter acontecido".
 */
public final class Cobra {

    /**
     * O corpo, com a cabeça no índice 0 e o rabo no fim.
     *
     * <p>É uma lista e não um deque porque o desenho e a checagem de colisão
     * acessam segmentos por índice a cada quadro, e {@code ArrayList} dá isso em
     * tempo constante enquanto {@code ArrayDeque} percorreria do começo.
     */
    private final List<Celula> corpo = new ArrayList<>();

    private final Campo campo;
    private Direcao direcao;

    /** Direções que ainda vão acontecer, na ordem. */
    private final List<Direcao> fila = new ArrayList<>();

    /**
     * Cria a cobra de comprimento inicial, deitada para a direita e com a
     * cabeça no meio da linha vertical do campo.
     *
     * @param campo      o tabuleiro
     * @param comprimento número de segmentos, no mínimo 2
     */
    public Cobra(Campo campo, int comprimento) {
        this.campo = campo;
        if (comprimento < 2) {
            throw new IllegalArgumentException(
                    "A cobra precisa de ao menos 2 segmentos, veio " + comprimento);
        }
        if (comprimento > campo.getLargura()) {
            // Deitada e apontando para a direita, a cobra ocupa `comprimento`
            // colunas. Sem esta checagem o corpo sairia pela coluna 0 em
            // coordenadas negativas e a cobra nasceria dentro da parede.
            throw new IllegalArgumentException("A cobra de " + comprimento
                    + " segmentos não cabe na largura " + campo.getLargura()
                    + " do campo");
        }
        int cabecaY = campo.getAltura() / 2;
        // A cabeça fica no meio quando há espaço e desloca para a direita
        // quando a cobra é comprida demais para caber a partir do meio.
        int cabecaX = Math.min(campo.getLargura() - 1,
                Math.max(campo.getLargura() / 2, comprimento - 1));
        for (int i = 0; i < comprimento; i++) {
            corpo.add(new Celula(cabecaX - i, cabecaY));
        }
        this.direcao = Direcao.DIREITA;
    }

    /** Cria a cobra de comprimento 3, o mínimo jogável. */
    public Cobra(Campo campo) {
        this(campo, 3);
    }

    /**
     * Enfileira uma direção para o próximo passo.
     *
     * <p><b>A validação é feita contra a última direção da fila, e não contra a
     * direção atual.</b> É esse detalhe que evita a inversão de 180° num passo:
     * se a cobra está indo para a direita e o jogador aperta CIMA e ESQUERDA
     * dentro do mesmo tique, a fila vira [CIMA, ESQUERDA] e o passo seguinte
     * inverteria a cobra sobre o próprio corpo. Comparando com a última
     * enfileirada, ESQUERDA é rejeitada porque é oposta a CIMA.
     *
     * <p>A fila também limita o quanto o jogador pode "adiantar" direção, o
     * que impede que uma sequência rápida de teclas acumule viradas para longe
     * de onde a cobra vai estar.
     *
     * @param nova direção desejada
     * @return {@code true} se a direção foi aceita
     */
    public boolean enfileirar(Direcao nova) {
        if (nova == null) {
            return false;
        }
        if (fila.size() >= FILA_MAXIMA) {
            return false;
        }
        // A referência é a última direção que ainda vai acontecer, que é a
        // última da fila, ou a direção atual quando a fila está vazia.
        Direcao referencia = fila.isEmpty() ? direcao : fila.get(fila.size() - 1);
        if (nova == referencia) {
            return false; // repetir a mesma direção não muda nada
        }
        if (nova.eOposta(referencia)) {
            return false; // inversão de 180°: morte certa
        }
        fila.add(nova);
        return true;
    }

    /** Máximo de direções que podem ficar enfileiradas de uma vez. */
    private static final int FILA_MAXIMA = 2;

    /**
     * Limpa a fila de direções pendentes, sem mexer na direção atual.
     *
     * <p>Usado ao iniciar ou reiniciar a partida: teclas pressionadas durante o
     * fim de jogo anterior não podem vazar para a próxima.
     */
    public void limparFila() {
        fila.clear();
    }

    /** Número de direções esperando para acontecer. */
    public int tamanhoFila() {
        return fila.size();
    }

    /**
     * Tira da fila a próxima direção, ou devolve a atual se a fila estiver vazia,
     * e passa a valer como direção da cobra.
     *
     * <p>Atualizar o campo aqui é essencial. A direção enfileirada é a direção
     * <em>deste</em> passo, e se o campo só fosse atualizado no passo seguinte a
     * cobra voltaria para a direção antiga: o jogador viraria, a cabeça andaria
     * um passo para o lado pedido, e no passo seguinte a cobra voltaria a andar
     * para onde ia antes. A virada parecia funcionar por um tique e era
     * descartada no seguinte.
     *
     * <p>Separado de {@link #avancar} porque quem decide o destino é o
     * {@link JogoSnake}, que precisa aplicar a regra de borda <b>antes</b> de a
     * cabeça se mover — a direção tem de ser conhecida antes do passo, e não
     * durante ele.
     *
     * @return a direção deste passo
     */
    public Direcao consumirDirecao() {
        if (!fila.isEmpty()) {
            direcao = fila.removeFirst();
        }
        return direcao;
    }

    public Direcao getDirecao() {
        return direcao;
    }

    /** A cabeça. */
    public Celula cabeca() {
        return corpo.get(0);
    }

    /** O rabo, ou seja, a última célula. */
    public Celula rabo() {
        return corpo.get(corpo.size() - 1);
    }

    /** Célula na posição {@code i}: 0 é a cabeça, {@code tamanho()-1} é o rabo. */
    public Celula segmento(int i) {
        return corpo.get(i);
    }

    /** Número de segmentos. */
    public int tamanho() {
        return corpo.size();
    }

    /** Cópia do corpo, da cabeça ao rabo, para desenho e para os testes. */
    public List<Celula> segmentos() {
        return new ArrayList<>(corpo);
    }

    /**
     * Faz um passo na direção enfileirada (ou na atual, se a fila estiver vazia).
     *
     * <p>A cauda é removida <em>antes</em> do corpo ser devolvido, para que quem
     * chama possa checar colisão sobre um corpo já consistente com a regra.
     * Se a cobra estiver crescendo, a cauda permanece e a lista cresce.
     *
     * @return {@code true} se houve crescimento neste passo
     */
    public boolean moverPara(Direcao desejada, boolean comeu) {
        if (desejada != null) {
            direcao = desejada;
        }
        Celula cabeca = corpo.get(0);
        return inserirCabeca(new Celula(cabeca.getX() + direcao.dx(),
                cabeca.getY() + direcao.dy()), comeu);
    }

    /**
     * Move a cabeça para uma célula já calculada, consumindo a fila de direções.
     *
     * <p>Existe porque a regra de borda pertence ao {@link Campo}, não à cobra.
     * Com {@link Campo.Borda#WRAP} a cabeça que sai pela direita tem de aparecer
     * pela esquerda, e essa correção é feita por {@link Campo#traduzir} — mas
     * {@link #moverPara(Direcao, boolean)} calcula o destino sozinho e ignora a
     * correção. Passando o destino pronto, a cobra obedece à regra sem conhecer
     * o tabuleiro, e o modo wrap funciona sem código especial.
     *
     * @param destino célula de destino, já dentro do campo
     * @param comeu   se a cabeça cameu neste passo
     * @return {@code true} se houve crescimento
     */
    public boolean avancarPara(Celula destino, boolean comeu) {
        return inserirCabeca(destino, comeu);
    }

    /**
     * Insere a cabeça e tira a cauda, a menos que a cobra tenha comido.
     *
     * <p>Comer é o que faz a cobra crescer: a cabeça entra e a cauda não sai,
     * então o corpo ganha um segmento. Sem comer, a cauda sai e o comprimento
     * se mantém. Este é o único mecanismo de crescimento, e a ordem
     * inseriu-antes-de-remover é o que o {@link JogoSnake} depende para checar
     * colisão sobre um corpo consistente com a regra.
     */
    private boolean inserirCabeca(Celula destino, boolean comeu) {
        corpo.add(0, destino);
        if (!comeu) {
            corpo.remove(corpo.size() - 1);
        }
        return comeu;
    }

    /**
     * Faz um passo consumindo a próxima direção da fila, se houver.
     *
     * @return {@code true} se houve crescimento neste passo
     */
    public boolean avancar(boolean comeu) {
        Direcao d = consumirDirecao();
        Celula c = corpo.get(0);
        return inserirCabeca(new Celula(c.getX() + d.dx(), c.getY() + d.dy()), comeu);
    }

    /**
     * Encurta a cobra em até {@code quantos} segmentos.
     *
     * <p>Nunca deixa a cobra com menos de 2 segmentos, porque uma cobra de
     * cabeça só não tem corpo e não pode nem colidir com ela mesma.
     *
     * @param quantos quantos segmentos remover
     * @return quantos foram de fato removidos
     */
    public int encurtar(int quantos) {
        int removidos = 0;
        for (int i = 0; i < quantos && corpo.size() > 2; i++) {
            corpo.remove(corpo.size() - 1);
            removidos++;
        }
        return removidos;
    }

    /**
     * Verifica se a cabeça caiu sobre algum segmento do próprio corpo.
     *
     * <p>Deve ser chamado <em>depois</em> de {@link #avancar}, sobre o corpo já
     * atualizado — ver a nota de classe sobre a ordem.
     *
     * @return {@code true} se houve colisão consigo mesma
     */
    public boolean colidiuComSi() {
        Celula cabeca = corpo.get(0);
        int i = 0;
        for (Celula c : corpo) {
            if (i++ == 0) {
                continue; // a própria cabeça não conta
            }
            if (c.getX() == cabeca.getX() && c.getY() == cabeca.getY()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica se a cabeça está sobre uma célula ocupada por um objeto externo
     * (comida, poder, obstáculo).
     *
     * @param outra célula a comparar
     * @return {@code true} se a cabeça está na mesma célula
     */
    public boolean cabecaEm(Celula outra) {
        return outra != null && cabeca().getX() == outra.getX() && cabeca().getY() == outra.getY();
    }

    /**
     * A forma de cada segmento, da cabeça ao rabo, para o desenho.
     *
     * <p>Cada segmento sabe de onde a cobra veio e para onde vai, e é isso que
     * decide se ele é reto ou canto. Desenhar um quadrado por célula sem essa
     * informação produz uma centipede, não uma cobra — é o erro visual mais
     * comum em implementações de Snake.
     *
     * @return a forma de cada segmento, alinhada por índice com {@link #segmento(int)}
     */
    public List<Forma> formas() {
        List<Forma> lista = new ArrayList<>(corpo.size());
        int n = corpo.size();
        for (int i = 0; i < n; i++) {
            Direcao entrada = i > 0 ? direcaoDe(corpo.get(i - 1), corpo.get(i)) : null;
            Direcao saida = i < n - 1 ? direcaoDe(corpo.get(i), corpo.get(i + 1)) : null;
            lista.add(Forma.de(entrada, saida));
        }
        return lista;
    }

    /** Direção de um segmento vizinho para o outro (vizinho -> atual). */
    private static Direcao direcaoDe(Celula de, Celula para) {
        int dx = Integer.compare(para.getX(), de.getX());
        int dy = Integer.compare(para.getY(), de.getY());
        if (dx > 0) {
            return Direcao.DIREITA;
        }
        if (dx < 0) {
            return Direcao.ESQUERDA;
        }
        if (dy > 0) {
            return Direcao.BAIXO;
        }
        return Direcao.CIMA;
    }

    /** A célula da cabeça como par ordenado, para comparação e conjuntos. */
    public long chaveCabeca() {
        return cabeca().chave();
    }

    /** O campo onde a cobra está. */
    public Campo getCampo() {
        return campo;
    }
}
