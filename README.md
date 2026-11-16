# SmartStock

API REST para controle de estoque de mercados. O sistema acompanha lotes e validades, **aplica desconto
dinâmico** nos produtos perto de vencer e avisa quando um produto está **abaixo do estoque mínimo**.

## Tecnologias

- Java 25 e Spring Boot 4 (Web MVC, Data JPA, Validation, Security, Mail)
- MySQL (padrão), PostgreSQL e SQL Server (drivers incluídos) e H2 em memória (testes)
- Lombok, Maven

## Como rodar

1. Crie o banco no MySQL:
   ```sql
   CREATE DATABASE smart_stock_market;
   ```
2. Defina as variáveis de ambiente (as senhas **não** ficam no código):

   | Variável | Obrigatória | Para que serve |
   |---|---|---|
   | `DB_PASSWORD` | sim | senha do banco |
   | `APP_SENHA` | sim | senha de acesso à API (mínimo 8 caracteres) |
   | `APP_USUARIO` | não | usuário de acesso à API (padrão: `admin`) |
   | `DB_URL`, `DB_USERNAME` | não | padrão: MySQL em `localhost:3306` e `root` |
   | `DDL_AUTO` | não | `update` (padrão, cria/ajusta tabelas) ou `none`/`validate` em banco existente |
   | `APP_FUSO` | não | fuso das datas (padrão: `America/Sao_Paulo`) |
   | `DESCONTO_FAIXAS` | não | faixas de desconto (padrão: `2:50,5:30,10:10`) |
   | `SOMENTE_LEITURA` | não | `true` = a API só consulta e avisa (padrão: `false`) |

   - Windows (PowerShell): `$env:DB_PASSWORD="sua_senha"; $env:APP_SENHA="outra_senha"`
   - Linux/macOS: `export DB_PASSWORD=sua_senha APP_SENHA=outra_senha`
   - IntelliJ: *Run > Edit Configurations > Environment variables*
3. Suba a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```
   A API responde em `http://localhost:8080`.

## Autenticação

Todos os endpoints exigem **HTTP Basic** (usuário e senha definidos por `APP_USUARIO` e `APP_SENHA`).
Sem credenciais, a API responde `401`. Em produção, use HTTPS: no Basic, usuário e senha
trafegam em Base64, que não é criptografia.

```bash
curl -u admin:SUA_APP_SENHA localhost:8080/api/produtos
```

## Paginação

As listagens (`/api/produtos`, `/api/produtos/precos-atuais`, `/api/estoque/lotes`,
`/api/estoque/alertas-validade` e `/api/estoque/alertas-reposicao`) aceitam `?page=0&size=20`
(página começa em 0; `size` máximo: 100). A resposta traz os itens em `content` e os totais em `page`:

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
| GET | `/api/produtos/{id}/preco-atual` | **Quanto cobrar agora**, com o desconto de validade já aplicado |
| GET | `/api/produtos/codigo/{codigoBarras}/preco-atual` | O mesmo, pelo código de barras (para o caixa/PDV) |
| GET | `/api/produtos/precos-atuais` | Preço atual de todos os produtos (paginado, por nome) |
| GET | `/api/estoque/lotes` | Lista lotes |
| POST | `/api/estoque/lotes` | Entrada de estoque (novo lote) |
| POST | `/api/estoque/baixa` | Saída/venda (FEFO) já com o desconto aplicado e o valor total |
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

# quanto cobrar agora pelo produto lido no caixa
curl -u admin:SUA_APP_SENHA localhost:8080/api/produtos/codigo/789100/preco-atual

# registrar uma venda
curl -u admin:SUA_APP_SENHA -X POST localhost:8080/api/estoque/baixa -H "Content-Type: application/json" \
  -d '{"produtoId":1,"quantidade":2}'
```

Resposta de `preco-atual` (lote que vence em 1 dia, preço de tabela R$ 10,00):

```json
{
  "produtoId": 1, "codigoBarras": "789100", "produto": "Leite integral",
  "precoOriginal": 10.00, "precoAtual": 5.00, "percentualDesconto": 50,
  "dataValidadeProximoLote": "2026-11-17", "diasParaVencer": 1,
  "estoqueDisponivel": 30, "status": "DESCONTO DE 50% APLICADO: vence em 1 dia(s)"
}
```

Resposta de `baixa` (2 un. do lote que vence em 1 dia + 1 un. de um lote sem desconto):

```json
{
  "produtoId": 1, "produto": "Leite integral", "quantidadeRetirada": 3, "estoqueRestante": 27,
  "valorTotal": 20.00,
  "itens": [
    { "loteId": 4, "quantidade": 2, "dataValidade": "2026-11-17", "percentualDesconto": 50, "precoUnitario": 5.00 },
    { "loteId": 5, "quantidade": 1, "dataValidade": "2026-12-06", "percentualDesconto": 0,  "precoUnitario": 10.00 }
  ]
}
```

## Regras de negócio

**Desconto por validade** (classe `PoliticaDesconto`, faixas em `smartstock.desconto.faixas`):

| Dias para vencer | Desconto (padrão) |
|---|---|
| 0 a 2 | 50% |
| 3 a 5 | 30% |
| 6 a 10 | 10% |
| mais de 10 | nenhum |
| já vencido | nenhum (o status indica "retirar da venda") |
| lote sem validade | nenhum |

- **Configuração das faixas:** lista `diasAte:percentual` em ordem crescente. Exemplo:
  `DESCONTO_FAIXAS=1:70,7:20,15:5` (até 1 dia 70%, até 7 dias 20%, até 15 dias 5%). Use `nenhuma` para
  desligar o desconto. Uma configuração inválida impede o sistema de subir, com mensagem explicando o erro.
- **Desconto aplicado:** o preço atual de um produto é o do **lote que será vendido primeiro (FEFO)**,
  exatamente a unidade que o caixa vai entregar. A baixa aplica o desconto de cada lote retirado e devolve
  o valor total. O `precoOriginal` cadastrado nunca é alterado: o desconto é calculado na hora, sempre
  com a data de hoje, então não existe preço "esquecido" de ontem.
- **Reposição:** lotes vencidos não contam como estoque disponível.
- **Baixa (FEFO):** vende primeiro o lote que vence antes; lotes vencidos nunca são vendidos.
- **Alertas de validade:** a janela é `smartstock.dias-alerta` (padrão 10), mas nunca menor que a maior faixa
  de desconto, para todo lote que já recebe desconto aparecer no alerta.
- **Aviso automático:** todo dia às 8h (fuso `smartstock.fuso`; configurável em `smartstock.notificacao.cron`)
  o sistema junta os alertas de validade e de reposição e os envia. Se não houver alerta, nada é enviado.
  Por padrão os alertas vão só para o log; para receber por **e-mail**, veja a próxima seção.
- **Fuso horário:** "hoje" é calculado em `smartstock.fuso` (padrão `America/Sao_Paulo`), e não no fuso do
  servidor. Assim, um servidor em UTC não vê os produtos "vencendo um dia antes" depois das 21h.

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

## Usando um banco existente

O SmartStock foi feito para ser ligado ao banco de um mercado só com configuração, desde que o banco tenha
**o equivalente a duas tabelas**:

| Tabela (nome lógico) | Colunas que o sistema precisa |
|---|---|
| `produtos` | chave (`pk_produto`), `codigo_barras`, `nome`, `categoria`, `preco_original`, `estoque_minimo` |
| `lotes` | chave (`pk_lote`), `produto_id` (referência ao produto), `quantidade_atual`, `data_validade` (pode ser nula = sem validade) |

### Passo a passo

1. **Driver e URL.** Os drivers de MySQL, PostgreSQL e SQL Server já estão no projeto. Basta definir `DB_URL`,
   `DB_USERNAME` e `DB_PASSWORD`:

   | Banco | Exemplo de `DB_URL` |
   |---|---|
   | MySQL | `jdbc:mysql://servidor:3306/mercado?useSSL=false&serverTimezone=UTC` |
   | PostgreSQL | `jdbc:postgresql://servidor:5432/mercado` |
   | SQL Server | `jdbc:sqlserver://servidor:1433;databaseName=mercado;encrypt=true;trustServerCertificate=true` |

   Para outro banco (Oracle, MariaDB...), adicione o driver no `pom.xml`; o dialeto é detectado sozinho.
2. **Não deixe o sistema mexer no esquema do cliente:** defina `DDL_AUTO=none` (ou `validate`, que só confere
   se as tabelas e colunas existem e acusa o que faltar).
3. **Informe os nomes reais** no `application.properties` (ou como variáveis de ambiente). Só é preciso
   listar o que for diferente do padrão:

   ```properties
   smartstock.banco.nomes.produtos=tb_mercadoria
   smartstock.banco.nomes.pk_produto=cod_mercadoria
   smartstock.banco.nomes.codigo_barras=ean
   smartstock.banco.nomes.preco_original=preco_venda
   smartstock.banco.nomes.lotes=tb_lote_estoque
   smartstock.banco.nomes.pk_lote=cod_lote
   smartstock.banco.nomes.produto_id=cod_mercadoria
   smartstock.banco.nomes.quantidade_atual=qtd
   smartstock.banco.nomes.data_validade=validade
   ```

   Nomes lógicos aceitos: tabelas `produtos` e `lotes`; colunas `pk_produto`, `codigo_barras`, `nome`,
   `categoria`, `preco_original`, `estoque_minimo`, `pk_lote`, `produto_id`, `quantidade_atual`,
   `data_validade`. Como variável de ambiente, use o formato `SMARTSTOCK_BANCO_NOMES_CODIGO_BARRAS=ean`.
   Para forçar aspas no identificador (nomes com maiúsculas no PostgreSQL, por exemplo), escreva o valor
   entre aspas duplas: `"Produtos"`. Schema diferente do padrão: `spring.jpa.properties.hibernate.default_schema=meu_schema`.
4. **Se o ERP/PDV é dono dos dados**, ligue `SOMENTE_LEITURA=true`: a API só consulta e avisa (GET e
   `/api/avisos/enviar`); cadastro, entrada, baixa e exclusão respondem `403`.

### E se o banco do mercado não tiver essa estrutura?

- **Estoque guardado como um número só no produto (sem lotes/validade):** não há como saber a validade,
  então o sistema não tem o que avisar. Crie uma tabela de lotes, ou uma **view** chamada como a tabela
  lógica `lotes` (com as colunas acima) que monte os lotes a partir das tabelas do ERP, e aponte
  `smartstock.banco.nomes.lotes` para ela. Com views, use `SOMENTE_LEITURA=true`.
- **Colunas com tipos muito diferentes** (ex.: validade como texto) ou **chave composta:** use uma view que
  converta para o formato esperado.
- **Colunas extras no banco do cliente:** sem problema, o sistema ignora o que não mapeia.

## Testes

```bash
./mvnw test
```

Os testes usam H2 em memória e não precisam do MySQL. Cobrem descontos (incluindo faixas configuráveis e
configuração inválida), preço atual, baixa com desconto, FEFO, reposição, aviso, fuso, segurança, modo
somente leitura e o mapeamento de tabelas/colunas de um banco existente.

## Estrutura

```
src/main/java/com/smartstock
├── DemoApplication.java   # classe principal (@EnableScheduling)
├── config/                # segurança (HTTP Basic), relógio/fuso e mapeamento de nomes do banco
├── controller/            # endpoints REST
├── notificacao/           # aviso diário e canais (log, e-mail)
├── service/               # regras de negócio (PoliticaDesconto, PrecoService, EstoqueService, ProdutoService)
├── repository/            # acesso ao banco (Spring Data JPA)
├── model/                 # entidades (Produto, Lote)
├── dto/                   # objetos de entrada e saída
└── exception/             # erros de negócio e tratamento global
```
