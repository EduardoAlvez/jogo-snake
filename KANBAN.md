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
| `testes-jogo-snake` | desenvolvimento | `1.0-SNAPSHOT` | pom com JUnit + Surefire + JaCoCo, e os 129 testes em `src/test` |
| `main` | release | a versionar na release | **sem** `src/test` e sem JUnit, Surefire ou JaCoCo |

A diferença para o Pong é deliberada. Lá a `main` também guarda `src/test` e a
dependência do JUnit, porque a `main` foi criada a partir da branch de testes na
hora da release (`testes-jogo-pong` é ancestral de `main`, e a única diferença
entre as duas são três arquivos de documentação). O resultado ficou oposto ao
que o nome da branch sugere. Aqui a `main` carrega só o jogo.

O histórico dos 4 primeiros commits ainda contém os arquivos de teste, porque
eles foram escritos antes de a separação existir. A partir do commit que criou a
`main`, nenhum arquivo de teste entra nela.

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
| 1 | ~~Alta~~ | ui | ~~**`TelaSnake` não existe**~~ | **Concluído.** Janela, tabuleiro, cobra com as 14 formas, HUD (pontos, recorde, nível, comprimento), faixa de borda, rodapé com cards de poder, pausa, briefing com escolha de borda e dificuldade, tela de fim com selo de novo recorde, e o laço de tempo que mede o tempo decorrido entre tiques (um travamento da máquina atrasa a cobra, não a teletransporta). A janela **abre de verdade** — ver "Validação visual" |
| 2 | ~~Alta~~ | build | ~~**`logo.ico` e `tools/GerarLogo.java` ausentes**~~ | **Concluído.** `tools/DesenharLogoSnake.java` desenha o mestre `logo-256.png` por código, e `tools/GerarLogo.java` deriva os 6 PNGs menores e monta o `.ico`. Ver "Ícone e logo" abaixo |
| 3 | — | core | ~~**`JogoSnake` testado só nas regras básicas**~~ resolvido no round 2: os 32 testes de `JogoSnakeTest` cobrem comer, as duas bordas, colidir com o corpo, a cabeça na célula liberada pela cauda, campo lotado, os dois poderes com renovação, pausa, reinício e a fila de direções; **mais** (a) `JogoSnakeSoakTest`, que joga 20 000 passos reais em partidas encadeadas, e (b) `intervaloMs` nas três dificuldades, com o piso de 60 ms e o teto de nível. O (c) — recorde em disco — já estava resolvido em `DesenhoJogoTest`, que aponta `user.home` para uma pasta temporária e apaga tudo depois. **O soak não achou bug** — ver "Round 2" |
| 4 | ~~Alta~~ | ui | ~~**Testes de UI desde o começo**~~ | **Concluído: 37 testes em `ui`** (24 de layout + 13 de desenho), e ambos os grupos foram verificados por mutação. `LayoutSnake` cobre a geometria e achou um defeito real de 8px entre tabuleiro e rodapé. `DesenhoJogo` cobre os pixels e achou o selo de recorde piscando (bug 5 abaixo) |
| 5 | Média | skin | As 4 skins | `Skin` + `CatalogoSkins` + `SkinVetorial` (a base, sempre presente) + `SkinSprites` (14 tiles: 4 cabeças, 4 rabos, 2 corpos retos, 4 cantos, com fallback vetorial se o PNG faltar). `Clássico`, `Chapéu`, `Colorida` e `Dedinho`, com persistência em `~/.jogo-snake-skin.properties` e troca sem reiniciar a partida |
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

O item 2 está resolvido e o `package` passou. Isso é **build verificado**, que é uma
coisa diferente de release: prova que o launch4j acha o ícone, que o jar monta e
que o executável abre. Não prova que o jogo está acabado, e o que falta está
escrito na lista abaixo para não se perder de vista.

### O que o build verificado cubriu

- `mvn clean package` verde, **129 testes** (85 + 7 de `LogoTest` + 6 do round 2 + 31 de `audio`)
- `target/jogo-snake.exe` gerado, e **abrindo de verdade**: processo `javaw.exe` no
  ar com janela de título `Jogo Snake`
- `logo.ico` embutido no executável, `logo-{16..256}.png` no classpath (8 recursos
  no jar), `Main-Class` correta, e **zero `*Test.class` no artefato**

Nada disso foi publicado: **sem tag, sem release, sem push**. Não é entrega.

### O que ainda falta para o jogo estar pronto

| | Camada | O que falta |
|---|---|---|
| 1 | `skin/` | as 4 skins do briefing. O pacote existe **vazio**: 0 arquivos |
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
- **`JogoSnakeTest`: 32 testes, todos verdes** — comer, `MORRE` mata e `WRAP` dá
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

## Números

| | Pong | Snake |
|---|---|---|
| Testes | 180 | **129** |
| Testes em `ui` | 27 (só o `BotaosTest`) | **44** (24 de layout + 13 de pixels + 7 do ícone) |
| Classes de `core` sem teste | — | `Campo`, `Direcao`, `Celula`, `Forma`, `Comida`, `Poder`, `RegistroDeRecordes` |
| Bugs reais achados por teste | 2 (o hover e o prêmio congelado, ambos na 1.0.1) | **5** (4 por teste de regra, 1 por teste de pixel) |
| Linha de comando | — | `/c/Users/dudu2/.m2/wrapper/dists/apache-maven-3.9.11/d6d3cbd4012d4c1d840e93277aca316c/bin/mvn` (o `mvn` não está no `PATH`) |

A coluna do Pong é a da `v1.0.1`. A do Snake é depois do item 6, e **129 testes não
são 129 de cobertura**: `Campo`, `Direcao`, `Celula`, `Forma`, `Comida`, `Poder` e
`RegistroDeRecordes` continuam sem teste próprio, e são classes cujas regras o
`JogoSnake` só usa por inteiro.
