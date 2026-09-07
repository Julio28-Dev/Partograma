# Partograma

Um APK desenvolvido para auxiliar enfermeiras e profissionais da área da saúde, utilizando
um gráfico para registrar e acompanhar a evolução do trabalho de parto, além de monitorar a
saúde da mãe e do bebê.

Este repositório documenta a reconstrução, em código-fonte Android/Java editável, do
aplicativo original de TCC (`partograma.apk`), mantido aqui intacto como fonte de verdade.

## Conteúdo

- **`partograma.apk`** — APK original, referência definitiva de aparência e comportamento.
- **`REVERSE_ENGINEERING.md`** — análise completa de engenharia reversa: telas, fluxos,
  esquema de dados, regras de negócio e bugs identificados no APK original.
- **`partograma-app/`** — projeto-fonte Android reconstruído a partir dessa análise,
  organizado e compilável (`./gradlew assembleDebug`). Ver `partograma-app/README.md` para
  detalhes de build e estrutura do código.

## Status

Reconstrução fiel ao original concluída e validada por build (compila e empacota com
sucesso). Melhorias e novas funcionalidades ficam para uma etapa posterior, depois que a
fidelidade visual for confirmada rodando o app num emulador/dispositivo.
