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
INSERT INTO Mensagens VALUES(3004, 'Senha pessoal verificada negativamente para <login_name>.');
INSERT INTO Mensagens VALUES(3005, 'Primeiro erro da senha pessoal contabilizado para <login_name>.');
INSERT INTO Mensagens VALUES(3006, 'Segundo erro da senha pessoal contabilizado para <login_name>.');
INSERT INTO Mensagens VALUES(3007, 'Terceiro erro da senha pessoal contabilizado para <login_name>.');
INSERT INTO Mensagens VALUES(3008, 'Acesso do usuario <login_name> bloqueado pela autenticação etapa 2.');

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
INSERT INTO Mensagens VALUES(5005, 'Opção 4 do menu principal selecionada por <login_name>.');

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

-- Carregamento da chave privada
INSERT INTO Mensagens VALUES(7001, 'Tela de carregamento da chave privada apresentada para <login_name>.');
INSERT INTO Mensagens VALUES(7002, 'Caminho da chave privada inválido fornecido por <login_name>.');
INSERT INTO Mensagens VALUES(7003, 'Frase secreta inválida fornecida por <login_name>.');
INSERT INTO Mensagens VALUES(7004, 'Erro de validação da chave privada com o certificado digital de <login_name>.');
INSERT INTO Mensagens VALUES(7005, 'Chave privada validada com sucesso para <login_name>.');
INSERT INTO Mensagens VALUES(7006, 'Botão voltar de carregamento para o menu principal pressionado por <login_name>.');

-- Consulta de arquivos secretos
INSERT INTO Mensagens VALUES(8001, 'Tela de consulta de arquivos secretos apresentada para <login_name>.');
INSERT INTO Mensagens VALUES(8002, 'Botão voltar de consulta para o menu principal pressionado por <login_name>.');
INSERT INTO Mensagens VALUES(8003, 'Botão Listar de consulta pressionado por <login_name>.');
INSERT INTO Mensagens VALUES(8004, 'Caminho de pasta inválido fornecido por <login_name>.');
INSERT INTO Mensagens VALUES(8005, 'Lista de arquivos apresentada para <login_name>.');
INSERT INTO Mensagens VALUES(8006, 'Arquivo <arq_name> selecionado por <login_name> para decriptação.');
INSERT INTO Mensagens VALUES(8007, 'Arquivo <arq_name> verificado (integridade e autenticidade) com sucesso para <login_name>.');
INSERT INTO Mensagens VALUES(8008, 'Falha na decriptação do arquivo <arq_name> para <login_name>.');
INSERT INTO Mensagens VALUES(8009, 'Falha na verificação de integridade e autenticidade do arquivo <arq_name> para <login_name>.');

-- Saída do sistema
INSERT INTO Mensagens VALUES(9001, 'Tela de saída apresentada para <login_name>.');
INSERT INTO Mensagens VALUES(9002, 'Botão encerrar sessão pressionado por <login_name>.');
INSERT INTO Mensagens VALUES(9003, 'Botão encerrar sistema pressionado por <login_name>.');
INSERT INTO Mensagens VALUES(9004, 'Botão voltar de sair para o menu principal pressionado por <login_name>.');

----------------------------
-- Inserção de dados para testes 
----------------------------

-- 1) Insere o grupo "administrador"
INSERT INTO Grupos(nome)
VALUES('administrador');

-- Recupera o gid gerado (no SQLite, digamos que seja 1)
SELECT gid FROM Grupos WHERE nome='administrador';

INSERT INTO Usuarios(nome, email, gid, senha_hash, totp_key_encrypted, kid)
VALUES (
  'Administrador',
  'admin@inf1416.puc-rio.br',
  1,  -- <=== substitua pelo gid retornado acima
  '$2y$08$Hlf7uRLbPWydlvOcU1w6P.6PuYGe67.auipDO6DIZ0G.jxlrdKh8C',
  X'D703C733AE2FDC981CB973E1084F31AA9BF7330543BC847F4663B2330D47BBC7',
  1  -- <=== vamos preencher o kid só depois
);

SELECT uid FROM Usuarios WHERE email='admin@inf1416.puc-rio.br';


-- 2) Insere no Chaveiro o certificado PEM e o blob da chave privada criptografada
-- Substitua o X'...' pelo HEX do seu arquivo .key criptografado (AES-256/ECB/PKCS5Padding)
INSERT INTO Chaveiro(uid, certificado, chave_privada)
VALUES(
  1,  -- aqui vamos supor que o próximo usuário terá UID=1; na prática insira após criar o usuário ou trate em duas etapas
-----BEGIN CERTIFICATE-----
'MIIEQzCCAyugAwIBAgIBETANBgkqhkiG9w0BAQsFADCBhDELMAkGA1UEBhMCQlIx
CzAJBgNVBAgMAlJKMQwwCgYDVQQHDANSaW8xDDAKBgNVBAoMA1BVQzEQMA4GA1UE
CwwHSU5GMTQxNjETMBEGA1UEAwwKQUMgSU5GMTQxNjElMCMGCSqGSIb3DQEJARYW
Y2FAZ3JhZC5pbmYucHVjLXJpby5icjAeFw0yNTA0MjcyMjI1NDhaFw0yNjA0Mjcy
MjI1NDhaMHsxCzAJBgNVBAYTAkJSMQswCQYDVQQIDAJSSjEMMAoGA1UECgwDUFVD
MRAwDgYDVQQLDAdJTkYxNDE2MRYwFAYDVQQDDA1BZG1pbmlzdHJhdG9yMScwJQYJ
KoZIhvcNAQkBFhhhZG1pbkBpbmYxNDE2LnB1Yy1yaW8uYnIwggEiMA0GCSqGSIb3
DQEBAQUAA4IBDwAwggEKAoIBAQDDnq2WpTioReNQ3EapxCdmUt9khsS2BHf/YB7t
jGILCzQegnV1swvcH+xfd9FUjR7pORFSNvrfWKt93t3l2Dc0kCvVffh5BSnXIwwb
W94O+E1Yp6pvpyflj8YI+VLy0dNCiszHAF5ux6lRZYcrM4KiJndqeFRnqRP8zWI5
O1kJJMXzCqIXwmXtfqVjWiwXTnjU97xfQqKkmAt8Z+uxJaQxdZJBczmo/jQAIz1g
x+SXA4TshU5Ra4sQYLo5+FgAfA2vswHGXA6ba3N52wydZ2IYUJL2/YmTyfxzRnsy
uqbL+hcOw6bm+g0OEIIC7JduKpinz3BieiO15vameAJlqpedAgMBAAGjgccwgcQw
CQYDVR0TBAIwADAsBglghkgBhvhCAQ0EHxYdT3BlblNTTCBHZW5lcmF0ZWQgQ2Vy
dGlmaWNhdGUwHQYDVR0OBBYEFJ5Q2aq4LQ4HEMsalRoM3F7W3YQAMB8GA1UdIwQY
MBaAFCOBO8MZK5WZ2crqO2v+HCRzwoKOMEkGA1UdHwRCMEAwPqA8oDqGOGh0dHBz
Oi8vd3d3LWRpLmluZi5wdWMtcmlvLmJyL2luZjE0MTYvYWNfaW5mMTQxNl9jcmwu
cGVtMA0GCSqGSIb3DQEBCwUAA4IBAQBDqDIk15eFlgcfTExjZIRP2IsSLx8sRQAm
4ZKx9oImBFwU/kGJKcQgkijtFDs+gXkiNJtgJpuEHFoBUxLu8oJk5puSh6Gt+fMH
9ZfMgkL80AhC99TbXnTxF5jDKzL7ZTMuSWKSqkbnALnGAAKpHk1caybEiTDVrcs2
OtGrtby93Vz8dKH8v8irkBRIrg7Lw8YfK55EWKByDz02dqM73uG4mfLA5xJBbPBp
pZWvepgCdh7YdHkW8mligEq8Rl26Khn0eHrWV1M+e2leo3y95pp6Vj6exIPcMw7L
e+JfbH2AarWT+BtLvJrfhCNSpmKv2vH9Gfiv3P8kYdFuU9yqzVe+
-----END CERTIFICATE-----',
   X'33A5D968C4BB961297BE1D28091DA4C5F30C4CE30453115956DCC226E40F173AADDE97E68780A2AA65417273972EA73CADFCC86C04B06A22482731744C9E84F391C108697EA6778470251492FD91D8E629AFBEEE56E6F39D6C2F3AB5802182714F67746C5808CAD1E246103EB78915E0DB210C014F8B2B7C2311D642F20CB6EBD4CB44A65FFB51A61A19A6BE236E9AC6E983D53B7C1DFF3AAE9B70CB69FB3B6E2AD567F1F771513EF0FD0830EDCDACAD8CA4EAE057944DA11532F05D2E9DEBE30F418295A12CA18B6360A21B4300F089A4FBACC2E955C263C5563591DCFF6623EE8329D420A706D4F657FFDF26F16AF1A92C61084B83BCD62B82C5D916E9A9451E44C47241DF0CFFBC66A09F0C923ABEF7790B8AB09CDFA1A6116CAEC70D7550EA287CC3C2F6C1A8B027B0A2CEE66E93BA749224BE1650D535D4D0238C81E7187CEB771E1D9861F62EF38AF6120A953BD4EB34BF61754B004AF828AB358466E4356065B0DEA9D41B388953F2DB1C1A9AD61464DEA8A6EA77D97E5B2FA0A577F0D3D2D457BA4D06E8E34133043B77BCEF6770D5E870AE8C4D712007FCAB5E36E786A3DC567A451EB56EE5C65A89489F594E6042F2F1091BAFBF4F1FDB39F0A7FF8ED51EBD0585CE76CE5095838661C0D5AD07D425235B3041B32CFFCF71807D270BD82FB80969653640421EC91E89855EAEF1983739FC0290553013438EE8AEE63B5056BC56D5D968B00BBBA516797FBA7A053C3ADCD01E46A196021462D41DB2F93E2FE219C265493FB26D6E66D63FE344E0D5369797C72318C39397EA6476C90FD7BE072674B81FB56B523138EF5E14E7071558C3E5AFC82C6CACCF819AFC3F140C11CC7BDC04742A9B6AFC03B3E729D4453709EBD3238BFA56808B728D1C97C39E4AD2B11C1BEA2847A44689FE1ACF832544DE9C07C3E0C38266ED2E777ADD33EAA7A60463BC4553A87AA9B533B250DE3256C62D6DDAC441777C1B4CA9E9F1F7AF9C195F3B87FB1681E85688D294E44C21C88CA97514D7CD693C37C5377AC39FF28BB28F56B436BAFD7CC26C6B3C0175626E06A50F8C3C6D7348D9B7BE9C8BCA0D33DF33B2A62411A19C3236E9C03B4C704C3872BBE3A17F4AA6A021C7D66643C4D3785C627734D4635CB9D532C45ABEA5D3CCF4B1FB55A59F91571763C5D1E0E047D5FCDDDA2F69C149A39191E4F14BEFB8D50C9B468CCB4A419ED9A3F1A0498C6D6DDB59EAE0059A6B7FC1B5AB6D7EE40471BB89F0726DBAC3DA6F0AFC4731F779F3630A670EA060E2F8785ECBEA1D6F565D16628A3CF239D337B6329BBC1A69D09D91EB58E45FA4A4337ADC132D522788B77740B213EEA43FD1425479614C70EA79A14001B58AE397E4144482B57DEF3785B0CFFDE60B5F2CBC7F50A8D3A52E479F81C1A3307E7FEE3FB55177B77D83013600979359871ACBB89405371E3F7E387783A966DDEE538A1DDA5CFCF38D2156C5FE992FBFCA5C24917D5BF770B6A0CD7EDB70E1266F49C9CDFC268136738314FE3D4E49B9BF68F655D7CCFD2F92E5006DF9DB985FF5C971C9C6D926991E147FF9127591EA704D3E2FF45ABCE4F834817ABFEF1EC1AF376DC88DFC967D19C6B9CD5FF06DC7A72FD061A2BD7D37E0E149C81F4E89B4CE4296BCFF782B9F7FA7ABC2D67F290D083BCB4A422FBBCBE256E0A7AEFBC3E606017AD2F4AA9DE6445962C217AD87D599B62469002D159ACAF4CE3CC476522CC8733BE12296709168F192792C2ADA1473836B2D2B7A9834036F06FBBB2243167BDB989633CAE91FB925ABE859AA0911E5399AEF1EF17BE28D7CC4DCFD3AAF3B41A5ED587EC64EB4519F703249DDDAC46788E0A1AFAB90B6B23AE60BB0B5F94D137F7A5BEE708EA81883FC5BFB2711EA9FC58219D4A3823F01E555498FC538D1FFA4917FBD08C5068ACC9B9731BADA4C9CEA2E7A3C23C7DD5024F6F193642171160635C64F511A776EACC297C7D636CA11DEB576E14B902CC3E2CC931B406D54C91FA1EA0EE8272FD510509FD0EA5E6C2228EBA53A7ADF5B7C9B85F3C066BA9EF58FF6DAA55F7C9F95E407E029A679C9C43A844903E1BF86A6AB660E04E9AA68EC01095D612B26786670D472E0F1E6725778EC1BE3DCCB9837542873416FE6CEDB3A457476BC0F24A39DE5D456B99A2251968B40E936C9CFB648967761B4DB62093237E48FBE7256F00856B94B8A409759C9070655FCF0E48777F0DB3027F8FDABC42C704C587CA5A5336A4C92C34D0E7E89655DD29D1130209FE6F68B441BF421D7BB65BFFC7B0F9D8F08B50C33933D6F049ABB016AE962632BC9B3CA136BDA4C42358DF05E90994268EADF236E23B949A62F520B75B9F9AA9270042C56859AFE0375241437DDB1AE59EDB3EF027EB840E2EB693DCBDEB3FF02BF43E7D66098F5D8CC713894EB769BF7330543BC847F4663B2330D47BBC7'  -- blob da chave_privada AES-cryptografada
);

-- Recupera o kid gerado (por exemplo, kid=1)
SELECT kid FROM Chaveiro WHERE uid=1;

UPDATE Usuarios
   SET kid = 1   -- <=== substitua pelo kid retornado acima
 WHERE email = 'admin@inf1416.puc-rio.br';




