package com.portfolio.snake.skin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/** O catálogo: ids únicos, busca por id, e o ciclo da tecla de troca. */
public class CatalogoSkinsTest {

    @Test
    public void nenhumIdRepete() {
        Set<String> vistos = new HashSet<>();
        for (Skin s : CatalogoSkins.todas()) {
            assertTrue("id repetido: " + s.getId(), vistos.add(s.getId()));
        }
        assertEquals("a contagem de ids tem que bater com a de skins",
                CatalogoSkins.todas().size(), vistos.size());
    }

    @Test
    public void oPadraoEstaNoCatalogo() {
        // se o padrao saisse do catalogo, o BRIEFING mostraria uma skin que a
        // tecla N nunca alcanca
        assertTrue(CatalogoSkins.todas().contains(CatalogoSkins.padrao()));
    }

    @Test
    public void porIdEncontraCadaSkin() {
        for (Skin s : CatalogoSkins.todas()) {
            assertSame("porId devolveu outra skin para " + s.getId(),
                    s, CatalogoSkins.porId(s.getId()));
        }
    }

    @Test
    public void porIdDeIdNaoExistenteDevolveNulo() {
        // e NAO a padrao: quem chama precisa poder distinguir "nao sei" de
        // "sei, e e a padrao", senao um id apagado do catalogo viraria silencioso
        assertNull(CatalogoSkins.porId("dedinho-antigo"));
        assertNull(CatalogoSkins.porId(""));
        assertNull(CatalogoSkins.porId(null));
    }

    @Test
    public void porIdIgnoraMaiusculasEDeixinhas() {
        // um arquivo editado a mao nao respeita a caixa; "Classico" tem que achar
        assertSame(CatalogoSkins.CLASSSICO, CatalogoSkins.porId("Classico"));
    }

    @Test
    public void aProximaCiclaPeloCatalogoInteiro() {
        List<Skin> todas = CatalogoSkins.todas();
        Skin atual = CatalogoSkins.padrao();
        for (int i = 0; i < todas.size(); i++) {
            atual = CatalogoSkins.seguinte(atual);
            assertSame("no passo " + i + " do ciclo", todas.get((i + 1) % todas.size()),
                    atual);
        }
        assertSame("depois de um ciclo inteiro volta ao comeco",
                CatalogoSkins.padrao(), atual);
    }

    @Test
    public void aProximaDaUltimaVoltaParaAPrimera() {
        Skin ultima = CatalogoSkins.todas().get(CatalogoSkins.todas().size() - 1);
        assertSame(CatalogoSkins.todas().get(0), CatalogoSkins.seguinte(ultima));
    }

    @Test
    public void aProximaDeSkinInexistenteVoltaParaOComeco() {
        // o usuario pode ter gravado um id que foi removido do catalogo
        Skin fantasma = new Skin("fantasma", "Fantasma", Color.RED, Color.BLUE,
                Color.GREEN, Color.BLACK, Color.WHITE);
        assertSame(CatalogoSkins.todas().get(0), CatalogoSkins.seguinte(fantasma));
    }

    @Test
    public void aProximaDeNullDevolveAPadrao() {
        // antes de ler o disco, a tela pergunta a proxima skin de uma skin que
        // ainda nao existe
        assertSame(CatalogoSkins.padrao(), CatalogoSkins.seguinte(null));
    }

    @Test
    public void nenhumaSkinRepeteOsTonsDeCorpo() {
        // a mutacao aqui e "Colorida recebe o verde do Classico", que faria a
        // troca de skin continuar visivel (o nome muda) sem mudar nada na tela
        for (int i = 0; i < CatalogoSkins.todas().size(); i++) {
            Skin a = CatalogoSkins.todas().get(i);
            for (int j = i + 1; j < CatalogoSkins.todas().size(); j++) {
                Skin b = CatalogoSkins.todas().get(j);
                assertNotEquals(a.getId() + " e " + b.getId() + " tem o mesmo corpo claro",
                        a.getCorpoClaro(), b.getCorpoClaro());
                assertNotEquals(a.getId() + " e " + b.getId() + " tem o mesmo corpo escuro",
                        a.getCorpoEscuro(), b.getCorpoEscuro());
            }
        }
    }

    @Test
    public void oSombraTemAlfaMenorQue255() {
        // a sombra do vinco dos cantos precisa deixar o canto aparecer; com alfa
        // 255 ela some, e com 0 o vinco nao existe
        for (Skin s : CatalogoSkins.todas()) {
            assertTrue(s.getId() + " com sombra totalmente opaca", s.getSombra().getAlpha() < 255);
            assertTrue(s.getId() + " com sombra invisivel", s.getSombra().getAlpha() > 0);
        }
    }

    @Test
    public void aListaDoCatalogoNaoPodeSerAlteradaPorQuemChamou() {
        try {
            CatalogoSkins.todas().add(CatalogoSkins.padrao());
            fail("a lista do catalogo aceitou alteracao");
        } catch (UnsupportedOperationException esperado) {
            assertNotNull(CatalogoSkins.padrao());
        }
    }

    @Test
    public void oNomeDeCadaSkinNaoEhVazio() {
        // o nome aparece no briefing; vazio apareceria como "Skin  [N] troca"
        for (Skin s : CatalogoSkins.todas()) {
            assertNotNull(s.getId(), s.getId());
            assertTrue("nome vazio em " + s.getId(), s.getNome().trim().length() > 1);
        }
    }
}