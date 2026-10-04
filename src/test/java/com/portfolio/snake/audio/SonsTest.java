package com.portfolio.snake.audio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.portfolio.snake.audio.Sons.Efeito;

/**
 * Os sons, conferidos no <b>produto</b>: o arquivo que o jogo carrega, não o
 * gerador que o produziu.
 *
 * <p>{@code tools/} não entra no build, então um teste do
 * {@code GerarSons} passaria com o que está no disco errado. Aqui tudo é lido
 * pelo classpath e, na metade dos casos, <b>de dentro de um jar de verdade</b>.
 *
 * <p><b>Nenhum teste aqui diz que o som soa bem.</b> Isso exige ouvido humano, e
 * um teste que fingisse medir "qualidade" seria teatro. O que dá para medir sem
 * ears é se a onda está bem formada: não mudo, não clipado, começando e
 * terminando em zero, e os quatro sons diferentes entre si.
 */
public class SonsTest {

    @Rule
    public TemporaryFolder pasta = new TemporaryFolder();

    /** Tamanho do cabeçalho WAV PCM, sem blocos extras. */
    private static final int CABECALHO = 44;

    /** Amostra considered "não muda": RMS acima disto já é som. */
    private static final double RMS_MINIMO = 0.01;

    // ------------------------------------------------------------------    // ------------------------------------------------------------------
    // O produto está no classpath
    // ------------------------------------------------------------------

    @Test
    public void osQuatroSonsEstaoNoClasspath() {
        for (Efeito e : Efeito.values()) {
            assertNotNull("o recurso /sons/" + e.getArquivo() + " nao esta no classpath",
                    Sons.class.getResourceAsStream("/sons/" + e.getArquivo()));
        }
    }

    /**
     * O teste que o Pong Pagou para ter — e desta vez ele roda o
     * <b>{@code Sons} de verdade</b>.
     *
     * <p>A primeira versão deste teste montava o próprio {@code URLClassLoader} e
     * decodificava por conta própria. Ela provava que <i>um</i> jar decodifica,
     * e passava verde com o {@code Sons} completamente quebrado — verificado por
     * mutação: tirando o {@code BufferedInputStream} do {@code Sons}, nada caía.
     * Era a armadilha do "cenário não alcança a linha testada", cometida no
     * teste que existia justamente para impedir isso.
     *
     * <p>Agora o jar leva a classe compilada junto com os {@code .wav}, e ela é
     * carregada por um {@code URLClassLoader} cujo pai é o
     * <b>platform loader</b>. Assim {@code javax.sound} resolve, {@code target/classes}
     * fica invisível, e o {@code Sons} que roda é o do jar: os recursos dele
     * saem de entradas de jar, que não suportam {@code mark}.
     *
     * <p>Esse detalhe é o cerne do defeito do Pong: de {@code target/classes} o
     * JDK devolve um {@code BufferedInputStream} com {@code mark} funcionando, e
     * o teste passa. Só de dentro do jar a ausência de {@code mark} aparece.
     */
    @Test
    public void oSonsDeVerdadeCarregaOsSonsDeDentroDeUmJar() throws Exception {
        File jar = jarComOsSonsEOClass();
        // pai = platform loader: java.* e javax.* resolvem, o classpath do
        // teste nao. Se o Sons nao vier do jar, o teste nem compila o bastante
        // para rodar.
        try (URLClassLoader cl = new URLClassLoader(
                new URL[]{jar.toURI().toURL()}, ClassLoader.getPlatformClassLoader())) {

            Class<?> sons = cl.loadClass("com.portfolio.snake.audio.Sons");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Class<?> efeito = cl.loadClass("com.portfolio.snake.audio.Sons$Efeito");
            java.lang.reflect.Method carregar = sons.getMethod("carregar", efeito);

            for (Object e : efeito.getEnumConstants()) {
                byte[] dados = (byte[]) carregar.invoke(null, e);
                assertNotNull("o Sons do jar nao carregou " + e, dados);
                assertTrue("o som saiu mudo do jar: " + e, rms(dados) > RMS_MINIMO);
            }
        }
    }

    @Test
    public void semBufferALeituraDoJarFalha() throws Exception {
        File jar = jarComOsSons();
        try (URLClassLoader cl = new URLClassLoader(new URL[]{jar.toURI().toURL()}, null);
             InputStream cru = cl.getResourceAsStream(caminhoDe(Efeito.COMER))) {
            assertFalse("este teste so tem valor se o fluxo cru nao suportar mark/reset",
                    cru.markSupported());
            try {
                AudioInputStream a = AudioSystem.getAudioInputStream(cru);
                AudioSystem.getAudioFileFormat(a);
                org.junit.Assert.fail("leu de um jar sem buffer sem falhar: "
                        + "ou o JDK mudou, ou o teste acima parou de provar o que="
                        + "prova e precisa ser revisto");
            } catch (IOException esperada) {
                assertTrue("a falha tem de ser a de mark/reset, e nao outra coisa",
                        String.valueOf(esperada.getMessage()).contains("mark/reset"));
            }
        }
    }

    // ------------------------------------------------------------------
    // A onda está bem formada
    // ------------------------------------------------------------------

    /**
     * Começa e termina em zero.
     *
     * <p>É o teste que pega o clique. Uma onda que salta de 0 para o valor
     * máximo no primeiro sample — ou que termina num valor alto e é cortada —
     * estala no começo e no fim. Não se ouve isso num teste, mas ouve-se no
     * jogo, e é o defeito mais comum em som gerado por código.
     */
    @Test
    public void todosOsSonsComecamETerminamEmZero() throws Exception {
        for (Efeito e : Efeito.values()) {
            Amostra a = amostra(e);
            double[] s = a.amostras;
            assertTrue(e + " comeca com um salto de " + Math.abs(s[0]),
                    Math.abs(s[0]) < 0.02);
            assertTrue(e + " termina com um salto de " + Math.abs(s[s.length - 1]),
                    Math.abs(s[s.length - 1]) < 0.02);
        }
    }

    @Test
    public void nenhumSomEstaMudo() throws Exception {
        for (Efeito e : Efeito.values()) {
            Amostra a = amostra(e);
            assertTrue(e + " esta mudo (rms " + rms(a.dados) + ")", rms(a.dados) > RMS_MINIMO);
        }
    }

    /**
     * Nenhum som clipado.
     *
     * <p>Clipping é amostra batendo em 32767 e sendo cortada: o som fica com
     * harmônicos duros e um chiado. O gerador normaliza em 0.89 justamente
     * para não chegar aí, e este teste vigia essa promessa.
     */
    @Test
    public void nenhumSomEstaClipado() throws Exception {
        for (Efeito e : Efeito.values()) {
            Amostra a = amostra(e);
            int noTeto = 0;
            for (double v : a.amostras) {
                if (Math.abs(v) >= 0.99) {
                    noTeto++;
                }
            }
            assertTrue(e + " tem " + noTeto + " amostra(s) no limite do full scale",
                    noTeto < a.amostras.length / 1000);
        }
    }

    /** As durações batem com o que o gerador promete: curtas e úteis. */
    @Test
    public void asDuracoesSaoAsPretendidas() throws Exception {
        assertTrue("comer", duracao(Efeito.COMER) < 0.20);
        assertTrue("poder", duracao(Efeito.PODER) < 0.40);
        assertTrue("parede", duracao(Efeito.PAREDE) < 0.35);
        assertTrue("fim", duracao(Efeito.FIM) < 0.80);
        assertTrue("ninguem pode ser um arquivo vazio", duracao(Efeito.COMER) > 0.02);
    }

    /**
     * Os quatro sons são diferentes entre si.
     *
     * <p>Sem isto, um erro no gerador que fizesse os quatro saírem iguais
     * passaria: o teste de "não está mudo" continuaria verde para os quatro. É
     * o teste que impede um jogo onde comer, pegar poder e morrer soam igual.
     */
    @Test
    public void osQuatroSonsSaoDiferentesEntreSi() throws Exception {
        List<Efeito> efeitos = Arrays.asList(Efeito.values());
        for (int i = 0; i < efeitos.size(); i++) {
            for (int j = i + 1; j < efeitos.size(); j++) {
                Efeito a = efeitos.get(i);
                Efeito b = efeitos.get(j);
                assertFalse(a + " e " + b + " sao o mesmo som",
                        Arrays.equals(amostra(a).dados, amostra(b).dados));
            }
        }
    }

    // ------------------------------------------------------------------
    // O jogo continua funcionando
    // ------------------------------------------------------------------

    /**
     * {@code carregar} devolve o mesmo array na segunda chamada.
     *
     * <p>A segunda parte é a importante: o cache é o que impede o laço do jogo de
     * decodificar um WAV a cada comida. Sem ele o jogo funciona e fica lento, e
     * lentidão não aparece em nenhum teste.
     */
    @Test
    public void oCacheEntregaOMesmoArray() {
        byte[] primeira = Sons.carregar(Efeito.COMER);
        assertNotNull(primeira);
        assertTrue("a segunda chamada re-decodificou em vez de usar o cache",
                primeira == Sons.carregar(Efeito.COMER));
    }

    @Test
    public void todosOsEfeitosCarregamPeloProduzPath() {
        for (Efeito e : Efeito.values()) {
            assertNotNull("carregar devolveu null para " + e, Sons.carregar(e));
        }
    }

    /** Efeito inexistente devolve {@code null}, e não estoura. */
    @Test
    public void carregarUmEfeitoInexistenteNaoExplode() {
        assertEquals(null, Sons.carregar(null));
    }

    /**
     * {@code tocar} nunca lança, nem com efeito nulo.
     *
     * <p>É a rede que o jogo inteiro depende: uma exceção de áudio aqui
     * derrubaria a partida.
     */
    @Test
    public void tocarNuncaLanca() {
        Sons.tocar(null);
        for (Efeito e : Efeito.values()) {
            Sons.tocar(e);
        }
    }

    @Test
    public void oMudoDesligaOTocar() {
        Sons.mudo(true);
        try {
            assertTrue(Sons.isMudo());
            assertFalse("com o som mudo, nao se diz que ha audio disponivel", Sons.disponivel());
            Sons.tocar(Efeito.COMER); // nao pode explodir nem tocar
        } finally {
            Sons.mudo(false);
        }
        assertTrue(Sons.disponivel());
    }

    /**
     * O som realmente sai, quando a máquina tem placa.
     *
     * <p>Este é o teste que mais se aproxima do que o jogador ouve, e ainda é
     * um proxy: ele abre um {@code Clip} de verdade e conta os quadros que a
     * placa aceitou tocar. Numa máquina sem placa, ou com a placa ocupada, o
     * teste é pulado <b>e diz que pulou</b> — nunca fica verde em silêncio.
     */
    @Test
    public void oSomRealmenteTocaQuandoHaPlaca() throws Exception {
        assumeTrue("esta maquina nao tem placa de som", haDispositivo());

        byte[] dados = Sons.carregar(Efeito.COMER);
        assertNotNull(dados);
        Clip clip = null;
        try {
            clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(
                    new BufferedInputStream(new ByteArrayInputStream(dados))));
            clip.start();
            Thread.sleep(150); // o som dura 90 ms; 150 cobre com folga
            assertTrue("a placa nao tocou nenhum quadro",
                    clip.getFramePosition() > 0);
        } catch (LineUnavailableException semPlaca) {
            org.junit.Assume.assumeNoException("a placa esta ocupada", semPlaca);
        } finally {
            if (clip != null && clip.isOpen()) {
                clip.close();
            }
        }
    }

    @Test
    public void oFormatoDeclaradoEoEsperado() throws Exception {
        for (Efeito e : Efeito.values()) {
            try (InputStream in = new BufferedInputStream(
                    Sons.class.getResourceAsStream("/sons/" + e.getArquivo()))) {
                AudioFormat f = AudioSystem.getAudioInputStream(in).getFormat();
                assertEquals(e + ": taxa de amostragem", 44100f, f.getSampleRate(), 0.5);
                assertEquals(e + ": bits", 16, f.getSampleSizeInBits());
                assertEquals(e + ": canais", 1, f.getChannels());
            }
        }
    }

    // ------------------------------------------------------------------
    // Ferramentas
    // ------------------------------------------------------------------

    /** Um WAV decodificado: os bytes do arquivo e as amostras já normalizadas. */
    private static final class Amostra {
        final byte[] dados;
        final double[] amostras;

        Amostra(byte[] dados, double[] amostras) {
            this.dados = dados;
            this.amostras = amostras;
        }
    }

    private static Amostra amostra(Efeito e) throws IOException, UnsupportedAudioFileException {
        byte[] dados = Sons.carregar(e);
        assertNotNull("nao conseguiu carregar " + e, dados);
        AudioFormat f = formatoDe(dados);
        int quadros = f.getFrameSize() == 0
                ? (dados.length - CABECALHO) / 2
                : (dados.length - CABECALHO) / f.getFrameSize();
        int amostras = Math.max(0, quadros * f.getChannels());
        double[] s = new double[amostras];
        for (int i = 0; i < amostras; i++) {
            int b0 = CABECALHO + i * 2;
            int b1 = b0 + 1;
            if (b1 >= dados.length) {
                break;
            }
            s[i] = (short) ((dados[b0] & 0xFF) | (dados[b1] & 0xFF) << 8) / 32768.0;
        }
        return new Amostra(dados, s);
    }

    private static double duracao(Efeito e) throws IOException, UnsupportedAudioFileException {
        byte[] dados = Sons.carregar(e);
        AudioFormat f = formatoDe(dados);
        // divide pelo frameSize antes: sao 2 bytes por amostra, e sem isso a
        // duracao sai dobrada (0.6 s em vez de 0.3 s)
        return ((dados.length - CABECALHO) / (double) f.getFrameSize()) / f.getSampleRate();
    }

    /** RMS do arquivo WAV, pulando o cabeçalho. */
    private static double rms(byte[] dados) throws Exception {
        AudioFormat f = formatoDe(dados);
        int quadros = (dados.length - CABECALHO) / f.getFrameSize();
        double soma = 0;
        for (int i = 0; i < quadros; i++) {
            int b0 = CABECALHO + i * 2;
            double v = (short) ((dados[b0] & 0xFF) | (dados[b0 + 1] & 0xFF) << 8) / 32768.0;
            soma += v * v;
        }
        return Math.sqrt(soma / Math.max(1, quadros));
    }

    /**
     * O formato declarado nos bytes.
     *
     * <p>Passa por {@code getAudioInputStream} em vez de ler o cabeçalho na mão:
     * o objetivo é usar exatamente o caminho que o jogo usa, e ler o cabeçalho
     * mediria o meu interpretador, não o decoder.
     */
    private static AudioFormat formatoDe(byte[] dados)
            throws IOException, UnsupportedAudioFileException {
        try (InputStream comBuffer = new BufferedInputStream(new ByteArrayInputStream(dados))) {
            AudioInputStream a = AudioSystem.getAudioInputStream(comBuffer);
            AudioFormat f = a.getFormat();
            a.close();
            return f;
        }
    }

    /** Caminho dentro do jar, <b>sem</b> barra inicial. */
    private static String caminhoDe(Efeito e) {
        return "sons/" + e.getArquivo();
    }

    /**
     * Monta um jar com os quatro {@code .wav}, do jeito que o
     * {@code launch4j} monta o executável.
     */
    private File jarComOsSons() throws IOException {
        File jar = pasta.newFile("sons.jar");
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(jar.toPath()))) {
            for (Efeito e : Efeito.values()) {
                z.putNextEntry(new ZipEntry("sons/" + e.getArquivo()));
                try (InputStream in = Sons.class.getResourceAsStream("/sons/" + e.getArquivo())) {
                    copy(in, z);
                }
                z.closeEntry();
            }
        }
        return jar;
    }

    /**
     * Monta um jar com os {@code .wav} <b>e a classe {@code Sons} compilada</b>.
     *
     * <p>A classe entrar no jar é o que muda a natureza do teste: sem ela, o
     * classloader do teste carrega o {@code Sons} de {@code target/classes}, os
     * recursos vêm de diretório, o JDK entrega um fluxo com {@code mark}, e o
     * teste passa sem exercitar a condição que quebrou o Pong.
     *
     * <p>As classes vêm de {@code target/classes} porque é dali que o
     * {@code surefire} acabou de compilar — o mesmo artefato que o
     * {@code launch4j} empacota.
     */
    private File jarComOsSonsEOClass() throws IOException {
        File jar = pasta.newFile("sons-com-classe.jar");
        Path classes = Paths.get("target", "classes");
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(jar.toPath()))) {
            for (Efeito e : Efeito.values()) {
                z.putNextEntry(new ZipEntry("sons/" + e.getArquivo()));
                try (InputStream in = Sons.class.getResourceAsStream("/sons/" + e.getArquivo())) {
                    copy(in, z);
                }
                z.closeEntry();
            }
            for (String nome : new String[]{"Sons.class", "Sons$Efeito.class"}) {
                Path origem = classes.resolve(
                        Paths.get("com", "portfolio", "snake", "audio", nome));
                assertTrue("a classe compilada nao esta em target/classes: " + origem,
                        Files.exists(origem));
                z.putNextEntry(new ZipEntry("com/portfolio/snake/audio/" + nome));
                Files.copy(origem, z);
                z.closeEntry();
            }
        }
        return jar;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] pedaco = new byte[8192];
        int lido;
        while ((lido = in.read(pedaco)) > 0) {
            out.write(pedaco, 0, lido);
        }
    }

    private static boolean haDispositivo() {
        try {
            Clip c = AudioSystem.getClip();
            c.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}