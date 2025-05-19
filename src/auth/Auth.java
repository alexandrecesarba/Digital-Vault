// Alexandre (2010292) e Enrico (2110927)
package auth;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import totp.TOTP;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import main.java.util.Node;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;
import javax.crypto.SecretKey;
import java.util.Base64;
import javax.crypto.KeyGenerator;

public class Auth {

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
    public static X509Certificate readCertificate(byte[] bytes) throws CertificateException, IOException {
        String text = new String(bytes, StandardCharsets.UTF_8);
        int b = text.indexOf("-----BEGIN CERTIFICATE-----");
        int e = text.indexOf("-----END CERTIFICATE-----");
        if (b >= 0 && e > b) {
            text = text.substring(b, e + "-----END CERTIFICATE-----".length());
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate)cf.generateCertificate(in);
        }
    }
    
    // 3. CARREGAMENTO E DECRIPTOGRAFIA DA CHAVE PRIVADA (AES-256/ECB/PKCS5Padding)
    public static PrivateKey loadPrivateKey(String passphrase, Path keyPath) throws Exception {
        // Lê bytes do arquivo .key
        byte[] encryptedFileBytes = Files.readAllBytes(keyPath);
        return getPrivateKey(passphrase, encryptedFileBytes);
    }

    public static PrivateKey getPrivateKey(String passphrase, byte[] key) throws Exception{
        // 1) Deriva chave AES-256 a partir da passphrase
        SecureRandom prng = SecureRandom.getInstance("SHA1PRNG", "SUN"); // Especificar o provider para consistência
        prng.setSeed(passphrase.getBytes(StandardCharsets.UTF_8));

        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256, prng); // Tamanho da chave AES-256
        SecretKey aesKey = kg.generateKey();
        SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey.getEncoded(), "AES");

        // 2) Prepara cipher AES/ECB/PKCS5Padding para decriptografia
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

        // 3) Aplica decriptografia AES
        // O resultado esperado é o conteúdo PEM da chave como bytes
        byte[] decryptedPemBytes = cipher.doFinal(key);

        // 4) Converte os bytes decriptografados para uma string PEM
        //    e remove os delimitadores PEM e todos os espaços em branco/quebras de linha.
        String pemContent = new String(decryptedPemBytes, StandardCharsets.UTF_8)
            .replaceAll("-----BEGIN PRIVATE KEY-----", "")
            .replaceAll("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", ""); // Remove todos os caracteres de espaço em branco (espaços, tabs, newlines etc.)

        // 5) Decodifica o conteúdo Base64 da string PEM para obter os bytes DER PKCS#8
        byte[] pkcs8DerBytes = Base64.getDecoder().decode(pemContent);

        // 6) Constrói a PrivateKey RSA a partir dos bytes DER PKCS#8
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8DerBytes);
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

    public static byte[] encryptTOTPKey(String base32Secret, String userPassword) throws Exception {
        // 1) derivar key AES-256 a partir da senha:
        SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
        prng.setSeed(userPassword.getBytes(StandardCharsets.UTF_8));
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256, prng);
        SecretKey aesKey = kg.generateKey();
    
        // 2) criptografar o Base32:
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, aesKey);
        return c.doFinal(base32Secret.getBytes(StandardCharsets.UTF_8));
    }
    
    public static String decryptTOTPKey(byte[] encryptedTotp, String userPassword) throws Exception {
        // 1) deriva a MESMA key AES-256 a partir da senha:
        SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
        prng.setSeed(userPassword.getBytes(StandardCharsets.UTF_8));
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256, prng);
        SecretKey aesKey = kg.generateKey();
    
        // 2) descriptografa:
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] plain = c.doFinal(encryptedTotp);
        return new String(plain, StandardCharsets.UTF_8);
    }
    

    // criptografa o conteúdo da sua chave privada (PKCS#8 bytes) → byte[]
    // public static byte[] encryptPrivateKey(byte[] privateKeyBytes) throws Exception {
    //     SecretKeySpec keySpec = new SecretKeySpec(APP_MASTER_KEY, "AES");
    //     Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    //     cipher.init(Cipher.ENCRYPT_MODE, keySpec);
    //     return cipher.doFinal(privateKeyBytes);
    // }

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

    public static String recoverPassword(Node root, String userHash) {
        return dfsRecover(root, "", userHash);
    }
    private static String dfsRecover(Node node, String prefix, String userHash) {
        if (node == null) return null;
        String candidate = prefix + node.val;
        if (node.esq == null && node.dir == null) {
            if (OpenBSDBCrypt.checkPassword(userHash, candidate.toCharArray()))
                return candidate;
            else
                return null;
        }
        String left = dfsRecover(node.esq, candidate, userHash);
        if (left != null) return left;
        return dfsRecover(node.dir, candidate, userHash);
    }

public static byte[] decryptEnvelope(byte[] encryptedEnvelope, PrivateKey privateKey) throws Exception {
    // Initialize RSA cipher in PKCS1 padding mode
    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    cipher.init(Cipher.DECRYPT_MODE, privateKey);

    // Decrypt the envelope to get the PRNG seed
    byte[] seed = cipher.doFinal(encryptedEnvelope);

    return seed;
}

public static byte[] decryptFile(byte[] seed, byte[] encryptedData) throws Exception {
    // Initialize PRNG with provided seed
    SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
    prng.setSeed(seed);

    // Generate AES key from PRNG
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256, prng);
    SecretKey aesKey = keyGen.generateKey();

    // Initialize AES cipher for decryption
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, aesKey);

    // Decrypt the data
    return cipher.doFinal(encryptedData);
}

public static boolean verifySignature(byte[] text, byte[] signature, X509Certificate certificate) throws Exception {
    // Initialize RSA-SHA1 signature verifier
    Signature verifier = Signature.getInstance("SHA1withRSA");
    verifier.initVerify(certificate.getPublicKey());
    
    // Update with the text to verify
    verifier.update(text);
    
    // Verify the signature
    return verifier.verify(signature);
}



}
