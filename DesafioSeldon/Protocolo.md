# Protocolo do Desafio Seldon: dados e classificação da frota

## 1. Objetivo

Este documento descreve como o Desafio Seldon lê os dados dos dispositivos, normaliza o consumo de bateria e calcula as classificações exibidas no aplicativo. Também registra como o app trata valores inválidos encontrados no arquivo fornecido e quais limitações afetam a interpretação dos resultados.

## 2. Origem dos dados

A frota é carregada do arquivo `app/src/main/assets/frota.json`, empacotado junto com o aplicativo. O objeto raiz contém a propriedade `devices`, que é uma lista de dispositivos. O arquivo também pode conter metadados como `schemaVersion`, `exportedAt`, `fleetId` e `deviceCount`; a tela usa a lista `devices`.

Cada item em `devices` pode conter:

| Campo | Tipo esperado | Significado |
|---|---|---|
| `id` | texto | Identificador único do dispositivo |
| `model` | texto | Modelo do aparelho |
| `soc` | texto | Sistema em chip / processador |
| `tier` | texto | Faixa do dispositivo, se fornecida |
| `ramMb` | número inteiro | Memória RAM em MB |
| `androidApi` | número inteiro | Nível da API Android |
| `batteryCapacityMah` | número | Capacidade nominal da bateria em mAh |
| `batteryLevelPct` | número | Carga atual da bateria em porcentagem |
| `screenOnMinutes` | número | Tempo de tela ligada em minutos |
| `energyConsumed` | número | Energia ou carga consumida no período |
| `energyUnit` | texto | Unidade de `energyConsumed`: `mAh` ou `mWh` |
| `nominalVoltage` | número | Tensão nominal em volts; usada para converter mWh |
| `jankFramesPct` | número | Percentual de quadros perdidos |
| `crashCount7d` | número inteiro | Falhas registradas nos últimos 7 dias |
| `lastSeen` | texto | Data e hora do último contato, preferencialmente ISO 8601 UTC |

Campos que podem estar ausentes são tratados como sem dados. O identificador e o modelo devem estar presentes para que o dispositivo seja útil na lista.

## 3. Normalização do consumo

O aplicativo apresenta o consumo normalizado em mAh:

- Se `energyUnit` for `mAh`, mantém `energyConsumed`.
- Se `energyUnit` for `mWh` e `nominalVoltage` for maior que zero, calcula `mAh = mWh / nominalVoltage`.
- Se não houver unidade reconhecida ou tensão válida para converter mWh, o consumo normalizado fica indisponível.

A tela arredonda nível de bateria, capacidade, tempo de tela e consumo normalizado para números inteiros. Esse arredondamento é somente visual: os cálculos de classificação usam os valores numéricos originais.

## 4. Cálculo dos drenos

Os cálculos só são feitos quando há capacidade válida, tempo de tela maior que zero e consumo normalizado disponível.

```text
horasDeTela = screenOnMinutes / 60
drenoAbsolutoMahPorHora = consumptionMah / horasDeTela
drenoRelativoPctPorHora = (drenoAbsolutoMahPorHora / batteryCapacityMah) * 100
```

- O dreno absoluto é expresso em mAh por hora.
- O dreno relativo é expresso em porcentagem da capacidade da bateria por hora.

## 5. Regras de classificação

A avaliação segue esta ordem. Uma condição crítica prevalece sobre uma condição de atenção.

| Classificação interna | Condições |
|---|---|
| `UNKNOWN` | Capacidade ausente ou `<= 0`, tempo de tela ausente ou `<= 0`, ou consumo ausente/não conversível |
| `CRITICAL` | Dreno relativo `>= 12%/h`, ou `crashCount7d >= 3`, ou `jankFramesPct >= 15%` |
| `ATTENTION` | Dreno relativo `>= 7%/h`, ou `crashCount7d >= 1`, ou `jankFramesPct >= 8%` |
| `OK` | Nenhuma condição anterior se aplica |

Quando `crashCount7d` ou `jankFramesPct` estiver ausente, a avaliação considera zero para esses indicadores. A ausência de capacidade, tempo de tela ou consumo impede a classificação numérica e resulta em `UNKNOWN`.

## 6. Regra adotada e tratamento dos dois defeitos

A regra adotada é validar primeiro os dados necessários ao cálculo. Se a capacidade da bateria for ausente ou menor/igual a zero, se o tempo de tela for ausente ou menor/igual a zero, ou se o consumo não puder ser normalizado, o aparelho recebe `UNKNOWN` (`SEM DADOS`). Essa validação vem antes dos limites de crítico e atenção, pois não é possível calcular o dreno relativo com segurança.

Os dados fornecidos contêm estes dois valores inválidos para o cálculo:

| Dispositivo | Defeito observado | Tratamento adotado | Resultado |
|---|---|---|---|
| `DEV-0311` | `batteryCapacityMah` é `0` | Capacidade zero é considerada inválida. Não se tenta inferir a capacidade a partir do modelo nem substituir o valor por uma estimativa. | `SEM DADOS`; o dreno relativo não é calculado. |
| `DEV-0745` | `screenOnMinutes` é `0` | Tempo de tela zero é considerado insuficiente. O app não divide por zero nem tenta estimar o tempo. | `SEM DADOS`; os drenos não são calculados. |

Os registros originais permanecem inalterados. O app mostra a justificativa da ausência de dados em vez de fabricar um resultado. Mesmo que outro indicador desse dispositivo alcance um limiar de atenção ou crítico, a falta de um dado obrigatório para o cálculo mantém o status como `SEM DADOS`.

## 7. Ameaças à validade

Os resultados dependem da qualidade e da representatividade do arquivo recebido. Os dois registros inválidos não podem ser classificados pelo dreno e reduzem a quantidade de dispositivos avaliáveis. Se uma análise posterior calcular proporções apenas entre os aparelhos com classificação numérica, excluir esses registros poderá enviesar os percentuais; por isso, a quantidade de `SEM DADOS` deve ser informada junto aos resultados.

Além disso:

- O arquivo é uma exportação específica, não uma medição contínua da frota.
- Os limiares de 7%/h, 12%/h, 8% de quadros perdidos, 15% de quadros perdidos e as contagens de falhas são regras operacionais; não demonstram, por si só, a causa do problema nem foram validados pelo app contra medições externas.
- A interpretação depende de `energyUnit`, `nominalVoltage`, tempo de tela e capacidade estarem corretos e medidos de forma comparável entre dispositivos.
- O código trata a ausência de `crashCount7d` e `jankFramesPct` como zero; se esses campos estiverem ausentes por falha de coleta, o status poderá parecer melhor do que deveria.

Portanto, a classificação é um sinal de triagem baseado nos dados fornecidos, e não uma confirmação independente da condição física do aparelho.

## 8. Rótulos apresentados

| Status interno | Texto na interface |
|---|---|
| `OK` | `SAUDÁVEL` |
| `ATTENTION` | `ATENÇÃO` |
| `CRITICAL` | `CRÍTICO` |
| `UNKNOWN` | `SEM DADOS` |

Na tela de detalhes, o app apresenta uma justificativa textual baseada nos indicadores que atingiram os limites da classificação. Para `UNKNOWN`, informa quais dados necessários estão ausentes ou inválidos.

## 9. Favoritos

Os favoritos são guardados como IDs, um por linha, no arquivo privado `favoritos.txt` do aplicativo. Eles são independentes do asset `frota.json`: favoritar não altera os dados da frota. O armazenamento privado mantém as seleções entre aberturas do aplicativo, mas é removido se o app for desinstalado ou seus dados forem apagados.

## 10. Telas e filtros

- A lista mostra identificador, modelo, nível da bateria, status e controle de favorito.
- A tela de detalhes mostra os atributos disponíveis, os drenos calculados e a justificativa.
- Os filtros por status (`SAUDÁVEL`, `ATENÇÃO`, `CRÍTICO` e `SEM DADOS`) podem ser combinados com o filtro `Favoritos`.
- Ao abrir um dispositivo e voltar, a interface mantém os filtros e as posições de rolagem da lista.

## 11. Organização do código

- `data/FrotaRepository.kt`: lê o asset, interpreta o JSON e cria objetos do domínio.
- `data/FavoritesRepository.kt`: lê e grava os IDs favoritos por meio de funções fornecidas pela Activity.
- `domain/Device.kt`: representa o dispositivo e os tipos de status/avaliação.
- `domain/AssessDevice.kt`: concentra fórmulas, limites e justificativas.
- `ui/FrotaApp.kt`: apresenta lista, detalhes, filtros e chips.
- `MainActivity.kt`: conecta os assets e o armazenamento local à interface Compose.

O `AndroidManifest.xml` não declara permissão de internet. A leitura da frota ocorre localmente pelo asset.

