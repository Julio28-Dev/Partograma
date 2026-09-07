# Partograma (reconstrução do partograma.apk)

Este projeto é a reconstrução, em código-fonte Android/Java editável, do aplicativo original
`partograma.apk` (pasta pai deste diretório). A análise completa de engenharia reversa que
fundamentou esta reconstrução está em `../REVERSE_ENGINEERING.md`.

## Requisitos

- Android Studio (Giraffe/Koala ou mais recente) ou o `gradlew` deste projeto.
- JDK 17+ (Android Studio já traz um compatível).
- Ao abrir no Android Studio, ele mesmo baixa o SDK/plataformas que faltarem.

## Build por linha de comando

```
./gradlew assembleDebug
```

O APK de debug é gerado em `app/build/outputs/apk/debug/app-debug.apk`. Este build já foi
validado nesta sessão (compila e empacota com sucesso).

## Estrutura do projeto

```
app/src/main/
  AndroidManifest.xml
  java/com/jninfo/partograma/partograma/
    TelaInicial.java        Tela de abertura / tutorial (4 passos)
    Menu.java                Cadastro e lista de pacientes ("leitos")
    Detalhes.java             Formulario de registro horario do partograma
    relatorio.java            Tela de relatorio (tabela + grafico + exportar imagem)
    data/PatientRepository.java        Acesso ao SharedPreferences "ArquivoPreferencia"
    notifications/ExamReminderScheduler.java   Agendamento dos 15 lembretes por paciente
    notifications/MyNotificationPublisher.java Broadcast receiver que exibe a notificacao
    report/ReportGridBinder.java       Logica do grafico do partograma na tela de relatorio
  res/
    layout/            4 telas (activity_tela_inicial, activity_menu, activity_detalhes,
                        activity_relatorio -- este ultimo e o grafico do partograma, com
                        centenas de marcadores posicionados em coordenadas fixas)
    drawable*, mipmap*/ Imagens e icones extraidos do APK original (mesmos arquivos, bit a bit)
    values/            Cores, strings, arrays dos spinners, tema
```

Não há banco de dados nem chamadas de rede: toda a persistência é feita via
`SharedPreferences` (arquivo "ArquivoPreferencia"), exatamente como no APK original.

## O que foi mantido de propósito igual ao original (mesmo parecendo estranho)

Ver `REVERSE_ENGINEERING.md`, seções 7 e 8, para a lista completa e a justificativa de cada
um. Resumo rápido:

- Limite de 10 pacientes simultâneos ("Leito cheio").
- 15 lembretes de exame por paciente, um por hora, incluindo o atraso incorreto do 10º
  lembrete (usa 1h em vez de 10h -- bug original preservado).
- Notificações sem canal (`NotificationChannel`): em Android 8+ elas são agendadas mas não
  chegam a aparecer -- limitação real do app original, preservada.
- Navegar entre horas com os botões "-"/"+" na tela de Detalhes não recarrega os campos do
  formulário -- só "Novo Registro" faz isso.
- O aviso de BCF fora da faixa (120–160) nunca "volta ao normal" sozinho.
- Os botões de Zoom/Compartilhar do relatório não fazem nada em Android < 6.0.

## Únicos desvios conscientes do comportamento original

1. **Migração de `com.android.support` para AndroidX** -- mesma API pública, apenas o pacote
   Java mudou. Necessário porque as bibliotecas antigas não são mais publicadas.
2. **`minSdkVersion` 16 → 21** -- apenas para compatibilidade com ferramentas de build atuais.
   `targetSdkVersion` permanece 26, igual ao original.
3. **Códigos de notificação sem colisão entre pacientes** -- no original, o código que
   diferencia os alarmes de cada paciente foi digitado à mão 150 vezes e contém erros de
   transcrição a partir do 7º/10º paciente (inclusive reaproveitando constantes do Android
   como se fossem números). Isso não é uma regra de negócio, é erro de digitação; aqui usamos
   uma fórmula única sem colisões.
4. **Comparação de "mesma posição de Lee" usando `.equals()`** em vez da comparação por
   referência (`==`) usada no original. Ver `REVERSE_ENGINEERING.md`, seção 8, para a
   explicação completa -- a versão original depende de um detalhe interno não documentado do
   Android e praticamente nunca funciona como pretendido.

Nenhuma tela, texto, cor, navegação ou regra de negócio observável foi alterada.
