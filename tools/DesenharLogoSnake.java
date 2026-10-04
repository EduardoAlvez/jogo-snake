import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Desenha o logo mestre do Jogo Snake, o {@code logo-256.png}.
 *
 * <p>O mestre é gerado por código em vez de feito à mão por dois motivos.
 * O primeiro é reproducibility: um PNG binário que ninguém sabe refazer é
 * defeito esperando — foi exatamente o que aconteceu no Pong, e é o que trava
 * este projeto hoje. O segundo é que a composição sai da paleta que o jogo já
 * usa, então o ícone é literalmente um frame do jogo e não uma cobra genérica.
 *
 * <p>As cores vêm de {@code DesenhoJogo}: fundo {@code 0x0B0E13 -> 0x11161F},
 * corpo alternando {@code COBRA_B}/{@code COBRA_A} por segmento, cabeça
 * {@code COBRA_CABECA} com olhos {@code 0x0C1014}, e a comida {@code COMIDA}
 * com halo e brilho, como em {@code desenharComida}.
 *
 * <p>Uso: {@code java tools/DesenharLogoSnake.java}
 */
public class DesenharLogoSnake {

    private static final int LADO = 256;
    private static final int MARGEM = 6;
    private static final int RAIO_CANTO = 52;

    private static final Color FUNDO_TOPO = new Color(0x0B0E13);
    private static final Color FUNDO_BASE = new Color(0x11161F);
    private static final Color COBRA_A = new Color(0x2BA85F);
    private static final Color COBRA_B = new Color(0x3DDC84);
    private static final Color COBRA_CABECA = new Color(0x7CF0AC);
    private static final Color OLHO = new Color(0x0C1014);
    private static final Color COMIDA = new Color(0xFF6B6B);

    private static final double CORPO_NA_CABECA = 21.0;
    private static final double CORPO_NA_CAuda = 13.0;
    private static final double CABECA_RAIO = 23.0;
    private static final double COMIDA_RAIO = 14.0;

    private static final int AMOSTRAS = 96;

    public static void main(String[] args) throws IOException {
        Path recursos = Path.of(args.length > 0 ? args[0] : "src/main/resources");
        BufferedImage logo = desenhar();

        Path destino = recursos.resolve("logo-" + LADO + ".png");
        if (!ImageIO.write(logo, "png", destino.toFile())) {
            throw new IOException("ImageIO nao tem writer de PNG");
        }
        System.out.println("Mestre: " + destino + "  " + LADO + "x" + LADO
                + "  " + Files.size(destino) + " bytes");
    }

    private static BufferedImage desenhar() {
        BufferedImage imagem = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = imagem.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Fundo: cantos fora do retângulo arredondado ficam com alpha 0, e é isso
        // que o GerarLogo precisa para conseguir escrever a máscara AND do .ico.
        g2.setPaint(new GradientPaint(0, MARGEM, FUNDO_TOPO, 0, LADO - MARGEM, FUNDO_BASE));
        g2.fill(new RoundRectangle2D.Double(MARGEM, MARGEM,
                LADO - 2 * MARGEM, LADO - 2 * MARGEM, RAIO_CANTO, RAIO_CANTO));

        desenharComida(g2);
        desenharCobra(g2);

        g2.dispose();
        return imagem;
    }

    /** A cobra em S, do canto superior esquerdo para o inferior direito. */
    private static void desenharCobra(Graphics2D g2) {
        Point2D inicio = new Point2D.Double(76, 80);
        Point2D controle1 = new Point2D.Double(168, 80);
        Point2D controle2 = new Point2D.Double(88, 176);
        Point2D fim = new Point2D.Double(180, 176);

        // O corpo é uma sequência de círculos alternando as duas cores, que é
        // como o jogo pinta segmento a segmento. Uma linha contínua não daria
        // o xadrez do corpo.
        for (int i = 0; i <= AMOSTRAS; i++) {
            double t = (double) i / AMOSTRAS;
            Point2D p = bezier(inicio, controle1, controle2, fim, t);
            double raio = CORPO_NA_CABECA + (CORPO_NA_CAuda - CORPO_NA_CABECA) * t;
            g2.setColor((i % 2 == 0) ? COBRA_B : COBRA_A);
            g2.fill(new Ellipse2D.Double(p.getX() - raio, p.getY() - raio, raio * 2, raio * 2));
        }

        // A cabeça fica na origem da curva, que aponta para +x, então os olhos
        // se separam em y e avançam em x — como em desenharOlhos.
        g2.setColor(COBRA_CABECA);
        g2.fill(new Ellipse2D.Double(inicio.getX() - CABECA_RAIO, inicio.getY() - CABECA_RAIO,
                CABECA_RAIO * 2, CABECA_RAIO * 2));

        double raioOlho = 4.0;
        double separacao = 8.0;
        double avanco = 8.0;
        g2.setColor(OLHO);
        g2.fill(new Ellipse2D.Double(inicio.getX() + avanco - raioOlho,
                inicio.getY() + separacao - raioOlho, raioOlho * 2, raioOlho * 2));
        g2.fill(new Ellipse2D.Double(inicio.getX() + avanco - raioOlho,
                inicio.getY() - separacao - raioOlho, raioOlho * 2, raioOlho * 2));

        // Contorno fino para a cabeça não sumir no corpo em 16px.
        g2.setColor(new Color(COBRA_CABECA.getRed(), COBRA_CABECA.getGreen(),
                COBRA_CABECA.getBlue(), 160));
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(new Ellipse2D.Double(inicio.getX() - CABECA_RAIO, inicio.getY() - CABECA_RAIO,
                CABECA_RAIO * 2, CABECA_RAIO * 2));
    }

    /** A comida no canto superior direito: halo, corpo e brilho, como no jogo. */
    private static void desenharComida(Graphics2D g2) {
        double x = 200;
        double y = 96;

        g2.setColor(new Color(COMIDA.getRed(), COMIDA.getGreen(), COMIDA.getBlue(), 60));
        g2.fill(new Ellipse2D.Double(x - COMIDA_RAIO * 1.5, y - COMIDA_RAIO * 1.5,
                COMIDA_RAIO * 3, COMIDA_RAIO * 3));

        g2.setColor(COMIDA);
        g2.fill(new Ellipse2D.Double(x - COMIDA_RAIO, y - COMIDA_RAIO,
                COMIDA_RAIO * 2, COMIDA_RAIO * 2));

        double brilho = COMIDA_RAIO * 0.26;
        g2.setColor(new Color(255, 255, 255, 190));
        g2.fill(new Ellipse2D.Double(x + COMIDA_RAIO * 0.22 - brilho, y + COMIDA_RAIO * 0.2 - brilho,
                brilho * 2, brilho * 2));
    }

    /** Cubica de Bezier avaliada direto, para amostrar a curva sem PathIterator. */
    private static Point2D bezier(Point2D p0, Point2D p1, Point2D p2, Point2D p3, double t) {
        double u = 1 - t;
        double a = u * u * u;
        double b = 3 * u * u * t;
        double c = 3 * u * t * t;
        double d = t * t * t;
        return new Point2D.Double(
                a * p0.getX() + b * p1.getX() + c * p2.getX() + d * p3.getX(),
                a * p0.getY() + b * p1.getY() + c * p2.getY() + d * p3.getY());
    }
}
