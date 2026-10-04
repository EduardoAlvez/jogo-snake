import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Gera os quatro efeitos sonoros do Snake, por código.
 *
 * <p>O mesmo argumento do logo: um {@code .wav} binário que ninguém sabe
 * refazer é defeito esperando. Aqui cada som é uma fórmula, e
 * {@code mvn} não precisa de nenhum arquivo externo para recriá-los.
 *
 * <p><b>PCM 16 bits, mono, 44 100 Hz</b> — o formato mais simples que o
 * {@code javax.sound} lê sem depender de nenhum codec.
 *
 * <p><b>O envelope é a parte que importa.</b> Uma onda que começa ou termina
 * longe de zero solta um estalo exatamente na transition: o clique aparece no
 * começo e no fim de cada som, e é o defeito que mais aparece em som de jogo
 * gerado às pressas. Por isso todo som começa e termina em zero, e o teste
 * {@code SonsTest} confere isso nos arquivos gerados.
 */
public final class GerarSons {

    /** Taxa de amostragem. 44 100 Hz é o padrão do Java Sound. */
    private static final int TAXA = 44100;

    /**
     * Amplitude máxima. Fica abaixo de 1 de propósito: em 1.0 o pico vira
     * 32767 e distorce, e o som estala em vez de soar.
     */
    private static final double TETO = 0.89;

    private GerarSons() {
    }

    public static void main(String[] args) throws IOException {
        Path saida = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("src", "main", "resources", "sons");
        Files.createDirectories(saida);

        grava(saida.resolve("comer.wav"), comer());
        grava(saida.resolve("poder.wav"), poder());
        grava(saida.resolve("parede.wav"), parede());
        grava(saida.resolve("fim.wav"), fim());
    }

    // ------------------------------------------------------------------
    // Os quatro sons
    // ------------------------------------------------------------------

    /**
     * Comer: um bip curto que sobe de tom.
     *
     * <p>É o som que mais repete — pode soar cinco vezes por segundo. Por isso é
     * o mais curto (90 ms) e o mais quieto dos quatro: um bip longo aqui
     * vira um zumbido, porque os exemplares se sobrepõem.
     */
    private static double[] comer() {
        int n = amostras(0.090);
        double[] saida = new double[n];
        double inicio = 660, fim = 990;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double f = inicio + (fim - inicio) * t;
            // onda quadrada: o "bipe" de jogo de videogame clássico
            double onda = Math.sin(2 * Math.PI * f * i / TAXA) >= 0 ? 0.55 : -0.55;
            saida[i] = onda * envelope(i, n, 0.06, 0.35) * 0.55;
        }
        return saida;
    }

    /**
     * Poder: três notas subindo, uma por vez.
     *
     * <p>Duas formas só não bastam para decir "consegui isto": é o som de
     * recompensa do jogo, e o intervalo pequeno impede o som de virar o mesmo
     * bip do {@code comer}, que é justamente o que o jogador precisa distinguir.
     */
    private static double[] poder() {
        int n = amostras(0.300);
        double[] saida = new double[n];
        double[] notas = {523.25, 659.25, 783.99}; // dó, mi, sol
        int porNota = n / notas.length;
        for (int i = 0; i < n; i++) {
            int qual = Math.min(notas.length - 1, i / porNota);
            int dentro = i - qual * porNota;
            double f = notas[qual];
            double onda = Math.sin(2 * Math.PI * f * i / TAXA);
            // a última nota sai mais longa, para o som não terminar a seco
            double cauda = qual == notas.length - 1 ? 0.75 : 0.35;
            saida[i] = onda * envelope(dentro, porNota, 0.10, cauda) * 0.5;
        }
        return saida;
    }

    /**
     * Bater na parede: um baque grave com um estalo de ruído na frente.
     *
     * <p>Ruído não determinístico seria o caminho preguiçoso, então o gerador
     * tem um LCG próprio: o mesmo código produz sempre o mesmo arquivo.
     */
    private static double[] parede() {
        int n = amostras(0.220);
        double[] saida = new double[n];
        Lcg ruido = new Lcg(20260904L);
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double grave = Math.sin(2 * Math.PI * 110 * i / TAXA);
            // o ruído só aparece nos primeiros 18%: o "impacto"
            double estalo = t < 0.18 ? ruido.proximo() * (1 - t / 0.18) * 0.5 : 0;
            saida[i] = (grave * 0.8 + estalo) * envelope(i, n, 0.02, 0.55) * 0.7;
        }
        return saida;
    }

    /**
     * Fim de partida: uma nota descendo, com vibrato.
     *
     * <p>O mais longo dos quatro (550 ms), porque é o som que o jogador ouve uma
     * vez por partida e é o que fecha o ciclo. A queda de tom é o que diz
     * "acabou" sem precisar de texto.
     */
    private static double[] fim() {
        int n = amostras(0.550);
        double[] saida = new double[n];
        double inicio = 440, alvo = 110;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double f = inicio + (alvo - inicio) * t;
            // vibrato leve: sem ele a nota longa soa como um bipe de modem
            double vib = 1 + 0.012 * Math.sin(2 * Math.PI * 5.5 * i / TAXA);
            double onda = Math.sin(2 * Math.PI * f * vib * i / TAXA);
            saida[i] = onda * envelope(i, n, 0.04, 0.70) * 0.6;
        }
        return saida;
    }

    // ------------------------------------------------------------------
    // Ferramentas de síntese
    // ------------------------------------------------------------------

    private static int amostras(double segundos) {
        return (int) Math.round(segundos * TAXA);
    }

    /**
     * Ataque e decaimento, com ataque e cauda em zero.
     *
     * <p>Os dois primeiros extremos em zero é o que impede o clique: a amostra
     * 0 e a última saem praticamente nulas, então a onda começa e termina no
     * silêncio em vez de pular para o valor full-scale.
     */
    private static double envelope(int i, int total, double ataque, double cauda) {
        int ataqueN = Math.max(1, (int) Math.round(ataque * total));
        int caudaN = Math.max(1, (int) Math.round(cauda * total));
        if (i < ataqueN) {
            return i / (double) ataqueN;
        }
        int desdeFim = total - 1 - i;
        if (desdeFim < caudaN) {
            return desdeFim / (double) caudaN;
        }
        return 1.0;
    }

    /** Gerador linear congruente, para o ruído ser reprodutível. */
    private static final class Lcg {
        private long estado;

        Lcg(long semente) {
            this.estado = semente;
        }

        double proximo() {
            estado = (estado * 6364136223846793005L + 1442695040888963407L);
            // os 53 bits altos dão um número bem distribuído em [-1, 1)
            return ((estado >> 11) / (double) (1L << 53)) * 2 - 1;
        }
    }

    // ------------------------------------------------------------------
    // Escrita do WAV
    // ------------------------------------------------------------------

    /**
     * Grava um WAV PCM de 16 bits, mono.
     *
     * <p>Os tamanhos do cabeçalho são escritos por conta própria em vez de usar
     * {@code AudioSystem.write}, para o formato não depender do que a máquina
     * tem instalado: o mesmo código gera o mesmo arquivo em qualquer máquina.
     */
    private static void grava(Path arquivo, double[] amostras) throws IOException {
        double pico = 0;
        for (double a : amostras) {
            pico = Math.max(pico, Math.abs(a));
        }
        double escala = pico > TETO ? TETO / pico : 1.0;

        int dados = amostras.length * 2;
        try (OutputStream saida = Files.newOutputStream(arquivo)) {
            DataOutputStream d = new DataOutputStream(saida);
            d.writeBytes("RIFF");
            writeLE32(d, 36 + dados);
            d.writeBytes("WAVE");
            d.writeBytes("fmt ");
            writeLE32(d, 16);
            writeLE16(d, 1); // PCM sem compressão
            writeLE16(d, 1); // mono
            writeLE32(d, TAXA);
            writeLE32(d, TAXA * 2); // bytes por segundo
            writeLE16(d, 2); // bytes por amostra entrelaçada
            writeLE16(d, 16); // bits por amostra
            d.writeBytes("data");
            writeLE32(d, dados);
            for (double a : amostras) {
                int v = (int) Math.round(a * escala * 32767);
                // sem isto, uma amostra de 1.0000001 vira 32767 e depois -32768
                v = Math.max(-32768, Math.min(32767, v));
                writeLE16(d, v);
            }
        }
        System.out.printf("%-12s %5d amostras  %6.1f ms  pico %.2f%n",
                arquivo.getFileName(), amostras.length, amostras.length * 1000.0 / TAXA,
                pico * escala);
    }

    private static void writeLE16(DataOutputStream d, int v) throws IOException {
        d.write(v & 0xFF);
        d.write((v >> 8) & 0xFF);
    }

    private static void writeLE32(DataOutputStream d, int v) throws IOException {
        d.write(v & 0xFF);
        d.write((v >> 8) & 0xFF);
        d.write((v >> 16) & 0xFF);
        d.write((v >> 24) & 0xFF);
    }
}