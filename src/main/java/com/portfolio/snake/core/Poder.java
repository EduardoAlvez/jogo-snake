package com.portfolio.snake.core;

/**
 * Os power-ups do jogo.
 *
 * <p>O 1.0 traz só dois, e a escolha é deliberada: os outros (câmera lenta, ímã,
 * encolher) estão no backlog. Cada poder novo é um efeito sobre o
 * {@link JogoSnake} e mais um caso na regra simultâneo/reversão — o Pong
 * mostrou que é aí que moram os bugs.
 */
public enum Poder {

    /**
     * Atravessa a parede em vez de morrer, por alguns segundos.
     *
     * <p>Vale nas duas bordas: com {@link Campo.Borda#MORRE} a parede deixa de
     * matar enquanto o efeito está ativo, e com {@link Campo.Borda#WRAP} não
     * muda nada — o jogador já atravessa de qualquer forma. Por isso o
     * fantasma não dá pontos extras no modo wrap, senão seria dominante.
     */
    FANTASMA("F", 5.0, 0x9B59B6),

    /** Dobra os pontos enquanto durar. */
    PONTOS_X2("2", 10.0, 0xF1C40F);

    private final String rotulo;
    private final double duracao;
    private final int corRgb;

    Poder(String rotulo, double duracao, int corRgb) {
        this.rotulo = rotulo;
        this.duracao = duracao;
        this.corRgb = corRgb;
    }

    /** Sigla desenhada no card do poder. */
    public String getRotulo() {
        return rotulo;
    }

    /** Duração do efeito em segundos. */
    public double getDuracao() {
        return duracao;
    }

    /** Cor do card, em RGB empacotado. */
    public int getCorRgb() {
        return corRgb;
    }

    /**
     * O efeito já expirou.
     *
     * @param restante segundos que faltam
     * @return {@code true} se não resta tempo
     */
    public boolean expirou(double restante) {
        return restante <= 0;
    }

    /**
     * Uma instância ativa do poder, com o tempo restante.
     *
     * <p>É uma classe separada e não um {@code double} solto no jogo, porque o
     * estado de um poder é triplo: presente, ativo e expirado. Espalhar três
     * variáveis em booleanos é como o especial do Pong virou sobreposição entre
     * dois jogadores — o estado existia, mas nenhuma regra o cobria por inteiro.
     */
    public static final class Ativo {

        private final Poder tipo;
        private double restante;

        public Ativo(Poder tipo) {
            this.tipo = tipo;
            this.restante = tipo.getDuracao();
        }

        public Poder getTipo() {
            return tipo;
        }

        public double getRestante() {
            return restante;
        }

        /** Segundos restantes, arredondados para cima como o HUD mostra. */
        public int segundosRestantes() {
            return (int) Math.ceil(restante);
        }

        /** Consome um tique do relógio. */
        public void tick(double segundos) {
            this.restante -= segundos;
        }

        /**
         * Recomeça a contagem do efeito.
         *
         * <p>Comer dois fantasmas seguidos dá 5 s de novo, e não 10 s. Somar os
         * tempos deixaria a parede morta por tempo demais e o poder perderia o
         * sentido de ser um resgate curto.
         *
         * @param segundos nova duração, em segundos
         */
        public void renovar(double segundos) {
            this.restante = segundos;
        }

        /** {@code true} quando o efeito acabou e deve ser removido. */
        public boolean acabou() {
            return tipo.expirou(restante);
        }
    }
}
