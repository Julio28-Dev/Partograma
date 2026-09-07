# Partograma

Um aplicativo desenvolvido para auxiliar enfermeiras e profissionais da área da saúde,
utilizando um gráfico para registrar e acompanhar a evolução do trabalho de parto, além de
monitorar a saúde da mãe e do bebê.

## Conteúdo

- **`partograma-app/`** — projeto-fonte Android (Java), organizado e compilável
  (`./gradlew assembleDebug`). Ver `partograma-app/README.md` para detalhes de build e
  estrutura do código.

## Funcionalidades

- Cadastro e acompanhamento de até 10 pacientes simultaneamente.
- Registro horário de batimentos cárdio-fetais (BCF), dilatação do colo, integridade da
  bolsa, características do líquido amniótico, plano de Lee, frequência das contrações,
  medicação e examinador responsável.
- Alertas automáticos quando BCF ou a evolução da dilatação saem da faixa esperada.
- Lembretes agendados para os horários de exame de cada paciente.
- Geração de um relatório gráfico do partograma, com opção de compartilhar/exportar como
  imagem.
