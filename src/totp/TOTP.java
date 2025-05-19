// Alexandre (2010292) e Enrico (2110927)

package totp;
import totp.Base32;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;


public class TOTP {
    private byte[] key = null;
    private long timeStepInSeconds = 30;

    /**
     * Construtor da classe. Recebe a chave secreta em BASE32 e o intervalo
     * de tempo a ser adotado (default = 30 segundos). Decodifica a chave secreta
     * e armazena em key. Em caso de erro, lança Exception.
     */
    public TOTP(String base32EncodedSecret, long timeStepInSeconds) throws Exception {
        this.timeStepInSeconds = timeStepInSeconds;
        Base32 b32 = new Base32(Base32.Alphabet.BASE32, false, false);
        this.key = b32.fromString(base32EncodedSecret);
        if (this.key == null)
          throw new IllegalArgumentException("Chave Base32 inválida");
    }

    /**
     * Recebe o HASH HMAC-SHA1 e determina o código TOTP de 6 dígitos,
     * prefixado com zeros quando necessário.
     */
    private String getTOTPCodeFromHash(byte[] hash) {
        int offset = hash[hash.length - 1] & 0x0F;
        int binary =
              ((hash[offset]     & 0x7F) << 24)
            | ((hash[offset + 1] & 0xFF) << 16)
            | ((hash[offset + 2] & 0xFF) <<  8)
            |  (hash[offset + 3] & 0xFF);
        int otp = binary % 1_000_000;
        return String.format("%06d", otp);
    }

    /**
     * Recebe o contador (8 bytes big-endian) e a chave secreta para produzir
     * o hash HMAC-SHA1.
     */
    private byte[] HMAC_SHA1(byte[] counter, byte[] keyByteArray) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA1");
        SecretKeySpec spec = new SecretKeySpec(keyByteArray, "HmacSHA1");
        hmac.init(spec);
        return hmac.doFinal(counter);
    }

    /**
     * Calcula o TOTP para um dado intervalo de tempo, usando
     * getTOTPCodeFromHash e HMAC_SHA1.
     */
    private String TOTPCode(long timeInterval) {
        // converte timeInterval para 8 bytes big-endian
        byte[] counter = new byte[8];
        long value = timeInterval;
        for (int i = 7; i >= 0; i--) {
            counter[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        try {
            byte[] hash = HMAC_SHA1(counter, key);
            return getTOTPCodeFromHash(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar TOTP", e);
        }
    }

    /**
     * Gera o código TOTP baseado no relógio atual.
     */
    public String generateCode() {
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        long timeInterval = currentTimeSeconds / timeStepInSeconds;
        return TOTPCode(timeInterval);
    }

    /**
     * Valida um código TOTP (inputTOTP), considerando uma
     * janela de atraso/adiantamento de 30 segundos (±1 timeStep).
     */
    public boolean validateCode(String inputTOTP) {
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        long timeInterval = currentTimeSeconds / timeStepInSeconds;
        // testa timeInterval -1, 0 e +1
        for (long i = -1; i <= 1; i++) {
            if (TOTPCode(timeInterval + i).equals(inputTOTP)) {
                return true;
            }
        }
        return false;
    }
}
