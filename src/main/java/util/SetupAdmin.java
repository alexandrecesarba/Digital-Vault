// Alexandre (2010292) e Enrico (2110927)
// package main.java.util;

// import auth.Auth;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.nio.charset.StandardCharsets;

// /**
//  * Utility para gerar os INSERTs de Administrador:
//  *  - insere grupo
//  *  - insere usuário (com senha bcrypt e TOTP criptografado)
//  *  - insere chaveiro (certificado PEM + chave privada criptografada)
//  *
//  * Basta editar as variáveis abaixo, rodar este main, e colar
//  * os comandos SQL resultantes no seu script de criação de banco.
//  */
// public class SetupAdmin {

//     // —————— CONFIGURAÇÃO ——————
//     static final String NOME_ADMIN     = "Administrador";
//     static final String EMAIL_ADMIN    = "admin@inf1416.puc-rio.br";
//     static final String GRUPO_ADMIN    = "administrador";
//     static final String SENHA_ADMIN    = "89234012";           // escolha a senha
//     static final String TOTP_BASE32    = "JBSWY3DPEHPK3PXP";   // seu segredo Base32

//     // caminhos para os seus arquivos PEM/KEY (PKCS#8)  
//     static final Path CERT_PATH     = Paths.get("Pacote-T4/Keys/admin-x509.crt");
//     static final Path PRIVKEY_PATH  = Paths.get("Pacote-T4/Keys/admin-pkcs8-aes.key");
//     // ——————————————————————————

//     public static void main(String[] args) throws Exception {
//         // 1) Gera o hash bcrypt custo=8
//         String bcryptHash = Auth.hashPassword(SENHA_ADMIN);

//         // 2) Criptografa o TOTP Base32
//         byte[] encTotp = Auth.encryptTotpKey(TOTP_BASE32);

//         // **--------------  TESTE TOTP LOCAL --------------**
//         String currentTotpCode = Auth.generateTOTP(TOTP_BASE32);
//         System.out.println("=== CÓDIGO TOTP ATUAL ===  " + currentTotpCode);
//         // **------------------------------------------------**

//         // 3) Lê e prepara o PEM do certificado
//         String certPem = new String(Files.readAllBytes(CERT_PATH), StandardCharsets.UTF_8)
//                             .replace("'", "''");  

//         // 4) Criptografa a chave privada raw (PKCS#8 bytes)
//         byte[] rawPriv = Files.readAllBytes(PRIVKEY_PATH);
//         byte[] encPriv = Auth.encryptPrivateKey(rawPriv);

//         // 5) Imprime SQL de INSERT
//         System.out.println("-- =================================================================");
//         System.out.println("-- 1) Insere o grupo de Administradores");
//         System.out.println("INSERT INTO Grupos(nome) VALUES('" + GRUPO_ADMIN + "');");
//         System.out.println();

//         System.out.println("-- =================================================================");
//         System.out.println("-- 2) Insere o Usuário (sem kid ainda)");
//         System.out.println("--    Você deve copiar o gid retornado pelo comando:");
//         System.out.println("--      SELECT gid FROM Grupos WHERE nome='" + GRUPO_ADMIN + "';");
//         System.out.println("INSERT INTO Usuarios(nome, email, gid, senha_hash, totp_key_encrypted, kid) VALUES (");
//         System.out.println("  '" + NOME_ADMIN + "',");
//         System.out.println("  '" + EMAIL_ADMIN + "',");
//         System.out.println("  /* <substitua pelo gid acima> */,");
//         System.out.println("  '" + bcryptHash + "',");
//         System.out.println("  " + toHexLiteral(encTotp) + ",");
//         System.out.println("  /* <substitua por kid após inserir o Chaveiro> */");
//         System.out.println(");");
//         System.out.println();

//         System.out.println("-- =================================================================");
//         System.out.println("-- 3) Insere o Chaveiro (PEM + chave privada criptografada)");
//         System.out.println("--    Você deve copiar o uid gerado pelo INSERT em Usuarios:");
//         System.out.println("--      SELECT uid FROM Usuarios WHERE email='" + EMAIL_ADMIN + "';");
//         System.out.println("INSERT INTO Chaveiro(uid, certificado, chave_privada) VALUES (");
//         System.out.println("  /* <substitua pelo uid acima> */,");
//         System.out.println("  '" + certPem + "',");
//         System.out.println("  " + toHexLiteral(encPriv));
//         System.out.println(");");
//         System.out.println();

//         System.out.println("-- =================================================================");
//         System.out.println("-- 4) Atualiza o kid no usuário:");
//         System.out.println("--    Após executar o INSERT acima, obtenha:");
//         System.out.println("--      SELECT kid FROM Chaveiro WHERE uid=<uid>;");
//         System.out.println("UPDATE Usuarios");
//         System.out.println("  SET kid = /* <substitua pelo kid acima> */");
//         System.out.println("  WHERE email = '" + EMAIL_ADMIN + "';");
//         System.out.println("-- =================================================================");
//     }

//     /** Converte array de bytes em literal hex do SQLite: X'AB12CD…' */
//     private static String toHexLiteral(byte[] data) {
//         StringBuilder sb = new StringBuilder("X'");
//         for (byte b : data) {
//             sb.append(String.format("%02X", b));
//         }
//         sb.append("'");
//         return sb.toString();
//     }
// }



