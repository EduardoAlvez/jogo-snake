package com.portfolio.snake.core;

import static com.portfolio.snake.core.JogoSnake.Dificuldade.DIFICIL;
import static com.portfolio.snake.core.JogoSnake.Dificuldade.FACIL;
import static com.portfolio.snake.core.JogoSnake.Dificuldade.MEDIO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * As regras da partida, e principalmente a <b>ordem</b> dos passos.
 *
 * <p>Nenhum teste aqui abre janela: o núcleo não conhece Swing. A semente do
 * {@code Random} é fixada em todos os casos, para o sorteio da comida não virar
 * factoria de teste instável.
 */
public class JogoSnakeTest {

    private static final long SEMENTE = 42L;

    private static Campo morre(int lado) {
        return new Campo(lado, lado, Campo.Borda.MORRE);
    }

    private static Campo wrap(int lado) {
        return new Campo(lado, lado, Campo.Borda.WRAP);
    }

/** Jogo pronto para comer, com a cobra deitada para a direita. */
    private static JogoSnake jogo(Campo campo) {
        JogoSnake j = new JogoSnake(campo, MEDIO, SEMENTE);
        j.iniciar();
        return j;
    }

    /**
     * Jogo na dificuldade pedida.
     *
     * <p>Existe porque o helper {@code jogo()} fixa a média: sem este, os
     * números de {@code FACIL} e {@code DIFICIL} nunca eram lidos por teste
     * nenhum, e trocar dois deles de lugar não derrubaria nada.
     */
    private static JogoSnake em(JogoSnake.Dificuldade d) {
        return em(d, morre(80));
    }

    private static JogoSnake em(JogoSnake.Dificuldade d, Campo campo) {
        JogoSnake j = new JogoSnake(campo, d, SEMENTE);
        j.iniciar();
        return j;
    }

    /** Jogo com o nível no teto da dificuldade, alcançado comendo de verdade. */
    private static JogoSnake noNivelMaximoDe(JogoSnake.Dificuldade d) {
        JogoSnake j = em(d);
        comer(j, d.nivelMaximo() + 5);
        assertEquals("o helper só serve se o teto foi mesmo alcançado",
                d.nivelMaximo(), j.getNivel());
        return j;
    }

    /** Quanto o intervalo cai ao subir exatamente um nível. */
    private static int quedaPorNivel(JogoSnake.Dificuldade d) {
        JogoSnake j = em(d);
        int antes = j.intervaloMs();
        comer(j, 1);
        return antes - j.intervaloMs();
    }

    /**
     * Come {@code vezes} vezes seguidas, sempre colocando a comida na célula da
     * frente. O food é colocado pelo caminho da direção <b>atual</b>, e não
     * assumindo "direita": assim o helper continua valendo se a cobra virar.
     */
    private static void comer(JogoSnake j, int vezes) {
        for (int i = 0; i < vezes; i++) {
            Direcao d = j.getCobra().getDirecao();
            Celula cabeca = j.getCobra().cabeca();
            j.definirComida(cabeca.getX() + d.dx(), cabeca.getY() + d.dy());
            j.passo(0.15);
        }
    }

    /**
     * Monta a cobra deitada numa linha, com a cabeça onde o teste quer.
     *
     * <p>O caminho é escrito à mão em vez de chamar {@code definirCobra} com
     * coordenadas soltas, porque montar o corpo inteiro é a parte que o teste
     * precisa controlar para as colisões serem o que ele pensa que são.
     */
    private static Cobra deitada(Campo campo, int cabecaX, int cabecaY, int comprimento) {
        return new Reta(campo, cabecaX, cabecaY, comprimento).cobra;
    }

    /** Auxiliar: monta uma cobra horizontal a partir da cabeça para trás. */
    private static final class Reta {
        private final Cobra cobra;

        Reta(Campo campo, int cabecaX, int cabecaY, int comprimento) {
            this.cobra = new Cobra(campo, comprimento);
            // reconstrói o corpo na linha desejada
            while (cobra.tamanho() < comprimento) {
                cobra.avancar(false);
            }
            // caminha até a cabeça chegar onde o teste quer
            int passos = Math.abs(cabecaX - cobra.cabeca().getX());
            Direcao d = cabecaX >= cobra.cabeca().getX() ? Direcao.DIREITA : Direcao.ESQUERDA;
            for (int i = 0; i < passos; i++) {
                cobra.limparFila();
                cobra.moverPara(d, false);
            }
            if (cobra.cabeca().getY() != cabecaY) {
                throw new IllegalArgumentException("o caminho horizontal não chega em y=" + cabecaY);
            }
        }
    }

    // ------------------------------------------------------------------
    // Começar
    // ------------------------------------------------------------------

    @Test
    public void oJogoComecaPausado() {
        JogoSnake j = new JogoSnake(morre(20), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        assertEquals(JogoSnake.Estado.PAUSADO, j.getEstado());
    }

    @Test
    public void umPassoNaoFazNadaComOJogoPausado() {
        JogoSnake j = new JogoSnake(morre(20), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        Celula antes = j.getCobra().cabeca();
        j.passo(0.15);
        assertEquals(antes, j.getCobra().cabeca());
        assertEquals(0, j.getPassos());
    }

    @Test
    public void iniciarTiraOPausa() {
        JogoSnake j = new JogoSnake(morre(20), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        j.iniciar();
        assertEquals(JogoSnake.Estado.JOGANDO, j.getEstado());
    }

    @Test
    public void aComidaNasceForaDoCorpo() {
        JogoSnake j = new JogoSnake(morre(20), JogoSnake.Dificuldade.MEDIO, SEMENTE);
        assertFalse(j.getCobra().cabecaEm(j.getComida().celula()));
        for (Celula s : j.getCobra().segmentos()) {
            assertNotEquals(j.getComida().celula(), s);
        }
    }

    // ------------------------------------------------------------------
    // Comer
    // ------------------------------------------------------------------

    @Test
    public void comerDezPontosEEsticaACobra() {
        JogoSnake j = jogo(morre(20));
        j.definirComida(j.getCobra().cabeca().getX() + 1, j.getCobra().cabeca().getY());
        int tamanhoAntes = j.getCobra().tamanho();

        j.passo(0.15);

        assertEquals(10, j.getPontos());
        assertEquals(tamanhoAntes + 1, j.getCobra().tamanho());
        assertEquals(1, j.getComidas());
    }

    @Test
    public void aComidaSomeDaCelulaOndeFoiComida() {
        JogoSnake j = jogo(morre(20));
        int x = j.getCobra().cabeca().getX() + 1;
        int y = j.getCobra().cabeca().getY();
        j.definirComida(x, y);

        j.passo(0.15);

        assertNotEquals(new Celula(x, y), j.getComida().celula());
    }

    @Test
    public void aNovaComidaNuncaNasceSobreACobra() {
        JogoSnake j = jogo(morre(20));
        for (int i = 0; i < 60; i++) {
            // mantém a comida sempre à frente, para comer 60 vezes seguidas
            Celula cabeca = j.getCobra().cabeca();
            j.definirComida(cabeca.getX() + 1, cabeca.getY());
            if (j.getCobra().cabeca().getX() >= j.getCampo().getLargura() - 2) {
                j.virar(Direcao.BAIXO);
                j.passo(0.15);
                j.virar(Direcao.DIREITA);
                j.passo(0.15);
                continue;
            }
            j.passo(0.15);
            assertFalse("comida nasceu sobre a cobra no passo " + i,
                    j.getCobra().cabecaEm(j.getComida().celula()));
        }
    }

    @Test
    public void comerSobeONivelEEncurtaOIntervalo() {
        JogoSnake j = jogo(morre(20));
        int intervaloAntes = j.intervaloMs();
        Celula cabeca = j.getCobra().cabeca();
        j.definirComida(cabeca.getX() + 1, cabeca.getY());

        j.passo(0.15);

        assertEquals(2, j.getNivel());
        assertTrue(j.intervaloMs() < intervaloAntes);
    }

    @Test
    public void oNivelTemTeto() {
        JogoSnake j = jogo(morre(20));
        for (int i = 0; i < 60; i++) {
            Celula cabeca = j.getCobra().cabeca();
            j.definirComida(cabeca.getX() + 1, cabeca.getY());
            j.passo(0.15);
        }
        assertTrue(j.getNivel() <= JogoSnake.Dificuldade.MEDIO.nivelMaximo());
        assertTrue(j.intervaloMs() >= 60);
    }

    // ------------------------------------------------------------------
    // intervaloMs: as três dificuldades, e não só a média
    // ------------------------------------------------------------------

    /**
     * Antes estes valores só eram exercitados em {@code MEDIO}, porque o helper
     * {@code jogo()} fixa a dificuldade. Com a média coberta, uma troca
     * acidental dos números de {@code FACIL} e {@code DIFICIL} passava batida —
     * e a troca giveaway é justamente a que o jogador mais sente, porque é a
     * diferença entre o jogo parecer rápido e parecer impossível.
     */
    @Test
    public void oIntervaloInicialCaiComADificuldade() {
        assertEquals(170, em(FACIL).intervaloMs());
        assertEquals(150, em(MEDIO).intervaloMs());
        assertEquals(130, em(DIFICIL).intervaloMs());
        assertTrue(em(FACIL).intervaloMs() > em(MEDIO).intervaloMs());
        assertTrue(em(MEDIO).intervaloMs() > em(DIFICIL).intervaloMs());
    }

    @Test
    public void cadaNivelReduzOIntervaloPeloValorDaDificuldade() {
        assertEquals(4, quedaPorNivel(FACIL));
        assertEquals(5, quedaPorNivel(MEDIO));
        assertEquals(7, quedaPorNivel(DIFICIL));
    }

    /**
     * O piso de 60 ms é o que impede o intervalo de chegar a zero (ou negativo)
     * no nível alto. Em {@code DIFICIL} a conta sem piso dá {@code 130 - 7*19 = -3}:
     * sem o {@code Math.max} a cobra andaria com {@code dt} negativo.
     */
    @Test
    public void oIntervaloTemPisoDe60ms() {
        // FACIL no teto: 170 - 4*19 = 94, ainda acima do piso
        assertEquals(94, noNivelMaximoDe(FACIL).intervaloMs());
        // MEDIO e DIFICIL no teto caem abaixo de 60 e são aparados pelo piso
        assertEquals(60, noNivelMaximoDe(MEDIO).intervaloMs());
        assertEquals(60, noNivelMaximoDe(DIFICIL).intervaloMs());
    }

    @Test
    public void oNivelParaNoTeto() {
        JogoSnake j = jogo(morre(60));
        comer(j, JogoSnake.Dificuldade.MEDIO.nivelMaximo() + 10);
        assertEquals("o nível tem que parar no teto, senão o intervalo continua caindo",
                JogoSnake.Dificuldade.MEDIO.nivelMaximo(), j.getNivel());
    }

    // ------------------------------------------------------------------
    // A parede: os dois modos
    // ------------------------------------------------------------------

    @Test
    public void aParedeMataQuandoABordaE_morre() {
        JogoSnake j = jogo(morre(6));
        // leva a cabeça para a última coluna, ainda dentro do campo
        while (j.getCobra().cabeca().getX() < 5) {
            j.passo(0.15);
        }
        assertEquals(5, j.getCobra().cabeca().getX());

        j.passo(0.15);

        assertEquals(JogoSnake.Estado.FIM, j.getEstado());
    }

    @Test
    public void aParedeMata_e_aCabecaFicaDentroDoCampo() {
        JogoSnake j = jogo(morre(6));
        while (j.getCobra().cabeca().getX() < 5) {
            j.passo(0.15);
        }
        j.passo(0.15);
        // morreu antes de se mover: a cabeça não pode ter vazido para x=6
        assertTrue(j.getCampo().dentro(j.getCobra().cabeca().getX(),
                j.getCobra().cabeca().getY()));
    }

    @Test
    public void noWrapACobraDaAVoltaNaParede() {
        JogoSnake j = jogo(wrap(6));
        while (j.getCobra().cabeca().getX() < 5) {
            j.passo(0.15);
        }
        assertEquals(5, j.getCobra().cabeca().getX());

        j.passo(0.15);

        assertEquals(JogoSnake.Estado.JOGANDO, j.getEstado());
        assertEquals(0, j.getCobra().cabeca().getX());
    }

    @Test
    public void noWrapOCorpoTambemDaAVolta() {
        JogoSnake j = jogo(wrap(6));
        while (j.getCobra().cabeca().getX() < 4) {
            j.passo(0.15);
        }
        j.passo(0.15);
        for (Celula s : j.getCobra().segmentos()) {
            assertTrue("segmento " + s + " fora do campo",
                    j.getCampo().dentro(s.getX(), s.getY()));
        }
    }

    @Test
    public void noWrapAComidaNaParedeOpostaAindaEComida() {
        // a cabeça vai para a coluna 0 pelo wrap e come a comida lá
        JogoSnake j = jogo(wrap(6));
        while (j.getCobra().cabeca().getX() < 5) {
            j.passo(0.15);
        }
        j.definirComida(0, j.getCobra().cabeca().getY());

        j.passo(0.15);

        assertEquals(10, j.getPontos());
    }

    // ------------------------------------------------------------------
    // Colisão com o próprio corpo
    // ------------------------------------------------------------------

    @Test
    public void baterNoCorpoTerminaAPartida() {
        JogoSnake j = jogo(morre(20));
        // quadrado de 2x2 com comprimento 5: a cabeça entra no corpo
        //
        // O comprimento é 5 e não 4 de propósito. Com 4 segmentos o quadrado de
        // 2x2 fecha sem nunca se tocar — é o caso legal, coberto pelo teste
        // aCabecaPodeEntrarNaCelulaQueACaudaLiberou. Com 5, a cabeça fecha o
        // quadrado em cima de um segmento que ainda não saiu, e aí é morte.
        j.definirCobra(new Cobra(morre(20), 5));
        // tira a comida do caminho: se a cobra comer por acaso o corpo cresce,
        // a cauda não sai e a colisão acontece em outro lugar — o teste
        // passaria ou falharia por causa do sorteio, e não da regra
        j.definirComida(0, 0);
        for (int i = 0; i < 3; i++) {
            j.passo(0.15);
        }
        j.virar(Direcao.BAIXO);
        j.passo(0.15);
        j.virar(Direcao.ESQUERDA);
        j.passo(0.15);
        j.virar(Direcao.CIMA);
        j.passo(0.15);

        assertEquals(JogoSnake.Estado.FIM, j.getEstado());
    }

    @Test
    public void aCabecaPodeEntrarNaCelulaQueACaudaLiberou() {
        // é a continuação do teste de cauda do CobraTest, agora pela porta da
        // entrada do JogoSnake: quadrado de 2x2, comprimento 4
        JogoSnake j = jogo(morre(20));
        j.definirCobra(new Cobra(morre(20), 4));
        j.definirComida(0, 0);
        for (int i = 0; i < 3; i++) {
            j.passo(0.15);
        }

        j.virar(Direcao.BAIXO);
        j.passo(0.15);
        j.virar(Direcao.ESQUERDA);
        j.passo(0.15);
        j.virar(Direcao.CIMA);
        j.passo(0.15);

        // a cabeça entrou na célula que a cauda liberou: continua jogando
        assertEquals(JogoSnake.Estado.JOGANDO, j.getEstado());
    }

    @Test
    public void aposMorrerVirarNaoFazNada() {
        JogoSnake j = jogo(morre(6));
        while (j.getCobra().cabeca().getX() < 5) {
            j.passo(0.15);
        }
        j.passo(0.15);
        assertEquals(JogoSnake.Estado.FIM, j.getEstado());

        assertFalse(j.virar(Direcao.CIMA));
    }

    // ------------------------------------------------------------------
    // Campo lotado: o terceiro bug clássico
    // ------------------------------------------------------------------

    /**
     * Monta uma cobra andando por um caminho explícito, comendo em cada passo.
     *
     * <p>Só a API pública é usada, e é por isso que o caminho não pode passar
     * duas vezes pela mesma célula: a cobra só cresce aqui, então nenhuma célula
     * é liberada, e repetir uma seria um corpo com duas cabeças na mesma casa.
     *
     * @param campo  o tabuleiro
     * @param caminho células a visitar, em ordem, a partir da posição inicial
     * @return a cobra resultante
     */
    private static Cobra serpente(Campo campo, int[][] caminho) {
        Cobra c = new Cobra(campo, 2);
        int x = c.cabeca().getX();
        int y = c.cabeca().getY();
        for (int[] passo : caminho) {
            Direcao d = direcaoPara(x, y, passo[0], passo[1]);
            c.limparFila();
            c.moverPara(d, true);
            x = passo[0];
            y = passo[1];
        }
        return c;
    }

    private static Direcao direcaoPara(int deX, int deY, int paraX, int paraY) {
        if (paraX > deX) {
            return Direcao.DIREITA;
        }
        if (paraX < deX) {
            return Direcao.ESQUERDA;
        }
        if (paraY > deY) {
            return Direcao.BAIXO;
        }
        return Direcao.CIMA;
    }

    @Test(timeout = 2000)
    public void campoLotadoDáVitoriaEmVezDeTravar() {
        // campo 3x2 = 6 células, e a cobra é formada para cobrir as 6
        Campo pequeno = new Campo(3, 2, Campo.Borda.MORRE);
        JogoSnake j = jogo(pequeno);
        // a cabeça nasce em (1,1) com o corpo em (0,1); o caminho passa pelas
        // 4 células que faltam sem repetir nenhuma
        j.definirCobra(serpente(pequeno, new int[][] {
            {2, 1}, {2, 0}, {1, 0}, {0, 0},
        }));
        assertEquals(pequeno.totalCelulas(), j.getCobra().tamanho());

        j.passo(0.15);

        assertEquals(JogoSnake.Estado.VITORIA, j.getEstado());
    }

    @Test
    public void aVitóriaVemAntesDaColisão() {
        // com a grade lotada não há célula livre para a cabeça ocupar: se a
        // colisão fosse checada antes, o jogador perderia no exato instante em
        // que ganhou
        Campo pequeno = new Campo(3, 2, Campo.Borda.MORRE);
        JogoSnake j = jogo(pequeno);
        j.definirCobra(serpente(pequeno, new int[][] {
            {2, 1}, {2, 0}, {1, 0}, {0, 0},
        }));

        j.passo(0.15);

        assertEquals(JogoSnake.Estado.VITORIA, j.getEstado());
    }

    @Test(timeout = 2000)
    public void sortearComidaEmCampoLotadoNaoTrava() {
        // a própria função de sorteio, com todas as células ocupadas
        Campo pequeno = morre(2);
        java.util.Set<Long> todas = new java.util.HashSet<>();
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                todas.add(new Celula(x, y).chave());
            }
        }
        assertEquals(null,
                Comida.sortearLivre(pequeno, new java.util.Random(SEMENTE), todas));
    }

    // ------------------------------------------------------------------
    // Poderes
    // ------------------------------------------------------------------

    @Test
    public void oPoderDePontosDobraOAproximacao() {
        JogoSnake j = jogo(morre(20));
        assertEquals(1, j.multiplicador());
        // a cobra passa por cima do poder
        Celula cabeca = j.getCobra().cabeca();
        j.definirPoderNoChao(Poder.PONTOS_X2, cabeca.getX() + 1, cabeca.getY());
        j.passo(0.15);

        assertEquals(2, j.multiplicador());
        assertTrue(j.temPoder(Poder.PONTOS_X2));
    }

    @Test
    public void comODobroAtivoAComidaValeVinte() {
        JogoSnake j = jogo(morre(20));
        Celula cabeca = j.getCobra().cabeca();
        j.definirPoderNoChao(Poder.PONTOS_X2, cabeca.getX() + 1, cabeca.getY());
        j.passo(0.15);
        assertEquals(2, j.multiplicador());
        assertEquals("pegar o poder não pontua", 0, j.getPontos());

        // agora a comida, no passo seguinte
        Celula c2 = j.getCobra().cabeca();
        j.definirComida(c2.getX() + 1, c2.getY());
        j.passo(0.15);

        assertEquals(20, j.getPontos());
    }

    @Test
    public void oPoderExpiraEVoltaAoNormal() {
        JogoSnake j = jogo(morre(20));
        Celula cabeca = j.getCobra().cabeca();
        j.definirPoderNoChao(Poder.PONTOS_X2, cabeca.getX() + 1, cabeca.getY());
        j.passo(0.15);
        assertTrue(j.temPoder(Poder.PONTOS_X2));

        j.passo(11.0); // passa dos 10 s do poder

        assertFalse(j.temPoder(Poder.PONTOS_X2));
        assertEquals(1, j.multiplicador());
    }

    @Test
    public void doisFantasmasRenovamEmVezDeEmpilhar() {
        JogoSnake j = jogo(morre(20));
        Celula cabeca = j.getCobra().cabeca();
        j.definirPoderNoChao(Poder.FANTASMA, cabeca.getX() + 1, cabeca.getY());
        j.passo(0.15);
        assertEquals(5.0, j.restanteDe(Poder.FANTASMA), 0.001);

        // passa quase 5 s e pega outro fantasma
        j.passo(4.0);
        Celula c2 = j.getCobra().cabeca();
        j.definirPoderNoChao(Poder.FANTASMA, c2.getX() + 1, c2.getY());
        j.passo(0.15);

        // renovou para 5 s, não 5 + 5
        assertEquals(5.0, j.restanteDe(Poder.FANTASMA), 0.001);
    }

    @Test
    public void oFantasmaSeguraAParedeQueMata() {
        JogoSnake j = jogo(morre(6));
        Celula cabeca = j.getCobra().cabeca();
        j.definirPoderNoChao(Poder.FANTASMA, cabeca.getX() + 1, cabeca.getY());
        j.passo(0.15);
        assertTrue(j.temPoder(Poder.FANTASMA));

        // corre até a parede
        while (j.getEstado() == JogoSnake.Estado.JOGANDO
                && j.getCobra().cabeca().getX() < 5) {
            j.passo(0.15);
        }
        j.passo(0.15); // este é o passo que bateria na parede

        assertEquals(JogoSnake.Estado.JOGANDO, j.getEstado());
        // e a cabeça reaparece do lado oposto, dentro do campo: ficar fora da
        // grade quebraria o desenho, que só sabe colocar segmento dentro dela
        assertEquals(0, j.getCobra().cabeca().getX());
        for (Celula s : j.getCobra().segmentos()) {
            assertTrue("segmento " + s + " fora do campo",
                    j.getCampo().dentro(s.getX(), s.getY()));
        }
    }

    @Test
    public void oFantasmaNaoFazNadaNoWrap() {
        // no wrap a parede já não mata: o poder é inútil e não pode dar vantagem
        JogoSnake j = jogo(wrap(6));
        Celula cabeca = j.getCobra().cabeca();
        j.definirPoderNoChao(Poder.FANTASMA, cabeca.getX() + 1, cabeca.getY());
        j.passo(0.15);

        assertTrue(j.temPoder(Poder.FANTASMA));
        // o multiplicador de pontos continua 1: o fantasma não pontua
        assertEquals(1, j.multiplicador());
    }

    @Test
    public void oPoderNoChaoSaiDaListaQuandoColetado() {
        JogoSnake j = jogo(morre(20));
        Celula cabeca = j.getCobra().cabeca();
        j.definirPoderNoChao(Poder.PONTOS_X2, cabeca.getX() + 1, cabeca.getY());
        assertEquals(1, j.poderesNoChao().size());

        j.passo(0.15);

        assertEquals(0, j.poderesNoChao().size());
    }

    // ------------------------------------------------------------------
    // A fila de direções dentro do laço do jogo
    // ------------------------------------------------------------------

    @Test
    public void aFilaConsomeUmaDirecaoPorPasso() {
        // Regressão: consumirDirecao() e avancarPara() já puxavam da fila, e
        // duas viradas enfileiradas sumiam em um único passo. A virada parecia
        // funcionar por um tique e era descartada no seguinte.
        JogoSnake j = jogo(morre(20));
        j.definirComida(0, 0);
        assertTrue(j.virar(Direcao.BAIXO));
        assertTrue(j.virar(Direcao.ESQUERDA));
        assertEquals(2, j.getCobra().tamanhoFila());

        j.passo(0.15);

        assertEquals("uma virada por passo, nao duas", 1, j.getCobra().tamanhoFila());
        assertEquals(Direcao.BAIXO, j.getCobra().getDirecao());
    }

    @Test
    public void duasViradasSeguidasValemNaOrQueForamDigitadas() {
        JogoSnake j = jogo(morre(20));
        j.definirComida(0, 0);
        Celula inicio = j.getCobra().cabeca();
        j.virar(Direcao.BAIXO);
        j.virar(Direcao.ESQUERDA);

        j.passo(0.15);
        assertEquals(new Celula(inicio.getX(), inicio.getY() + 1), j.getCobra().cabeca());
        j.passo(0.15);
        assertEquals(new Celula(inicio.getX() - 1, inicio.getY() + 1), j.getCobra().cabeca());
        assertEquals(0, j.getCobra().tamanhoFila());
    }

    // ------------------------------------------------------------------
    // Reiniciar
    // ------------------------------------------------------------------

    @Test
    public void reiniciarZeraOPontosEVoltaAoPausado() {
        JogoSnake j = jogo(morre(20));
        Celula cabeca = j.getCobra().cabeca();
        j.definirComida(cabeca.getX() + 1, cabeca.getY());
        j.passo(0.15);
        assertEquals(10, j.getPontos());

        j.reiniciar();

        assertEquals(0, j.getPontos());
        assertEquals(0, j.getComidas());
        assertEquals(1, j.getNivel());
        assertEquals(JogoSnake.Estado.PAUSADO, j.getEstado());
    }

    @Test
    public void reiniciarLimpaAFormaDeDirecao() {
        // teclas do fim da partida anterior não podem vazar para a próxima
        JogoSnake j = jogo(morre(6));
        while (j.getCobra().cabeca().getX() < 5) {
            j.passo(0.15);
        }
        j.passo(0.15);
        assertEquals(JogoSnake.Estado.FIM, j.getEstado());

        j.reiniciar();
        j.iniciar();
        j.passo(0.15);

        // a primeira direção do jogo novo é a inicial, à direita
        assertEquals(Direcao.DIREITA, j.getCobra().getDirecao());
    }

    @Test
    public void pausarEFicarComACabeçaParada() {
        JogoSnake j = jogo(morre(20));
        Celula antes = j.getCobra().cabeca();

        j.pausar();
        j.passo(0.15);

        assertEquals(antes, j.getCobra().cabeca());
        assertEquals(JogoSnake.Estado.PAUSADO, j.getEstado());
    }
}
