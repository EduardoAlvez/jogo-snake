package com.portfolio.snake.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Testes da {@link Cobra}, com foco nos dois erros clássicos de Snake.
 *
 * <p>Os dois são bugs de <em>ordem</em>, não de cálculo: a fórmula está certa e
 * mesmo assim o jogo se comporta errado. Por isso os testes de colisão
 * <b>percorrem</b> a cobra em espiral e classificam cada passo, em vez de
 * desenhar um caso na mão — assim não dependem de um traço que eu acertei.
 */
public class CobraTest {

    private static Campo campoMorre(int lado) {
        return new Campo(lado, lado, Campo.Borda.MORRE);
    }

    private static Cobra novaCobra(int lado) {
        return new Cobra(campoMorre(lado), 8);
    }

    private static Cobra novaCobra(int lado, int comprimento) {
        return new Cobra(campoMorre(lado), comprimento);
    }

    // ------------------------------------------------------------------
    // Erro clássico 1: colisão com o corpo tem de respeitar a ordem da cauda
    // ------------------------------------------------------------------

    /**
     * Percorre uma espiral e verifica que a cabeça <b>nunca</b> é tratada como
     * colisão por ter entrado na célula que a cauda estava liberando.
     *
     * <p>Se o código checasse a colisão antes de remover a cauda, este é
     * exatamente o passo que mataria o jogador sem motivo — a cobra anda em
     * quadradinhos de 2×2, e em cada quadrado a única célula livre à frente da
     * cabeça é a que a cauda está deixando. O percurso precisa exercitar esse
     * caso pelo menos uma vez, senão o teste passaria sem provar nada.
     *
     * <p>O outro lado — cabeça em segmento que <b>sobreviveu</b> — está em
     * {@link #cabecaEmSegmentoQueSobreviveuMata}. Os dois juntos fecham o
     * problema; nenhum dos dois sozinho provaria que a checagem olha para o
     * lugar certo.
     *
     * <p>A cobra é curta de propósito. Com comprimento 4, o quadrado de 2×2 é
     * fechado e a cabeça entra na célula da cauda <b>todo</b> passo; com 8
     * segmentos o laço tem duas camadas e a cabeça nunca alcança a cauda, que é
     * por isso que este caso não apareceria num percurso longo.
     */
    @Test
    public void cabecaNuncaColidePorEntrarNaCelulaLiberada() {
        Cobra cobra = novaCobra(7, 4);
        // quadrado de 2x2 repetido: DIREITA, BAIXO, ESQUERDA, CIMA, ...
        Direcao[] trilha = {
            Direcao.DIREITA, Direcao.BAIXO, Direcao.ESQUERDA, Direcao.CIMA,
            Direcao.DIREITA, Direcao.BAIXO, Direcao.ESQUERDA, Direcao.CIMA,
            Direcao.DIREITA, Direcao.BAIXO, Direcao.ESQUERDA, Direcao.CIMA,
        };

        int naCaudaLiberada = 0;
        boolean colisaoIncorreta = false;

        Direcao anterior = null;
        for (Direcao d : trilha) {
            // A referência de validação é a mesma que a Cobra usa: a última
            // direção da fila, ou a direção atual quando a fila está vazia.
            Direcao referencia = anterior != null ? anterior : cobra.getDirecao();
            boolean precisaEnfileirar = d != referencia;

            if (precisaEnfileirar
                    && cobra.getCampo().paredeMata(cobra.cabeca().getX(), cobra.cabeca().getY(),
                            d.dx(), d.dy())) {
                break; // bateu na parede: a espiral terminou
            }
            if (precisaEnfileirar) {
                // Se a direção fosse rejeitada, a cobra andaria para outro lado e
                // toda a geometria contada abaixo deixaria de valer. A trilha tem
                // que ser aceita passo a passo.
                assertTrue("a trilha exige todas as direções aceitas, falhou em " + d,
                        cobra.enfileirar(d));
                anterior = d;
            }

            // a cauda que vai sair neste passo
            Celula caudaQueSai = cobra.rabo();

            cobra.avancar(false);

            Celula novaCabeca = cobra.cabeca();
            if (novaCabeca.getX() == caudaQueSai.getX() && novaCabeca.getY() == caudaQueSai.getY()) {
                naCaudaLiberada++;
                if (cobra.colidiuComSi()) {
                    colisaoIncorreta = true; // a cauda já tinha saído: não é colisão
                }
            }
        }

        assertFalse("cabeça na célula liberada pela cauda não é colisão", colisaoIncorreta);
        assertTrue("o percurso precisa exercitar a entrada na célula liberada",
                naCaudaLiberada > 0);
    }

    /**
     * Caso mínimo e legível do mesmo fato: a cobra vira um quadrado de 2×2, e a
     * cabeça termina exatamente na célula que a cauda liberou.
     */
    @Test
    public void quadradoDeDoisPorDoisNaoMata() {
        Campo campo = campoMorre(20);
        Cobra cobra = new Cobra(campo, 4);
        // começa em (10,10) indo para a direita; corpo (10,10)(9,10)(8,10)(7,10)
        cobra.enfileirar(Direcao.CIMA);
        cobra.avancar(false);
        cobra.enfileirar(Direcao.ESQUERDA);
        cobra.avancar(false);
        // cabeça (9,9), corpo (9,9)(10,9)(10,10)(9,10)
        assertEquals(new Celula(9, 9), cobra.cabeca());
        assertEquals(new Celula(9, 10), cobra.rabo());

        cobra.enfileirar(Direcao.BAIXO);
        cobra.avancar(false);
        // cabeça (9,10): era a posição do rabo, que saiu neste passo
        assertEquals(new Celula(9, 10), cobra.cabeca());
        assertFalse("entrar na célula que a cauda liberou é legal", cobra.colidiuComSi());
    }

    /**
     * Contrapartida indispensável: se a cabeça entra numa célula que o corpo
     * ainda ocupa, é morte. Sem este teste, o anterior passaria mesmo com uma
     * checagem que nunca retorna verdadeiro.
     */
    @Test
    public void cabecaEmSegmentoQueSobreviveuMata() {
        Campo campo = campoMorre(20);
        Cobra cobra = new Cobra(campo, 5);
        // monta um U apertado de 2 de largura, com 5 segmentos:
        // (10,10)(10,11)(10,12)(11,12)(12,12) — cabeça em (10,10) indo para CIMA
        cobra.enfileirar(Direcao.DIREITA);
        cobra.avancar(false);
        cobra.avancar(false);
        // cabeça (12,10), corpo (12,10)(11,10)(10,10)(9,10)(8,10)
        cobra.enfileirar(Direcao.BAIXO);
        cobra.avancar(false);
        // cabeça (12,11), corpo (12,11)(12,10)(11,10)(10,10)(9,10)
        cobra.enfileirar(Direcao.ESQUERDA);
        cobra.avancar(false);
        // cabeça (11,11), corpo (11,11)(12,11)(12,10)(11,10)(10,10)
        cobra.enfileirar(Direcao.CIMA);
        cobra.avancar(false);
        // cabeça (11,10): era o segundo segmento, que NÃO saiu neste passo
        assertEquals(new Celula(11, 10), cobra.cabeca());
        assertTrue("bater no segundo segmento é colisão", cobra.colidiuComSi());
    }

    /** A cabeça nunca conta como colisão com ela mesma. */
    @Test
    public void cabecaNaoColideComElaMesma() {
        assertFalse(novaCobra(10).colidiuComSi());
    }

    // ------------------------------------------------------------------
    // Erro clássico 2: a fila de direções é validada contra o elemento errado
    // ------------------------------------------------------------------

    /**
     * O caso que só falha com a validação errada. A cobra vai para a direita e o
     * jogador aperta CIMA e BAIXO no mesmo tique.
     *
     * <p>Se BAIXO fosse validado contra a direção <em>atual</em> (DIREITA), ele
     * seria aceito, porque não é oposta a DIREITA. Aí, no passo seguinte, a
     * cabeça inverteria e o jogador morreria sem causa aparente. Validado contra
     * a última enfileirada (CIMA), BAIXO é o oposto e é rejeitado.
     */
    @Test
    public void teclasOpostasNoMesmoTickNaoInvertemACobra() {
        Cobra cobra = new Cobra(campoMorre(20), 4);
        assertEquals(Direcao.DIREITA, cobra.getDirecao());

        assertTrue("CIMA é perpendicular, entra", cobra.enfileirar(Direcao.CIMA));
        assertFalse("BAIXO é o oposto de CIMA, que está na fila",
                cobra.enfileirar(Direcao.BAIXO));
        assertEquals("a fila não pode guardar as duas", 1, cobra.tamanhoFila());

        cobra.avancar(false);
        assertEquals(Direcao.CIMA, cobra.getDirecao());
        // e a cabeça subiu, sem inverter
        assertEquals(new Celula(10, 9), cobra.cabeca());
    }

    /** O retorno de uma direção já enfileirada é sempre rejeitado. */
    @Test
    public void naoAceitaRetornoNaFila() {
        Cobra cobra = new Cobra(campoMorre(20), 4);
        assertTrue(cobra.enfileirar(Direcao.BAIXO));
        assertFalse("CIMA é o retorno de BAIXO", cobra.enfileirar(Direcao.CIMA));
        assertEquals(1, cobra.tamanhoFila());
    }

    /**
     * Duas viradas de 90° em sequência são legais e precisam entrar: a validação
     * rejeita só o oposto, não o perpendicular. Se este teste falhas, a correção
     * ficou ampla demais e travou o jogador.
     */
    @Test
    public void aceitaDuasViradasDeNoventa() {
        Cobra cobra = new Cobra(campoMorre(20), 4);
        assertTrue(cobra.enfileirar(Direcao.CIMA));
        // ESQUERDA é oposta a DIREITA (a atual), mas perpendicular a CIMA (a da
        // fila). A validação correta aceita.
        assertTrue("perpendicular à última da fila, não o oposto",
                cobra.enfileirar(Direcao.ESQUERDA));
        assertEquals(2, cobra.tamanhoFila());

        cobra.avancar(false);
        assertEquals(Direcao.CIMA, cobra.getDirecao());
        cobra.avancar(false);
        assertEquals(Direcao.ESQUERDA, cobra.getDirecao());
    }

    /** Repetir a direção atual não enche a fila à toa. */
    @Test
    public void repetirDirecaoAtualNaoEntraNaFila() {
        Cobra cobra = new Cobra(campoMorre(20), 4);
        assertFalse(cobra.enfileirar(Direcao.DIREITA));
        assertEquals(0, cobra.tamanhoFila());
    }

    /** A fila é limitada, para o jogador não adiantar viradas demais. */
    @Test
    public void filaTemLimite() {
        Cobra cobra = new Cobra(campoMorre(20), 4);
        assertTrue(cobra.enfileirar(Direcao.CIMA));
        assertTrue(cobra.enfileirar(Direcao.ESQUERDA));
        assertFalse("fila cheia", cobra.enfileirar(Direcao.BAIXO));
        assertEquals(2, cobra.tamanhoFila());
    }

    /** Teclas do fim de jogo anterior não podem vazar para a partida nova. */
    @Test
    public void limparFilaDescartaDirecoesPendentes() {
        Cobra cobra = new Cobra(campoMorre(20), 4);
        cobra.enfileirar(Direcao.CIMA);
        assertEquals(1, cobra.tamanhoFila());
        cobra.limparFila();
        assertEquals(0, cobra.tamanhoFila());
    }

    // ------------------------------------------------------------------
    // Movimento, crescimento e encurtamento
    // ------------------------------------------------------------------

    @Test
    public void avancaUmPassoNaDirecaoAtual() {
        Cobra cobra = new Cobra(campoMorre(20), 3);
        assertEquals(new Celula(10, 10), cobra.cabeca());
        cobra.avancar(false);
        assertEquals(new Celula(11, 10), cobra.cabeca());
        assertEquals(3, cobra.tamanho());
    }

    @Test
    public void comerEsticaACobraEmUmSegmento() {
        Cobra cobra = new Cobra(campoMorre(20), 3);
        cobra.avancar(false);
        assertEquals("sem comer, a cauda sai e o tamanho se mantém", 3, cobra.tamanho());
        cobra.avancar(true);
        assertEquals("a cabeça entra e a cauda não sai: cresceu 1", 4, cobra.tamanho());
        cobra.avancar(true);
        assertEquals(5, cobra.tamanho());
    }

    @Test
    public void encurtarRemoveDoRabo() {
        Cobra cobra = new Cobra(campoMorre(20), 5);
        assertEquals(2, cobra.encurtar(2));
        assertEquals(3, cobra.tamanho());
    }

    @Test
    public void encurtarNuncaDeixaMenosDeDoisSegmentos() {
        Cobra cobra = new Cobra(campoMorre(20), 5);
        cobra.encurtar(10);
        assertEquals("cobra nunca fica sem corpo", 2, cobra.tamanho());
    }

    @Test
    public void segmentosSaoDaCabecaAoRabo() {
        Cobra cobra = new Cobra(campoMorre(20), 3);
        assertEquals(new Celula(10, 10), cobra.segmento(0));
        assertEquals(new Celula(9, 10), cobra.segmento(1));
        assertEquals(new Celula(8, 10), cobra.segmento(2));
    }

    /** A fila é consumida na ordem em que foi digitada. */
    @Test
    public void filaEhConsumidaNaOrdem() {
        Cobra cobra = new Cobra(campoMorre(20), 4);
        cobra.enfileirar(Direcao.CIMA);
        cobra.enfileirar(Direcao.ESQUERDA);
        cobra.avancar(false);
        assertEquals(Direcao.CIMA, cobra.getDirecao());
        assertEquals(1, cobra.tamanhoFila());
        cobra.avancar(false);
        assertEquals(Direcao.ESQUERDA, cobra.getDirecao());
        assertEquals(0, cobra.tamanhoFila());
    }

    // ------------------------------------------------------------------
    // A cabeça e o rabo apontam para o lado certo
    // ------------------------------------------------------------------

    /**
     * A cabeça aponta para onde a cobra está andando.
     *
     * <p><b>Este é o teste do bug 6.</b> O nome {@code CABECA_X} diz a direção para
     * onde a cabeça vai, e {@link Cobra#formas()} entregava a direção para
     * <i>trás</i> nos dois extremos do corpo: cabeça→pescoço. Uma cobra andando
     * para a direita recebia {@code CABECA_ESQUERDA}. Os olhos de todas as skins
     * vetoriais estavam virados desde o começo, e só a arte do Dedinho tornou o
     * defeito visível.
     *
     * <p>Nenhum teste afirmava isso antes: o único que olhava {@code formas()} usava
     * {@code ehCanto()}, e canto não muda com a inversão. Era a classe com mais
     * teste do projeto no eixo errado.
     *
     * <p>O caminho é uma espiral de quatro lados, porque virar 180° é recusado pela
     * regra de fila e um cenário degenerado esconderia o defeito em vez de
     * mostrá-lo.
     */
    @Test
    public void aCabecaApontaParaOndeACobraAnda() {
        Cobra cobra = novaCobra(40);
        assertCabeca(cobra, Direcao.DIREITA, Forma.CABECA_DIREITA);

        viraPara(cobra, Direcao.BAIXO);
        assertCabeca(cobra, Direcao.BAIXO, Forma.CABECA_BAIXO);

        viraPara(cobra, Direcao.ESQUERDA);
        assertCabeca(cobra, Direcao.ESQUERDA, Forma.CABECA_ESQUERDA);

        viraPara(cobra, Direcao.CIMA);
        assertCabeca(cobra, Direcao.CIMA, Forma.CABECA_CIMA);
    }

    /**
     * O rabo tem a ponta livre virada para fora do corpo.
     *
     * <p>A mesma inversão do bug 6 atingia o rabo pelo outro lado: {@code RABO_X} é
     * a direção da ponta, e a ponta livre fica do lado oposto ao do corpo.
     *
     * <p>A expectativa é montada aqui com uma tabela escrita à mão de propósito.
     * Derivá-la de {@link Forma} seria circular — o teste passaria com o defeito
     * presente, porque repetiria a mesma conta que ele errou. A tabela é a
     * afirmação independente de como o rabo deve ficar.
     */
    @Test
    public void oRaboApontaParaForaDoCorpo() {
        Cobra cobra = novaCobra(40);
        Celula rabo = cobra.rabo();
        Celula vizinho = cobra.segmento(cobra.tamanho() - 2);

        // de onde o rabo vem, ou seja, o lado em que o corpo esta
        int dx = Integer.compare(vizinho.getX(), rabo.getX());
        int dy = Integer.compare(vizinho.getY(), rabo.getY());
        Forma esperado;
        if (dx > 0) {
            esperado = Forma.RABO_ESQUERDA;
        } else if (dx < 0) {
            esperado = Forma.RABO_DIREITA;
        } else if (dy > 0) {
            esperado = Forma.RABO_CIMA;
        } else {
            esperado = Forma.RABO_BAIXO;
        }

        assertEquals("a ponta livre do rabo tem de ficar do lado oposto ao corpo",
                esperado, cobra.formas().get(cobra.tamanho() - 1));
    }

    private static void viraPara(Cobra cobra, Direcao destino) {
        for (int i = 0; i < 5; i++) {
            cobra.moverPara(destino, false);
        }
    }

    private static void assertCabeca(Cobra cobra, Direcao andando, Forma esperado) {
        assertEquals("a direcao da cobra nao e a que o testeMandou",
                andando, cobra.getDirecao());
        assertEquals("a cabeca aponta para tras",
                esperado, cobra.formas().get(0));
    }
}
