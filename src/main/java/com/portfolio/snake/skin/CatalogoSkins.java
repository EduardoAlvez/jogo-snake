package com.portfolio.snake.skin;

import java.awt.Color;
import java.util.List;

/**
 * As skins do jogo.
 *
 * <p>Catálogo fechado e imutável: a lista é devolvida sem copia, mas as skins
 * são imutáveis, então não há como alguém alterar o catálogo por acidente.
 *
 * <p><b>A quarta é arte, não paleta.</b> {@code Dedinho} é a cobra desenhada
 * como um dedo, com a unha de cabeça, e por isso vem com PNGs em
 * {@code resources/skins/dedinho/}. As três primeiras são só cor e continuam
 * vetoriais: o mesmo pintor desenha as quatro, e a escolha entre vetor e PNG é
 * feita por forma, na hora de pintar.
 */
public final class CatalogoSkins {

    /** A skin de sempre: verde, a que o jogo já tinha. */
    public static final Skin CLASSSICO = new Skin(
            "classico", "Clássico",
            new Color(0x3DDC84), new Color(0x2BA85F), new Color(0x7CF0AC),
            new Color(0, 0, 0, 40), new Color(12, 16, 20));

    /** Azul e branco, com sombra mais forte para o chapéu pesar. */
    public static final Skin CHAPEU = new Skin(
            "chapeu", "Chapéu",
            new Color(0x7FB3FF), new Color(0x2C5FB8), new Color(0xB8D4FF),
            new Color(10, 20, 50, 55), new Color(8, 16, 40));

    /** Rosa-violeta, o par mais distante do fundo escuro. */
    public static final Skin COLORIDA = new Skin(
            "colorida", "Colorida",
            new Color(0xFF6BD6), new Color(0x7A2BD6), new Color(0xFFB3EC),
            new Color(40, 0, 60, 60), new Color(30, 0, 20));

    /**
     * O dedo com a unha. Só a quarta skin que tem PNGs.
     *
     * <p>As cores aqui são as do prompt que gerou a arte, e servem para o
     * fallback vetorial — que é o que aparece se um PNG faltar. Os valores não
     * são enfeite: sem eles a skin não teria cor nenhuma para o desenho
     * vetorial, e o degradê de um dedo ficaria cinza.
     */
    public static final Skin DEDINHO = new Skin(
            "dedinho", "Dedinho",
            new Color(0xE8B08A), new Color(0xC98A63), new Color(0xF7D9C8),
            new Color(0x3A, 0x24, 0x18, 60), new Color(0xF7D9C8));

    private static final List<Skin> TODAS =
            List.of(CLASSSICO, CHAPEU, COLORIDA, DEDINHO);

    private CatalogoSkins() {
    }

    /** Todas as skins, na ordem em que a tecla de troca percorre. */
    public static List<Skin> todas() {
        return TODAS;
    }

    /** A skin usada quando não há preferência salva, ou quando a salva não existe. */
    public static Skin padrao() {
        return CLASSSICO;
    }

    /**
     * A skin de um id, ou {@code null} se o id não for de nenhuma.
     *
     * <p>Devolve {@code null} em vez de lançar: quem chama é a leitura de um
     * arquivo em disco que o usuário pode ter editado à mão, e um id errado tem
     * que cair na skin padrão, não derrubar o jogo na abertura.
     *
     * @param id identificador gravado
     * @return a skin, ou {@code null}
     */
    public static Skin porId(String id) {
        if (id == null) {
            return null;
        }
        for (Skin s : TODAS) {
            // sem equalsIgnoreCase: o arquivo vai para a pasta do usuario, e o
            // usuario digita "Classico". A busca por posicao continua exata.
            if (s.getId().equalsIgnoreCase(id)) {
                return s;
            }
        }
        return null;
    }

    /**
     * A próxima depois da informada, voltando à primeira no fim.
     *
     * <p>Puro: não guarda estado, não tem "atual". É o que permite trocar de skin
     * no meio da partida sem recarregar nada — quem segura a skin atual é a tela,
     * e ela só pergunta quem vem depois.
     *
     * @param atual a skin de agora; {@code null} ou desconhecida devolve a padrão
     * @return a próxima skin do ciclo
     */
    public static Skin seguinte(Skin atual) {
        if (atual == null) {
            return padrao();
        }
        for (int i = 0; i < TODAS.size(); i++) {
            if (TODAS.get(i).getId().equalsIgnoreCase(atual.getId())) {
                return TODAS.get((i + 1) % TODAS.size());
            }
        }
        return TODAS.get(0);
    }
}