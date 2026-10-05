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
    ├── LayoutSnake.java   Geometria: onde cada coisa é desenhada
    ├── DesenhoJogo.java   O desenho, sem janela (pinta em qualquer Graphics2D)
    └── TelaSnake.java     A janela: teclado, relógio e repaint

src/main/resources/
├── logo-{16,24,32,48,64,128,256}.png   # Ícone da janela, uma por resolução
├── logo.ico                              # Ícone do executável (.exe)
├── skins/                                # PNGs opcionais das skins
└── sons/                                # .wav sintetizados

tools/
└── GerarLogo.java                        # Deriva os PNGs e o logo.ico do logo-256.png

target/                                   # Saída do build (não versionada)
```

## Branches

Mesma convenção do Pong, e mais limpa:

| Branch | Papel | Versão | Testes |
|---|---|---|---|
| `testes-jogo-snake` | desenvolvimento | `1.0-SNAPSHOT` | pom com JUnit + Surefire + JaCoCo, e os 163 testes em `src/test` |
| `main` | release | a versionar na release | **`src/test` junto com o jogo**, como no Pong — 163 testes, `mvn clean package` verde |

**Política da `main`, revista**: a `main` deve receber `src/test` junto com o jogo,
como no Pong, e continua sendo mergeada **só na hora da release**. Separar "o que é
entregue" de "o que é verificado" é erro meu, não uma convenção: uma `main` sem
testes é um artefato que ninguém consegue reprovar. O que se separa é o *quando*,
não o *o quê*.

**A merge da release aconteceu** (`--no-ff`, porque as duas branches divergiam a
partir de `63dc513`): a `main` tinha `pom.xml` com JUnit/Surefire/JaCoCo declarados e
nenhum arquivo em `src/test`, e o Launch4j apontava para
`src/main/resources/logo.ico`, que não existia lá — por inspeção do `pom.xml`, o
`mvn package` dela falhava. Os dois pontos foram consertados de uma vez, e agora
**verificado por execução**: 163 testes verdes, `BUILD SUCCESS` e
`target/jogo-snake.exe` gerado na própria `main`.

A merge teve 6 conflitos, e todos se resolveram tomando a versão da branch de
desenvolvimento, porque nela cada arquivo é superset do da `main` — a `main` nunca
tinha algo que a branch não tivesse. Ainda assim, dois precisam de nota:

- **`pom.xml` precisou de edição manual, não de resolução.** O `<name>` dizia
  "(com testes)" e o `<description>` afirmava que "a main carrega só o jogo, sem
  `src/test`" — frase que a própria merge tornava falsa. Resolver o conflito
  mecânicamente teria publicado um `pom.xml` que descreve a branch errada, e isso
  aparece em toda página do repositório. O `<description>` também dizia "grade
  20x20", o que já era falso antes da merge.
- **`TelaSnake.java` tinha uma linha sem indentação** (`am.put(nome, ...)`, coluna
  0), quebrada em `f1837fe` e nunca vista porque ninguém abre esse arquivo para
  ler. A `main` tinha a linha correta; a merge resolveu a favor da branch e o
  defeito ia junto. Corrigido aqui. É a mesma armadilha do item 5 do `KANBAN`, em
  forma diferente: nenhum teste falha com uma indentação errada.

## Decisões já fechadas

- **1P apenas.** Dois jogadores ficam para depois; o Pong já cobriu o caso de
  duas bolas, e aqui a prioridade é a IA de um jogador só.
- **Duas bordas**, escolhidas no briefing: `Campo.Borda.MORRE` (a parede mata,
  Snake clássico) e `Campo.Borda.WRAP` (a cobra dá a volta).
- **Quatro skins**: `Clássico`, `Chapéu`, `Colorida` e `Dedinho` — esta última é
  a própria cobra desenhada como um dedo apontando, com a unha como cabeça.
  A entrega foi em duas etapas: **paleta** (as três primeiras) e depois
  **sprites** (o `Dedinho`). `Dedinho` não entrou na paleta: é arte por forma, não
  uma troca de cor. As 14 peças do Dedinho saem de **4** imagens giradas em
  código, não de 14 desenhos — ver "A camada `skin`".
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
| 1 | ~~Alta~~ | ui | ~~**`TelaSnake` não existe**~~ | **Concluído.** Janela, tabuleiro, cobra com as 14 formas, HUD (pontos, recorde, nível, comprimento), faixa de borda, rodapé com cards de poder, pausa, briefing com escolha de borda e dificuldade, tela de fim com selo de novo recorde, e o laço de tempo que mede o tempo decorrido entre tiques (um travamento da máquina atrasa a cobra, não a teletransporta). A janela **abre de verdade** — ver "Validação visual" |
| 2 | ~~Alta~~ | build | ~~**`logo.ico` e `tools/GerarLogo.java` ausentes**~~ | **Concluído.** `tools/DesenharLogoSnake.java` desenha o mestre `logo-256.png` por código, e `tools/GerarLogo.java` deriva os 6 PNGs menores e monta o `.ico`. Ver "Ícone e logo" abaixo |
| 3 | — | core | ~~**`JogoSnake` testado só nas regras básicas**~~ resolvido no round 2: os 39 testes de `JogoSnakeTest` cobrem comer, as duas bordas, colidir com o corpo, a cabeça na célula liberada pela cauda, campo lotado, os dois poderes com renovação, pausa, reinício e a fila de direções; **mais** (a) `JogoSnakeSoakTest`, que joga 20 000 passos reais em partidas encadeadas, e (b) `intervaloMs` nas três dificuldades, com o piso de 60 ms e o teto de nível. O (c) — recorde em disco — já estava resolvido em `DesenhoJogoTest`, que aponta `user.home` para uma pasta temporária e apaga tudo depois. **O soak não achou bug** — ver "Round 2" |
| 4 | ~~Alta~~ | ui | ~~**Testes de UI desde o começo**~~ | **Concluído: 50 testes em `ui`** (24 de layout + 19 de desenho + 7 do ícone), e ambos os grupos foram verificados por mutação. `LayoutSnake` cobre a geometria e achou um defeito real de 8px entre tabuleiro e rodapé. `DesenhoJogo` cobre os pixels e achou o selo de recorde piscando (bug 5 abaixo) |
| 5 | Média | skin | As 4 skins | **Concluído: 4 de 4.** `Skin` + `CatalogoSkins` + `RegistroDeSkin` (cores, persistência em `~/.jogo-snake-skin.properties`, troca por `[N]` sem reiniciar) e `SkinSprites` (carrega `/skins/<id>/<FORMA>.png`, PNG ausente cai no vetorial). As 3 primeiras são paleta; o `Dedinho` são 14 PNGs de arte por forma. Grade padrão 20×20 → **16×16**. Ver "A camada `skin`" |
| 6 | ~~Média~~ | audio | ~~**Sons e o silêncio no executável**~~ | **Concluído: 31 testes em `audio`** (15 do `Sons` + 13 da `Trilha` + 3 do motivo no `core`). Os quatro efeitos são gerados por código, e o teste carrega o **próprio `Sons` de dentro de um jar** — foi o que transforma a regressão do Pong em um teste que morde |
| 7 | Média | fx | Partículas | Comer comida, coletar poder, bater na parede. Reaproveitar o desenho do Pong |
| 8 | Média | — | **README** | Não existe. Tem que descrever o que o Snake **tem**, e não o que ele vai ter, com o mesmo cuidado do Pong depois do badge: nada de "validado por smoke test" sem o teste |
| 9 | Baixa | ui | Segundo jogador | **Adiado pelo usuário.** A entrada de dois jogadores no mesmo teclado é a parte chata de interface |
| 10 | Baixa | core | Obstáculos | Paredes internas e blocos fixos. Muda a ordem da checagem de colisão e, portanto, os testes do item 3 |
| 11 | Baixa | core | Mais poderes | Câmera lenta, ímã, encolher. Cada um é um efeito no `JogoSnake` e mais um par de casos na regra de simultâneo/reversão |

## Validação visual

**A janela abre, e ninguém olhou para ela.** As duas frases precisam ficar
juntas, porque sozinhas cada uma mente.

O que foi verificado, por máquina: a `TelaSnake` sobe, o processo `java` fica no
ar e o Windows acha uma janela visível de título `Jogo Snake`. Um screenshot de
1280×720 foi gravado em `%TEMP%/opencode/snake2.png`.

O que **não** foi verificado: a aparência. Quem escreveu isto não consegue ver
imagem — não há entrada visual. Um screenshot ter sido gravado não é o mesmo que
alguém ter olhado para ele, e dizer que "o jogo foi validado visualmente" com
base num PNG aberto e nunca visto seria repetir, em outro lugar, o erro que este
projeto passou a documentar.

O que os 13 testes de `DesenhoJogo` garantem é mais estreito e, para o que
importa, mais forte: que a cena **pinta** o que deveria, conferindo os pixels de
um `BufferedImage` do mesmo tamanho da janela. A cobra aparece na célula da
cabeça, a comida aparece na célula da comida, o HUD e o rodapé têm texto, o
briefing cobre o tabuleiro, a cena é pintada por inteiro sem buracos, e pintar a
mesma partida duas vezes dá exatamente a mesma imagem. Isso pega a classe de
defeito "a janela abre e não mostra nada", e **não** pega "a janela abre e mostra
algo feio".

Falta o olho de uma pessoa, nesta lista:

- a cobra parece uma cobra, e não uma centipede de quadrados soltos (é para isso
  que existe o `Forma`, com entrada e saída por segmento)
- os 4 cantos aparecem nas viradas
- a cabeça na borda no modo `WRAP`
- os cards de poder não saindo do campo com 2 poderes ativos ao mesmo tempo
- o ícone na barra de tarefas e no Alt+Tab (o item 2 está resolvido: são 7
  resoluções por `setIconImages`, e o `.exe` embute o `.ico` — mas **ninguém
  olhou**)
- o recorde sobreviving ao fechamento da janela

## Fazendo (Doing)

**Status: em andamento. O jogo não está pronto, e o `.exe` não é signal disso.**

O item 2 está resolvido, o áudio tocou e a paleta das skins está em pé. O
`package` passou. Isso é **build verificado**, que é uma coisa diferente de
release: prova que o launch4j acha o ícone, que o jar monta e que o executável
abre. Não prova que o jogo está acabado, e o que falta está escrito na lista
abaixo para não se perder de vista.

**Onde paramos agora**: a camada `skin` está **inteira** — paleta das 3 skins
vetoriais e os 14 PNGs do `Dedinho` — com **163 testes verdes** e o Dedinho
validado a olho pelo autor. Ainda **sem tag, sem release, sem push**. O que falta
são as partículas, a validação visual do resto e o README — ver "O que ainda
falta para o jogo estar pronto" e "A camada `skin`".

### O que o build verificado cubriu

- `mvn clean package` verde, **163 testes** (59 em `core` + 28 em `audio` + 26 em `skin` + 50 em `ui`)
- `target/jogo-snake.exe` gerado, e **abrindo de verdade**: processo `javaw.exe` no
  ar com janela de título `Jogo Snake`
- `logo.ico` embutido no executável, `logo-{16..256}.png` no classpath (8 recursos
  no jar), `Main-Class` correta, e **zero `*Test.class` no artefato**

Nada disso foi publicado: **sem tag, sem release, sem push**. Não é entrega.

### O que ainda falta para o jogo estar pronto

| | Camada | O que falta |
|---|---|---|
| 1 | `skin/` | ~~os sprites das 4 skins~~ **resolvido**: `SkinSprites` pronto e testado, e o `Dedinho` com os 14 PNGs gerados por `tools/RecortarDedinho.java` |
| 1b | `skin/` | encolher os PNGs do Dedinho: 5,4 MB em resolução cheia para serem mostrados a 28 px. O `.exe` foi de 1 MB para 5,9 MB |
| 3 | `fx/` | as partículas. O pacote existe **vazio**: 0 arquivos |
| 4 | ui | validação visual **humana** (a lista em "Validação visual") |
| 5 | — | item 8: o README |

O item 3 do backlog saiu desta lista no round 2: `intervaloMs` nas três
dificuldades está coberto e a partida longa virou soak. Os números e o que o
soak **não** pegou estão em "Round 2 — o item 3 do backlog".

Um `.exe` que abre não é um jogo pronto. O que existe hoje é um Snake jogável,
completo em regra, e **mudo, sem tema e sem efeito**.

O item 8 (README) é o último de propósito, e ele tem uma regra que este projeto
aprendeu duas vezes: **descrever o que o Snake tem, e não o que ele vai ter**.
Nada de "validado visualmente" sem o que foi validado, e nada de "testado" sem o
nome do teste. Se o README for escrito agora, ele descreve um jogo mudo sem skin,
e passa a mentir assim que os itens 5 e 6 existirem.

## Ícone e logo

Duas ferramentas, e o motivo de o mestre ser gerado por código:

- `tools/DesenharLogoSnake.java` desenha o `logo-256.png` a partir de Java2D. Um
  PNG binário que ninguém sabe refazer é defeito esperando — foi o que travou este
  projeto, e no Pong o `logo-256.png` foi committed pronto e nunca pôde ser
  regenerado. Aqui a arte sai da paleta que o jogo já usa (`DesenhoJogo`):
  gradiente `0x0B0E13 → 0x11161F`, corpo alternando `0x2BA85F`/`0x3DDC84` como o
  jogo alterna por segmento, cabeça `0x7CF0AC` com os olhos `0x0C1014`, e a comida
  `0xFF6B6B` com halo e brilho. O ícone é um frame do jogo, não uma cobra genérica.
- `tools/GerarLogo.java` deriva os 6 PNGs e monta o `.ico` (BMP com máscara AND até
  48px, PNG comprimido acima). O JDK não tem writer de ICO no `ImageIO`.

```bash
java tools/DesenharLogoSnake.java   # gera o mestre 256x256
java tools/GerarLogo.java           # deriva os 6 PNGs e o logo.ico
```

A diferença para a versão que veio do Pong: **a máscara AND é de verdade**. O bit 1
da máscara significa transparente, e a versão do Pong a escrevia zerada — correto
para uma imagem opaca, errado para esta. Aqui o bit 1 é posto onde o alpha fica
abaixo de 128, e as linhas são gravadas de baixo para cima como manda um
`BITMAPINFOHEADER` de altura positiva.

`LogoTest` (7 testes) confere o **produto**, não o gerador — `tools/` nem entra no
build. Verificado por mutação:

| Mutação | Cai |
|---|---|
| máscara AND zerada (o defeito do Pong) | 2 testes: `aEntradaPequenaDoIcoDeclaraTransparencia` e `aMascaraDoIcoConcordaComOAlphaDoPng` |
| fundo opaco, sem cobra e sem comida | 3 testes, incluindo `oMestreTemCantoTransparente` |

**O que estes testes não pegam, e é preciso escrever:** inverter a ordem das linhas
da máscara **não** é detectado. A única região transparente é a borda e os quatro
cantos do retângulo arredondado, e essa forma é simétrica na vertical — inverter as
linhas produz um `.ico` byte a byte idêntico, e os 7 testes continuam verdes
(verificado). Não existe entrada que diferencie as duas ordens com esta imagem. A
ordem está correta, mas a suíte não a vigia; `aTransparenciaDoLogoEhSimetricaNaVertical`
existe para registrar essa limitação, e se a transparência deixar de ser simétrica
um dia, ele passa a falhar e avisa que a cobertura mudou.

`TelaSnake` agora chama `setIconImages` com as 7 resoluções, em vez de
`setIconImage` com uma só — com uma, o Windows escala a imagem e o ícone fica
borrado na barra de tarefas.

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
- **`Comida`** — posição e valor, mais `sortearLivre` com número de tentativas
  limitado, o que é o terceiro erro clássico do Snake resolvido
- **`Poder`** — `FANTASMA` (5 s) e `PONTOS_X2` (10 s) com `Ativo`, relógio e
  renovação
- **`RegistroDeRecordes`** — um inteiro em `~/.jogo-snake-recorde`; arquivo
  corrompido devolve 0 em vez de impedir o jogo de abrir
- **`JogoSnake`** — as regras e a **ordem dos passos**, escrita e numerada no
  método: relógio dos poderes → direção e destino com a borda já corrigida →
  parede → mover (a cauda sai aqui) → vitória → colisão → pontos e nível →
  resorts. Dificuldade, pausa, briefing, reinício e recorde
- **`JogoSnakeTest`: 39 testes, todos verdes** — comer, `MORRE` mata e `WRAP` dá
  a volta, o corpo também dá a volta, comer na parede oposta no wrap, colidir
  com o corpo, cabeça na célula liberada, campo lotado com `timeout` de 2 s, os
  dois poderes com renovação em vez de empilhar, o fantasma segurando a parede
  e sendo inútil no wrap, pausa, reinício e a fila de direções
- **A vitória é checada antes de qualquer outra regra**, inclusive da parede
- **`Campo.dobrarParaDentro`** — dobra a coordenada sempre, para o fantasma
  segurar a parede sem tirar a cabeça da grade
- **`LayoutSnake`** — a geometria da tela separada da janela: onde ficam o HUD,
  a faixa de borda, o tabuleiro, o rodapé, os cards de poder e os painéis de
  sobreposição. É a mesma lição do Pong aplicada antes de o defeito existir, e
  não depois
- **`DesenhoJogo`** — o desenho fora da janela, pintando em qualquer
  `Graphics2D`. Com um `BufferedImage` do tamanho da janela como destino, um
  teste headless confere **os pixels que a tela mostraria**. Cores, tabuleiro,
  grade, comida, poderes, cobra com as 14 formas, olhos na direção do movimento,
  HUD, faixa de borda, rodapé e as três sobreposições
- **`TelaSnake`** — janela, laço de tempo e teclado. O tempo entre tiques é
  **medido**, não assumido: um travamento da máquina atrasa a cobra em vez de
  teletransportá-la através do próprio corpo. O teclado usa
  `WHEN_IN_FOCUSED_WINDOW` e não um `KeyListener`, porque com listener um clique
  em qualquer lugar tira o foco e o jogo deixa de responder sem explicar por quê
- **`LayoutSnakeTest`: 24 testes** e **`DesenhoJogoTest`: 13 testes**, ambos
  verificados por mutação
- **A janela abre de verdade** — processo no ar e título `Jogo Snake` encontrado
  pelo Windows. Isso prova que sobe, e não que está bonito: ver "Validação
  visual"
- **Ícone e `.ico`** — `DesenharLogoSnake` gera o mestre 256×256 por Java2D a
  partir da paleta do jogo, e `GerarLogo` deriva os 7 PNGs e monta o `.ico` com a
  máscara AND de verdade (a do Pong era zerada, o que declara a imagem toda opaca).
  `LogoTest` confere o produto em 7 testes, com 2 mutações verificadas e uma
  limitação registrada. `TelaSnake` passou a `setIconImages` com as 7 resoluções
- **Primeiro `.exe` do projeto** — `mvn clean package` verde com 98 testes, e o
  executável **abrindo** (`javaw.exe` no ar, janela `Jogo Snake`). É build
  verificado, não release: sem tag, sem publicação, sem push

### Round 2 — o item 3 do backlog

**`(b) intervaloMs` nas três dificuldades — 4 testes novos, e eles têm dentes**

O helper `jogo()` fixava `MEDIO`, então os números de `FACIL` e `DIFICIL` não eram
lidos por teste nenhum. Agora `em(dificuldade)` abre o jogo em qualquer uma das
três, e o que se vê é o valor exato: 170 / 150 / 130 ms no início, caindo 4 / 5 /
7 ms por nível, com o piso de 60 ms e o teto de nível 20.

Cinco mutações, cada uma caindo pelo teste cujo nome é a regra quebrada:

| Mutação | Testes que caem |
|---|---|
| trocar 170 ↔ 130 (fácil vira lenta, difícil vira fácil) | `oIntervaloInicialCaiComADificuldade`, `oIntervaloTemPisoDe60ms` |
| `reducaoPorNivel` de `FACIL`: 4 → 5 | `cadaNivelReduzOIntervalo...`, `oIntervaloTemPisoDe60ms` |
| **tirar o piso de 60 ms** | só `oIntervaloTemPisoDe60ms` |
| `nivelMaximo`: 20 → 999 | `oNivelParaNoTeto`, `oIntervaloTemPisoDe60ms` |
| `intervaloMs` ignorando o nível | 3 testes, incluindo o `comerSobeONivel...` que já existia |

O piso de 60 ms é o que segura a última: em `DIFICIL` a conta sem piso dá
`130 - 7*19 = -3`, e um `dt` negativo andaria a cobra para trás.

**(a) Partida inteira — `JogoSnakeSoakTest`, 2 testes**

20 000 passos de jogo de verdade, sem atalho: quando a partida acaba, outra começa
com a semente seguinte. Semente fixa, então o soak é reprodutível. A cada passo
são conferidos os invariantes — cabeça dentro do campo, **todos** os segmentos
dentro do campo, cobra entre 2 e o total de células, nível em faixa, intervalo
acima do piso, comida dentro do campo, e a cabeça nunca sobre o corpo enquanto a
partida continua.

O piloto é descartável e o próprio teste diz isso: ele existe só para a cobra não
morrer no terceiro passo. Não verifica comportamento do piloto.

**O quanto o soak realmente alcança** (medido, não torcido):

| | parede que mata | parede que dá volta |
|---|---|---|
| passos | 20 000 | 20 000 |
| partidas encadeadas | 20 | 18 |
| maior nível | **20** (o teto) | **20** (o teto) |
| maior comprimento | **92** | **99** |
| passos com poder ativo | 1 536 | 1 374 |

Nenhum teste de caso fixo chega em nível 20 nem em cobra de 90 segmentos. É
cobertura de espaço de estados, não de mutação.

**O soak não achou bug, e é preciso dizer.** Cinco mutações no núcleo foram
tentadas contra ele:

| Mutação | Pego pelo soak? | Pego por quem |
|---|---|---|
| S1 desligar a colisão com o corpo | sim | `baterNoCorpoTerminaAPartida` também pegava |
| S3 tirar o teto do nível | sim | `oNivelParaNoTeto` também pegava |
| S2 desligar a morte na parede | **não** | `aParedeMataQuandoABordaE_morre` pega |
| S4 deixar a cobra encolher abaixo de 2 | **não** | `encurtarNuncaDeixaMenosDeDoisSegmentos` pega |
| S5 fazer o poder não expirar | **não** | um teste de expiração pega |

Ou seja: **para as cinco mutações testadas, a suíte de casos fixos já pegava
tudo.** O soak não traz nenhum kill exclusivo — o que ele acrescenta é percorrer
as sequências longas. S2 e S4 escapam porque o piloto é bom demais: ele não
encosta na parede nem se atropela, então a variante quebrada simplesmente nunca
chega a ser exercitada. Isso é limite do piloto, não buraco na suíte.

**Um erro meu que vale registrar**: a primeira versão do harness de mutação
reportou "5 mutações, 2 testes caem" para todas — o mesmo par, sempre. Era falso.
O Maven não estava rodando (o `bash` do `PATH` do Python é o relay do WSL, que
não está instalado), e o harness lia o XML de uma execução anterior. Só apareceu
porque as cinco linhas eram idênticas, o que é impossível para mutações
diferentes. **Harness que não confere o código de retorno mente com confiança.**

### Round 3 — o item 6 do backlog: os sons

**31 testes novos**: 15 no `Sons`, 13 na `Trilha` e 3 no motivo da derrota, dentro
do `JogoSnakeTest`. Os quatro efeitos são gerados por `tools/GerarSons.java` (PCM
16-bit, mono, 44100 Hz), então o repositório não carrega binário de terceiros.

O teste que importa é `oSonsDeVerdadeCarregaOsSonsDeDentroDeUmJar`: ele empacota
o **próprio `Sons`** num jar e o carrega com `ClassLoader.getPlatformClassLoader()`.
Isso é o que transforma a regressão do Pong em algo que morde — verificado por
mutação, entregar o fluxo do classpath direto ao decoder derruba **10 de 15**.

| Mutação | Testes que caem |
|---|---|
| **A7**: fluxo do classpath direto ao decoder (o defeito do Pong) | **10 de 15** |
| A1: tirar o `BufferedInputStream` só do `leBytes` | 0 — e está certo: a implementação lê para a memória e decodifica de `ByteArrayInputStream`, então o buffer ali é defesa, não carga |
| Trilha M1: som de morte invertido | `morrerNaParedeTocaParedeEFim` |
| Trilha M2: deixar "comer" soar no mesmo passo da morte | `oPassoQueComeEMata...` |
| Trilha M3: parede soar em qualquer morte | `morrerNoCorpoTocaSoFim` |
| Trilha M4: não exigir que a partida estivesse jogando | `umTiqueComAPartidaJaTerminada...` |
| Trilha M5: comida contando `>=` | `umTiqueComAPartidaJaTerminada...` |
| Trilha M6: poder já ativo soar de novo | `umPoderQueContinuaAtivo...` |

**O motivo da morte no núcleo.** Os dois jeitos de morrer são `Estado.FIM`, então a
interface não tinha como escolher entre dois sons diferentes. `JogoSnake.Motivo`
(`PAREDE`/`CORPO`) existe só para a tela ler — o núcleo não decide nada com ele. E
ele é apagado no `reiniciar`, senão a partida nova morria na parede com o som da
colisão no corpo.

**A regra de som mora fora da janela.** `Trilha` compara o retrato de antes do passo
com o de depois e devolve a lista de efeitos. Dentro da `JFrame` isso não teria
teste, e regra de jogo sem teste é regra que alguém "ajusta" sem querer — o mesmo
motivo que levou `DesenhoJogo` e `LayoutSnake` para fora.

**Validação humana: o som tocou no `.exe`.** O usuário abriu `target/jogo-snake.exe`
e ouviu o efeito. É o passo que faltava no Pong, e é o único que a suíte não faz:
ela prova que os bytes decodificam, não que alguém ouve alguma coisa.

### A thread que foi desfeita

A primeira versão carregava os sons numa **thread daemon** "para não travar a
janela". Era otimização sem alvo: o que é caro — `AudioSystem.getClip()` e
`clip.open()` — acontece em `tocar`, dentro do `Timer`, ou seja, **na EDT**, e
continua lá. A thread só tinha tirado da EDT a parte barata: ler 102 KB e parsear
quatro cabeçalhos WAV.

O que ela custou: um `AtomicBoolean` para `preparar()` ser idempotente, um
`ConcurrentHashMap` no lugar de um `EnumMap` (porque duas threads escreviam no
mesmo mapa), dois testes de concorrência e a discussão de publicação segura do
`byte[]`.

Foi desfeita. Hoje `Sons.carregarTodos()` é um laço síncrono, o cache é um
`EnumMap` e a tela chama uma vez no `iniciarRelogio()`.

**Um registro honesto sobre aqueles dois testes:** a primeira versão rodava as
threads na `Sons` da suíte, com o cache **já quente** — então só exercitava
leituras concorrentes, que são inofensivas. Verificado por mutação, não pegava nem a
reintrodução do `EnumMap`: 0 em 3 execuções. Além disso podiam travar para sempre
num `CyclicBarrier` e deixar o JVM aberto segurando os WAVs, o que chegou a fazer o
`mvn clean` falhar. Nada disso era guarda do cache: era uma corrida fabricada, e a
linha de defesa real do `byte[]` é o `ConcurrentHashMap`, não um teste de stress.

### Bugs reais que os testes acharam (e que a leitura não pegaria)

1. **A cobra ignorava as viradas do jogador.** `consumirDirecao()` e
   `avancarPara()` puxavam da fila. Com uma virada enfileirada o jogo funcionava,
   porque a segunda leitura caía no caso "fila vazia". Com duas viradas no mesmo
   tique, a segunda leitura consumia a virada seguinte: a cabeça andava para a
   primeira direção e o campo `direcao` ficava com a segunda. O jogador virava, a
   cobra obedecia por um tique e voltava a andar para onde ia antes — e
   apertar duas teclas seguidas ainda estourava `NoSuchElementException`.
   Verificado por mutação: reintroduzir o segundo consumo derruba 4 testes.
2. **O fantasma tirava a cabeça do campo.** `Campo.traduzir` só dobra a
   coordenada no modo `WRAP`, então com o fantasma segurando uma parede que mata
   a cabeça saía da grade — e o próprio javadoc dizia que ela daria a volta.
3. **A partida travava com o campo lotado.** A vitória era checada dentro do
   `if (comeu)`, mas um campo lotado não tem onde pôr comida, então o jogador
   preenchia a grade e o jogo nunca declarava vitória. Pior: checada depois da
   parede, a cabeça encostada na parede morria no instante em que ganhou.
4. **Dois testes de colisão dependiam do sorteio da comida.** A cobra comia por
   acaso, o corpo crescia, a cauda não saía e a colisão acontecia em outro lugar:
   passavam ou falhavam por causa da semente, e não da regra. A comida agora é
   posta fora do caminho nos dois.
5. **O selo de "novo recorde" piscava.** O desenho da tela de fim decidia *e
   gravava* o recorde, e como `RegistroDeRecordes.registrar` só aceita número
   maior que o já salvo, a primeira repinturação gravava e mostrava o selo, e a
   segunda lia o mesmo número de volta, concluía que não era recorde e **apagava
   o selo**. Um defeito de uma única repinturação, invisível para a lógica do
   jogo — que estava correta — e para qualquer teste de regra, porque o problema
   estava num pixel e num `if` dentro do desenho. A decisão saiu do desenho e foi
   para o laço do jogo, que grava uma vez no instante em que a partida acaba.
   Este é o **mesmo tipo de defeito do badge do Pong** — estado de interface
   decidido no método de pintura — e é a razão de `DesenhoJogo` existir separado
   da `JFrame`.

   O teste que o prende é `desenharNaoGravaORecorde`, e ele só ficou bom depois
   de uma tentativa que **não pegava nada**: a partida de teste terminava com
   zero pontos, então a linha que grava o recorde nunca era alcançada e o teste
   passava com o defeito presente. Corrigido o cenário para a cobra de fato
   comer antes de morrer, a reintrodução do defeito derruba o teste. Um teste
   verde não prova nada até se ver ele cair.

O padrão é o mesmo do Pong: **a ordem das regras não dá exceção, ela só faz o
jogo se comportar errado**. Três dos quatro bugs acima passaram pelo código
parecendo certo. E o quinto tinha outra forma: a lógica estava certa e o desenho
mentia.

## A camada `skin`: paleta e os sprites do Dedinho

Adecided a ordem depois de olhar a conta: 14 formas × 4 skins = **56 PNGs**, cada um
com alfa, com a variante de cor da sua skin. Isso é um dia de arte, não um turno.
A paleta entrega o item 5 na mesma forma que o jogador sente — a cobra muda de cor
com `[N]` — e deixa o `SkinSprites` como um incremento próprio, sem reescrever o
que já está testado.

### O que existe

- `Skin`: valor imutável, igualdade por valor. Sem `equals`, uma skin lida do disco
  nunca seria igual à do catálogo, e o `==` do `TelaSnake` nunca bateria.
- `CatalogoSkins`: as três paletas, `porId` (case-insensitive) e `seguinte`, que
  devolve **uma skin nova** e não mexe na instância guardada.
- `RegistroDeSkin`: `~/.jogo-snake-skin.properties`, chave `skin=<id>`.
- `DesenhoJogo` recebe a skin no construtor; `TelaSnake` troca e salva.

O que a skin **não** toca: `JogoSnake` não sabe que skins existem. Pontuação,
tamanho e velocidade não mudam ao trocar, e o teste de pixels usa o mesmo
`JogoSnake` para as três — se a troca tivesse mexido na partida, o desenho mudaria
por outro motivo e o teste não provaria nada.

### Os sprites do `Dedinho`: 4 imagens, 14 peças

A primeira tentativa pediu **uma folha 7×2** com as 14 peças, num gerador de
imagem. A folha veio com 14 ilustrações *sem relação entre si*: dentro de cada grupo
(cantos, rabos, cabeças) o melhor casamento entre duas peças foi de 44% a 66%, e
cerca de metade de cada PNG é fundo transparente — ou seja, os desenhos opacos
praticamente não se pareciam. A arte também invadia as fronteiras das células (a
cobertura de alfa é contínua nos limites da grade), então 12 das 14 peças saíram do
tamanho inteiro da célula. Girar não corrigiria aquilo, porque não eram a mesma
forma em quatro orientações.

A virada foi pedir **uma peça por imagem** e derivar o resto em código:

| Imagem pedida | Peças por giro de 90° |
|---|---|
| corpo deitado | `RETO_H`, `RETO_V` |
| cotovelo | `CANTO_NO`, `CANTO_NE`, `CANTO_SE`, `CANTO_SO` |
| cabeça com unha para a direita | `CABECA_DIREITA`, `CABECA_BAIXO`, `CABECA_ESQUERDA`, `CABECA_CIMA` |
| rabo com a ponta para cima | `RABO_CIMA`, `RABO_DIREITA`, `RABO_BAIXO`, `RABO_ESQUERDA` |

Simetria perfeita deixa de ser um pedido ao gerador e vira consequência da
construção. As 12 peças derivadas são giros exatos a 100,00% — verificado byte a
byte, não a olho.

`tools/RecortarDedinho.java` faz o resto: mede a cor real do fundo, remove o verde
em duas passadas, recorta na caixa da arte e gira. Os ângulos **não** estão
escritos à mão — a ferramenta traduz cada `Forma` num conjunto de lados que ela
ocupa, gira o conjunto, e procura o giro que leva o canônico até o alvo. Uma tabela
"`canônico` 0°, 90°, 180°, 270°" escrita à mão é exatamente o tipo de coisa que sai
transposta sem ninguém perceber: quatro peças viradas e o defeito só aparece
jogando. E saiu.

A base medida de cada grupo está escrita na chamada da ferramenta, e **não é o que
o prompt pediu**: o cotovelo foi pedido "entra pelo topo e sai pela direita"
(`CANTO_NE`) e veio espelhado, com o entalhe olhando para a base-direita e as pernas
para cima e para a esquerda, que é `CANTO_NO`.

### Três armadilhas que nenhuma falha de compilação denuncia

1. **O cotovelo é a única peça que não se descobre pelo nome.** A orientação das
   cabeças e dos rabos sai da direção da unha e da ponta, ambas medíveis. Num
   cotovelo não há unha, e a assimetria só aparece olhando **qual quadrante da
   caixa da arte está vazio** — o lado côncavo. Foi a leitura do quadrante vazio
   que revelou o espelhamento.
2. **JPEG não tem canal alfa, e `setRGB(0x00000000)` nele grava *preto*.**
   `ImageIO.read` devolve JPEG como `TYPE_3BYTE_BGR`; escrever transparência ali
   não cria transparência. O resultado era um PNG **100% opaco** com um retângulo
   preto em volta do dedo — e ele passava por dois testes, porque a caixa da arte
   saía certa (o preto é lido como arte) e a contagem de verde dava zero (preto não
   é verde). Só a inspeção do alfa revelou. A correção é converter para ARGB antes
   de tocar nos pixels, e a ferramenta agora **recusa gravar** se nenhum pixel
   ficar transparente: o mesmo silêncio não repete.
3. **O verde sai por dominância, não por proximidade.** Todas as cores do Dedinho
   têm o vermelho na frente (pele `rgb(232,176,138)`, unha `rgb(247,217,200)`,
   contorno `rgb(58,36,24)`), então "o verde manda sobre os outros dois canais" é o
   teste que separa o resíduo da pele. E a cor do fundo **não** pode ser a do
   prompt: as quatro fontes vieram entre `rgb(38,235,24)` e `rgb(55,226,34)`, e
   nenhum verde é o `#00FF00` pedido. Medir a borda resolve; zerar a constante não.

### O recuo da célula é do desenho, não da grade

O `folga` de 0,12 que afasta a arte dos limites da célula existe para o **vetorial**:
formas simples ficam separadas por um respiro, e é isso que faz a cobra ser lida
como segmentos e não como um retângulo único. Num PNG o recuo é **errado** — a arte
já vem com a própria margem desenhada dentro dela, então recuar de novo abre uma
fresta entre peças que deveriam encostar. No Dedinho o dedo saía picotado. A escolha
passou a ser por origem da arte: sprite preenche a célula, vetorial mantém o
respiro. Medido: 84 pixels contínuos na linha do corpo, zero frestas.

### Arquivo seguro, e por que o teste segura o disco

Um arquivo de preferência corrompido não pode impedir o jogo de abrir. O
`carregar` tem três camadas: arquivo ausente → padrão; chave ausente ou vazia →
padrão; id fora do catálogo → padrão; e o `catch (IOException | RuntimeException)`
por cima, para arquivo truncado ou lixo. O teste redireciona `user.home` para uma
pasta temporária e restaura no `@After`, então não toca no disco de verdade.

### Mutação (7 tentativas, 6 derrubadas)

| Mutação | Cai |
|---|---|
| `DesenhoJogo` volta à cor fixa em vez de `skin.getCorpo*` | `DesenhoJogoTest` |
| `DesenhoJogo` ignora a sombra da skin | `DesenhoJogoTest` |
| `porId` devolve o padrão em vez de `null` | `CatalogoSkinsTest` |
| `seguinte` sem `% TODAS.size()` (não dá a volta) | `CatalogoSkinsTest` |
| `porId` deixa de ignorar caixa | `CatalogoSkinsTest` |
| gravar posição em vez de id | `RegistroDeSkinTest` |
| **ler id desconhecido devolve a skin sem conferir** | **ninguém** |

A sétima **não é um furo, e é instrutiva**: a mutação introduz um
`NullPointerException`, e o `catch (RuntimeException)` do `carregar` engole a
exceção e devolve o padrão. O desfecho observável — "o jogo abre" — é o mesmo,
porque a garantia está duplicada: verificação de nulo *e* `catch`. Um teste no
desfecho não consegue dizer qual dos dois salvou. Preferi registrar isso a forçar
um teste que só passaria por acidente. Se algum dia o `catch` for estreitado, o
teste do desfecho continua valendo e passa a ser o único aviso.

### Dois erros meus que os testes pegaram antes de eu ver

1. **O teste de sombra passava vazio.** Eu escrevi "cada skin pinta diferente da
   clássica" e achei que cobria a sombra. Não cobria: as skins já diferem no corpo,
   então a imagem mudava de qualquer jeito e a sombra poderia estar fixa sem
   ninguém notar. Só apareceu na mutação, que não caiu. O teste que pega a sombra
   usa duas skins **idênticas em tudo menos a sombra**.
2. **E esse segundo teste também passava vazio**, por um motivo diferente: com 3
   segmentos o canto é um instante — assim que a cabeça dá um passo além da
   virada, o canto rolou para fora e a cobra volta a ser reta. Pior: a cobra
   começa indo para CIMA, então `virar(BAIXO)` era inverter e **foi recusado**,
   e o cenário virava "cobra reta" sem reclamar. Agora o teste faz a cobra crescer
   antes de virar, e afirma que o canto existe **antes** de olhar os pixels. É a
   armadilha do "cenário que não alcança a linha testada", e ela apareceu duas
   vezes no mesmo teste.

Também: `apagarRecursivamente`, no `@After`, assumia que o caminho era diretório e
estourou `NotDirectoryException` ao receber o arquivo. Os 9 erros vermelhos que
vieram daí eram do helper, não do código de produção — mas o helper errado
esconderia um defeito real, então virou `Files.isDirectory` + `exists`.

### Validação humana que ainda falta

Os 163 testes provam que a skin muda os pixels. Não provam que está bonito.

**Já validado a olho pelo autor**: as três paletas, o `Dedinho` com a cabeça e o
rabo na direção certa e as peças sem fresta. Foi essa validação que achou o defeito
de core abaixo.

**Ainda falta**: fechar e reabrir o `.exe` para conferir que a escolha persiste, e
verificar se a linha do briefing não colide com nada.

## Bug 6: a cabeça e o rabo apontavam para trás

**Onde**: `Cobra.formas()`, nos dois extremos do corpo.

**O que acontecia**: uma cobra andando para a **direita** recebia `CABECA_ESQUERDA` e
`RABO_DIREITA`. O nome da cabeça é a direção para onde ela vai, e a do rabo é a
direção de onde ele vem; mas `formas()` passava, nos dois extremos, a direção
**para dentro** do segmento — cabeça→pescoço, e pescoço→rabo. `Forma.deCabeca` e
`deRabo` esperavam o contrário.

**Quem foi atingido**: todo mundo. Os olhos das skins vetoriais estavam virados desde o
começo, e só não apareceram porque um vetorial todo bege com olhos levemente
trocados passa despercebido. As sprites do Dedinho transformaram um defeito
invisível em óbvio — que é o melhor argumento a favor de sprites: eles não
introduzem o bug, eles o denunciam.

**O detalhe que confirma que era convenção e não engano**: os javadocs do `Forma`
diziam *"Primeiro segmento, com a direção de saída"* desde o começo, e estavam
**certos**. Quem violava a convenção era o `formas()`. A correção alinhou o código
com a própria documentação, e não o contrário.

**Por que passou por 161 testes** (e o que foi feito depois): nenhum teste afirma qual `Forma` a cabeça recebe.
O único que olha `formas()` usa `ehCanto()`, e canto não muda com a inversão — no
meio do corpo entrada e saída viram junto, então o nome do canto é o mesmo. A classe
que mais.testing tinha era a que estava errada, e nenhum teste cobria o eixo
errado.

**O que foi feito depois**: `CobraTest` ganhou `aCabecaApontaParaOndeACobraAnda` e
`oRaboApontaParaForaDoCorpo`. O primeiro percorre uma espiral de quatro lados
affirmando que a cabeça bate com a direção de movimento; o segundo monta a
expectativa numa tabela escrita à mão, de propósito — derivá-la de `Forma` seria
circular e o teste passaria com o defeito presente. Reintroduzi o bug: os dois
caem com `expected:<CABECA_DIREITA> but was:<CABECA_ESQUERDA>` e
`expected:<RABO_ESQUERDA> but was:<RABO_DIREITA>`, e passam com o fix.

**A armadilha da família**: é a mesma do "cenário que não alcança a linha testada".
Um teste que afirma "a cabeça não aponta para trás" precisa montar uma cobra que
ande para a direita; montar uma parada ou andando para a esquerda deixa o defeito
invisível.

## Números

| | Pong | Snake |
|---|---|---|
| Testes | 180 | **231** |
| Testes em `ui` | 27 (só o `BotaosTest`) | **118** (30 de layout + 28 de pixels + 7 do ícone + 25 de botão + 13 de menu + 11 da janela) |
| Classes de `core` sem teste | — | `Campo`, `Direcao`, `Celula`, `Forma`, `Comida`, `Poder`, `RegistroDeRecordes` |
| Bugs reais achados por teste | 2 (o hover e o prêmio congelado, ambos na 1.0.1) | **9** (4 por teste de regra, 1 por teste de pixel, 4 pelos testes do menu) |
| Bugs achados por validação visual | 0 | **2** (a cabeça e o rabo para trás, bug 6; e os rótulos do menu debaixo dos botões) |
| Linha de comando | — | `/c/Users/dudu2/.m2/wrapper/dists/apache-maven-3.9.11/d6d3cbd4012d4c1d840e93277aca316c/bin/mvn` (o `mvn` não está no `PATH`) |

A coluna do Pong é a da `v1.0.1`. A do Snake é depois do item 6, e **231 testes não
são 231 de cobertura**: `Campo`, `Direcao`, `Celula`, `Forma`, `Comida`, `Poder` e
`RegistroDeRecordes` continuam sem teste próprio, e são classes cujas regras o
`JogoSnake` só usa por inteiro.

## O menu com botões, e os 4 bugs que ele escondeu

O briefing era texto: as opções de borda e dificuldade eram linhas soltas, sem doc,
e o jogador escolhia com `[1]`–`[5]`. Viraram **8 botões** clicaveis, no padrao do
`Botao`/`Botaos` do Pong: borda (2), dificuldade (3), começar, trocar skin e
recomeçar. Visual dark com destaque dourado, e os atalhos antigos continuam.

O que o menu **não** fez foi ficar quieto. Os 4 defeitos abaixo sairam todos
da mesma confusão: "escolher" e "começar" eram a mesma operação.

### Bug 7: escolher a dificuldade ja começava a partida

`aplicarEscolha` e os botões chamavam `reiniciarCom`, que terminava em
`comecarPartida`. Escolher qualquer coisa sumia com o menu — e não sobrava
escolha nenhuma a fazer.

Corrigido separando os dois conceitos: `montarPartida` deixa o jogo no menu,
`comecarPartida` so começa. Agora a opção escolhida fica em dourado e o menu
continua em pe.

### Bug 8: a tecla apagava a outra escolha

`aplicarEscolha` decidia borda **e** dificuldade a partir do mesmo codigo: `[2]`
escolhia wrap e voltava a dificuldade para médio; `[3]` escolhia fácil e forçava
"parede mata". Era a complaint do autor, palavra por palavra: não dava para escolher
as duas coisas.

Cada tecla passou a mexer em **um** campo so, lendo o outro da partida atual — a
mesma regra que `acaoDe` ja usava para os botões. O teclado simplesmente nunca
teve essa regra.

### Bug 9: numero no meio do jogo reiniciava a corrida

`[1]`–`[5]` trocavam a partida inteira a qualquer momento, zerando o progresso
por uma tecla que o jogador nao associou a reiniciar. Agora so valem no menu
(`JogoSnake.noInicio()`). Para voltar ao menu durante o jogo, `[R]`.

`noInicio()` virou metodo do nucleo de proposito: o desenho decide entre menu e
tabuleiro, e a janela decide se as teclas valem alguma coisa. Duas copias da mesma
condição e uma delas esquecida num dia dariam o menu em cima de uma partida.

### Bug 10: a troca de skin calava os botões

`trocarSkin` recriava o pintor com o construtor de 2 argumentos, que nasce com
`AcoesMenu.NENHUMA`: depois do primeiro `[N]`, **todo** o menu ficava morto. O
construtor de 3 argumentos existe justamente para ligar o menu a janela, e foi
esquecido nesse ponto.

Corrigido reduzindo a construção do pintor a uma fabrica so
(`novoDesenho`): não ha mais onde esquecer as ações.

De passagem, o registro de botões passou a ser limpo no começo de cada quadro.
Antes os retangulos do briefing continuavam vivos depois que a partida começava, e
um clique no canto inferior durante a pausa voltava para o menu por baixo da tela
de pausa.

### O teste que passava verde e não provava nada

`DesenhoJogoTest` tinha um `cliqueEAtalhoPassamAMesmaEscolha` que **nao testava o
clique**: ele chamava a acao direto e depois criava uma `TelaSnake` para contar que
a tecla tambem mudeava o estado. O botao podia estar morto que o teste passava — e
passava. Por isso 210 testes verdes nao pegaram nenhum dos 4 bugs.

`TelaSnakeTest` (7 testes) entra no lugar certo: paint, e entao **clique de
verdade** no registro de botões. Os 4 bugs foram reintroduzidos um a um para
confirmar que cada teste cai — e caem.

### Os rotulos ficavam debaixo dos botoes

Achado pela validação visual do autor. Os rotulos `BORDA` e `DIFICULDADE` eram
desenhados em `painel.y + 92` e `+140`, com os botoes começando em `+96` e `+144`:
o rotulo entrava **dentro** da faixa do botao, e como o botao e pintado depois, o
preenchimento tapava o texto.

Cada rotulo ganhou faixa propria **acima** do seu grupo, e as posições do texto
saíram do desenho para o `LayoutSnake` — `titulo`, `subtitulo`, `rotuloBorda`,
`rotuloDificuldade` e `dica` agora sao geometria testavel, como os badges do Pong.
`rotuloEm()` com numeros magicos foi embora. `MenuSnakeTest` ganhou 4 testes que
proibem um rotulo de intersectar um botao.

**Verificado por execução**: 221 testes verdes, `BUILD SUCCESS`,
`target/jogo-snake.exe` gerado.

## A tela de fim ganha botao, e o texto que mentia

Faltava o botao no fim da partida. O registro e por quadro e a tela de fim nao
desenhava nenhum, entao o clique nao tinha onde cair: so o teclado servia.

Antes de codar, uma leitura respondeu por que "recomecar" e "voltar ao menu"
seriam o mesmo botao. `reiniciar()` deixa a partida em `PAUSADO` com ponto zero e
passo zero, que e exatamente o estado do menu — `noInicio()`. Logo os dois verbos
levavam ao mesmo lugar, e dois botoes iguais na mesma tela e pior que um botao so.

### A armadilha do `iniciar`

`JogoSnake.iniciar()` so sai do estado pausado:

```java
public void iniciar() {
    if (estado == Estado.PAUSADO) {
        estado = Estado.JOGANDO;
    }
}
```

O botao "jogar de novo" na tela de fim nao podia se limitar a chamar `comecar`:
a partida esta morta, `iniciar()` nao faria nada, e o botao seria um botao que
nao faz nada. O caminho e sempre dois passos — `reiniciar` e depois `comecar` —
e por isso `jogarDeNovo` existe separado no contrato, com o `comecar` de antes
intacto para o menu, onde a partida ainda esta pausada.

`Enter` na tela de fim sofre o mesmo vacuo e nao foi mexido: `comecarPartida`
chama `iniciar()`, que nao sai de `FIM`. E um no-op inofensivo, e nao um defeito
de tela — mexer nisso mudaria o que `Enter` faz no menu sem ninguem pedir.

O `[R]` continua voltando ao menu como no resto do jogo. O texto da tela dizia
"[R] jogar de novo", e isso nunca foi verdade: `R` vai para o menu. O texto passou
a dizer "[R] menu", que e o que a tecla faz. Nenhuma tecla mudou de sentido.

### Onde a geometria do texto estava

`desenharFim` posicionava as seis faixas de texto com numeros magicos dentro do
`paintComponent` (`p.y + 26`, `p.y + 64`, `p.y + 100`...), e e o mesmo defeito dos
rotulos do menu: posicao nascendo na pintura e invisivel para qualquer teste.
Virou `LayoutSnake.Fim`, com painel, seis faixas e os dois botoes, e `fim()` faz
o mesmo clamp que `menu()`. `Menu` e `Fim` implementam `LayoutSnake.Alvos` para
que o desenho registre botao nas duas telas sem duplicar o corpo — duplicar era
exatamente como a troca de skin conseguiu dar um pintor sem acoes.

A posicao dos botoes nao depende de ter ou nao recorde. Se dependesse, quem
acabasse de bater o recorde receberia o clique num ponto diferente do que viu no
quadro anterior.

### O teste de pixel que nao podia falhar

A mutacao "o selo de recorde some da faixa" **nao caiu**, e o motivo e o bom.

O selo "NOVO RECORDE" e dourado. O **titulo** da tela de fim tambem: quando ha
recorde novo, `centralizar(..., novoRecorde ? DESTAQUE : TEXTO)`. Os tres testes
do selo procuravam o dourado no painel inteiro, entao passavam por causa do
titulo — apagando a linha inteira que desenha o selo, seguiam verdes. Um teste que
nao distingue o que verifica.

Corrigido com `faixaDoSelo()`, que devolve `layout.fim().recorde`: a regiao passa
a ser a faixa do selo, que e o que separa as duas coisas. A mutacao agora cai, e
tambem cai quando o selo e desenhado na faixa errada.

**Verificado por execucao**: 231 testes verdes, `BUILD SUCCESS` e
`target/jogo-snake.exe` gerado. Oito mutacoes aplicadas uma a uma; as oito caem.
