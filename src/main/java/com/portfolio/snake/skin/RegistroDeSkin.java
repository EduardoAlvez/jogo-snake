package com.portfolio.snake.skin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * A skin escolhida, guardada na pasta do usuário.
 *
 * <p>Um arquivo {@code .properties} com uma chave só, e não um número como o
 * recorde: aqui o valor é um <b>id</b> ({@code classico}), não uma posição no
 * catálogo. Se um dia entrar uma skin nova no meio da lista, quem tem
 * {@code classico} gravado continua com a clássica; com posição, todo mundo
 * mudaria de pele sem ter pedido.
 *
 * <p>A leitura nunca derruba o jogo. Arquivo ausente, id que não existe mais,
 * arquivo truncado ou pasta sem permissão: tudo cai na skin padrão. Perder a
 * preferência é chato; não conseguir jogar é pior.
 */
public final class RegistroDeSkin {

    private static final String NOME_ARQUIVO = ".jogo-snake-skin.properties";
    private static final String CHAVE = "skin";

    private RegistroDeSkin() {
    }

    private static Path caminho() {
        return Paths.get(System.getProperty("user.home", "."), NOME_ARQUIVO);
    }

    /**
     * A skin salva, ou a padrão.
     *
     * @return a skin escolhida da última vez, sem nunca lançar
     */
    public static Skin carregar() {
        try {
            Path p = caminho();
            if (!Files.exists(p)) {
                return CatalogoSkins.padrao();
            }
            Properties props = new Properties();
            try (var entrada = Files.newInputStream(p)) {
                props.load(entrada);
            }
            Skin achada = CatalogoSkins.porId(props.getProperty(CHAVE));
            return achada != null ? achada : CatalogoSkins.padrao();
        } catch (IOException | RuntimeException e) {
            return CatalogoSkins.padrao();
        }
    }

    /**
     * Grava a skin escolhida.
     *
     * @param skin a skin a gravar; {@code null} devolve sem escrever
     * @return {@code true} se gravou
     */
    public static boolean salvar(Skin skin) {
        if (skin == null) {
            return false;
        }
        Properties props = new Properties();
        props.setProperty(CHAVE, skin.getId());
        try (var saida = Files.newOutputStream(caminho())) {
            props.store(saida, "skin escolhida do Jogo Snake");
            return true;
        } catch (IOException | RuntimeException e) {
            // Sem permissão de escrita, por exemplo: o jogo continua sem lembrar.
            return false;
        }
    }

    /** Apaga a preferência. Só para os testes. */
    static void apagar() {
        try {
            Files.deleteIfExists(caminho());
        } catch (IOException | RuntimeException e) {
            // Apagar uma preferência que não existe não é erro.
        }
    }

    /** Nome do arquivo, para o teste confirmar que não mexe fora da pasta do usuário. */
    static String nomeDoArquivo() {
        return NOME_ARQUIVO;
    }

    /** Grava um texto qualquer no arquivo, para o teste simular arquivo corrompido. */
    static void escreverLixo(String texto) throws IOException {
        Files.writeString(caminho(), texto, StandardCharsets.UTF_8);
    }
}