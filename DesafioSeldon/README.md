# Desafio Seldon

Aplicativo Android em Kotlin e Jetpack Compose para consultar dispositivos de `frota.json`, ver seus detalhes, acompanhar classificações e marcar favoritos.

## Executar no Android Studio

1. Extraia `DesafioSeldon.zip`.
2. No Android Studio, escolha **Open** e selecione a pasta `DesafioSeldon`, que contém `settings.gradle.kts`.
3. Aguarde a sincronização do Gradle. Se solicitado, instale o Android SDK 34.
4. Em **Settings > Build, Execution, Deployment > Build Tools > Gradle**, selecione JDK 17.
5. Inicie um emulador com Android 8.0/API 26 ou superior, ou conecte um aparelho compatível.
6. Selecione a configuração `app` e clique em **Run ▶**.

O projeto inclui `gradlew` e usa AGP 8.3.0, Gradle 8.4, Kotlin 1.9.23, Compose Compiler 1.5.13 e JVM 17. O SDK de compilação é API 34 e o mínimo para instalar é API 26. A sincronização do Gradle pode baixar dependências de desenvolvimento; o app em si não pede permissão de internet.

## Arquitetura

- **UI (`ui/FrotaApp.kt`)**: desenha lista e detalhes, status, filtros e favoritos. Mantém filtros e rolagem ao navegar para os detalhes e voltar.
- **Domain (`domain/Device.kt`, `domain/AssessDevice.kt`)**: contém o modelo de dispositivo, conversão de unidade, fórmulas, limiares e justificativas. As regras ficam fora dos composables.
- **Data (`data/FrotaRepository.kt`, `data/FavoritesRepository.kt`)**: lê `assets/frota.json`, converte os dados para o domínio e salva os IDs favoritos em arquivo privado.
- **Entrada Android (`MainActivity.kt`)**: conecta assets, armazenamento local e interface Compose.

Fluxo: asset `frota.json` → `FrotaRepository` → objetos `Device` → UI e `AssessDevice`. Favoritar chama a camada de dados para gravar os IDs localmente.

## Decisões de projeto

- Os dados dos dispositivos vêm de `app/src/main/assets/frota.json`.
- O Manifest não declara permissão `INTERNET`.
- Não foram usados imports `android.*`, `Context` ou bibliotecas JSON na lógica de domínio e parsing. A Activity usa AndroidX e fornece funções para ler os assets e o armazenamento local. O JSON é interpretado por um parser Kotlin simples.
- O consumo em mAh é mantido. O consumo em mWh é convertido pela fórmula `mAh = mWh / nominalVoltage`.
- Dreno absoluto: `consumptionMah / (screenOnMinutes / 60)`. Dreno relativo: `(drenoAbsoluto / batteryCapacityMah) × 100`.
- Os dados obrigatórios inválidos levam a `SEM DADOS`. Os limites são: crítico em dreno `>= 12%/h`, 3 falhas ou 15% de quadros perdidos; atenção em dreno `>= 7%/h`, 1 falha ou 8% de quadros perdidos.
- `DEV-0311` tem capacidade zero e `DEV-0745` tem tempo de tela zero. Esses valores não são estimados ou corrigidos; ambos ficam como `SEM DADOS`.
- Bateria, capacidade, tempo de tela e consumo são mostrados como inteiros arredondados; os cálculos usam os valores originais.
- Os favoritos persistem em `favoritos.txt` no armazenamento privado do app. Desinstalar o app ou apagar seus dados remove esse arquivo.
- Os filtros por status podem ser combinados com Favoritos.

## Testes e CI

Os testes unitários em `app/src/test` cobrem conversão de mWh, limites de dreno, falhas e quadros perdidos, prioridade de status e dados inválidos. O workflow `.github/workflows/android.yml` executa Android Lint e `./gradlew test` em cada push e pull request. Os testes são unitários de JVM; não iniciam emulador nem usam instrumentação.

Para executar localmente no Windows, use `.\gradlew.bat lint` e `.\gradlew.bat test` na raiz do projeto. No Linux/macOS, use `./gradlew lint` e `./gradlew test`. O workflow instala JDK 17 e Gradle 8.4 e mostra as duas etapas separadamente.

## Documentação e capturas

- [Protocolo.md](Protocolo.md): formato dos dados, regras, defeitos observados e ameaças à validade.
- [Lista de dispositivos](screenshots/lista-dispositivos.png)
- [Detalhes do dispositivo](screenshots/detalhes-dispositivo.png)

## Uso de inteligência artificial

O ChatGPT Codex foi utilizado como ferramenta de IA para apoiar o desenvolvimento do app e o entendimento do código em Kotlin.


