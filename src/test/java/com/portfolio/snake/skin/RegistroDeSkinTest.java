package com.portfolio.snake.skin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * A skin preferida em disco.
 *
 * <p>Cada teste aponta {@code user.home} para uma pasta temporária e apaga tudo
 * depois. Sem isso, o teste escreveria no disco de verdade e — pior — passaria
 * por causa do arquivo que uma execução anterior deixou, que é a forma mais
 * comum de um teste de disco parecer verde sem estar testando nada.
 */
public class RegistroDeSkinTest {

    private String homeOriginal;
    private Path casa;

    @Before
    public void apontarCasaParaPastaTemporaria() throws IOException {
        homeOriginal = System.getProperty("user.home");
        casa = Files.createTempDirectory("jogo-snake-skin");
        System.setProperty("user.home", casa.toString());
    }

    @After
    public void restaurarCasa() throws IOException {
        System.setProperty("user.home", homeOriginal);
        apagarRecursivamente(casa);
    }

    private static void apagarRecursivamente(Path p) throws IOException {
        if (!Files.exists(p)) {
            return;
        }
        // sem o isDirectory, um arquivo solto no caminho estoura NotDirectory
        // -- a primeira versao deste helper fazia exatamente isso no teardown
        if (Files.isDirectory(p)) {
            try (var itens = Files.list(p)) {
                for (Path item : itens.toList()) {
                    apagarRecursivamente(item);
                }
            }
        }
        Files.deleteIfExists(p);
    }

    private Path arquivo() {
        return casa.resolve(RegistroDeSkin.nomeDoArquivo());
    }

    @Test
    public void semArquivoDevolveAPadrao() {
        assertSame(CatalogoSkins.padrao(), RegistroDeSkin.carregar());
    }

    @Test
    public void salvarEVoltarDevolveAMesmaSkin() {
        for (Skin s : CatalogoSkins.todas()) {
            assertTrue("nao gravou " + s.getId(), RegistroDeSkin.salvar(s));
            assertSame("voltou outra skin depois de gravar " + s.getId(),
                    s, RegistroDeSkin.carregar());
        }
    }

    @Test
    public void oArquivoVaiNaPastaDoUsuario() {
        RegistroDeSkin.salvar(CatalogoSkins.CHAPEU);
        assertTrue("o arquivo foi parar fora de user.home: " + arquivo(),
                Files.exists(arquivo()));
    }

    @Test
    public void oPreferidoGravadoEOuNaoAPosicaoNoCatalogo() throws IOException {
        // a regressao que este formato evita: gravar "skin=1" e, quando entrar uma
        // skin nova no meio da lista, todo mundo mudar de pele sem ter pedido
        RegistroDeSkin.salvar(CatalogoSkins.CHAPEU);
        String texto = Files.readString(arquivo(), StandardCharsets.UTF_8);
        assertTrue("o id deveria estar no arquivo: " + texto, texto.contains("chapeu"));
        assertFalse("o arquivo nao pode guardar posicao: " + texto,
                texto.contains("skin=1"));
    }

    @Test
    public void idQueNaoEstaNoCatalogoVoltaParaAPadrao() throws IOException {
        // NAO pode ser "dedinho": ele virou um id de verdade quando os PNGs
        // chegaram, e o teste parou de testar o que ele diz testar.
        Files.writeString(arquivo(), "skin=dedinho-antigo\n", StandardCharsets.UTF_8);
        assertSame(CatalogoSkins.padrao(), RegistroDeSkin.carregar());
    }

    @Test
    public void arquivoVazioVoltaParaAPadrao() throws IOException {
        Files.writeString(arquivo(), "", StandardCharsets.UTF_8);
        assertSame(CatalogoSkins.padrao(), RegistroDeSkin.carregar());
    }

    @Test
    public void arquivoCorrompidoNaoImpedeOGameDeAbrir() throws IOException {
        // bytes que nao sao um .properties: o jogo tem que abrir na skin padrao,
        // porque perder a preferencia e chato e nao conseguir jogar e pior
        byte[] lixo = {(byte) 0x00, (byte) 0xFF, (byte) 0xC3, (byte) 0x28, (byte) 0xA0};
        Files.write(arquivo(), lixo);
        assertSame(CatalogoSkins.padrao(), RegistroDeSkin.carregar());
    }

    @Test
    public void chaveFaltandoVoltaParaAPadrao() throws IOException {
        Files.writeString(arquivo(), "outra-coisa=1\n", StandardCharsets.UTF_8);
        assertSame(CatalogoSkins.padrao(), RegistroDeSkin.carregar());
    }

    @Test
    public void gravarNullNaoEscreveNemRetornaSucesso() {
        assertFalse(RegistroDeSkin.salvar(null));
        assertFalse("um null nao pode ter criado arquivo", Files.exists(arquivo()));
    }

    @Test
    public void pastaDoUsuarioInexistenteNaoQuebra() throws IOException {
        // casa apagada: nem gravar nem ler podem lancar. E o jogo ainda tem de
        // abrir, entao carregar tem de devolver a padrao em vez de explodir
        apagarRecursivamente(casa);
        assertFalse("nao devia gravar sem pasta", RegistroDeSkin.salvar(CatalogoSkins.CHAPEU));
        assertSame(CatalogoSkins.padrao(), RegistroDeSkin.carregar());
        // o resto dos testes precisa da pasta de volta
        Files.createDirectories(casa);
    }

    @Test
    public void apagarNaoErraQuandoNaoExisteArquivo() {
        RegistroDeSkin.apagar();
        RegistroDeSkin.apagar();
    }

    @Test
    public void gravarDuasVezesSobrescreveEmVezDeAcumular() throws IOException {
        RegistroDeSkin.salvar(CatalogoSkins.CHAPEU);
        RegistroDeSkin.salvar(CatalogoSkins.COLORIDA);
        assertSame(CatalogoSkins.COLORIDA, RegistroDeSkin.carregar());
    }

    @Test
    public void oTextoLixoDoTesteRealmenteCorrompeOArquivo() throws IOException {
        // premissa do teste de corrupcao: se escrever lixo produzisse um
        // .properties valido, o teste de cima estaria passando sem motivo
        // um NUL escrito como escape unicode viraria um byte nulo no proprio
        // fonte, porque o lexer resolve \\u antes de qualquer outra coisa
        RegistroDeSkin.escreverLixo(String.valueOf((char) 0) + (char) 0 + "binario");
        assertEquals(CatalogoSkins.padrao(), RegistroDeSkin.carregar());
    }
}