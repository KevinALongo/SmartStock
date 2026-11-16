# SmartStock

API REST para controle de estoque de mercados. O sistema acompanha lotes e validades,
sugere **desconto dinâmico** para produtos perto de vencer e avisa quando um produto
está **abaixo do estoque mínimo**.

## Tecnologias

- Java 25 e Spring Boot 4 (Web MVC, Data JPA, Validation, Security, Mail)
- MySQL (produção/desenvolvimento) e H2 em memória (testes)
- Lombok, Maven

## Como rodar

1. Crie o banco no MySQL:
   ```sql
   CREATE DATABASE smart_stock_market;
   ```
2. Defina as variáveis de ambiente (as senhas **não** ficam no código):

   | Variável | Obrigatória | Para que serve |
   |---|---|---|
   | `DB_PASSWORD` | sim | senha do MySQL |
   | `APP_SENHA` | sim | senha de acesso à API (mínimo 8 caracteres) |
   | `APP_USUARIO` | não | usuário de acesso à API (padrão: `admin`) |
   | `DB_URL`, `DB_USERNAME` | não | padrão: `localhost:3306` e `root` |

   - Windows (PowerShell): `$env:DB_PASSWORD="sua_senha"; $env:APP_SENHA="outra_senha"`
   - Linux/macOS: `export DB_PASSWORD=sua_senha APP_SENHA=outra_senha`
   - IntelliJ: *Run > Edit Configurations > Environment variables*
3. Suba a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```
   As tabelas são criadas automaticamente. A API responde em `http://localhost:8080`.

## Autenticação

Todos os endpoints exigem **HTTP Basic** (usuário e senha definidos por `APP_USUARIO` e `APP_SENHA`).
Sem credenciais, a API responde `401`. Em produção, use HTTPS: no Basic, usuário e senha
trafegam em Base64, que não é criptografia.

```bash
curl -u admin:SUA_APP_SENHA localhost:8080/api/produtos
```

## Paginação

As listagens (`/api/produtos`, `/api/estoque/lotes`, `/api/estoque/alertas-validade` e
`/api/estoque/alertas-reposicao`) aceitam `?page=0&size=20` (página começa em 0; `size` máximo: 100).
A resposta traz os itens em `content` e os totais em `page`:

```json
{ "content": [ ... ], "page": { "size": 20, "number": 0, "totalElements": 42, "totalPages": 3 } }
```

## Endpoints

| Método | Rota | O que faz |
|---|---|---|
| GET | `/api/produtos` | Lista produtos |
| GET | `/api/produtos/{id}` | Busca um produto |
| POST | `/api/produtos` | Cadastra produto |
| PUT | `/api/produtos/{id}` | Atualiza produto |
| DELETE | `/api/produtos/{id}` | Exclui produto (se não tiver lotes) |
| GET | `/api/estoque/lotes` | Lista lotes |
| POST | `/api/estoque/lotes` | Entrada de estoque (novo lote) |
| POST | `/api/estoque/baixa` | Saída/venda (retira primeiro do lote que vence antes) |
| GET | `/api/estoque/alertas-validade` | Lotes perto de vencer, com preço sugerido |
| GET | `/api/estoque/alertas-reposicao` | Produtos abaixo do estoque mínimo |
| POST | `/api/avisos/enviar` | Dispara o aviso agora (para testar o e-mail) |

### Exemplos

```bash
# cadastrar produto
curl -u admin:SUA_APP_SENHA -X POST localhost:8080/api/produtos -H "Content-Type: application/json" \
  -d '{"codigoBarras":"789100","nome":"Leite integral","categoria":"Laticínios","precoOriginal":6.50,"estoqueMinimo":20}'

# dar entrada em um lote (dataValidade é opcional para não perecíveis)
curl -u admin:SUA_APP_SENHA -X POST localhost:8080/api/estoque/lotes -H "Content-Type: application/json" \
  -d '{"produtoId":1,"quantidade":30,"dataValidade":"2026-12-01"}'

# registrar uma venda
curl -u admin:SUA_APP_SENHA -X POST localhost:8080/api/estoque/baixa -H "Content-Type: application/json" \
  -d '{"produtoId":1,"quantidade":2}'
```

## Regras de negócio

**Desconto por validade** (classe `PoliticaDesconto`):

| Dias para vencer | Desconto |
|---|---|
| 0 a 2 | 50% |
| 3 a 5 | 30% |
| 6 a 10 | 10% |
| mais de 10 | nenhum |
| já vencido | nenhum (o status indica "retirar da venda") |

- **Reposição:** lotes vencidos não contam como estoque disponível.
- **Baixa (FEFO):** vende primeiro o lote que vence antes; lotes vencidos nunca são vendidos.
- **Aviso automático:** todo dia às 8h (Brasília; configurável em `smartstock.notificacao.cron`)
  o sistema junta os alertas de validade e de reposição e os envia. Se não houver alerta, nada é enviado.
  Por padrão os alertas vão só para o log; para receber por **e-mail**, veja a próxima seção.
- A janela dos alertas de validade é configurável em `smartstock.dias-alerta` (padrão: 10).

## Notificação por e-mail

Desligada por padrão. Para ligar, defina estas variáveis de ambiente:

| Variável | Exemplo |
|---|---|
| `NOTIFICACAO_EMAIL_ATIVO` | `true` |
| `EMAIL_DESTINATARIO` | `gerente@mercado.com` (vários: separados por vírgula) |
| `EMAIL_REMETENTE` | `alertas@mercado.com` (opcional) |
| `SPRING_MAIL_HOST` | `smtp.gmail.com` |
| `SPRING_MAIL_USERNAME` | seu e-mail de envio |
| `SPRING_MAIL_PASSWORD` | senha SMTP (no Gmail, use uma *senha de app*, não a senha da conta) |

A porta padrão é 587 (STARTTLS). Para testar sem esperar as 8h:
`curl -u admin:SUA_APP_SENHA -X POST localhost:8080/api/avisos/enviar`.
Falhas de envio ficam registradas no log e não derrubam a aplicação.

**WhatsApp** não está implementado: exige conta e provedor próprios (Twilio ou WhatsApp Business API).
Para adicionar, crie uma classe que implemente `Notificador`.

## Testes

```bash
./mvnw test
```

Os testes usam H2 em memória e não precisam do MySQL.

## Estrutura

```
src/main/java/com/smartstock
├── DemoApplication.java   # classe principal (@EnableScheduling)
├── config/                # segurança (HTTP Basic)
├── controller/            # endpoints REST
├── notificacao/           # aviso diário e canais (log, e-mail)
├── service/               # regras de negócio (PoliticaDesconto, EstoqueService, ProdutoService)
├── repository/            # acesso ao banco (Spring Data JPA)
├── model/                 # entidades (Produto, Lote)
├── dto/                   # objetos de entrada e saída
└── exception/             # erros de negócio e tratamento global
```
