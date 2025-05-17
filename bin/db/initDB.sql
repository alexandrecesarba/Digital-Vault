-- --------------------------------------------------
-- Remove quaisquer versões antigas  
-- --------------------------------------------------
DROP TABLE IF EXISTS Registros;
DROP TABLE IF EXISTS Mensagens;
DROP TABLE IF EXISTS Chaveiro;
DROP TABLE IF EXISTS Usuarios;
DROP TABLE IF EXISTS Grupos;

-- --------------------------------------------------
-- Grupos — antes era apenas campo groupName em User  
-- --------------------------------------------------
CREATE TABLE Grupos (
  gid   INTEGER PRIMARY KEY AUTOINCREMENT,
  nome  TEXT    NOT NULL UNIQUE
);

-- --------------------------------------------------
-- Chaveiro — armazena certificado PEM e chave privada  
-- --------------------------------------------------
CREATE TABLE Chaveiro (
  kid             INTEGER PRIMARY KEY AUTOINCREMENT,
  uid             INTEGER NOT NULL UNIQUE,
  certificado     TEXT    NOT NULL,    -- PEM em Base64
  chave_privada   BLOB    NOT NULL,    -- AES-256 criptografada
  FOREIGN KEY(uid) REFERENCES Usuarios(uid)
);

-- --------------------------------------------------
-- Usuários — agora separados em três campos em vez de muitos  
-- --------------------------------------------------
CREATE TABLE Usuarios (
  uid                   INTEGER PRIMARY KEY AUTOINCREMENT,
  nome                  TEXT    NOT NULL,
  email                 TEXT    NOT NULL UNIQUE,      -- login name
  gid                   INTEGER NOT NULL,             -- referência a Grupos
  senha_hash            TEXT    NOT NULL,             -- bcrypt 2y$08$... (60 chars)
  totp_key_encrypted    BLOB    NOT NULL,             -- segredo TOTP (BASE32) criptografado com AES/ECB/PKCS5Padding
  kid                   INTEGER NULL,             -- referência a Chaveiro
  blocked               INTEGER NOT NULL DEFAULT 0,   -- 0 = não bloqueado, 1 = bloqueado
  FOREIGN KEY(gid) REFERENCES Grupos(gid),
  FOREIGN KEY(kid) REFERENCES Chaveiro(kid)
);

-- --------------------------------------------------
-- Mensagens — mesma lógica, guarda MID e texto :contentReference[oaicite:0]{index=0}:contentReference[oaicite:1]{index=1}
-- --------------------------------------------------
CREATE TABLE Mensagens (
  mid    INTEGER PRIMARY KEY,
  texto  TEXT    NOT NULL
);

-- --------------------------------------------------
-- Registros — cada evento (RID), com timestamp, MID, UID opcional e arquivo  
-- --------------------------------------------------
CREATE TABLE Registros (
  rid      INTEGER PRIMARY KEY AUTOINCREMENT,
  datahora DATETIME DEFAULT CURRENT_TIMESTAMP,
  mid      INTEGER NOT NULL,   -- código da mensagem
  uid      INTEGER,            -- usuário envolvido (quando aplicável)
  arquivo  TEXT,               -- nome do arquivo processado (quando aplicável)
  FOREIGN KEY(mid) REFERENCES Mensagens(mid),
  FOREIGN KEY(uid) REFERENCES Usuarios(uid)
);

-- Sessão e partida de sistema
INSERT INTO Mensagens VALUES(1001, 'Sistema iniciado.');
INSERT INTO Mensagens VALUES(1002, 'Sistema encerrado.');
INSERT INTO Mensagens VALUES(1003, 'Sessão iniciada para <login_name>.');
INSERT INTO Mensagens VALUES(1004, 'Sessão encerrada para <login_name>.');
INSERT INTO Mensagens VALUES(1005, 'Partida do sistema iniciada para cadastro do administrador.');
INSERT INTO Mensagens VALUES(1006, 'Partida do sistema iniciada para operação normal pelos usuários.');

-- Etapa 1 – Login
INSERT INTO Mensagens VALUES(2001, 'Autenticação etapa 1 iniciada.');
INSERT INTO Mensagens VALUES(2002, 'Autenticação etapa 1 encerrada.');
INSERT INTO Mensagens VALUES(2003, 'Login name <login_name> identificado com acesso liberado.');
INSERT INTO Mensagens VALUES(2004, 'Login name <login_name> identificado com acesso bloqueado.');
INSERT INTO Mensagens VALUES(2005, 'Login name <login_name> não identificado.');

-- Etapa 2 – Senha pessoal
INSERT INTO Mensagens VALUES(3001, 'Autenticação etapa 2 iniciada para <login_name>.');
INSERT INTO Mensagens VALUES(3002, 'Autenticação etapa 2 encerrada para <login_name>.');
INSERT INTO Mensagens VALUES(3003, 'Senha pessoal verificada positivamente para <login_name>.');
-- INSERT INTO Mensagens VALUES(3004, 'Senha pessoal verificada negativamente para <login_name>.');
INSERT INTO Mensagens VALUES(3004, 'Primeiro erro da senha pessoal contabilizado para <login_name>.');
INSERT INTO Mensagens VALUES(3005, 'Segundo erro da senha pessoal contabilizado para <login_name>.');
INSERT INTO Mensagens VALUES(3006, 'Terceiro erro da senha pessoal contabilizado para <login_name>.');
INSERT INTO Mensagens VALUES(3007, 'Acesso do usuario <login_name> bloqueado pela autenticação etapa 2.');

-- Etapa 3 – TOTP
INSERT INTO Mensagens VALUES(4001, 'Autenticação etapa 3 iniciada para <login_name>.');
INSERT INTO Mensagens VALUES(4002, 'Autenticação etapa 3 encerrada para <login_name>.');
INSERT INTO Mensagens VALUES(4003, 'Token verificado positivamente para <login_name>.');
INSERT INTO Mensagens VALUES(4004, 'Primeiro erro de token contabilizado para <login_name>.');
INSERT INTO Mensagens VALUES(4005, 'Segundo erro de token contabilizado para <login_name>.');
INSERT INTO Mensagens VALUES(4006, 'Terceiro erro de token contabilizado para <login_name>.');
INSERT INTO Mensagens VALUES(4007, 'Acesso do usuario <login_name> bloqueado pela autenticação etapa 3.');

-- Menu principal
INSERT INTO Mensagens VALUES(5001, 'Tela principal apresentada para <login_name>.');
INSERT INTO Mensagens VALUES(5002, 'Opção 1 do menu principal selecionada por <login_name>.');
INSERT INTO Mensagens VALUES(5003, 'Opção 2 do menu principal selecionada por <login_name>.');
INSERT INTO Mensagens VALUES(5004, 'Opção 3 do menu principal selecionada por <login_name>.');

-- Cadastro de usuário
INSERT INTO Mensagens VALUES(6001, 'Tela de cadastro apresentada para <login_name>.');
INSERT INTO Mensagens VALUES(6002, 'Botão cadastrar pressionado por <login_name>.');
INSERT INTO Mensagens VALUES(6003, 'Senha pessoal inválida fornecida por <login_name>.');
INSERT INTO Mensagens VALUES(6004, 'Caminho do certificado digital inválido fornecido por <login_name>.');
INSERT INTO Mensagens VALUES(6005, 'Chave privada verificada negativamente para <login_name> (caminho inválido).');
INSERT INTO Mensagens VALUES(6006, 'Chave privada verificada negativamente para <login_name> (frase secreta inválida).');
INSERT INTO Mensagens VALUES(6007, 'Chave privada verificada negativamente para <login_name> (assinatura digital inválida).');
INSERT INTO Mensagens VALUES(6008, 'Confirmação de dados aceita por <login_name>.');
INSERT INTO Mensagens VALUES(6009, 'Confirmação de dados rejeitada por <login_name>.');
INSERT INTO Mensagens VALUES(6010, 'Botão voltar de cadastro para o menu principal pressionado por <login_name>.');

-- Consulta de arquivos secretos
INSERT INTO Mensagens VALUES(7001, 'Tela de consulta de arquivos secretos apresentada para <login_name>.');
INSERT INTO Mensagens VALUES(7002, 'Botão voltar de consulta para o menu principal pressionado por <login_name>.');
INSERT INTO Mensagens VALUES(7003, 'Botão Listar de consulta pressionado por <login_name>.');
INSERT INTO Mensagens VALUES(7004, 'Caminho de pasta inválido fornecido por <login_name>.');
INSERT INTO Mensagens VALUES(7005, 'Arquivo de índice decriptado com sucesso para <login_name>.');
INSERT INTO Mensagens VALUES(7006, 'Arquivo de índice verificado (integridade e autenticidade) com sucesso para <login_name>.');
INSERT INTO Mensagens VALUES(7007, 'Falha na decriptação do arquivo de índice para <login_name>.');
INSERT INTO Mensagens VALUES(7008, 'Falha na verificação (integridade e autenticidade) do arquivo de índice para <login_name>.');
INSERT INTO Mensagens VALUES(7009, 'Lista de arquivos presentes no índice apresentada para <login_name>.');
INSERT INTO Mensagens VALUES(7010, 'Arquivo <arq_name> selecionado por <login_name> para decriptação.');
INSERT INTO Mensagens VALUES(7011, 'Acesso permitido ao arquivo <arq_name> para <login_name>.');
INSERT INTO Mensagens VALUES(7012, 'Acesso negado ao arquivo <arq_name> para <login_name>.');
INSERT INTO Mensagens VALUES(7013, 'Arquivo <arq_name> decriptado com sucesso para <login_name>.');
INSERT INTO Mensagens VALUES(7014, 'Arquivo <arq_name> verificado (integridade e autenticidade) com sucesso para <login_name>.');
INSERT INTO Mensagens VALUES(7015, 'Falha na decriptação do arquivo <arq_name> para <login_name>.');
INSERT INTO Mensagens VALUES(7016, 'Falha na verificação (integridade e autenticidade) do arquivo <arq_name> para <login_name>.');

-- Tela de saída
INSERT INTO Mensagens VALUES(8001, 'Tela de saída apresentada para <login_name>.');
INSERT INTO Mensagens VALUES(8002, 'Botão encerrar sessão pressionado por <login_name>.');
INSERT INTO Mensagens VALUES(8003, 'Botão encerrar sistema pressionado por <login_name>.');
INSERT INTO Mensagens VALUES(8004, 'Botão voltar de sair para o menu principal pressionado por <login_name>.');
