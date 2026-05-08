# agenda-assistant

Assistente de agendamento baseado em chat, desenvolvido em **Java + Spring Boot**.  
A ideia é permitir que pessoas que hoje anotam compromissos em papel ou “na cabeça” passem a organizar a agenda conversando com um bot (Telegram agora, WhatsApp e outras plataformas no futuro).

## Visão geral

O projeto fornece um **motor de agenda** que:

- cria compromissos via mensagens de chat;
- lista os próximos horários;
- envia lembretes antes dos compromissos;
- avisa quando já existe algo marcado naquele horário.

Na primeira versão, a integração é feita com **Telegram Bot API** usando Spring Boot, aproveitando o suporte de _starters_ para bots e de agendamento de tarefas com `@Scheduled`.[web:26][web:41]

## Funcionalidades (MVP)

- **Cadastro de compromissos via chat**
  - O usuário conversa com o bot para informar:
    - data (ex.: `10/05/2026`, `hoje`, `amanhã`);
    - horário (ex.: `14:30`);
    - descrição (ex.: `Corte de cabelo João`).
  - O bot confirma a criação do compromisso.

- **Consulta de agenda**
  - Comandos típicos (podem variar conforme implementação atual):
    - `/hoje` – lista compromissos de hoje;
    - `/amanha` – lista compromissos de amanhã;
    - `/proximos` – mostra os próximos N compromissos.

- **Lembretes automáticos**
  - Uma tarefa agendada no Spring verifica periodicamente os compromissos próximos e envia mensagens de lembrete para o usuário.[web:41][web:43]

- **Detecção básica de conflito**
  - Antes de criar um novo compromisso, o serviço pode checar se já existe algo na mesma data/horário e avisar o usuário.

## Roadmap

Próximas etapas planejadas para o projeto:

- Integração com **Google Calendar** (salvar/ler eventos diretamente da agenda do usuário).
- Adaptação do núcleo de agenda para suportar múltiplos canais:
  - WhatsApp;
  - outros mensageiros.
- Painel simples (web) para visualização da agenda além do chat.
- Configuração de lembretes personalizados (ex.: 10, 30 ou 60 minutos antes).

## Stack tecnológica

- **Linguagem:** Java 17+
- **Framework:** Spring Boot 3 (Web, Scheduling)
- **Mensageria:** Telegram Bot API (via starter Spring, ex.: `telegrambots-spring-boot-starter` ou similar)[web:26][web:166]
- **Persistência:** JPA/Hibernate (H2 em memória ou PostgreSQL, conforme perfil de execução)
- **Build:** Maven/Gradle
- **Containerização (opcional):** Docker / Docker Compose

## Arquitetura (alto nível)

O projeto é organizado em camadas:

- **core / domain**
  - Entidades de agendamento, por exemplo: `Appointment`.
  - Regras de negócio de criação, validação de horário e conflito.

- **application / services**
  - `SchedulingService` – orquestra criação e listagem de compromissos.
  - `ReminderService` – roda tarefas agendadas com `@Scheduled` para enviar lembretes.[web:41]

- **adapters / infrastructure**
  - Integração com os canais de chat (Telegram, futuramente WhatsApp).
  - Repositórios JPA e configuração de banco de dados.

Essa separação facilita adicionar novos canais mantendo o mesmo núcleo de agenda.

## Como rodar localmente

### Pré-requisitos

- Java 17+
- Maven ou Gradle
- (Opcional) Docker, se você usar banco de dados via container

### Configuração

1. Clone o repositório:

   ```bash
   git clone https://github.com/feerodgs/agenda-assistant.git
   cd agenda-assistant
   ```

2. Configure as variáveis de ambiente ou arquivos de propriedades necessários, por exemplo:

   - Token do bot (Telegram):

     ```properties
     bot.token=SEU_TELEGRAM_BOT_TOKEN_AQUI
     bot.username=SEU_BOT_USERNAME
     ```

   - Configurações de banco (H2/PostgreSQL), se aplicável.

3. Execute a aplicação:

   ```bash
   ./mvnw spring-boot:run
   # ou
   mvn spring-boot:run
   ```

   ou, se usar Gradle:

   ```bash
   ./gradlew bootRun
   ```

## Como usar

1. No Telegram, procure pelo bot (nome configurado no BotFather) e envie `/start`.
2. Use os comandos disponíveis, por exemplo:
   - `/novo` – iniciar fluxo para cadastrar um novo compromisso;
   - `/hoje` – ver os horários de hoje;
   - `/amanha` – ver os horários de amanhã;
   - `/proximos` – listar próximos compromissos.

3. A aplicação enviará lembretes automaticamente próximo ao horário dos compromissos (conforme configuração do scheduler).

> Os comandos exatos podem variar conforme a versão do código. Consulte a classe de bot ou o help interno (`/help`) para a lista atualizada.

## Estrutura de pastas (exemplo)

```text
agenda-assistant/
├── src/
│   ├── main/
│   │   ├── java/com/SEU_PACOTE/agendaassistant/
│   │   │   ├── bot/           # Integração com Telegram (controllers/handlers)
│   │   │   ├── core/          # Entidades e regras de negócio de agenda
│   │   │   ├── service/       # SchedulingService, ReminderService etc.
│   │   │   └── config/        # Configurações do Spring, schedulers, beans
│   │   └── resources/
│   │       ├── application.yml
│   │       └── ...
│   └── test/...
└── pom.xml / build.gradle
```

## Contribuindo

Sugestões, issues e PRs são bem-vindos.  
Algumas ideias de contribuição:

- novos canais (WhatsApp, e-mail, outros);
- melhorias de UX na conversa com o bot;
- suporte a múltiplos fusos horários;
- exportação/importação de agenda.

---

_Projeto em desenvolvimento para estudo e portfólio._
