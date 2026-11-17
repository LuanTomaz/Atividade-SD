# Protocolo de Invocação Remota sobre UDP

Implementação da atividade de Sistemas Distribuídos sobre o protocolo de requisição-resposta usando sockets UDP em Java.

## Funcionalidades

- Comunicação cliente-servidor com `DatagramSocket`.
- Primitivas equivalentes a:
  - `doOperation`: invocação remota pelo cliente;
  - `getRequest`: recebimento da requisição pelo servidor;
  - `sendReply`: envio da resposta pelo servidor.
- Calculadora remota com soma, subtração, multiplicação e divisão.
- Identificação única das requisições.
- Timeout e retransmissão limitada.
- Deduplicação de requisições retransmitidas.
- Tratamento de mensagens inválidas e erros da operação.

## Estrutura

```text
src\rpcudp\
├── CalculatorClient.java
├── CalculatorServer.java
├── CalculatorService.java
├── MessageCodec.java
├── ProtocolTest.java
├── RemoteRef.java
└── RemoteServer.java
```

## Pré-requisitos

- JDK 8 ou superior.
- PowerShell ou outro terminal.
- Acesso à pasta raiz do projeto:

```powershell
cd "C:\Users\luant\OneDrive\Documentos\Atividade-SD"
```

## Compilação

Execute:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -Xlint:all -d out src\rpcudp\*.java
```

Os arquivos compilados serão gerados na pasta `out`.

## Execução dos testes automatizados

```powershell
java -cp out rpcudp.ProtocolTest
```

Saída esperada:

```text
Todos os testes do protocolo passaram.
```

Os testes verificam:

- Operações matemáticas válidas.
- Argumentos inválidos.
- Divisão por zero.
- Comunicação UDP real em `localhost`.
- Processamento completo de uma requisição remota.

## Instanciando o servidor

Abra um terminal na pasta do projeto e execute:

```powershell
java -cp out rpcudp.CalculatorServer 5000
```

O número `5000` é a porta UDP utilizada pelo servidor. A saída esperada é:

```text
Servidor de calculadora ouvindo na porta 5000
```

Mantenha esse terminal aberto.

## Invocando o cliente

Abra outro terminal e execute os comandos abaixo:

```powershell
java -cp out rpcudp.CalculatorClient 127.0.0.1 5000 soma 40,2
java -cp out rpcudp.CalculatorClient 127.0.0.1 5000 subtracao 10,7
java -cp out rpcudp.CalculatorClient 127.0.0.1 5000 multiplicacao 6,7
java -cp out rpcudp.CalculatorClient 127.0.0.1 5000 divisao 20,4
```

Resultados esperados:

```text
Resultado remoto: 42
Resultado remoto: 3
Resultado remoto: 42
Resultado remoto: 5
```

Formato do comando:

```text
CalculatorClient <host> <porta> <operacao> <a,b>
```

Operações disponíveis:

| Operação | Exemplo |
|---|---|
| `soma` | `10,5` resulta em `15` |
| `subtracao` | `10,5` resulta em `5` |
| `multiplicacao` | `10,5` resulta em `50` |
| `divisao` | `10,5` resulta em `2` |

## Testes de erro e timeout

### Divisão por zero

Com o servidor em execução:

```powershell
java -cp out rpcudp.CalculatorClient 127.0.0.1 5000 divisao 5,0
```

O cliente deve informar:

```text
Operacao remota rejeitada: Divisao por zero
```

### Argumentos inválidos

```powershell
java -cp out rpcudp.CalculatorClient 127.0.0.1 5000 soma abc,2
```

O servidor deve retornar um erro explícito, sem ser encerrado.

### Timeout e retransmissão

Não inicie o servidor e execute:

```powershell
java -cp out rpcudp.CalculatorClient 127.0.0.1 5999 soma 1,2
```

Após três tentativas, o cliente deve informar que o tempo de espera foi excedido:

```text
Tempo esgotado aguardando resposta após 3 tentativa(s)
```

O cliente utiliza timeout de 1 segundo e até duas retransmissões.

## Protocolo da mensagem

Cada datagrama possui um cabeçalho binário com:

1. versão (`1 byte`);
2. tipo da mensagem (`REQUEST` ou `RESPONSE`, `1 byte`);
3. identificador da requisição (`long`, `8 bytes`);
4. identificador da operação (`int`, `4 bytes`);
5. status (`1 byte`);
6. tamanho do payload (`int`, `4 bytes`);
7. payload UTF-8.

Os identificadores das operações são:

| ID | Operação |
|---:|---|
| 1 | Soma |
| 2 | Subtração |
| 3 | Multiplicação |
| 4 | Divisão |

## Checklist de validação

- [ ] JDK instalado e disponível no `PATH`.
- [ ] Fontes compilados sem erros.
- [ ] `ProtocolTest` executado com sucesso.
- [ ] Servidor iniciado e aguardando na porta configurada.
- [ ] Soma executada remotamente.
- [ ] Subtração executada remotamente.
- [ ] Multiplicação executada remotamente.
- [ ] Divisão executada remotamente.
- [ ] Divisão por zero rejeitada corretamente.
- [ ] Argumentos inválidos rejeitados corretamente.
- [ ] Timeout testado sem servidor.
- [ ] Servidor encerrado com `Ctrl+C`.

## Relação com o enunciado

- **Endereçamento:** host e porta identificam o servidor remoto.
- **Numeração:** cada requisição possui um `requestId`.
- **Empacotamento:** `MessageCodec` transforma os dados em bytes.
- **Confirmação:** o servidor responde a cada requisição válida.
- **Controle de erro:** respostas informam mensagens inválidas e falhas da operação.
- **Retransmissão:** o cliente reenvia a requisição após timeout.
- **Deduplicação:** o servidor reutiliza a resposta de uma requisição já processada.
