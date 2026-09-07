# Partograma

Aplicativo Android (Java) para acompanhamento do trabalho de parto: registro horário de
BCF, dilatação, plano de Lee, frequência de contrações e demais dados do partograma, com
alertas automáticos e geração de relatório gráfico.

## Requisitos

- Android Studio (Giraffe/Koala ou mais recente) ou o `gradlew` deste projeto.
- JDK 17+ (Android Studio já traz um compatível).
- Ao abrir no Android Studio, ele mesmo baixa o SDK/plataformas que faltarem.

## Build por linha de comando

```
./gradlew assembleDebug
```

O APK de debug é gerado em `app/build/outputs/apk/debug/app-debug.apk`.

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
    notifications/ExamReminderScheduler.java   Agendamento dos lembretes por paciente
    notifications/MyNotificationPublisher.java Broadcast receiver que exibe a notificacao
    report/ReportGridBinder.java       Logica do grafico do partograma na tela de relatorio
  res/
    layout/            4 telas (activity_tela_inicial, activity_menu, activity_detalhes,
                        activity_relatorio -- este ultimo e o grafico do partograma, com
                        marcadores posicionados em coordenadas fixas)
    drawable*, mipmap*/ Imagens e icones do app
    values/            Cores, strings, arrays dos spinners, tema
```

Não há banco de dados nem chamadas de rede: toda a persistência é feita via
`SharedPreferences` (arquivo "ArquivoPreferencia").

## Notas de build

- Usa AndroidX (`androidx.appcompat`, `androidx.constraintlayout`, `androidx.core`).
- `minSdkVersion` 21, `targetSdkVersion` 26.
- `targetSdkVersion` 26 significa que, em Android 8+, as notificações agendadas não
  possuem canal (`NotificationChannel`) e por isso não chegam a aparecer ao usuário nessas
  versões — vale a pena revisar isso se for atualizar o `targetSdkVersion` no futuro.
