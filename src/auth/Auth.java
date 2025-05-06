package auth;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import totp.TOTP;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import main.java.util.Node;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;

public class Auth {

    private static final byte[] APP_MASTER_KEY = 
    "0123456789ABCDEF0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);

    // 1. HASH DE SENHA: Bcrypt 2y custo=8 via BouncyCastle
    public static String hashPassword(String plainPassword) throws Exception {
        // gera salt aleatório de 16 bytes
        byte[] salt = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(salt);
        // custo = 8
        return OpenBSDBCrypt.generate(plainPassword.toCharArray(), salt, 8);
    }

    public static boolean authenticatePassword(String plainPassword, String storedHash) {
        return OpenBSDBCrypt.checkPassword(storedHash, plainPassword.toCharArray());
    }

    // 2. LEITURA DE CERTIFICADO X.509
    public static X509Certificate readCertificate(byte[] certBytes) throws Exception {
        try (InputStream in = new ByteArrayInputStream(certBytes)) {
            return (X509Certificate) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(in);
        }
    }

    // 3. CARREGAMENTO E D E C R I P T O G R A F I A  DA CHAVE PRIVADA (AES-256/ECB/PKCS5Padding)
    public static PrivateKey loadPrivateKey(String secretPhrase, Path keyFile)
            throws Exception {
        // deriva chave AES-256 a partir da frase secreta
        SecureRandom rand = SecureRandom.getInstance("SHA1PRNG");
        rand.setSeed(secretPhrase.getBytes(StandardCharsets.UTF_8));
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256, rand);
        SecretKeySpec aesKey = new SecretKeySpec(kg.generateKey().getEncoded(), "AES");

        // decripta o arquivo
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] encrypted = Files.readAllBytes(keyFile);
        byte[] decrypted = cipher.doFinal(encrypted);

        // reconstrói PrivateKey (PKCS#8)
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decrypted);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    // 4. TESTE DE CHAVE PRIVADA (8 192 bytes → SHA1withRSA)
    public static boolean testPrivateKey(PrivateKey privKey, X509Certificate cert)
            throws Exception {
        byte[] challenge = new byte[8192];
        SecureRandom.getInstanceStrong().nextBytes(challenge);

        // assina o desafio
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initSign(privKey);
        signer.update(challenge);
        byte[] signature = signer.sign();

        // verifica com a chave pública
        Signature verifier = Signature.getInstance("SHA1withRSA");
        verifier.initVerify(cert.getPublicKey());
        verifier.update(challenge);
        return verifier.verify(signature);
    }

    // 5. TOTP (gera e valida usando a classe TOTP implementada)
    public static String generateTOTP(String base32Secret) throws Exception {
        TOTP totp = new TOTP(base32Secret, 30);
        return totp.generateCode();
    }

    public static boolean validateTOTP(String base32Secret, String code) throws Exception {
        TOTP totp = new TOTP(base32Secret, 30);
        return totp.validateCode(code);
    }

    // 6. AUTENTICAÇÃO VIA TECLADO VIRTUAL
    //    inputs: lista de dígitos clicados na ordem (ex: [1,5,3,2,0] → "15320")
    public static boolean authenticateVirtualKeyboard(List<Integer> inputs, String storedHash) {
        StringBuilder sb = new StringBuilder();
        for (int d : inputs) {
            sb.append(d);
        }
        return authenticatePassword(sb.toString(), storedHash);
    }

    public static String decryptTOTPKey(byte[] encryptedKey) throws Exception {
        // 1) monta o SecretKeySpec com a chave mestra
        SecretKeySpec keySpec = new SecretKeySpec(APP_MASTER_KEY, "AES");

        // 2) inicializa o Cipher em modo DECRYPT com AES/ECB/PKCS5Padding
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        // 3) faz a descriptografia
        byte[] plain = cipher.doFinal(encryptedKey);

        // 4) converte de volta para String (Base32 em UTF-8)
        return new String(plain, StandardCharsets.UTF_8);
    }

        // criptografa o segredo TOTP (String Base32) → byte[]
        public static byte[] encryptTotpKey(String base32Secret) throws Exception {
            SecretKeySpec keySpec = new SecretKeySpec(APP_MASTER_KEY, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(base32Secret.getBytes(StandardCharsets.UTF_8));
        }

        // criptografa o conteúdo da sua chave privada (PKCS#8 bytes) → byte[]
        public static byte[] encryptPrivateKey(byte[] privateKeyBytes) throws Exception {
            SecretKeySpec keySpec = new SecretKeySpec(APP_MASTER_KEY, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(privateKeyBytes);
        }

        /**
     * Varre a árvore de escolhas e testa cada caminho
     * @param root árvore construída pelo PasswordView
     * @param userHash o hash bcrypt da senha real
     */
    public static boolean verificaArvoreSenha(Node root, String userHash) {
        return dfsVerify(root, "", userHash);
    }

    private static boolean dfsVerify(Node node, String prefix, String userHash) {
        if (node == null) return false;
        String novo = prefix + node.val;
        // se for folha (sem filhos) então testamos:
        if (node.esq == null && node.dir == null) {
            // compara a senha em texto puro com o hash
            if (OpenBSDBCrypt.checkPassword(userHash, novo.toCharArray())) {
                return true;
            } else {
                return false;
            }
        }
        // senão continua descendo em ambos
        if (dfsVerify(node.esq, novo, userHash)) return true;
        if (dfsVerify(node.dir, novo, userHash)) return true;
        return false;
    }
}
