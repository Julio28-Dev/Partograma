# Engenharia Reversa — partograma.apk

`partograma.apk` é a fonte de verdade deste TCC e não foi alterado. Este documento registra
tudo o que foi descoberto nele e fundamenta a reconstrução em código-fonte na pasta
`partograma-app/`. Ferramentas usadas: `apktool` 2.9.3, `jadx` 1.5.1, `androguard`, `aapt`
(build-tools 33.0.2), inspeção manual do bytecode/recursos descompilados.

---

## 1. Informações do APK

| Item | Valor |
|---|---|
| Nome do app | Partograma |
| Package name | `com.jninfo.partograma.partograma` |
| Version code / name | 1 / "1.0" |
| minSdkVersion | 16 (Android 4.1) |
| targetSdkVersion | 26 (Android 8.0) |
| Build type original | `debug` (`android:debuggable="true"` hardcoded no manifest) |
| Linguagem | Java (100% do código próprio) |
| UI | Views tradicionais (XML) — **sem** Jetpack Compose |
| Bibliotecas | `com.android.support` (AppCompat, ConstraintLayout, Design) versão **26.1.0**; `android.arch.lifecycle` 27.0.0-SNAPSHOT (dependência transitiva do AppCompat, não usada diretamente pelo app) |
| Banco de dados | **Nenhum** (sem SQLite/Room) |
| Armazenamento | `SharedPreferences` único, nome `"ArquivoPreferencia"` |
| Rede / API | **Nenhuma.** Sem permissão de INTERNET, sem URLs, sem Firebase, sem bibliotecas HTTP |
| Bibliotecas nativas (`lib/`) | Nenhuma |
| Assets (`assets/`) | Nenhum |
| Permissões | `WRITE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE` |

### Classes próprias do app (`com.jninfo.partograma.partograma`)

| Classe | Papel |
|---|---|
| `TelaInicial` | Activity de abertura / tutorial |
| `Menu` | Activity de lista/cadastro de pacientes |
| `Detalhes` | Activity de registro horário do partograma |
| `relatorio` (minúsculo, nome original) | Activity de relatório/gráfico |
| `MyNotificationPublisher` | `BroadcastReceiver` que publica a notificação agendada |
| `Pessoa` | Modelo de dados com 16 campos por hora — **não usado em nenhuma Activity** (código morto, provavelmente de uma versão anterior do design). Não foi replicado no novo projeto. |
| `ScrollViewHelper` | Classe vazia, não usada. Não replicada. |
| `ScreenshotType` | Enum vazio (`FULL`, `CUSTOM`), não usado. Não replicado. |
| `ScreenshotUtils` | Utilitário de screenshot **declarado mas nunca chamado** — a tela de relatório reimplementa a mesma lógica inline. Não replicado (seria código morto). |

---

## 2. Estrutura de telas

### 2.1 TelaInicial (tela de abertura / tutorial)

- **Como acessar:** é a activity `LAUNCHER`, primeira tela ao abrir o app.
- **Elementos:** imagem de fundo em tela cheia, texto explicativo, botões "ANTERIOR"/"PRÓXIMO", ícone de "?" (só aparece no último passo).
- **Estados:** 4 passos (índice 0–3), guardados em memória (variável `i`, não persistida):
  1. "Bem vindo ao aplicativo de Partograma" — fundo `imagem1`, botão Anterior invisível.
  2. "Para criar um novo paciente basta inserir o nome no campo e clicar em adicionar." — fundo `imagem4`.
  3. "Para excluir um paciente segure em cima do nome e escolha opção deletar." — fundo `imagem3`.
  4. "Qualquer dúvida aperte neste interrogação" — fundo `imagem5`, botão vira "Pronto", ícone de ajuda aparece.
- **Ações:**
  - Botão Próximo: avança o passo; no passo 4, marca `SharedPreferences["tutorial"]="completo"` e vai para `Menu`.
  - Botão Anterior: volta um passo.
- **Persistência:** só a flag "tutorial concluído" (`ArquivoPreferencia["tutorial"]`). Se já concluído, `onCreate` já dispara `startActivity(Menu)` imediatamente (a própria tela de tutorial continua sendo desenhada por baixo por uma fração de segundo — comportamento original, preservado).
- **Observação:** as imagens `imagem2.png` e `imagem6.png` existem nos recursos mas **não são usadas** em nenhum passo do tutorial (assets órfãos).

### 2.2 Menu (lista de pacientes)

- **Como acessar:** depois do tutorial, ou diretamente se o tutorial já foi concluído.
- **Elementos:** campo de texto "Insira o Nome", botão "Adicionar", botão de ajuda (ícone "?", que só fecha a tela — `finish()`), lista de pacientes cadastrados.
- **Ações:**
  - Adicionar: cria o paciente no primeiro "leito" livre (1 a 10). Se todos os 10 estiverem ocupados, mostra diálogo **"Leito cheio"**. Ao criar, agenda 15 notificações de lembrete (ver seção 7).
  - Toque simples num nome: abre `Detalhes` passando o nome.
  - Toque longo num nome: diálogo "DELETAR PACIENTE? / DESEJA DELETAR O PACIENTE {nome}?" → ao confirmar, cancela as notificações do paciente, apaga todos os dados dele e mostra Toast **"Paciente Deleteado"** (grafia original, preservada).
- **Limite:** até 10 pacientes simultâneos.

### 2.3 Detalhes (registro horário)

- **Como acessar:** tocando em um paciente no Menu.
- **Elementos:** nome do paciente, última data de registro, seletor de hora (1–16, botões "-"/"+"), campo BCF (batimentos cárdio-fetais), campo Dilatação do colo (cm), spinner Integridade da bolsa (ROTA/ÍNTEGRA), spinner Líquido amniótico (5 opções), spinner Plano de Lee (-AM,-3..+4), imagem de posição fetal com botão "Girar" (8 posições, imagens `posicao1.jpg`..`posicao8.jpg`), spinner Frequência das contrações (LEVE/MODERADA/ALTA), campos Ocitocina (ml), Misoprostol (comprimidos), Outros medicamentos, Examinador, botões "Novo Registro" e "Salvar", botão "Gerar" (abre o relatório).
- **Fluxo de edição:**
  1. Ao abrir, todos os campos ficam **travados** (somente leitura) e mostram o último registro salvo (se houver).
  2. "Novo Registro" **destrava** os campos, grava a data/hora atual e **limpa** BCF/Dilatação/Ocitocina/Misoprostol/Outros medicamentos (mas não trava outros campos como examinador — comportamento original).
  3. "-"/"+"" apenas mudam o número da hora exibida; **não recarregam** os dados daquela hora automaticamente.
  4. "Salvar" valida que BCF e Dilatação não estejam vazios (senão mostra diálogo "Preencha todos os campos BCF ou Dilatação"), grava o registro na hora exibida, recalcula os avisos e trava os campos de novo.
- **Validações / avisos** (texto exibido acima do formulário, com faixa de fundo ficando rosa em caso de alerta): ver seção 7 (Regras de negócio).

### 2.4 relatorio (relatório / gráfico)

- **Como acessar:** botão "Gerar" em Detalhes.
- **Elementos:** nome do paciente, tabela com uma coluna por hora já registrada (dia, hora do dia, integridade da bolsa, líquido amniótico, ocitocina, medicamentos, examinador), gráfico do partograma sobreposto a uma imagem de fundo (grade impressa), botões "Compartilhar" e "Zoom".
- **Gráfico:** marcadores pré-posicionados (coordenadas fixas em dip) que são apenas mostrados/escondidos e coloridos conforme os dados — ver seção 6.
- **Ações:**
  - "Zoom": tira um screenshot da tela inteira e abre um seletor de apps para "Enviar Partograma..." (Intent `ACTION_VIEW`).
  - "Compartilhar": tira um screenshot só da área do gráfico e abre um seletor de apps "Send mail..." (Intent `ACTION_SEND`).
  - Ambos pedem permissão de armazenamento em tempo de execução (Android 6+) e **não fazem nada em versões anteriores ao Android 6.0** (bug/limitação original).
  - Ambos salvam a imagem sempre no mesmo arquivo `Pictures/screenshot.png` (um pode sobrescrever o outro).

---

## 3. Fluxos completos

```
TelaInicial → [tutorial 4 passos] → Menu

Menu → digitar nome → "Adicionar"
  → [leito livre] → paciente criado, 15 notificações agendadas → aparece na lista
  → [10 leitos ocupados] → diálogo "Leito cheio"

Menu → toque em paciente → Detalhes (dados travados, mostrando ultimo registro)
  → "Novo Registro" → campos destravados e limpos, data/hora atual
    → preencher BCF, Dilatação, spinners, etc.
    → "Salvar" → [BCF ou Dilatação vazios] → diálogo de validação (permanece destravado)
              → [ok] → grava registro da hora exibida, recalcula avisos, trava campos
  → "-" / "+" → apenas muda o numero da hora exibida (não recarrega dados)
  → "Gerar" → relatorio (tabela + gráfico do paciente)
      → "Zoom" → [permissão negada] pede permissão
                → [permissão concedida, Android 6+] screenshot cheio → chooser "Enviar Partograma..."
      → "Compartilhar" → [idem] → screenshot do gráfico → chooser "Send mail..."

Menu → toque longo em paciente → diálogo "DELETAR PACIENTE?"
  → cancelar → nada acontece
  → confirmar → cancela notificações, apaga todos os dados do paciente, Toast "Paciente Deleteado"
```

---

## 4. Dados

### 4.1 Local (SharedPreferences "ArquivoPreferencia")

Não há banco de dados. Todas as chaves seguem o padrão abaixo (`{slot}` = 1..10, `{hora}` = 1..16):

| Chave | Conteúdo |
|---|---|
| `tutorial` | `"completo"` ou ausente |
| `nome{slot}` | Nome do paciente daquele leito |
| `pessoa{slot}hora` | Número (string) da última hora exibida/salva |
| `pessoa{slot}temhora{hora}` | `"tem"` se aquela hora já foi salva |
| `pessoa{slot}dia{hora}` / `horadodia{hora}` | Data e hora do registro (strings formatadas) |
| `pessoa{slot}batimentos{hora}` | BCF (string numérica) |
| `pessoa{slot}dilatacao{hora}` | Dilatação em cm (string numérica) |
| `pessoa{slot}integridade{hora}` | "ROTA" / "ÍNTEGRA" |
| `pessoa{slot}liquido{hora}` | Uma das 5 opções de líquido amniótico |
| `pessoa{slot}lee{hora}` | Uma das 9 opções do Plano de Lee |
| `pessoa{slot}freqcontracao{hora}` | "LEVE" / "MODERADA" / "ALTA" |
| `pessoa{slot}ocitosina{hora}`, `mesoprostol{hora}`, `remedios{hora}`, `examinador{hora}` | Texto livre |
| `pessoa{slot}posicao{hora}` | Inteiro 1–8 (posição/rotação da imagem fetal) |
| `pessoa{slot}avisobatimento`, `avisolee`, `avisodilatacao` | Texto do último aviso calculado (cacheado; **não é por hora**, é um por paciente) |

Não há cache, arquivos temporários, nem armazenamento externo estruturado — apenas a imagem
exportada em `Pictures/screenshot.png` (ver seção 2.4).

### 4.2 Enviado/recebido de servidor

Não existe. Nenhuma chamada de rede foi encontrada em nenhuma classe, em nenhum recurso XML,
nem no bytecode (`androguard` + busca textual por `http`, domínios e IPs em todo o APK
descompilado). **Confirmado: aplicativo 100% offline.**

---

## 5. APIs

**NÃO IDENTIFICADO / NÃO EXISTE.** Nenhum endpoint, URL, header, corpo de requisição,
autenticação ou tratamento de erro de rede foi encontrado. O app não solicita a permissão
`INTERNET`, o que por si só impede qualquer chamada de rede em tempo de execução.

---

## 6. Interface

- **Cores:** `colorPrimary` = `colorPrimaryDark` = `#FFDFF4F3` (verde-água claro); `colorAccent` =
  `#FFFF4081` (rosa/magenta) — a mesma cor é usada, via `Color.parseColor`, para destacar
  alertas de regra de negócio nas telas de Detalhes e Relatório.
- **Tema:** `Theme.AppCompat.Light.DarkActionBar`.
- **Tipografia:** fonte padrão do sistema; tamanhos usados diretamente em `sp` nos layouts
  (18–30sp para títulos e avisos, tamanho padrão para os demais).
- **Ícone do app:** o ícone padrão gerado pelo Android Studio (robozinho verde
  `#3DDC84` sobre grade `#26A69A`) — **nunca foi customizado** pelo desenvolvedor original.
- **Layout:**
  - `TelaInicial` e `Menu`: `RelativeLayout` simples.
  - `Detalhes`: `ScrollView` + `ConstraintLayout`.
  - `relatorio`: `ConstraintLayout` com uma imagem de fundo (`ativo2xvai.png`, 350×550dp,
    a grade impressa do partograma) e **614 Views** sobrepostas em coordenadas fixas
    (triângulos de dilatação, pontos de BCF, ícones de posição fetal/Lee, ícones de
    frequência de contração, barras de alerta/ação) — mostradas/escondidas conforme os
    dados do paciente. Ver detalhamento no `ReportGridBinder.java` do projeto reconstruído.
- **Animações/transições:** nenhuma personalizada — apenas as transições padrão do Android
  entre Activities.
- **Ícones/imagens próprias do app** (todos recuperados do APK e copiados para o novo
  projeto, byte a byte): `imagem1–6` (tutorial, 2 e 6 não usadas), `posicao1–8` (posição
  fetal), `lee`/`lee2–8` (ícones do gráfico de Lee), `barradialtacao3–9` (barras de
  alerta/ação), `forte`/`media`/`fraca` (ícones de frequência de contração), `triangulo`,
  `ponto`, `ativo2x`/`ativo2xvai`/`ativo4x` (fundo do gráfico em diferentes escalas —
  `ativo2xvai` é o usado por padrão; `ativo2x`/`ativo4x` não são referenciados em nenhum
  layout, possivelmente reservados para um recurso de zoom nunca implementado), `relatorio.jpg`/`relatorio2.png` (não referenciados em nenhum layout — órfãos), `ic_help_black_48dp`.

---

## 7. Regras de negócio observadas

1. **Limite de pacientes:** no máximo 10 simultâneos.
2. **BCF normal:** 120–160 bpm. Fora da faixa, mostra aviso e destaca a tela em rosa. O
   aviso **não volta automaticamente** ao normal quando uma leitura seguinte está dentro da
   faixa — fica "preso" até a próxima leitura fora da faixa (ou até sair e reabrir a tela,
   que recarrega o texto salvo, não recalcula).
3. **Dilatação (linha de alerta clássica do partograma):** hora 1 sempre mostra "OK"; das
   horas 2 a 16, compara com a hora anterior: `+1cm` = "Aumentou 1cm/h" (normal); `0cm` =
   alerta "Não aumentou 1cm/h, Está igual após de 2 toques!"; qualquer outra diferença =
   alerta "Não aumentou 1cm/h". As duas últimas destacam a tela em rosa.
4. **Plano de Lee:** compara com a hora anterior (e, se ela não existir, com duas horas
   atrás) para avisar "Mesma posição". A partir da hora 2, a tela **sempre** fica rosa ao
   salvar, independente do resultado da comparação (bug original, ver seção 8).
5. **Notificações de lembrete:** ao cadastrar um paciente, agenda 15 lembretes horários
   ("Examinar hora 2 de: {nome}" até "hora 16"), cancelados ao excluir o paciente.
6. **Gráfico do relatório:** cada valor registrado (dilatação, BCF, Lee, frequência de
   contração) tem uma posição fixa pré-desenhada no layout; a barra de alerta/ação é
   posicionada pela dilatação da hora 1 (admissão).

---

## 8. Pontos que não puderam ser recuperados fielmente (ou foram conscientemente ajustados)

Esta seção documenta tanto o que é **impossível de saber com certeza** (ex.: intenção
original do autor) quanto os **bugs reais identificados**, com a decisão tomada em cada caso.

### 8.1 Bug preservado tal como está

- **10º lembrete de notificação usa o atraso de 1 hora em vez de 10 horas.** No código
  original, `scheduleNotification{slot}_10` usa `3600000` ms (1h) para todos os 10
  pacientes, quando deveria usar `36000000` (10h), seguindo o padrão dos demais índices.
  Reproduzido exatamente em `ExamReminderScheduler.DELAYS_MS[9]`.
- **Botões Zoom/Compartilhar do relatório não fazem nada em Android < 6.0.** A checagem de
  versão envolve todo o corpo do clique, não apenas a checagem de permissão.
- **Aviso de BCF nunca "reseta" para normal.** Confirmado e preservado.
- **Navegar com "-"/"+" não recarrega os campos do formulário.** Confirmado e preservado.
- **Destaque rosa da tela em `avisarLee` dispara sempre a partir da hora 2**, mesmo quando
  a posição de Lee não se repete — no código original, a chamada que pinta o fundo de rosa
  está fora do `if` que checa a repetição (aparenta ser um erro de chaveamento do autor
  original). Preservado.
- **Mensagens de dilatação "dilatou completo em menos de 4 horas" e "não dilatou 10cm em 10
  horas" existem no código-fonte mas nunca aparecem para o usuário** — são sempre
  sobrescritas, na mesma função, pela mensagem baseada na comparação com a hora anterior,
  antes de a tela ser redesenhada. Confirmado lendo a ordem exata das instruções no
  bytecode/fonte descompilado. Como o resultado visível é idêntico com ou sem essas
  mensagens mortas, elas **não foram replicadas** (nenhuma diferença de comportamento).

### 8.2 Desvios conscientes (não é possível/sensato reproduzir literalmente)

- **Colisão de códigos de notificação entre pacientes.** No app original, o "request code"
  que identifica cada um dos 150 alarmes (10 pacientes × 15 lembretes) foi digitado à mão
  em 150 métodos quase idênticos. A partir do paciente de número 7 e principalmente no
  10, aparecem erros de transcrição — inclusive constantes do Android (como
  `PointerIconCompat.TYPE_ALIAS`) usadas como se fossem números arbitrários, e números
  repetidos que colidem com os de outros pacientes (ex.: paciente 7, lembretes 12 e 13,
  usam os mesmos códigos que o paciente 1). Isso faz alarmes de pacientes diferentes se
  cancelarem/substituírem entre si em alguns casos, de forma não intencional e sem
  relação com nenhuma regra de negócio — é um erro de cópia e cola manual. Reproduzir
  isso litealmente exigiria fixar ~150 "números mágicos" sem qualquer lógica, o que viola
  diretamente o pedido de código limpo e organizado. **Decisão:** usar uma fórmula única
  `(slot-1)*15 + (indice-1)`, sem colisões. Nenhum texto, quantidade ou horário de
  notificação foi alterado — apenas a forma interna de gerar um número de identificação.
- **Comparação "mesma posição de Lee" usa `==` (referência) em vez de `.equals()` no
  original.** Strings lidas do `SharedPreferences` em geral não são o mesmo objeto em
  memória mesmo tendo o mesmo conteúdo, então essa comparação **quase nunca é verdadeira**
  para dados reais — só coincide por acidente quando o Android reaproveita, dentro da
  mesma sessão do app (sem reiniciar o processo), o mesmo objeto String em cache interno
  do `SharedPreferencesImpl` (comportamento não documentado e não confiável). Reproduzir
  isso litealmente exigiria depender desse detalhe interno do Android, o que é frágil e,
  na prática, o torna um recurso que raramente funciona como o nome sugere. **Decisão:**
  implementado com `.equals()` (comparação de valor), que é o comportamento que o texto e o
  nome da funcionalidade claramente pretendem. Esta é a única mudança de comportamento
  *observável* desta reconstrução, e está isolada e comentada em `Detalhes.avisarLee(...)`.
  Se for importante reproduzir o bug exatamente como está (recurso que quase nunca funciona),
  isso pode ser revertido facilmente.

### 8.3 Não identificável / não teve como confirmar

- **Por que existem `imagem2.png`, `imagem6.png`, `ativo2x.png`, `ativo4x.png`,
  `relatorio.jpg`, `relatorio2.png` no APK sem nenhuma referência em código ou layout.**
  Provavelmente sobras de iterações anteriores do design (ex.: um recurso de zoom com
  imagem em outra escala, planejado e não finalizado). `NÃO IDENTIFICADO`.
- **Execução do APK original em um emulador/dispositivo real** não foi possível neste
  ambiente (não há Android SDK/emulador/dispositivo físico disponível na máquina onde a
  análise foi feita — apenas as ferramentas de linha de comando usadas para a engenharia
  reversa). Toda a análise de comportamento foi feita por leitura completa do bytecode
  Java descompilado (jadx) e dos recursos decodificados (apktool), não por observação
  visual em execução. A reconstrução foi compilada com sucesso (`./gradlew assembleDebug`
  gera um APK válido, com mesmo package/ícone/activity inicial que o original), mas não
  foi testada interativamente em tela. **Recomenda-se abrir `partograma-app/` no Android
  Studio e testar em um emulador antes de considerar a fidelidade 100% validada
  visualmente** (a fidelidade de código/regras de negócio foi validada por leitura
  exaustiva do original).

---

## 9. Escolha de tecnologia para a reconstrução (Etapa 4)

O app original é Android nativo em Java, sem nenhuma dependência de rede, banco de dados
ou UI declarativa. Reconstruí-lo na **mesma stack (Android/Java, Views XML tradicionais)**
é a única escolha que permite fidelidade total de aparência e comportamento sem reinterpretar
nada — qualquer outra tecnologia (Kotlin/Compose, Flutter, React Native, etc.) exigiria
reescrever toda a UI e teria maior risco de divergência visual, além de não haver nenhum
motivo técnico (a app não usa nada exclusivo de uma stack mais nova) para trocar.

Única adaptação: as bibliotecas de suporte antigas (`com.android.support:*:26.1.0`) foram
substituídas pelos equivalentes **AndroidX** (`androidx.appcompat`, `androidx.constraintlayout`,
`androidx.core`) — mesma API pública, apenas o pacote Java mudou — porque as antigas não são
mais publicadas nos repositórios do Google/Maven e não compilariam com as ferramentas atuais.
Essa é uma decisão de *build tooling*, não de arquitetura ou UX.

---

## 10. Estrutura do código reconstruído

Ver `partograma-app/README.md` para o mapa completo de pastas/arquivos. Resumo: as 4 telas
e o `BroadcastReceiver` mantêm os nomes de classe originais (`TelaInicial`, `Menu`,
`Detalhes`, `relatorio`, `MyNotificationPublisher`); a lógica que no original estava
duplicada dezenas/centenas de vezes (uma cópia por paciente, por hora ou por célula do
gráfico) foi extraída para três classes auxiliares parametrizadas — `PatientRepository`
(acesso a dados), `ExamReminderScheduler` (notificações) e `ReportGridBinder` (gráfico do
relatório) — que produzem exatamente o mesmo resultado observável, mas sem repetir código.

**Build validado nesta sessão:** `./gradlew assembleDebug` conclui com sucesso e gera
`app-debug.apk` (~4,4 MB), com package name, ícone e activity de abertura idênticos ao
`partograma.apk` original (conferido com `aapt dump badging` em ambos os APKs).
