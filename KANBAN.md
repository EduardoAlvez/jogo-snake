# Kanban — Jogo Snake

Segundo jogo do portfólio, depois do Pong. Java 17 + Swing, sem dependências
externas: o mesmo `pom.xml` de sempre (JUnit 4.13.2 + surefire + JaCoCo +
launch4j), que já está comprovado no Pong.

## Estrutura (subpacotes por camada)

```
src/main/java/com/portfolio/snake/
├── core/       Núcleo puro e testável (sem Swing)
│   ├── Campo.java / Celula.java / Direcao.java / Cobra.java / Forma.java
│   ├── Comida.java / Poder.java / JogoSnake.java / RegistroDeRecordes.java
├── skin/       Dados visuais e catálogo
├── fx/         Efeitos e animações (partículas)
├── audio/      Efeitos sonoros
└── ui/         Interface gráfica Swing
    └── TelaSnake.java

src/main/resources/
├── logo-{16,24,32,48,64,128,256}.png   # Ícone da janela, uma por resolução
├── logo.ico                              # Ícone do executável (.exe)
├── skins/                                # PNGs opcionais das skins
└── sons/                                # .wav sintetizados

tools/
└── GerarLogo.java

target/                                   # Saída do build (não versionada)
```

## Decisões já fechadas

- **1P apenas.** Dois jogadores ficam para depois; o Pong já cobriu o caso de
  duas bolas, e aqui a prioridade é a IA de um jogador só.
- **Duas bordas**, escolhidas no briefing: `Campo.Borda.MORRE` (a parede mata,
  Snake clássico) e `Campo.Borda.WRAP` (a cobra dá a volta).
- **Quatro skins**: `Clássico`, `Chapéu`, `Colorida` e `Dedinho` — esta última é
  a própria cobra desenhada como um dedo apontando, com a unha como cabeça.
- **Híbrido procedural/sprites**: o vetorial é a base e nunca some, e o PNG é
  uma sobreposição opcional. Sem arte externa o jogo continua completo e bonito,
  que é o que evita o bloqueio que a Forca tem.
- **Dois poderes no 1.0**: `FANTASMA` (5 s) e `PONTOS_X2` (10 s). Os outros
  (câmera lenta, ímã, encolher) estão no backlog. Cada poder novo é um efeito
  sobre o `JogoSnake` e mais um caso na regra de simultâneo/reversão — no Pong
  foi exatamente aí que morou o bug do especial.
- Skins preferidas em `~/.jogo-snake-skin.properties`; recorde em
  `~/.jogo-snake-recorde` (um inteiro, sem JSON).

## A Fazer (Backlog)

| # | Prioridade | Camada | Tarefa | Detalhes |
|---|-----------|--------|--------|----------|
| 1 | **Alta** | ui | **`TelaSnake` não existe** | O `pom.xml` declara `com.portfolio.snake.ui.TelaSnake` como classe principal, e ela **não foi criada**. Sem ela o `mvn package` não gera executável e `java -jar` falha. **A fazer**: janela, tabuleiro, HUD (pontos, recorde, nível, comprimento), pausa, briefing com escolha de borda e dificuldade, tela de fim com "novo recorde", e o laço de tempo que chama `JogoSnake.passo(dt)` no intervalo de `intervaloMs()` |
| 2 | **Alta** | build | **`logo.ico` e `tools/GerarLogo.java` ausentes** | O launch4j aponta para `src/main/resources/logo.ico`, que não existe, então o `package` falha na hora de montar o `.exe`. Falta o gerador dos PNGs por resolução e o `.ico` |
| 3 | **Alta** | core | **`JogoSnake` não tem nenhum teste** | É a classe que guarda a **ordem dos passos**, que é a regra inteira do jogo: relógio dos poderes → direção e destino com a borda já corrigida → parede → mover (a cauda sai aqui) → colisão → pontos e nível → resorts. Um passo fora de ordem não dá exceção, o jogo só se comporta errado. **A fazer**: comer dá 10 pontos e cresce 1; parede mata em `MORRE` e dá a volta em `WRAP`; colidir com o próprio corpo termina a partida; cabeça na célula que a cauda liberou **não** mata; campo lotado dá vitória e **não trava em laço infinito**; `x2` dobra só enquanto ativo; fantasma segura a parede em `MORRE` e é inútil em `WRAP`; dois fantasmas renovam 5 s em vez de empilhar 10 s; `reiniciar` limpa a fila de direções; a semente fixada torna o sorteio determinístico |
| 4 | **Alta** | ui | **Testes de UI desde o começo** | Lição direta do Pong: o defeito dos badges **não existia em nenhum teste** porque `TelaPong` não era referenciada por nenhum, e ele só apareceu por leitura de código. Aqui `ui` está vazia, então ainda não há o que testar — mas nenhum item de UI deve ser marcado como pronto sem teste de layout que **falhe** quando algo sai de `[0, largura]` ou `[0, altura]`: cards de poder, HUD, cabazão de fim de partida e o desenho da cobra com a cabeça na borda |
| 5 | Média | skin | As 4 skins | `Skin` + `CatalogoSkins` + `SkinVetorial` (a base, sempre presente) + `SkinSprites` (14 tiles: 4 cabeças, 4 rabos, 2 corpos retos, 4 cantos, com fallback vetorial se o PNG faltar). `Clássico`, `Chapéu`, `Colorida` e `Dedinho`, com persistência em `~/.jogo-snake-skin.properties` e troca sem reiniciar a partida |
| 6 | Média | audio | Sons e o silêncio no executável | Regressão do Pong: o áudio era silencioso no `.exe` e só apareceu em teste que **toca de dentro de um jar**, com `BufferedInputStream` desde a primeira linha. Comer, poder, bater na parede e fim de partida |
| 7 | Média | fx | Partículas | Comer comida, coletar poder, bater na parede. Reaproveitar o desenho do Pong |
| 8 | Média | — | **README** | Não existe. Tem que descrever o que o Snake **tem**, e não o que ele vai ter, com o mesmo cuidado do Pong depois do badge: nada de "validado por smoke test" sem o teste |
| 9 | Média | build | Descrição do `pom.xml` | Diz `Pong do portfolio` e `Branch de testes` no `main`. Corrigir e colocar o nome do jogo certo |
| 10 | Baixa | ui | Segundo jogador | **Adiado pelo usuário.** A entrada de dois jogadores no mesmo teclado é a parte chata de interface |
| 11 | Baixa | core | Obstáculos | Paredes internas e blocos fixos. Muda a ordem da checagem de colisão e, portanto, os testes do item 3 |
| 12 | Baixa | core | Mais poderes | Câmera lenta, ímã, encolher. Cada um é um efeito no `JogoSnake` e mais um par de casos na regra de simultâneo/reversão |

## Validação visual

**Nada foi validado visualmente, porque a interface ainda não existe.** Nenhuma
janela deste jogo foi aberta até agora. O que existe é código de núcleo
compilando e testes unitários.

Conferir quando a `TelaSnake` existir: a cobra desenhada como cobra e não como
uma centipede de quadrados soltos (é para isso que existe o `Forma`, com entrada
e saída por segmento), os 4 cantos aparecendo nas viradas, a cabeça na borda no
modo wrap, os cards de poder não saindo do campo com 2 poderes ativos ao mesmo
tempo, o ícone na barra de tarefas/Alt+Tab, e o recorde sobrevindo ao fechamento
da janela.

## Fazendo (Doing)

Núcleo em escrita: `Comida`, `Poder`, `JogoSnake` e `RegistroDeRecordes`
compilam, e **`JogoSnake` está sem teste nenhum** (backlog 3). A ordem dos passos
está escrita e comentada no método, mas comentário não é teste.

## Feito (Done)

- Scaffold do projeto: `pom.xml`, `LICENSE` (MIT), `.gitignore` e a estrutura por
  camadas `core`/`skin`/`fx`/`audio`/`ui`
- Repositório git inicializado, identidade configurada
- Primeiro commit `d0bc87c`: núcleo com os dois erros clássicos do Snake
  cobertos por teste
- **core**: `Direcao` — as 4 direções, `oposto`, `eOposta` e `mudaEixo`. A
  direção saber o próprio oposto é o que deixa o teste de 180° uma chamada em
  vez de quatro comparações repetidas — que é onde o bug se esconde
- **core**: `Campo` — grade, validação de no mínimo 2×2, `dentro`, `totalCelulas`,
  `traduzir` (dobra a coordenada no `WRAP`, devolve como está no `MORRE`) e
  `paredeMata`
- **core**: `Celula` — coordenada imutável, `equals`, `hashCode` e `chave` para
  comparação rápida
- **core**: `Cobra` — corpo em `ArrayList` com a cabeça no índice 0 (e não deque:
  o desenho e a colisão acessam por índice a cada quadro, que é tempo constante
  no `ArrayList`), fila de direções com limite de 2, crescimento só ao comer,
  `encurtar` com piso de 2 segmentos, `colidiuComSi`, `formas` e `avancarPara`
- **core**: `Forma` — as 14 formas (4 cabeças, 4 rabos, 2 corpos retos, 4
  cantos), derivadas dos vizinhos anterior e seguinte de cada segmento
- **core**: `Comida` — posição e valor, mais `sortearLivre` com **número de
  tentativas limitado**, que é o terceiro erro clássico do Snake: "sorteia até
  achar célula livre" trava para sempre quando a cobra cobre o campo inteiro, e
  aqui o método devolve `null` e o jogo declara vitória
- **core**: `Poder` — `FANTASMA` e `PONTOS_X2` com duração, cor e rótulo, e
  `Ativo` com relógio, `renovar` (dois fantasmas dão 5 s, não 10 s) e `acabou`
- **core**: `RegistroDeRecordes` — arquivo com um inteiro na pasta do usuário;
  arquivo corrompido devolve 0 em vez de impedir o jogo de abrir
- **`CobraTest`: 16 testes, todos verdes** — movimento, crescimento, cauda
  liberando a célula, colisão, limite e ordem da fila, e recusa de inversão de
  180° nas duas situações possíveis (com a fila vazia e com uma direção já
  enfileirada)
- **Os dois erros clássicos verificados por mutação, não por leitura**: introduzi
  cada um de propósito e os testes caem — 5 falhas ao validar a fila contra a
  direção atual em vez da última enfileirada, e 4 falhas quando a cauda não sai
  no movimento normal. Um teste que passa sem mutação não distingue
  implementação certa de implementação que só parece certa
- **Dois defeitos do construtor corrigidos**: a cobra aceitasse comprimento
  menor que 2, e deitada para a direita numa grade estreita o corpo saía em
  coordenada negativa e a cobra nascia dentro da parede

## Números

| | Pong | Snake |
|---|---|---|
| Testes | 149 | **16** |
| Testes em `ui` | 0 | 0 (a `ui` não existe) |
| Camadas sem teste | — | `Campo`, `Direcao`, `Celula`, `Forma`, `Comida`, `Poder`, `JogoSnake`, `RegistroDeRecordes` |
| Linha de comando | — | `/c/Users/dudu2/.m2/wrapper/dists/apache-maven-3.9.11/d6d3cbd4012d4c1d840e93277aca316c/bin/mvn` (o `mvn` não está no `PATH`) |

O Snake está bem **atrás** do Pong em cobertura, e isso é o número que manda aqui:
`JogoSnake` concentra toda a ordem das regras e ainda não tem um teste só.
