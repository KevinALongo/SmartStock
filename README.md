# SmartStock

API REST para controle de estoque de mercados. O sistema acompanha lotes e validades,
sugere **desconto dinâmico** para produtos perto de vencer e avisa quando um produto
está **abaixo do estoque mínimo**.

## Tecnologias

- Java 25 e Spring Boot 4 (Web MVC, Data JPA, Validation)
- MySQL (produção/desenvolvimento) e H2 em memória (testes)
- Lombok, Maven

## Como rodar

1. Crie o banco no MySQL:
   ```sql
   CREATE DATABASE smart_stock_market;
   ```
2. Defina a senha do banco como variável de ambiente (ela **não** fica no código):
   - Windows (PowerShell): `$env:DB_PASSWORD="sua_senha"`
   - Linux/macOS: `export DB_PASSWORD=sua_senha`
   - IntelliJ: *Run > Edit Configurations > Environment variables*

   Opcionais: `DB_URL` e `DB_USERNAME` (padrão: `localhost:3306` e `root`).
3. Suba a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```
   As tabelas são criadas automaticamente. A API responde em `http://localhost:8080`.

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

### Exemplos

```bash
# cadastrar produto
curl -X POST localhost:8080/api/produtos -H "Content-Type: application/json" \
  -d '{"codigoBarras":"789100","nome":"Leite integral","categoria":"Laticínios","precoOriginal":6.50,"estoqueMinimo":20}'

# dar entrada em um lote (dataValidade é opcional para não perecíveis)
curl -X POST localhost:8080/api/estoque/lotes -H "Content-Type: application/json" \
  -d '{"produtoId":1,"quantidade":30,"dataValidade":"2026-12-01"}'

# registrar uma venda
curl -X POST localhost:8080/api/estoque/baixa -H "Content-Type: application/json" \
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
- **Aviso automático:** todo dia às 8h (Brasília) os alertas são escritos no log. O envio por
  e-mail/WhatsApp ainda não foi implementado (ponto de extensão em `EstoqueService.verificarEAvisar`).
- A janela dos alertas de validade é configurável em `smartstock.dias-alerta` (padrão: 10).

## Testes

```bash
./mvnw test
```

Os testes usam H2 em memória e não precisam do MySQL.

## Estrutura

```
src/main/java/com/smartstock
├── DemoApplication.java   # classe principal (@EnableScheduling)
├── controller/            # endpoints REST
├── service/               # regras de negócio (PoliticaDesconto, EstoqueService, ProdutoService)
├── repository/            # acesso ao banco (Spring Data JPA)
├── model/                 # entidades (Produto, Lote)
├── dto/                   # objetos de entrada e saída
└── exception/             # erros de negócio e tratamento global
```
