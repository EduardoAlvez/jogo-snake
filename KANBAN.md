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
└── GerarLogo.java

target/                                   # Saída do build (não versionada)
```

## Branches

Mesma convenção do Pong, e mais limpa:

| Branch | Papel | Versão | Testes |
|---|---|---|---|
| `testes-jogo-snake` | desenvolvimento | `1.0-SNAPSHOT` | pom com JUnit + Surefire + JaCoCo, e os 85 testes em `src/test` |
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
| 2 | **Alta** | build | **`logo.ico` e `tools/GerarLogo.java` ausentes** | O launch4j aponta para `src/main/resources/logo.ico`, que não existe, então o `package` falha na hora de montar o `.exe`. Falta o gerador dos PNGs por resolução e o `.ico` |
| 3 | Média | core | **`JogoSnake` testado só nas regras básicas** | Os 32 testes de `JogoSnakeTest` cobrem comer, as duas bordas, colidir com o corpo, a cabeça na célula liberada pela cauda, campo lotado, os dois poderes com renovação, pausa, reinício e a fila de direções. **Falta**: (a) a semente fixa é usada, mas nenhum teste roda a partida inteira por muitos passos para caçar comportamento intermitente; (b) nenhum teste cobre `intervaloMs` nas três dificuldades; (c) ~~o recorde nunca é gravado em disco nos testes~~ resolvido em `DesenhoJogoTest`, que aponta `user.home` para uma pasta temporária e apaga tudo depois |
| 4 | ~~Alta~~ | ui | ~~**Testes de UI desde o começo**~~ | **Concluído: 37 testes em `ui`** (24 de layout + 13 de desenho), e ambos os grupos foram verificados por mutação. `LayoutSnake` cobre a geometria e achou um defeito real de 8px entre tabuleiro e rodapé. `DesenhoJogo` cobre os pixels e achou o selo de recorde piscando (bug 5 abaixo) |
| 5 | Média | skin | As 4 skins | `Skin` + `CatalogoSkins` + `SkinVetorial` (a base, sempre presente) + `SkinSprites` (14 tiles: 4 cabeças, 4 rabos, 2 corpos retos, 4 cantos, com fallback vetorial se o PNG faltar). `Clássico`, `Chapéu`, `Colorida` e `Dedinho`, com persistência em `~/.jogo-snake-skin.properties` e troca sem reiniciar a partida |
| 6 | Média | audio | Sons e o silêncio no executável | Regressão do Pong: o áudio era silencioso no `.exe` e só apareceu em teste que **toca de dentro de um jar**, com `BufferedInputStream` desde a primeira linha. Comer, poder, bater na parede e fim de partida |
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
- o ícone na barra de tarefas e no Alt+Tab (depende do item 2)
- o recorde sobreviving ao fechamento da janela

## Fazendo (Doing)

O caminho crítico mudou: o jogo **abre** (85 testes verdes, janela no ar). O que
trava a release agora é o item 2 — `logo.ico` e `tools/GerarLogo.java` — porque o
`package` continua falhando ao montar o `.exe`.

O item 8 (README) anda junto, e ele tem uma regra nova que este projeto aprendeu
duas vezes: **descrever o que o Snake tem, e não o que ele vai ter**. Nada de
"validado visualmente" sem o que foi validado, e nada de "testado" sem o nome do
teste.

Falta, além disso, o item 3: `intervaloMs` nas três dificuldades, e uma partida
inteira corrida por muitos passos para caçar comportamento intermitente.

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
| Testes | 149 | **85** |
| Testes em `ui` | 0 | **37** (24 de layout + 13 de pixels) |
| Classes de `core` sem teste | — | `Campo`, `Direcao`, `Celula`, `Forma`, `Comida`, `Poder`, `RegistroDeRecordes` |
| Bugs reais achados por teste | 1 (badge fora do campo, achado por leitura) | **5** (4 por teste de regra, 1 por teste de pixel) |
| Linha de comando | — | `/c/Users/dudu2/.m2/wrapper/dists/apache-maven-3.9.11/d6d3cbd4012d4c1d840e93277aca316c/bin/mvn` (o `mvn` não está no `PATH`) |

O Snake ainda está **atrás** do Pong em cobertura: `Campo`, `Direcao`, `Celula`,
`Forma`, `Comida`, `Poder` e `RegistroDeRecordes` não têm teste próprio, e são
classes cujas regras o `JogoSnake` só usa por inteiro.
