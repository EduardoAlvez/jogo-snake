package com.portfolio.snake.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * O recorde, guardado em arquivo na pasta do usuário.
 *
 * <p>Um arquivo só com um número inteiro, sem JSON nem banco. O motivo é
 * concreto: um arquivo de texto com uma linha é o formato que sobrevive a uma
 * versão futura do jogo, a uma mudança de pasta e a um usuário que abre e corrige
 * à mão. O Pong guardava o placar em memória e perdia tudo ao fechar — o que só
 * apareceu porque a tela de fim de partida mostrava "recorde" como se fosse
 * durável.
 */
public final class RegistroDeRecordes {

    private static final String NOME_ARQUIVO = ".jogo-snake-recorde";
    private static final int VALOR_INICIAL = 0;

    private RegistroDeRecordes() {
    }

    private static Path caminho() {
        return Paths.get(System.getProperty("user.home", "."), NOME_ARQUIVO);
    }

    /**
     * Lê o recorde salvo.
     *
     * <p>Qualquer problema de leitura devolve zero. Um arquivo corrompido não
     * pode impedir o jogo de abrir — perder o recorde é chato, não conseguir
     * jogar é pior.
     *
     * @return a maior pontuação já alcançada
     */
    public static int maiorPontuacao() {
        try {
            Path p = caminho();
            if (!Files.exists(p)) {
                return VALOR_INICIAL;
            }
            String texto = Files.readString(p, StandardCharsets.UTF_8).trim();
            int v = Integer.parseInt(texto);
            return Math.max(VALOR_INICIAL, v);
        } catch (IOException | RuntimeException e) {
            return VALOR_INICIAL;
        }
    }

    /**
     * Salva o recorde, se for maior que o já salvo.
     *
     * @param pontuacao a pontuação da partida que acabou
     * @return {@code true} se houve um novo recorde
     */
    public static boolean registrar(int pontuacao) {
        if (pontuacao <= maiorPontuacao()) {
            return false;
        }
        try {
            Files.writeString(caminho(), String.valueOf(pontuacao),
                    StandardCharsets.UTF_8);
            return true;
        } catch (IOException | RuntimeException e) {
            // Sem permissão de escrita, por exemplo: o jogo continua sem salvar.
            return false;
        }
    }

    /** Apaga o recorde. Só para os testes e para um botão de "zerar". */
    static void apagar() {
        try {
            Files.deleteIfExists(caminho());
        } catch (IOException | RuntimeException e) {
            // Sem efeito: apagar um recorde que não existe não é erro.
        }
    }
}
