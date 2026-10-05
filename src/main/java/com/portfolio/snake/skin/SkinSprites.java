package com.portfolio.snake.skin;

import com.portfolio.snake.core.Forma;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Os PNGs de uma skin, um por {@code Forma}.
 *
 * <p>Só carrega. Nenhuma regra de desenho mora aqui: quem decide o que fazer com
 * um PNG ausente, e se os olhos ainda são desenhados por código, é o
 * {@code DesenhoJogo}.
 *
 * <p><b>Por que o nome do arquivo é o do enum.</b> O arquivo se chama
 * {@code CABECA_DIREITA.png} e é resolvido por {@link Forma#valueOf}. Assim não
 * existe tabela de nomes para manter em sincronia com o enum: se o {@code Forma}
 * ganhar uma forma, o PNG correspondente passa a ser procurado sem ninguém
 * editar nada, e a forma nova simplesmente não tem arte e cai no vetor.
 *
 * <p><b>Por que a ausência é normal.</b> As três skins de paleta não têm PNG
 * nenhum e nunca vão ter. Faltar arquivo é o estado esperado, não uma falha: por
 * isso {@link #para} devolve {@code null} em vez de lançar, e quem chama decide
 * o fallback. Um PNG corrompido, esse sim, é erro de verdade e sobe.
 *
 * <p>Os PNGs são lidos uma vez, na construção. Desenhar é chamado a 60Hz e abrir
 * 14 arquivos por quadro seria o tipo de lentidão que só aparece no jogo rodando.
 */
public final class SkinSprites {

    private final String skinId;
    private final Map<Forma, BufferedImage> porForma;

    private SkinSprites(String skinId, Map<Forma, BufferedImage> porForma) {
        this.skinId = skinId;
        this.porForma = porForma;
    }

    /**
     * Carrega os PNGs de uma skin. Os que existirem ficam disponíveis; os que
     * faltarem simplesmente não estão no mapa.
     *
     * @param skinId id da skin, o mesmo que vai para o disco
     * @return os sprites carregados, possivelmente vazio
     * @throws IllegalArgumentException se o id for vazio
     */
    public static SkinSprites carregar(String skinId) {
        if (skinId == null || skinId.isBlank()) {
            throw new IllegalArgumentException("skinId nao pode ser vazio: " + skinId);
        }
        Map<Forma, BufferedImage> porForma = new EnumMap<>(Forma.class);
        for (Forma forma : Forma.values()) {
            BufferedImage img = le(comPonteiro(skinId, forma));
            if (img != null) {
                porForma.put(forma, img);
            }
        }
        return new SkinSprites(skinId, porForma);
    }

    private static InputStream comPonteiro(String skinId, Forma forma) {
        return SkinSprites.class.getResourceAsStream("/skins/" + skinId + "/" + forma.name() + ".png");
    }

    /**
     * Lê um PNG do classpath.
     *
     * <p>Um recurso ausente é {@code null} e vira {@code null}: é o caso comum.
     * Já um recurso que existe e não é imagem é defeito de empacotamento, e
     * {@code ImageIO.read} devolvendo {@code null} ali seria um furo silencioso.
     */
    private static BufferedImage le(InputStream in) {
        if (in == null) {
            return null;
        }
        try (InputStream fecha = in) {
            BufferedImage img = ImageIO.read(fecha);
            if (img == null) {
                throw new IllegalStateException("recurso de sprite existe mas nao e uma imagem");
            }
            return img;
        } catch (IOException e) {
            throw new UncheckedIOException("nao deu para ler um sprite", e);
        }
    }

    /**
     * O PNG de uma forma, ou {@code null} se esta skin não tem arte para ela.
     *
     * @param forma a forma do segmento
     * @return a imagem, ou {@code null} para desenhar vetorial
     */
    public BufferedImage para(Forma forma) {
        Objects.requireNonNull(forma, "forma");
        return porForma.get(forma);
    }

    /** {@code true} se esta skin tem pelo menos um PNG. */
    public boolean temAlgum() {
        return !porForma.isEmpty();
    }

    /**
     * {@code true} se esta skin tem PNG para todas as formas.
     *
     * <p>Uma skin pela metade é o caso perigoso: a cobra sai com dedo no corpo e
     * quadrado na cauda, e parece defeito do desenho em vez de arte faltando.
     * Quem chama pode assim avisar antes de o jogador ver.
     */
    public boolean cobreTodasAsFormas() {
        return porForma.size() == Forma.values().length;
    }

    /** Id da skin destes sprites. */
    public String getSkinId() {
        return skinId;
    }

    /** Quantas formas têm arte. */
    public int quantasFormas() {
        return porForma.size();
    }

    @Override
    public String toString() {
        return skinId + " (" + porForma.size() + "/" + Forma.values().length + " formas)";
    }
}