import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Gera as resolutions menores e o {@code logo.ico} a partir do mestre
 * {@code logo-256.png}.
 *
 * <p>Uso: {@code java tools/GerarLogo.java [diretorio-de-recursos]}
 *
 * <p>Sem dependência externa: o JDK não tem writer de ICO no {@code ImageIO},
 * então o {@code .ico} é montado na mão (entradas BMP com máscara AND até 48px,
 * PNG comprimido acima disso). O {@code .ico} é little-endian, e os inteiros são
 * escritos byte a byte por causa disso.
 *
 * <p>Diferença em relação à versão do Pong: aqui a máscara AND é **de verdade**.
 * A versão do Pong escrevia a máscara zerada, o que é correto para uma imagem
 * opaca — mas o bit 1 da máscara significa *transparente*, então zerar tudo
 * declara a imagem inteira opaca. Como o logo do Snake tem cantos transparentes,
 * a máscara é montada pixel a pixel, com o bit 1 onde o alpha fica abaixo de
 * {@link #LIMIAR_ALFA}.
 */
public class GerarLogo {

    private static final int[] TAMANHOS = {16, 24, 32, 48, 64, 128, 256};
    private static final int MAIOR_RESOLUCAO_BMP = 48;
    private static final int CABECALHO_BMP = 40;
    private static final int TAMANHO_ENTRADA_ICO = 16;
    private static final int LIMIAR_ALFA = 128;

    public static void main(String[] args) throws IOException {
        Path recursos = Path.of(args.length > 0 ? args[0] : "src/main/resources");
        Path mestre = recursos.resolve("logo-" + TAMANHOS[TAMANHOS.length - 1] + ".png");

        BufferedImage original = ImageIO.read(mestre.toFile());
        if (original == null) {
            throw new IOException("Nao consegui ler a imagem mestre: " + mestre);
        }
        if (original.getWidth() != original.getHeight()) {
            throw new IOException("A imagem mestre precisa ser quadrada, veio "
                    + original.getWidth() + "x" + original.getHeight() + ": " + mestre);
        }

        System.out.println("Mestre: " + mestre + " (" + original.getWidth() + "x" + original.getHeight() + ")");

        List<BufferedImage> imagens = new ArrayList<>();
        for (int lado : TAMANHOS) {
            boolean ehMestre = lado == original.getWidth();
            BufferedImage imagem = ehMestre ? original : reduzir(original, lado);
            if (!ehMestre) {
                Path png = recursos.resolve("logo-" + lado + ".png");
                if (!ImageIO.write(imagem, "png", png.toFile())) {
                    throw new IOException("ImageIO nao tem writer de PNG");
                }
                System.out.println("  PNG  " + png + "  " + lado + "x" + lado
                        + "  " + Files.size(png) + " bytes");
            } else {
                System.out.println("  PNG  " + mestre + "  preservado (e o mestre)");
            }
            imagens.add(imagem);
        }

        Path ico = recursos.resolve("logo.ico");
        escreverIco(ico, imagens);
        System.out.println("  ICO  " + ico + "  " + Files.size(ico) + " bytes  ("
                + imagens.size() + " resolucoes)");
    }

    private static BufferedImage reduzir(BufferedImage origem, int lado) {
        BufferedImage atual = origem;
        int ladoAtual = atual.getWidth();
        while (ladoAtual / 2 >= lado) {
            ladoAtual = Math.max(lado, ladoAtual / 2);
            atual = desenhar(atual, ladoAtual);
        }
        return ladoAtual == lado ? atual : desenhar(atual, lado);
    }

    private static BufferedImage desenhar(BufferedImage origem, int lado) {
        BufferedImage destino = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        Graphics2D grafico = destino.createGraphics();
        grafico.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        grafico.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        grafico.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        grafico.drawImage(origem, 0, 0, lado, lado, null);
        grafico.dispose();
        return destino;
    }

    private static void escreverIco(Path destino, List<BufferedImage> imagens) throws IOException {
        List<byte[]> dados = new ArrayList<>();
        for (BufferedImage imagem : imagens) {
            dados.add(imagem.getWidth() <= MAIOR_RESOLUCAO_BMP ? bitmapDe(imagem) : pngDe(imagem));
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream saida = new DataOutputStream(buffer)) {
            escreverPequeno(saida, 0);
            escreverPequeno(saida, 1);
            escreverPequeno(saida, dados.size());

            int deslocamento = 6 + TAMANHO_ENTRADA_ICO * dados.size();
            for (int i = 0; i < imagens.size(); i++) {
                int lado = imagens.get(i).getWidth();
                int ladoNoCabecalho = lado >= 256 ? 0 : lado;
                saida.writeByte(ladoNoCabecalho);
                saida.writeByte(ladoNoCabecalho);
                saida.writeByte(0);
                saida.writeByte(0);
                escreverPequeno(saida, 1);
                escreverPequeno(saida, 32);
                escreverPequenoLongo(saida, dados.get(i).length);
                escreverPequenoLongo(saida, deslocamento);
                deslocamento += dados.get(i).length;
            }

            for (byte[] dado : dados) {
                saida.write(dado);
            }
        }
        Files.write(destino, buffer.toByteArray());
    }

    private static void escreverPequeno(DataOutputStream saida, int valor) throws IOException {
        saida.writeByte(valor & 0xFF);
        saida.writeByte((valor >> 8) & 0xFF);
    }

    private static void escreverPequenoLongo(DataOutputStream saida, int valor) throws IOException {
        saida.writeByte(valor & 0xFF);
        saida.writeByte((valor >> 8) & 0xFF);
        saida.writeByte((valor >> 16) & 0xFF);
        saida.writeByte((valor >> 24) & 0xFF);
    }

    private static byte[] bitmapDe(BufferedImage imagem) throws IOException {
        int lado = imagem.getWidth();
        int bytesDaLinha = lado * 4;
        int bytesDaLinhaDaMascara = ((lado + 31) / 32) * 4;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream saida = new DataOutputStream(buffer)) {
            escreverPequenoLongo(saida, CABECALHO_BMP);
            escreverPequenoLongo(saida, lado);
            escreverPequenoLongo(saida, lado * 2);
            escreverPequeno(saida, 1);
            escreverPequeno(saida, 32);
            escreverPequenoLongo(saida, 0);
            escreverPequenoLongo(saida, bytesDaLinha * lado);
            escreverPequenoLongo(saida, 0);
            escreverPequenoLongo(saida, 0);
            escreverPequenoLongo(saida, 0);
            escreverPequenoLongo(saida, 0);

            for (int y = lado - 1; y >= 0; y--) {
                for (int x = 0; x < lado; x++) {
                    int argb = imagem.getRGB(x, y);
                    saida.writeByte(argb & 0xFF);
                    saida.writeByte((argb >> 8) & 0xFF);
                    saida.writeByte((argb >> 16) & 0xFF);
                    saida.writeByte((argb >> 24) & 0xFF);
                }
            }

            // A máscara AND: um bit por pixel, 1 = transparente. Ela é BMP, e
            // BMP é de baixo para cima — as linhas de cima para baixo alinhariam
            // a transparência na diagonal oposta, o que não aparece em nenhum
            // log. Os bytes que sobram na linha vão zerados para o arquivo não
            // desalinhar.
            for (int y = lado - 1; y >= 0; y--) {
                for (int b = 0; b < bytesDaLinhaDaMascara; b++) {
                    int mascara = 0;
                    for (int bit = 0; bit < 8; bit++) {
                        int x = b * 8 + bit;
                        if (x >= lado) {
                            break;
                        }
                        int alfa = imagem.getRGB(x, y) >>> 24;
                        if (alfa < LIMIAR_ALFA) {
                            mascara |= 1 << (7 - bit);
                        }
                    }
                    saida.writeByte(mascara);
                }
            }
        }
        return buffer.toByteArray();
    }

    private static byte[] pngDe(BufferedImage imagem) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        if (!ImageIO.write(imagem, "png", buffer)) {
            throw new IOException("ImageIO nao tem writer de PNG");
        }
        return buffer.toByteArray();
    }
}
