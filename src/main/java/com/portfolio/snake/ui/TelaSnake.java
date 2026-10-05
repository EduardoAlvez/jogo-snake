package com.portfolio.snake.ui;

import com.portfolio.snake.audio.Sons;
import com.portfolio.snake.audio.Sons.Efeito;
import com.portfolio.snake.audio.Trilha;
import com.portfolio.snake.core.Campo;
import com.portfolio.snake.core.Direcao;
import com.portfolio.snake.core.JogoSnake;
import com.portfolio.snake.core.RegistroDeRecordes;
import com.portfolio.snake.skin.CatalogoSkins;
import com.portfolio.snake.skin.RegistroDeSkin;
import com.portfolio.snake.skin.Skin;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * A janela do jogo.
 *
 * <p>Aqui não há regra nenhuma: tudo que decide alguma coisa está em
 * {@link JogoSnake}. Esta classe só entrega as teclas, cuida do relógio e
 * repassa o desenho para {@link DesenhoJogo}, que não sabe nada de janela.
 * É o que permite testar as regras e até a imagem formada sem abrir nada: as
 * posições vivem em {@link LayoutSnake} e os pixels, em {@link DesenhoJogo},
 * e as duas cobrem um {@code Graphics2D} de um {@code BufferedImage} igual à
 * janela.
 *
 * <p>O laço é um {@link Timer} de Swing, com o período de
 * {@link JogoSnake#intervaloMs()}. O tempo que passa entre dois tiques é medido
 * em vez de assumido igual ao intervalo: se a máquina engasgar, a cobra não
 * pula duas casas, ela apenas demora um pouco mais a se mover. Sem isso, uma
 * pausa longa do sistema vira um teleporte através do próprio corpo.
 */
public final class TelaSnake extends JFrame {

    private static final long serialVersionUID = 1L;

    /** As resoluções do ícone, da menor para a maior. */
    private static final int[] TAMANHOS_ICONE = {16, 24, 32, 48, 64, 128, 256};

    private final transient LayoutSnake layout;
    /**
     * O desenho. Fica fora da janela para que um teste sem display consiga
     * pintar a cena num {@code BufferedImage} e conferir os pixels.
     */
    private transient DesenhoJogo desenho;
    /**
     * A skin em uso. Muda quando o jogador aperta [N], e só por isso: o pintor é
     * reconstruído junto, em vez de ganhar um setter. Um pintor com estado
     * mutável é o tipo de coisa que faz a tela mostrar uma skin e o teste
     * conferir outra.
     */
    private transient Skin skin;
    /**
     * A partida em uso. Só é trocada no briefing e no recomeçar; durante o
     * jogo a referência não muda, e é por isso que o laço pode ler o intervalo
     * dela a cada tique sem medo de corrida.
     */
    private transient JogoSnake jogo;

    private transient Timer timer;
    private long ultimoPasso;

    /**
     * Se a última partida batou o recorde.
     *
     * <p>Calculado uma vez, quando a partida termina, e só então lido pelo
     * desenho. Se o desenho decidisse isso sozinho, ele gravaria o arquivo a
     * cada repintura: a primeira vez gravaria e o selo apareceria, e da segunda
     * em diante leria o número de volta, veria que já não era maior e apagaria
     * o próprio selo. Um teste que olhasse a cor do pixel veria o selo
     * aparecer e desaparecer conforme o quanto a janela repintou.
     */
    private transient boolean recordeBatido;

    /**
     * Cria a janela com o layout padrão.
     *
     * @param jogo a partida a mostrar
     */
    public TelaSnake(JogoSnake jogo) {
        this(jogo, new LayoutSnake());
    }

    /**
     * Cria a janela com um layout dado.
     *
     * @param jogo    a partida a mostrar
     * @param layout  onde cada coisa é desenhada
     */
    public TelaSnake(JogoSnake jogo, LayoutSnake layout) {
        super("Jogo Snake");
        this.jogo = jogo;
        this.layout = layout;
        this.skin = RegistroDeSkin.carregar();
        this.desenho = new DesenhoJogo(layout, skin);
        this.recordeBatido = false;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setPreferredSize(new Dimension(layout.getLarguraJanela(), layout.getAlturaJanela()));
        setBackground(DesenhoJogo.FUNDO);
        setFocusable(true);
        setContentPane(new Painel());

        // O ícone em 7 resoluções, uma por contexto do Windows. Com setIconImage
        // (uma só), o SO escala a imagem e o ícone fica borrado na barra de
        // tarefas; com setIconImages ele escolhe a resolução certa para cada uso.
        // Os arquivos são gerados por tools/DesenharLogoSnake.java e
        // tools/GerarLogo.java, e verificados por LogoTest.
        try {
            List<java.awt.Image> icones = new ArrayList<>();
            for (int lado : TAMANHOS_ICONE) {
                icones.add(java.awt.Toolkit.getDefaultToolkit().getImage(
                        TelaSnake.class.getResource("/logo-" + lado + ".png")));
            }
            setIconImages(icones);
        } catch (RuntimeException e) {
            // recurso ausente ou ambiente sem toolkit: a janela abre igual
        }

        pack();
        setLocationRelativeTo(null);
    }

    /** O painel que sabe se pintar. Existe separado para o teste poder criar sem janela. */
    private final class Painel extends javax.swing.JPanel {

        private static final long serialVersionUID = 1L;

        Painel() {
            setBackground(DesenhoJogo.FUNDO);
            setPreferredSize(new Dimension(layout.getLarguraJanela(), layout.getAlturaJanela()));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                desenho.pintar(g2, jogo, recordeBatido);
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------
    // Tempo e teclado
    // ------------------------------------------------------------------

    /** Começa o laço do jogo, com o período que o núcleo pediu. */
    public void iniciarRelogio() {
        // carrega os sons uma vez, aqui na EDT. São 102 KB e quatro cabeçalhos:
        // trabalho de milissegundos. Uma versão anterior fazia isto numa thread
        // daemon para "não travar a janela", mas o que trava a EDT é o
        // getClip()+open() de cada toque, que continua no mesmo lugar.
        Sons.carregarTodos();
        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(jogo.intervaloMs(), e -> {
            // o retrato e tirado ANTES do passo, para saber o que mudou depois.
            // E lido aqui, no laco do relogio, e nunca dentro de paintComponent:
            // um som disparado no desenho tocaria uma vez por repintura.
            Trilha.Retrato antes = Trilha.tira(jogo);
            long agora = System.nanoTime();
            double segundos = (agora - ultimoPasso) / 1_000_000_000.0;
            ultimoPasso = agora;
            // um tique perdido de tempo não vira teleporte: limita-se o passo
            if (segundos > jogo.intervaloMs() / 1000.0 * 3) {
                segundos = jogo.intervaloMs() / 1000.0 * 3;
            }
            if (segundos < 0) {
                segundos = 0;
            }
            jogo.passo(segundos);
            anunciarSons(antes);
            conferirFimDaPartida();
            repaint();
            // a dificuldade muda o intervalo, então o Timer precisa acompanhá-la
            if (timer.getDelay() != jogo.intervaloMs()) {
                timer.setDelay(jogo.intervaloMs());
            }
        });
        ultimoPasso = System.nanoTime();
        timer.start();
    }

    /**
     * Toca os sons que o passo produziu.
     *
     * <p>A regra do que é evento mora em {@link Trilha}, fora da janela, onde
     * tem teste. Aqui fica só a entrega, que é o que a janela tem de fazer.
     *
     * @param antes o retrato de antes do passo
     */
    private void anunciarSons(Trilha.Retrato antes) {
        for (Efeito efeito : Trilha.dePara(antes, Trilha.tira(jogo))) {
            Sons.tocar(efeito);
        }
    }

    /**
     * Grava o recorde no instante em que a partida termina, e só uma vez.
     *
     * <p>É o único lugar do programa que decide se houve recorde. O desenho
     * recebe o resultado pronto. Se a decisão ficasse no desenho, cada
     * repintura reavaliaria o mesmo número: a primeira gravaria, e as seguintes
     * veriam que ele já não era maior e esconderiam o selo.
     */
    private void conferirFimDaPartida() {
        if (recordeBatido) {
            return;
        }
        JogoSnake.Estado estado = jogo.getEstado();
        if (estado != JogoSnake.Estado.FIM && estado != JogoSnake.Estado.VITORIA) {
            return;
        }
        recordeBatido = jogo.getPontos() > 0 && RegistroDeRecordes.registrar(jogo.getPontos());
    }

    /** Para o laço do jogo. */
    public void pararRelogio() {
        if (timer != null) {
            timer.stop();
        }
    }

    /**
     * Traduz uma tecla em ação.
     *
     * <p>Separado do tratamento de evento porque assim dá para testar o teclado
     * inteiro sem abrir janela: o que interessa não é a tecla, é a regra de que
     * não dá para inverter a cobra, e essa regra já é do núcleo.
     *
     * @param codigo o {@code getKeyCode} da tecla
     * @return {@code true} se a tecla foi tratada
     */
    public boolean tratarTecla(int codigo) {
        switch (codigo) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                return jogo.virar(Direcao.CIMA) || jogo.getCobra().getDirecao() == Direcao.CIMA;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                return jogo.virar(Direcao.BAIXO) || jogo.getCobra().getDirecao() == Direcao.BAIXO;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                return jogo.virar(Direcao.ESQUERDA) || jogo.getCobra().getDirecao() == Direcao.ESQUERDA;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                return jogo.virar(Direcao.DIREITA) || jogo.getCobra().getDirecao() == Direcao.DIREITA;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_P:
                alternarPausa();
                return true;
            case KeyEvent.VK_N:
                trocarSkin();
                return true;
            case KeyEvent.VK_R:
                jogo.reiniciar();
                recordeBatido = false;
                repaint();
                return true;
            case KeyEvent.VK_1:
            case KeyEvent.VK_2:
            case KeyEvent.VK_3:
            case KeyEvent.VK_4:
            case KeyEvent.VK_5:
                aplicarEscolha(codigo);
                return true;
            case KeyEvent.VK_ENTER:
                comecarPartida();
                return true;
            case KeyEvent.VK_ESCAPE:
                pararRelogio();
                dispose();
                System.exit(0);
                return true;
            default:
                return false;
        }
    }

    /**
     * Passa para a próxima skin, sem mexer na partida.
     *
     * <p>Sem reiniciar de propósito: trocar de pele é uma decisão de aparência e
     * custar a partida por causa disso seria absurdo. Nada aqui toca no
     * {@code JogoSnake}, então pontuação, tamanho e velocidade ficam como
     * estavam.
     */
    private void trocarSkin() {
        skin = CatalogoSkins.seguinte(skin);
        desenho = new DesenhoJogo(layout, skin);
        RegistroDeSkin.salvar(skin);
        repaint();
    }

    /** A skin em uso, para o teste e para quem quiser mostrar. */
    public Skin getSkin() {
        return skin;
    }

    private void alternarPausa() {
        if (jogo.getEstado() == JogoSnake.Estado.JOGANDO) {
            jogo.pausar();
        } else if (jogo.getEstado() == JogoSnake.Estado.PAUSADO
                && (jogo.getPontos() > 0 || jogo.getPassos() > 0)) {
            jogo.iniciar();
        }
        repaint();
    }

    /** Aplica a escolha de briefing e começa a partida. */
    public void aplicarEscolha(int codigo) {
        Campo.Borda borda = codigo == KeyEvent.VK_2 ? Campo.Borda.WRAP : Campo.Borda.MORRE;
        JogoSnake.Dificuldade dif = codigo == KeyEvent.VK_3 ? JogoSnake.Dificuldade.FACIL
                : codigo == KeyEvent.VK_5 ? JogoSnake.Dificuldade.DIFICIL
                        : JogoSnake.Dificuldade.MEDIO;
        reiniciarCom(borda, dif);
        repaint();
    }

    /** Começa a partida como ela foi montada. */
    public void comecarPartida() {
        jogo.iniciar();
        iniciarRelogio();
        repaint();
    }

    /** Troca a regra da borda e a dificuldade, recomeçando. */
    private void reiniciarCom(Campo.Borda borda, JogoSnake.Dificuldade dif) {
        Campo c = new Campo(layout.getColunas(), layout.getLinhas(), borda);
        this.jogo = new JogoSnake(c, dif, System.nanoTime());
        this.recordeBatido = false;
        comecarPartida();
    }

    // ------------------------------------------------------------------
    // Arranque
    // ------------------------------------------------------------------

    /**
     * Instala as teclas do jogo.
     *
     * <p>Usa {@code WHEN_IN_FOCUSED_WINDOW} em vez de um {@code KeyListener} na
     * janela. Com listener, as teclas só chegam se o componente certain estiver
     * com o foco, e um clique em qualquer coisa — um cartão, o painel, a própria
     * janela — tira o foco e <b>o jogo deixa de responder ao teclado sem
     * explicação nenhuma</b>. É o defeito mais comum em jogo Swing, e não
     * aparece em nenhum teste de regra: a regra está certa, quem não chega é a
     * tecla. Com {@code WHEN_IN_FOCUSED_WINDOW} a tecla chega com a janela
     * ativa, veio de onde vier o clique.
     */
    public void registrarTeclado() {
        javax.swing.InputMap im = getRootPane().getInputMap(
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap am = getRootPane().getActionMap();

        int[] teclas = {
            KeyEvent.VK_UP, KeyEvent.VK_W,
            KeyEvent.VK_DOWN, KeyEvent.VK_S,
            KeyEvent.VK_LEFT, KeyEvent.VK_A,
            KeyEvent.VK_RIGHT, KeyEvent.VK_D,
            KeyEvent.VK_SPACE, KeyEvent.VK_P,
            KeyEvent.VK_R,
            KeyEvent.VK_N,
            KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5,
            KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE,
        };
        for (int codigo : teclas) {
            final int tecla = codigo;
            String nome = "tecla-" + tecla;
            im.put(javax.swing.KeyStroke.getKeyStroke(tecla, 0), nome);
            am.put(nome, new javax.swing.AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    tratarTecla(tecla);
                }
            });
        }
        getRootPane().setFocusable(true);
    }

    /**
     * Abre o jogo.
     *
     * @param args ignorado
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LayoutSnake layout = new LayoutSnake();
            Campo campo = new Campo(layout.getColunas(), layout.getLinhas(),
                    Campo.Borda.MORRE);
            JogoSnake jogo = new JogoSnake(campo, JogoSnake.Dificuldade.MEDIO,
                    System.nanoTime());
            TelaSnake tela = new TelaSnake(jogo, layout);
            tela.registrarTeclado();
            tela.setVisible(true);
            tela.iniciarRelogio();
        });
    }
}
