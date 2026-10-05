import com.portfolio.snake.core.Forma;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gera os 14 PNGs do Dedinho a partir de <b>quatro</b> imagens, uma por forma
 * canônica, e tira as outras doze girando.
 *
 * <p><b>Por que quatro e não quatorze.</b> A primeira tentativa pediu uma folha
 * 7x2 com as 14 peças. A folha veio com 14 ilustrações <i>sem relação entre
 * si</i>: dentro de cada grupo (cantos, rabos, cabeças) o melhor casamento entre
 * duas peças foi de 44% a 66%, e cerca de metade de cada PNG é fundo
 * transparente — ou seja, os desenhos opacos praticamente não se pareciam. A
 * arte invadia as fronteiras das células (a cobertura de alfa é contínua nos
 * limites da grade), então 12 das 14 peças saíram do tamanho inteiro da célula.
 * Girar não corrigiria aquilo, porque não eram a mesma forma em quatro
 * orientações: eram formas diferentes.
 *
 * <p>Pedir uma peça por imagem é o que o gerador cumpre bem. E se cada grupo
 * vem de <b>uma</b> imagem, as outras três saem giradas <b>em código</b> — então
 * a simetria deixa de ser um pedido ao gerador e vira consequência da construção.
 * Simetria perfeita não é sorte: é aritmética de rotação.
 *
 * <p><b>Por que os ângulos são descobertos, não escritos.</b> Uma tabela
 * "canônico 0°, 90°, 180°, 270°" escrita à mão é exatamente o tipo de coisa que
 * sai transposta sem ninguém perceber: quatro peças viradas e o defeito só
 * aparece jogando. Então a ferramenta traduz cada {@link Forma} como um conjunto de
 * lados que ela ocupa, gira o conjunto, e procura qual giro leva o canônico até
 * o alvo. Se a semântica do {@code Forma} mudar, a tabela acompanha; se alguém
 * escrever o ângulo errado, o código nem compila.
 *
 * <p><b>Por que a arte sai sem ser quadrada.</b> {@code DesenhoJogo} desenha o sprite
 * com {@code drawImage} preenchendo o quadrado da célula, então cada PNG é
 * esticado até o quadrado de qualquer jeito. Centralizar num quadrado aqui só
 * acrescentaria uma etapa; o que importa é que a arte esteja <b>recortada na
 * caixa</b>, para a largura e a altura serem as da arte e não as da imagem
 * original.
 *
 * <p><b>O verde.</b> Mesmo argumento da folha: o gerador não entrega transparência
 * confiável, e o verde chapado é um fundo que ele cumpre. Duas passadas removem
 * o fundo — a segunda por <i>dominância</i>, e não por proximidade, que é o que
 * pega a franja e as manchas pintadas dentro do dedo. Zero verde é o esperado.
 *
 * <p>Uso:
 * <pre>
 * java tools/RecortarDedinho.java assets/dedinho src/main/resources/skins/dedinho
 * </pre>
 */
public final class RecortarDedinho {

    /** Nomes dos arquivos de entrada, dentro da pasta de origem. */
    private static final String ARQ_CORPO = "corpo.jpg";
    private static final String ARQ_COTOVELO = "cotovelo.jpg";
    private static final String ARQ_CABECA = "cabeca.jpg";
    private static final String ARQ_RABO = "rabo.jpg";

    /**
     * Tolerância do corte, por canal.
     *
     * <p>Não pode ser zero: a variação do verde entre imagens é grande (as quatro
     * fontes vieram entre {@code rgb(38,235,24)} e {@code rgb(55,226,34)}), e o
     * recorte suaviza a borda da arte.
     */
    private static final int TOLERANCIA = 70;

    /**
     * Margem para considerar "verde" na segunda passada.
     *
     * <p>Por dominância, não por proximidade. Todas as cores do Dedinho têm o
     * vermelho na frente — pele {@code rgb(232,176,138)}, unha
     * {@code rgb(247,217,200)}, contorno {@code rgb(58,36,24)} — então nenhuma
     * entra por esta regra. O jeito de saber que a conta errou é a pele virar
     * buraco.
     */
    private static final int MARGEM_VERDE = 20;

    /** Critério do relatório: verde claramente remanescente. */
    private static final int VERDE_SOBROU = 40;

    private RecortarDedinho() {
    }

    // ------------------------------------------------------------------
    // Lado da célula: a semântica que gira
    // ------------------------------------------------------------------

    /** Os quatro lados de uma célula, na ordem do giro horário. */
    private enum Lado {
        /** Topo. */
        T,
        /** Direita. */
        R,
        /** Base. */
        B,
        /** Esquerda. */
        L;

        /** Gira 90 graus no sentido horário, repetidas vezes. */
        private Lado gira(int quartos) {
            Lado atual = this;
            for (int i = 0; i < quartos; i++) {
                switch (atual) {
                    case T: atual = R; break;
                    case R: atual = B; break;
                    case B: atual = L; break;
                    default: atual = T; break;
                }
            }
            return atual;
        }
    }

    /**
     * Quais lados a forma ocupa.
     *
     * <p>Esta é a tabela que substitui os ângulos escritos à mão. Ela traduz o
     * nome de cada {@link Forma} para geometria, seguindo o {@code Forma.de}:
     * o canto é nomeado pela perna vertical e pela perna horizontal, a cabeça e o
     * rabo pela direção da ponta.
     *
     * <p>Cabeça e rabo compartilham a mesma geometria para uma mesma direção — as
     * duas se ligam pelo lado oposto à ponta. Elas se distinguem pelo desenho (a
     * cabeça tem unha, o rabo não tem), e como vêm de arquivos diferentes isso
     * nunca vira ambiguidade: a busca só roda dentro de um grupo.
     */
    private static Set<Lado> lados(Forma forma) {
        switch (forma) {
            case RETO_H: return EnumSet.of(Lado.L, Lado.R);
            case RETO_V: return EnumSet.of(Lado.T, Lado.B);
            case CANTO_SE: return EnumSet.of(Lado.B, Lado.R);
            case CANTO_SO: return EnumSet.of(Lado.B, Lado.L);
            case CANTO_NE: return EnumSet.of(Lado.T, Lado.R);
            case CANTO_NO: return EnumSet.of(Lado.T, Lado.L);
            case CABECA_CIMA:
            case RABO_CIMA: return EnumSet.of(Lado.T);
            case CABECA_DIREITA:
            case RABO_DIREITA: return EnumSet.of(Lado.R);
            case CABECA_BAIXO:
            case RABO_BAIXO: return EnumSet.of(Lado.B);
            case CABECA_ESQUERDA:
            case RABO_ESQUERDA: return EnumSet.of(Lado.L);
            default: throw new IllegalArgumentException("forma sem lados: " + forma);
        }
    }

    private static Set<Lado> gira(Set<Lado> original, int quartos) {
        Set<Lado> saida = EnumSet.noneOf(Lado.class);
        for (Lado lado : original) {
            saida.add(lado.gira(quartos));
        }
        return saida;
    }

    /**
     * Descobre quantos quartos de giro levam o canônico até o alvo.
     *
     * @return o giro, ou {@code -1} se o alvo não está no mesmo grupo
     */
    private static int quartosPara(Forma canonico, Forma alvo) {
        Set<Lado> base = lados(canonico);
        for (int q = 0; q < 4; q++) {
            if (gira(base, q).equals(lados(alvo))) {
                return q;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // main
    // ------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("uso: java RecortarDedinho.java <pasta-de-origem> <pasta-de-saida>");
            System.exit(2);
        }
        Path origem = Paths.get(args[0]);
        Path saida = Paths.get(args[1]);
        Files.createDirectories(saida);

        System.out.println("4 imagens canonicas -> 14 PNGs por rotacao em codigo");
        System.out.println("  " + ARQ_CORPO + "     -> RETO_H, RETO_V");
        System.out.println("  " + ARQ_COTOVELO + "  -> CANTO_*, base CANTO_NO");
        System.out.println("  " + ARQ_CABECA + "   -> CABECA_*, base CABECA_DIREITA");
        System.out.println("  " + ARQ_RABO + "     -> RABO_*, base RABO_CIMA");
        System.out.println();

        processa(origem.resolve(ARQ_CORPO), saida,
                Forma.RETO_H, List.of(Forma.RETO_H, Forma.RETO_V));
        processa(origem.resolve(ARQ_COTOVELO), saida,
                Forma.CANTO_NO,
                List.of(Forma.CANTO_NO, Forma.CANTO_NE, Forma.CANTO_SE, Forma.CANTO_SO));
        processa(origem.resolve(ARQ_CABECA), saida,
                Forma.CABECA_DIREITA,
                List.of(Forma.CABECA_DIREITA, Forma.CABECA_BAIXO,
                        Forma.CABECA_ESQUERDA, Forma.CABECA_CIMA));
        processa(origem.resolve(ARQ_RABO), saida,
                Forma.RABO_CIMA,
                List.of(Forma.RABO_CIMA, Forma.RABO_DIREITA,
                        Forma.RABO_BAIXO, Forma.RABO_ESQUERDA));
    }

    /**
     * Recorta, tira o verde e grava o grupo inteiro girando a partir do canônico.
     *
     * <p>A base medida de cada grupo está escrita na chamada: ela não é o que o
     * prompt pediu, é o que a imagem veio. Isso importa — o cotovelo foi pedido
     * "entra pelo topo e sai pela direita", que é {@code CANTO_NE}, e veio
     * espelhado, com o entalhe olhando para a base-direita e as pernas para cima e
     * para a esquerda, que é {@code CANTO_NO}. Chamar a base de {@code CANTO_NE}
     * teria gravado os quatro cantos virados.
     */
    private static void processa(Path arquivo, Path saida, Forma canonico, List<Forma> grupo)
            throws IOException {
        BufferedImage lida = ImageIO.read(arquivo.toFile());
        if (lida == null) {
            throw new IOException("nao deu para ler a imagem: " + arquivo);
        }
        BufferedImage img = paraArgb(lida);
        int[] fundo = estimaFundo(img);
        int[] transparentes = new int[1];
        int[] caixa = tiraFundoECaixa(img, fundo, transparentes);

        int areaTotal = img.getWidth() * img.getHeight();
        System.out.println(arquivo.getFileName() + "  " + img.getWidth() + "x" + img.getHeight()
                + "  fundo rgb(" + fundo[0] + ", " + fundo[1] + ", " + fundo[2] + ")"
                + "  transparentes " + transparentes[0] + " ("
                + String.format("%.1f", transparentes[0] * 100.0 / areaTotal) + "%)");
        if (caixa == null || transparentes[0] == 0) {
            throw new IllegalStateException(arquivo.getFileName()
                    + ": nada ficou transparente. A imagem nao tem fundo removivel, ou o"
                    + " recorte rodou sem canal alfa. Nao grava os PNGs: assim eles saem"
                    + " com um retangulo preto em volta da arte.");
        }

        int largura = caixa[2] - caixa[0] + 1;
        int altura = caixa[3] - caixa[1] + 1;
        System.out.println("  arte " + largura + "x" + altura
                + (largura > altura ? " (deitada)" : " (em pe)")
                + "   base medida: " + canonico);
        System.out.println("  " + grupo.get(0) + " 0" + "  ->  "
                + grupo.size() + " pecas por giro de 90 graus");

        for (Forma alvo : grupo) {
            int quartos = quartosPara(canonico, alvo);
            if (quartos < 0) {
                throw new IllegalStateException(alvo + " nao sai girando " + canonico
                        + ": as formas nao estao no mesmo grupo de rotacao");
            }
            BufferedImage png = rotaciona(recorta(img, caixa), quartos);
            ImageIO.write(png, "png", saida.resolve(alvo.name() + ".png").toFile());

            int w = png.getWidth();
            int h = png.getHeight();
            int sobrou = contaVerdeForte(png);
            String alerta = sobrou > 0 ? "  VERDE SOBROU: " + sobrou : "";
            System.out.println("    " + alvo.name()
                    + "  " + w + "x" + h
                    + "  " + String.format("giro %3d graus", quartos * 90)
                    + String.format("  espessura %3d px  proporcao %.2f", menor(w, h), (double) w / h)
                    + alerta);
        }
        System.out.println();
    }

    private static int menor(int a, int b) {
        return a < b ? a : b;
    }

    // ------------------------------------------------------------------
    // recorte e verde
    // ------------------------------------------------------------------

    /**
     * Copia a imagem para ARGB, que é o único tipo onde {@code setRGB} guarda
     * transparência.
     *
     * <p><b>Este é o bug que quase passou.</b> As quatro fontes são JPEG, e
     * {@code ImageIO.read} devolve JPEG como {@code TYPE_3BYTE_BGR}, <b>sem canal
     * alfa</b>. Numa imagem sem alfa, {@code setRGB(x, y, 0x00000000)} não grava
     * "transparente": grava <b>preto</b>. O resultado era um PNG 100% opaco com um
     * retângulo preto em volta do dedo, e ele passava despercebido por dois
     * testes: a caixa da arte saía certa (porque o preto é lido como arte, não
     * como fundo) e a contagem de verde dava zero (porque preto não é verde).
     * Só a inspeção do alfa revelou.
     *
     * <p>Por isso {@link #tiraFundoECaixa} agora conta quantos pixels ficaram
     * transparentes e o {@link #main} recusa continuar se esse número for zero.
     */
    private static BufferedImage paraArgb(BufferedImage lida) {
        if (lida.getType() == BufferedImage.TYPE_INT_ARGB) {
            return lida;
        }
        int w = lida.getWidth();
        int h = lida.getHeight();
        BufferedImage saida = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                saida.setRGB(x, y, 0xFF000000 | (lida.getRGB(x, y) & 0xFFFFFF));
            }
        }
        return saida;
    }

    /**
     * Descobre a cor real do fundo medindo a borda da imagem.
     *
     * <p>As quatro fontes vieram com fundos diferentes — {@code rgb(38,235,24)},
     * {@code rgb(41,235,25)}, {@code rgb(54,225,35)}, {@code rgb(55,226,34)} —
     * e nenhum é o {@code #00FF00} que o prompt pede. Com o valor do prompt
     * travado nenhuma amostra bateria e cada peça saía com dois mil pixels de
     * franja. O fundo é o que domina a borda, então a cor mais comum ali é ele.
     */
    private static int[] estimaFundo(BufferedImage img) {
        Map<Integer, List<int[]>> baldes = new HashMap<>();
        int w = img.getWidth();
        int h = img.getHeight();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < 2; y++) {
                guarda(baldes, img.getRGB(x, y));
                guarda(baldes, img.getRGB(x, h - 1 - y));
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < 2; x++) {
                guarda(baldes, img.getRGB(x, y));
                guarda(baldes, img.getRGB(w - 1 - x, y));
            }
        }
        List<int[]> melhor = List.of();
        for (List<int[]> c : baldes.values()) {
            if (c.size() > melhor.size()) {
                melhor = c;
            }
        }
        if (melhor.isEmpty()) {
            return new int[] { 0, 255, 0 };
        }
        int r = 0;
        int g = 0;
        int b = 0;
        for (int[] c : melhor) {
            r += c[0];
            g += c[1];
            b += c[2];
        }
        int n = melhor.size();
        return new int[] { r / n, g / n, b / n };
    }

    private static void guarda(Map<Integer, List<int[]>> baldes, int rgb) {
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;
        int chave = (r / 8) << 16 | (g / 8) << 8 | (b / 8);
        baldes.computeIfAbsent(chave, k -> new ArrayList<>()).add(new int[] { r, g, b });
    }

    /**
     * Tira o verde do fundo e devolve a caixa do que sobrou.
     *
     * <p>A imagem vem alterada: o fundo vira transparente no lugar. A caixa é
     * calculada na mesma passada, senão teria de percorrer a imagem duas vezes.
     *
     * @return {@code null} se não sobrou arte
     */
    private static int[] tiraFundoECaixa(BufferedImage img, int[] fundo) {
        return tiraFundoECaixa(img, fundo, new int[1]);
    }

    private static int[] tiraFundoECaixa(BufferedImage img, int[] fundo, int[] transparentes) {
        int minX = img.getWidth();
        int minY = img.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                if (eFundo(argb, fundo) || alfaDe(argb) < 8 || eVerdeDominante(argb)) {
                    img.setRGB(x, y, 0x00000000);
                    transparentes[0]++;
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        return maxX < 0 ? null : new int[] { minX, minY, maxX, maxY };
    }

    /** O verde manda sobre os outros dois canais? */
    private static boolean eVerdeDominante(int argb) {
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return g > r + MARGEM_VERDE && g > b + MARGEM_VERDE;
    }

    private static int alfaDe(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    /** O pixel é a cor de fundo medida? */
    private static boolean eFundo(int argb, int[] fundo) {
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return Math.abs(r - fundo[0]) <= TOLERANCIA
                && Math.abs(g - fundo[1]) <= TOLERANCIA
                && Math.abs(b - fundo[2]) <= TOLERANCIA;
    }

    /** Conta o verde que sobreviveu ao corte. */
    private static int contaVerdeForte(BufferedImage img) {
        int n = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                if (alfaDe(argb) < 8) {
                    continue;
                }
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                if (g > r + VERDE_SOBROU && g > b + VERDE_SOBROU) {
                    n++;
                }
            }
        }
        return n;
    }

    // ------------------------------------------------------------------
    // recorte e giro
    // ------------------------------------------------------------------

    /** Copia a caixa da arte para uma imagem nova, já em ARGB. */
    private static BufferedImage recorta(BufferedImage img, int[] caixa) {
        int largura = caixa[2] - caixa[0] + 1;
        int altura = caixa[3] - caixa[1] + 1;
        BufferedImage saida = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                saida.setRGB(x, y, img.getRGB(caixa[0] + x, caixa[1] + y));
            }
        }
        return saida;
    }

    /**
     * Gira no sentido horário, em quartos de volta.
     *
     * <p>Um giro de 90 troca largura por altura, então o tamanho da imagem muda.
     * É por isso que a rotação acontece sobre a arte <b>recortada</b> e não sobre
     * o PNG final: girar a imagem inteira levaria o descarte das bordas junto e as
     * peças não casariam entre si.
     */
    private static BufferedImage rotaciona(BufferedImage src, int quartos) {
        int w = src.getWidth();
        int h = src.getHeight();
        boolean troca = quartos % 2 == 1;
        BufferedImage saida = new BufferedImage(troca ? h : w, troca ? w : h,
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int px = src.getRGB(x, y);
                switch (quartos) {
                    case 0: saida.setRGB(x, y, px); break;
                    case 1: saida.setRGB(h - 1 - y, x, px); break;
                    case 2: saida.setRGB(w - 1 - x, h - 1 - y, px); break;
                    default: saida.setRGB(y, w - 1 - x, px); break;
                }
            }
        }
        return saida;
    }
}