package com.portfolio.snake.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * O estado e as regras da partida.
 *
 * <p>Não conhece Swing: a camada {@code ui} desenha o que este objeto diz e
 * chama {@link #passo(double)}. É essa separação que sustenta os testes sem
 * abrir janela.
 *
 * <p><b>A ordem dentro de {@link #passo} é a regra inteira.</b> Remover a cauda
 * antes de checar colisão, checar a comida depois de se mover, ou corrigir a
 * borda depois de gravar a cabeça produzem bugs que não aparecem por exceção —
 * o jogo apenas se comporta errado. Por isso {@link #passo} está numerado e cada
 * passo tem um motivo.
 */
public final class JogoSnake {

    /** Em que fase a partida está. */
    public enum Estado {
        /** Correndo. */
        JOGANDO,
        /** Congelada por pausa ou pelo briefing. */
        PAUSADO,
        /** A cobra bateu: não dá para virar nem comer. */
        FIM,
        /** A cobra cobriu o campo inteiro. */
        VITORIA
    }

    /** Como a cobra reage à parede. */
    public enum Dificuldade {
        /** Intervalo inicial de 170 ms, cai 4 ms por nível. */
        FACIL,
        /** Intervalo inicial de 150 ms, cai 5 ms por nível. */
        MEDIO,
        /** Intervalo inicial de 130 ms, cai 7 ms por nível. */
        DIFICIL;

        /** Milissegundos entre passos no começo da partida. */
        public int intervaloInicial() {
            switch (this) {
                case FACIL:
                    return 170;
                case DIFICIL:
                    return 130;
                case MEDIO:
                default:
                    return 150;
            }
        }

        /** Quanto o intervalo cai a cada nível, em milissegundos. */
        public int reducaoPorNivel() {
            switch (this) {
                case FACIL:
                    return 4;
                case DIFICIL:
                    return 7;
                case MEDIO:
                default:
                    return 5;
            }
        }

        /** Teto do nível, para o intervalo não se aproximar de zero. */
        public int nivelMaximo() {
            return 20;
        }
    }

    /**
     * Por que a partida acabou, quando acabou em derrota.
     *
     * <p>Existe para a interface poder distinguir a morte na parede da morte
     * no próprio corpo: os dois estados são {@link Estado#FIM}, e sem esta
     * distinção a interface não tem como escolher entre dois sons diferentes —
     * ela teria que tocar o mesmo som para duas mortes diferentes, ou adivinhar.
     *
     * <p>É leitura pura: nada do núcleo decide com isto, só informa.
     */
    public enum Motivo {
        /** A cabeça entrou na parede, e não havia fantasma segurando. */
        PAREDE,

        /** A cabeça entrou no próprio corpo. */
        CORPO
    }

    /** Pontos por comer a comida normal. */
    public static final int PONTOS_COMIDA = 10;

    /** A cada quantas comidas um poder pode aparecer. */
    public static final int COMIDAS_POR_PODER = 5;

    private final Campo campo;
    private final Dificuldade dificuldade;
    private final Random aleatorio;

    private Cobra cobra;
    private Comida comida;
    private final List<Poder.Ativo> poderes = new ArrayList<>();
    private final List<PoderNoChao> noChao = new ArrayList<>();

    private Estado estado = Estado.PAUSADO;
    private Motivo motivo;
    private int pontos;
    private int nivel = 1;
    private int comidas;
    private int maiorComprimento;
    private int passos;
    private int recorde;

    /**
     * Cria a partida. Fixar a semente torna o sorteio determinístico, e é o que
     * permite testar "comeu a comida e ganhou 10 pontos" sem rezar para o
     * {@code Random}.
     *
     * @param campo       tabuleiro e regra de borda
     * @param dificuldade quanto o jogo acelera
     * @param semente     semente do sorteio
     */
    public JogoSnake(Campo campo, Dificuldade dificuldade, long semente) {
        this.campo = campo;
        this.dificuldade = dificuldade;
        this.aleatorio = new Random(semente);
        this.cobra = new Cobra(campo, 3);
        this.comida = Comida.padrao(0, 0);
        this.recorde = RegistroDeRecordes.maiorPontuacao();
        colocarComidaLivre();
    }

    /** Partida com dificuldade média e semente aleatória. */
    public JogoSnake(Campo campo) {
        this(campo, Dificuldade.MEDIO, System.nanoTime());
    }

    /**
     * Põe a primeira comida numa célula livre.
     *
     * <p>Se o campo estiver lotado demais para ter comida, a comida fica em
     * (0,0) e o jogo segue: é um caso degenerado que só o teste força.
     */
    private void colocarComidaLivre() {
        Celula livre = Comida.sortearLivre(campo, aleatorio, celulasOcupadas());
        if (livre != null) {
            comida.moverPara(livre.getX(), livre.getY());
        }
    }

    // ------------------------------------------------------------------
    // O laço principal
    // ------------------------------------------------------------------

    /**
     * Avança um tique.
     *
     * <p>Os passos estão numerados porque a ordem é a regra:
     *
     * <ol>
     *   <li>consome o relógio dos poderes, para o efeito expirar no tempo certo;</li>
     *   <li>decide a direção do passo e a célula de destino, corrigindo a borda
     *       <b>antes</b> de a cabeça se mover — assim a comida é checada na célula
     *       onde a cabeça realmente vai cair, e não na célula sem wrap;</li>
     *   <li>trata a parede, que mata ou deixa passar conforme a regra e o poder;</li>
     *   <li>mover, que remove a cauda antes de qualquer checagem de colisão;</li>
     *   <li>checa a colisão com o próprio corpo, agora sobre o corpo correto;</li>
     *   <li>soma os pontos, cresce e sobe de nível;</li>
     *   <li>resorteia a comida, e às vezes um poder.</li>
     * </ol>
     *
     * @param segundos tempo desde o último tique
     */
    public void passo(double segundos) {
        if (estado != Estado.JOGANDO) {
            return;
        }
        // A partida termina no instante em que a grade fica coberta, antes de
        // qualquer outra regra. Checar depois não funciona: com a grade lotada
        // não há célula livre para a cabeça ocupar, e se a parede vier antes da
        // checagem o jogador morre no exato instante em que venceu.
        if (cobra.tamanho() == campo.totalCelulas()) {
            estado = Estado.VITORIA;
            return;
        }
        passos++;

        // 1. relógio dos poderes
        for (int i = poderes.size() - 1; i >= 0; i--) {
            Poder.Ativo p = poderes.get(i);
            p.tick(segundos);
            if (p.acabou()) {
                poderes.remove(i);
            }
        }

        // 2. direção e destino, já com a borda corrigida
        Direcao d = cobra.consumirDirecao();
        Celula cabeca = cobra.cabeca();
        int brutoX = cabeca.getX() + d.dx();
        int brutoY = cabeca.getY() + d.dy();
        int[] corrigida = new int[2];
        boolean dentro = campo.traduzir(brutoX, brutoY, corrigida);
        // 3. parede
        if (!dentro && !cobraAtravessaParede()) {
            estado = Estado.FIM;
            motivo = Motivo.PAREDE;
            return;
        }
        if (!dentro) {
            // o fantasma segurou a parede: a cabeça dá a volta em vez de ficar
            // fora da grade, onde o desenho não saberia onde colocá-la
            campo.dobrarParaDentro(brutoX, brutoY, corrigida);
        }
        Celula destino = new Celula(corrigida[0], corrigida[1]);

        // a comida só conta se a cabeça cair nela no destino final
        boolean comeu = dentro && destino.equals(comida.celula());
        boolean pegouPoder = dentro && noChao.stream().anyMatch(p -> p.celula.equals(destino));

        // 4. mover — a cauda sai aqui, antes de qualquer checagem
        cobra.avancarPara(destino, comeu);

        // 4b. cobriu o campo? A vitória é checada ANTES da colisão de propósito:
        // com a grade lotada não há célula livre para a cabeça ocupar, então
        // seguir em frente produziria uma colisão e o jogador perderia no
        // exato instante em que venceu. E não dá para esperar a comida: um
        // campo lotado não tem onde pôr comida nenhuma, o que travaria a partida
        // num impasse sem fim.
        if (cobra.tamanho() == campo.totalCelulas()) {
            estado = Estado.VITORIA;
            return;
        }

        // 5. colisão, sobre o corpo já atualizado
        if (cobra.colidiuComSi()) {
            estado = Estado.FIM;
            motivo = Motivo.CORPO;
            return;
        }

        // 6. pontuação, crescimento e nível
        if (comeu) {
            pontos += comida.getPontos() * multiplicador();
            comidas++;
            maiorComprimento = Math.max(maiorComprimento, cobra.tamanho());
            nivel = Math.min(dificuldade.nivelMaximo(), nivel + 1);
            if (recorde < pontos) {
                recorde = pontos;
            }
            surgirPoderSeTocou();
            if (!resortearComida()) {
                estado = Estado.VITORIA;
                return;
            }
        }
        if (pegouPoder) {
            coletarPoder(destino);
        }
    }

    /**
     * Se a parede deve ser atravessada neste passo.
     *
     * <p>Com {@link Campo.Borda#WRAP} a parede nunca mata e o poder do fantasma é
     * inútil — o jogador já atravessa de qualquer forma. Dar pontos de vantagem
     * por ele no modo wrap tornaria o poder dominante, então ele não faz nada
     * ali. Com {@link Campo.Borda#MORRE} o fantasma segura a parede, e a
     * cabeça reaparece do lado oposto pelo mesmo {@link Campo#traduzir} do wrap,
     * em vez de ficar fora da grade, onde o desenho não saberia onde colocá-la.
     *
     * @return {@code true} se a cobra pode passar
     */
    private boolean cobraAtravessaParede() {
        if (campo.getBorda() == Campo.Borda.WRAP) {
            return true;
        }
        return temPoder(Poder.FANTASMA);
    }

    /**
     * O multiplicador de pontos atual.
     *
     * @return 2 com o poder de pontos dobrados, 1 caso contrário
     */
    public int multiplicador() {
        return temPoder(Poder.PONTOS_X2) ? 2 : 1;
    }

    /** Uma comida a cada {@value #COMIDAS_POR_PODER} comidas pode soltar um poder. */
    private void surgirPoderSeTocou() {
        if (comidas % COMIDAS_POR_PODER != 0 || noChao.size() >= 2) {
            return;
        }
        Celula livre = Comida.sortearLivre(campo, aleatorio, celulasOcupadas());
        if (livre == null) {
            return;
        }
        Poder[] tipos = Poder.values();
        noChao.add(new PoderNoChao(tipos[aleatorio.nextInt(tipos.length)], livre));
    }

    /**
     * Recolhe o poder sob a cabeça e ativa o efeito.
     *
     * @param destino célula onde a cabeça caiu
     */
    private void coletarPoder(Celula destino) {
        for (int i = 0; i < noChao.size(); i++) {
            if (noChao.get(i).celula.equals(destino)) {
                Poder tipo = noChao.remove(i).tipo;
                for (Poder.Ativo a : poderes) {
                    if (a.getTipo() == tipo) {
                        // renovar em vez de empilhar: dois fantasmas dão 5 s,
                        // e não 10 s de parede infinita
                        a.renovar(tipo.getDuracao());
                        return;
                    }
                }
                poderes.add(new Poder.Ativo(tipo));
                return;
            }
        }
    }

    private boolean resortearComida() {
        Celula livre = Comida.sortearLivre(campo, aleatorio, celulasOcupadas());
        if (livre == null) {
            return false;
        }
        comida.moverPara(livre.getX(), livre.getY());
        return true;
    }

    /** Células ocupadas pela cobra e pelos poderes no chão. */
    private Set<Long> celulasOcupadas() {
        Set<Long> ocupadas = new HashSet<>();
        for (Celula c : cobra.segmentos()) {
            ocupadas.add(c.chave());
        }
        for (PoderNoChao p : noChao) {
            ocupadas.add(p.celula.chave());
        }
        return ocupadas;
    }

    // ------------------------------------------------------------------
    // Controle
    // ------------------------------------------------------------------

    /**
     * Pede uma direção. Fora de jogo a cobra não vira.
     *
     * @param d direção desejada
     * @return {@code true} se a direção foi aceita
     */
    public boolean virar(Direcao d) {
        if (estado != Estado.JOGANDO) {
            return false;
        }
        return cobra.enfileirar(d);
    }

    /** Começa ou retoma a partida. */
    public void iniciar() {
        if (estado == Estado.PAUSADO) {
            estado = Estado.JOGANDO;
        }
    }

    /**
     * A partida ainda não começou: é o estado em que o menu aparece.
     *
     * <p>Existe como método porque duas coisas precisam da mesma resposta — o
     * desenho decide entre menu e tabuleiro, e a janela decide se as teclas de
     * escolha valem alguma coisa. Se cada uma escrever a condição, basta uma
     * delas ser esquecida num dia para o menu reaparecer em cima de uma
     * partida em andamento, ou para as teclas reiniciarem o jogo no meio da
     * corrida.</p>
     */
    public boolean noInicio() {
        return estado == Estado.PAUSADO && pontos == 0 && passos == 0;
    }

    /** Pausa a partida, se ela estiver correndo. */
    public void pausar() {
        if (estado == Estado.JOGANDO) {
            estado = Estado.PAUSADO;
        }
    }

    /** Volta ao estado de pausa, como o botão de recomeçar. */
    public void voltarAoBriefing() {
        estado = Estado.PAUSADO;
    }

    /**
     * Zera a partida e volta ao briefing.
     *
     * <p>A cobra é recriada e a fila é lavada junto, senão as teclas pressionadas
     * no fim da partida anterior vazariam para a próxima e a cobra inverteria
     * logo no primeiro passo.
     */
    public void reiniciar() {
        this.cobra = new Cobra(campo, 3);
        this.poderes.clear();
        this.noChao.clear();
        this.pontos = 0;
        this.nivel = 1;
        this.comidas = 0;
        this.maiorComprimento = 0;
        this.passos = 0;
        this.estado = Estado.PAUSADO;
        // o motivo da derrota anterior também vaza se não for limpo: a partida
        // nova morreria na parede e a interface tocaria o som da colisão no corpo
        this.motivo = null;
        colocarComidaLivre();
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public Campo getCampo() {
        return campo;
    }

    public Cobra getCobra() {
        return cobra;
    }

    public Comida getComida() {
        return comida;
    }

    public Dificuldade getDificuldade() {
        return dificuldade;
    }

    /**
     * Por que a partida acabou em derrota, ou {@code null} se ainda não acabou.
     *
     * @return o motivo da derrota
     */
    public Motivo getMotivo() {
        return motivo;
    }

    /** Estado da partida. */
    public Estado getEstado() {
        return estado;
    }

    public int getPontos() {
        return pontos;
    }

    public int getNivel() {
        return nivel;
    }

    public int getComidas() {
        return comidas;
    }

    public int getMaiorComprimento() {
        return maiorComprimento;
    }

    public int getPassos() {
        return passos;
    }

    public int getRecorde() {
        return recorde;
    }

    /**
     * Verifica se há um efeito ativo.
     *
     * @param tipo o poder procurado
     * @return {@code true} se estiver ativo agora
     */
    public boolean temPoder(Poder tipo) {
        for (Poder.Ativo a : poderes) {
            if (a.getTipo() == tipo) {
                return true;
            }
        }
        return false;
    }

    /**
     * Segundos restantes de um poder.
     *
     * @param tipo o poder procurado
     * @return o tempo que falta, ou 0 se não estiver ativo
     */
    public double restanteDe(Poder tipo) {
        for (Poder.Ativo a : poderes) {
            if (a.getTipo() == tipo) {
                return a.getRestante();
            }
        }
        return 0;
    }

    /** Os efeitos ativos, para o HUD desenhar os cartões. */
    public List<Poder.Ativo> poderesAtivos() {
        return new ArrayList<>(poderes);
    }

    /** Os poderes esperando no chão, para o desenho. */
    public List<PoderNoChao> poderesNoChao() {
        return new ArrayList<>(noChao);
    }

    /** Milissegundos entre passos, dado o nível atual. */
    public int intervaloMs() {
        return Math.max(60,
                dificuldade.intervaloInicial() - dificuldade.reducaoPorNivel() * (nivel - 1));
    }

    // ------------------------------------------------------------------
    // Ganchos de teste
    // ------------------------------------------------------------------

    /**
     * Substitui a cobra, para os testes montarem situações exatas.
     *
     * <p>Só existe para os testes. Montar a situação com o método público
     * significaria rezar para o sorteio, ou expor um método que o jogo
     * usaria por engano.
     *
     * @param nova a cobra a usar a partir de agora
     */
    void definirCobra(Cobra nova) {
        this.cobra = nova;
    }

    /**
     * Coloca a comida numa célula exata, para os testes.
     *
     * @param x coluna
     * @param y linha
     */
    void definirComida(int x, int y) {
        this.comida.moverPara(x, y);
    }

    /**
     * Coloca um poder no chão, para os testes.
     *
     * @param tipo   o poder
     * @param x      coluna
     * @param y      linha
     */
    void definirPoderNoChao(Poder tipo, int x, int y) {
        this.noChao.add(new PoderNoChao(tipo, new Celula(x, y)));
    }

    /** Um poder esperando no chão: tipo e posição. */
    public static final class PoderNoChao {

        private final Poder tipo;
        private final Celula celula;

        PoderNoChao(Poder tipo, Celula celula) {
            this.tipo = tipo;
            this.celula = celula;
        }

        public Poder getTipo() {
            return tipo;
        }

        public Celula getCelula() {
            return celula;
        }
    }
}
