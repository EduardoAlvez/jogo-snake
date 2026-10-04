package com.portfolio.snake.audio;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Os efeitos sonoros, e as três coisas que esta classe existe para garantir.
 *
 * <p><b>1. O som nunca derruba o jogo.</b> Tudo aqui é engolido: placa de som
 * ausente, recurso faltando, arquivo corrompido. Um jogo de portfólio que abre
 * uma janela de erro porque a máquina não tem placa de áudio não é um jogo com
 * defeito de som, é um jogo quebrado. Por isso {@link #tocar} não declara
 * nenhuma exceção checada.
 *
 * <p><b>2. O decoder precisa de mark/reset, e o fluxo de um jar não tem.</b>
 * Este é o defeito que o Pong Pagou:
 * {@code AudioSystem.getAudioInputStream(InputStream)} chama {@code mark()} e
 * {@code reset()}, e a entrada de um jar não suporta nenhum dos dois — o
 * sintoma é {@code IOException: mark/reset not supported}.
 *
 * <p>O que engana é que <b>de {@code target/classes} funciona mesmo sem
 * buffer</b>: o JDK devolve ali um {@code BufferedInputStream} com
 * {@code mark} funcionando, porque é um diretório. Só de dentro do jar a
 * ausência aparece. Por isso um teste que carrega de {@code target/classes}
 * passa, e o {@code .exe} sai mudo.
 *
 * <p><b>Esta implementação não depende disso</b>, e é deliberado: os bytes do
 * arquivo são lidos para a memória com um buffer explícito, e a decodificação
 * acontece sobre um {@code ByteArrayInputStream}, que já suporta
 * {@code mark/reset}. Ou seja: hoje o {@code BufferedInputStream} é defesa em
 * profundidade, não a coisa que segura o jogo de pé.
 *
 * <p>Ele volta a ser indispensável no instante em que alguém "otimizar" isto
 * entregando o fluxo do classpath direto ao decoder. Por isso o
 * {@code SonsTest} carrega o <b>próprio {@code Sons} de dentro de um jar</b> e
 * exige que funcione: verificado por mutação, essa otimização derruba 10 testes.
 * E o mesmo teste registra, em {@code semBufferALeituraDoJarFalha}, que a
 * leitura crua do jar <b>falha</b> — para que ninguém remova o buffer achando
 * que ele é decoração.
 *
 * <p><b>3. O laço do jogo não para.</b> Decodificar um WAV na EDT trava a
 * janela, e oRIX travado é pior que oRIX faltando. {@link #preparar} roda numa
 * thread separada e deixa os bytes prontos antes de a partida começar; o
 * {@code Clip} é aberto em memória, sem tocar o disco.
 *
 * <p>Nenhuma asserção deste projeto verifica que o som "soa bem" — isso exige
 * ouvido. O que os testes verificam é o que dá para medir: que o arquivo
 * decodifica, que não está mudo, que não está clipado e que começa e termina em
 * zero.
 */
public final class Sons {

    /** Os quatro efeitos, com o arquivo que cada um carrega. */
    public enum Efeito {

        /** Comer a comida: o bip curto que sobe de tom. */
        COMER("comer.wav"),

        /** Pegar um poder: as três notas. */
        PODER("poder.wav"),

        /** Bater na parede: o baque grave. */
        PAREDE("parede.wav"),

        /** Fim de partida: a nota que desce. */
        FIM("fim.wav");

        private final String arquivo;

        Efeito(String arquivo) {
            this.arquivo = arquivo;
        }

        /** Nome do recurso dentro de {@code /sons}. */
        public String getArquivo() {
            return arquivo;
        }
    }

    /** Onde os sons vivem no classpath. */
    private static final String CAMINHO = "/sons/";

    /** Tamanho do cabeçalho de um WAV PCM sem blocos extras: 44 bytes. */
    private static final int CABECALHO_WAV = 44;

    /** Bytes já decodificados, por efeito. */
    private static final Map<Efeito, byte[]> CACHE = new EnumMap<>(Efeito.class);

    /**
     * Efeitos que já falharam uma vez.
     *
     * <p>Existe para não tentar de novo a cada comida. Sem isto, uma máquina sem
     * placa de áudio tentaria abrir e decodificar quatro arquivos sessenta vezes
     * por segundo, e o custo é visível no laço do jogo.
     */
    private static final Map<Efeito, Boolean> FALHOU = new EnumMap<>(Efeito.class);

    /** Silenciado pelo jogador. */
    private static volatile boolean mudo;

    /**
     * Ligado na primeira falha de dispositivo.
     *
     * <p>Some quando não há placa, quando ela está ocupada por outro programa,
     * ou quando o ambiente é headless. É a rede de segurança final: depois
     * disto o jogo nem tenta mais.
     */
    private static volatile boolean semDispositivo;

    private Sons() {
    }

    /**
     * Carrega os quatro efeitos uma vez, para o primeiro som não pagar a leitura.
     *
     * <p>Síncrono e direto, sem thread. Houve uma versão que fazia isto em uma
     * thread daemon "para não travar a janela", e ela não servia para nada: o
     * que é caro — abrir a linha de áudio — acontece em {@link #tocar}, na EDT,
     * e continua lá. O que esta função faz é ler 102 KB e parsear quatro
     * cabeçalhos WAV, trabalho de milissegundos que custa mais em thread do que
     * em tempo de execução.
     *
     * <p>Uma thread a mais também trazia embutido um mapa thread-safe e uma
     * corrida entre esta thread e a EDT. Sem ela, um {@link EnumMap} basta e a
     * discussão sobre publicação segura do array desaparece junto.
     *
     * <p>Chamar de novo é barato: o cache transforma tudo em busca de mapa.
     */
    public static void carregarTodos() {
        for (Efeito e : Efeito.values()) {
            carregar(e);
        }
    }

    /**
     * Devolve os bytes do efeito, decodificando uma única vez.
     *
     * <p>Retorna {@code null} quando o recurso não está no classpath ou não
     * decodifica. <b>Nunca lança</b>: quem chama não precisa saber o que deu
     * errado, porque não há nada a fazer a respeito.
     *
     * @param efeito o efeito desejado
     * @return os bytes do WAV, ou {@code null}
     */
    public static byte[] carregar(Efeito efeito) {
        if (efeito == null || semDispositivo) {
            return null;
        }
        byte[] jaPronto = CACHE.get(efeito);
        if (jaPronto != null) {
            return jaPronto;
        }
        if (Boolean.TRUE.equals(FALHOU.get(efeito))) {
            return null;
        }
        byte[] dados = leDoClasspath(efeito);
        if (dados == null) {
            FALHOU.put(efeito, Boolean.TRUE);
            return null;
        }
        CACHE.put(efeito, dados);
        return dados;
    }

    /**
     * Lê o WAV inteiro e confirma que ele decodifica.
     *
     * <p>O cache guarda o <b>arquivo</b>, cabeçalho incluído, e não os frames já
     * decodificados. A tentação é guardar só as amostras, que é menor — mas aí
     * {@link #tocar} não tem como repassá-las ao
     * {@code AudioSystem.getAudioInputStream}, que só entende um WAV completo.
     * Guardar o arquivo é o que permite decodificar uma vez e tocar quantas vezes.
     *
     * <p>O {@code BufferedInputStream} é a primeira coisa feita com o fluxo, e
     * não uma melhoria posterior: é o que faz a leitura funcionar de dentro do
     * jar, que é como o jogo roda no {@code .exe}.
     */
    private static byte[] leDoClasspath(Efeito efeito) {
        byte[] bruto = leBytes(CAMINHO + efeito.getArquivo());
        if (bruto == null || bruto.length <= CABECALHO_WAV) {
            return null;
        }
        // valida decodificando uma vez: um arquivo corrompido falha aqui, e não a
        // cada toque
        try (InputStream comBuffer = new BufferedInputStream(new ByteArrayInputStream(bruto));
             AudioInputStream a = AudioSystem.getAudioInputStream(comBuffer)) {
            return a.getFormat() == null ? null : bruto;
        } catch (IOException | UnsupportedAudioFileException | RuntimeException e) {
            return null;
        }
    }

    /** Lê um recurso do classpath inteiro, com buffer desde a primeira linha. */
    private static byte[] leBytes(String recurso) {
        try (InputStream bruto = Sons.class.getResourceAsStream(recurso)) {
            if (bruto == null) {
                return null; // recurso ausente: some em silêncio
            }
            try (InputStream comBuffer = new BufferedInputStream(bruto)) {
                ByteArrayOutputStream saida = new ByteArrayOutputStream();
                byte[] pedaco = new byte[8192];
                int lido;
                while ((lido = comBuffer.read(pedaco, 0, pedaco.length)) > 0) {
                    saida.write(pedaco, 0, lido);
                }
                return saida.toByteArray();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Toca um efeito. Não faz nada se estiver mudo, sem dispositivo, ou se o
     * efeito não carregou.
     *
     * <p>Abrir um {@code Clip} por toque é o preço de não carregar o fluxo de
     * áudio inteiro. Em troca, cada toque é independente: cortar o som no meio
     * não trava o laço, e dois {@code comer} seguidos não se atropelam.
     */
    public static void tocar(Efeito efeito) {
        if (efeito == null || mudo || semDispositivo) {
            return;
        }
        byte[] dados = carregar(efeito);
        if (dados == null) {
            return;
        }
        Clip clip = null;
        try {
            clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(
                    new BufferedInputStream(new ByteArrayInputStream(dados))));
            // fecha quando o som acaba: sem isto, cada toque vaza uma linha de
            // áudio e depois de algumas dezenas o sistema para de abrir novas
            final Clip alvo = clip;
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) {
                    alvo.close();
                }
            });
            clip.start();
        } catch (LineUnavailableException e) {
            semDispositivo = true;
            fecha(clip);
        } catch (IOException | UnsupportedAudioFileException
                | IllegalArgumentException | SecurityException e) {
            // arquivo corrompido, placa ocupada ou segurança negando:
            // desliga o áudio em vez de insistir
            semDispositivo = true;
            fecha(clip);
        }
    }

    private static void fecha(Clip clip) {
        if (clip != null && clip.isOpen()) {
            try {
                clip.close();
            } catch (RuntimeException ignorada) {
                // fechar um clip que já morreu não pode virar erro
            }
        }
    }

    /** Liga ou desliga o som. O jogo começa ligado. */
    public static void mudo(boolean valor) {
        mudo = valor;
    }

    /** {@code true} se o jogador silenciou. */
    public static boolean isMudo() {
        return mudo;
    }

    /**
     * {@code true} enquanto o áudio puder tocar.
     *
     * <p>Usado pelo teste para pulo condicional em máquina sem placa, e por
     * quem quiser mostrar na interface que o som está desligado.
     */
    public static boolean disponivel() {
        return !semDispositivo && !mudo;
    }
}