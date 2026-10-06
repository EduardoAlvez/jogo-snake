# Jogo Snake

Snake do portfólio: a cobra em uma grade 16×16, com borda que mata ou dá a
volta, dois poderes, quatro skins e recorde gravado em disco. Java 17 + Swing,
sem nenhuma dependência em tempo de execução — o `.exe` carrega o próprio jar.

## O que o jogo tem

- **Grade 16×16** com HUD de pontos, recorde, nível e comprimento.
- **Duas bordas**, escolhidas no menu: **morre** na parede (Snake clássico) ou
  **dá a volta** (`wrap`).
- **Três dificuldades**, cada vez mais rápidas: o intervalo entre passos
  começa em 170 ms (fácil), 150 ms (médio) ou 130 ms (difícil) e diminui a cada
  nível — nunca abaixo do piso de 60 ms.
- **Quatro skins**: Clássico, Chapéu, Colorida e **Dedinho** (a cobra desenhada
  como um dedo apontando, com a unha na cabeça). As três primeiras são paleta;
  o Dedinho usa sprites com o desenho vetorial como base.
- **Dois poderes**:
  - **Fantasma** (~5 s) — a parede deixa de matar enquanto dura;
  - **Pontos ×2** (~10 s) — dobra os pontos da partida.
- **Menu inicial com botões**: borda, dificuldade, skin e começar. **Tela de fim
  com botões** "Jogar de novo" e "Menu inicial", e selo de novo recorde.
- **Recorde em disco** (`~/.jogo-snake-recorde`, um inteiro) e **skin preferida
  lembrada** (`~/.jogo-snake-skin.properties`).
- **Sons sintetizados por código** (comer, parede, poder, fim) e **trilha** — sem
  arquivo de áudio externo, o que mantém o jogo completo dentro do jar.
- **Pausa** a qualquer momento.

## Controles

| Tecla | Ação |
|---|---|
| Setas / `W` `A` `S` `D` | Mudar a direção |
| `Espaço` / `P` | Pausar / continuar |
| `N` | Trocar de skin |
| `1` / `2` (no menu) | Escolher a borda (morre / dá a volta) |
| `3` / `4` / `5` (no menu) | Escolher a dificuldade (fácil / médio / difícil) |
| `Enter` (no menu) | Começar a partida |
| `R` | Recomeçar / voltar ao menu |
| `Esc` | Sair do jogo |

## Como rodar

Requer **Java 17** ou mais novo.

- **Windows**: execute `target/jogo-snake.exe` depois do build, ou:
- **Qualquer plataforma**:

```
mvn clean package
java -jar target/jogo-snake-1.0-SNAPSHOT.jar
```

O build gera também o executável Windows (`target/jogo-snake.exe`) com o ícone
embutido.

## Testes

**231 testes** com JUnit 4, cobertura com JaCoCo:

```
mvn test
```

A suíte cobre as regras do núcleo (incluindo um *soak* de 20 000 passos em
partidas encadeadas), as skins, o áudio (carregado de dentro de um jar real) e a
interface — geometria do `LayoutSnake`, pixels do `DesenhoJogo` e comportamentos
da janela — tudo sem abrir uma janela.

## Arquitetura

O código é separado em camadas, e nenhuma regra mora no desenho:

```
src/main/java/com/portfolio/snake/
├── core/       Regras puras e testáveis, sem Swing
├── skin/       Cores, catálogo e carregamento de sprites
├── audio/      Efeitos sonoros e trilha
└── ui/         A interface gráfica
    ├── LayoutSnake.java   A geometria: onde cada coisa é desenhada
    ├── DesenhoJogo.java   O desenho, sem janela (pinta em qualquer Graphics2D)
    └── TelaSnake.java     A janela: teclado, relógio e repaint
```

O laço do jogo mede o tempo entre tiques em vez de assumi-lo: se a máquina
engasgar, a cobra não pula duas casas — ela apenas demora um pouco mais a se
mover.

## Licença

MIT — ver [LICENSE](LICENSE).