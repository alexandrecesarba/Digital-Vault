// Alexandre (2010292) e Enrico (2110927)
package main.java.util;
import auth.Auth;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class KeyIntegrityTest {
  public static void main(String[] args) throws Exception {
    // 1) carrega e testa a chave do administrador
    String adminPass = "admin";
    PrivateKey adminKey = Auth.loadPrivateKey(
      adminPass,
      Paths.get("Pacote-T4\\Keys\\admin-pkcs8-aes.key")
    );
    X509Certificate adminCert = Auth.readCertificate(
      Files.readAllBytes(Paths.get("Pacote-T4/Keys/admin-x509.crt"))
    );
    System.out.println("Admin key OK? " + Auth.testPrivateKey(adminKey, adminCert));

    // 2) carrega e testa a chave do usuário 1
    String user1Pass = "user01";
    PrivateKey user1Key = Auth.loadPrivateKey(
      user1Pass,
      Paths.get("Pacote-T4/Keys/user01-pkcs8-aes.key")
    );
    X509Certificate user1Cert = Auth.readCertificate(
      Files.readAllBytes(Paths.get("Pacote-T4/Keys/user01-x509.crt"))
    );
    System.out.println("User1 key OK? " + Auth.testPrivateKey(user1Key, user1Cert));

    // 3) e do usuário 2
    String user2Pass = "user02";
    PrivateKey user2Key = Auth.loadPrivateKey(
      user2Pass,
      Paths.get("Pacote-T4/Keys/user02-pkcs8-aes.key")
    );
    X509Certificate user2Cert = Auth.readCertificate(
      Files.readAllBytes(Paths.get("Pacote-T4/Keys/user02-x509.crt"))
    );
    System.out.println("User2 key OK? " + Auth.testPrivateKey(user2Key, user2Cert));
  }
}
