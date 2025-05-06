package test.java;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.commons.codec.binary.Base32;
import totp.TOTP;

public class TOTPTest {

    // Uma chave Base32 válida (aleatória) para testes
    private static final String VALID_BASE32_SECRET = "JBSWY3DPEHPK3PXP"; 
    // Uma chave claramente inválida
    private static final String INVALID_BASE32_SECRET = "!!!!INVALID!!!!";

    private TOTP totp30s;

    @BeforeEach
    void setUp() throws Exception {
        // inicializa com passo de 30s (padrão)
        totp30s = new TOTP(VALID_BASE32_SECRET, 30);
    }

    @Test
    void constructor_withInvalidBase32_shouldThrow() {
        Exception ex = assertThrows(Exception.class, () -> {
            new TOTP(INVALID_BASE32_SECRET, 30);
        });
        assertTrue(ex.getMessage().contains("Chave Base32 inválida"));
    }

    @Test
    void generateCode_shouldReturn6DigitsNumericString() {
        String code = totp30s.generateCode();
        assertNotNull(code, "Código não pode ser nulo");
        assertEquals(6, code.length(), "Código deve ter 6 dígitos");
        assertTrue(code.matches("\\d{6}"), "Código deve conter apenas dígitos");
    }

    @Test
    void validateCode_shouldReturnTrueForGeneratedCode() {
        String code = totp30s.generateCode();
        // imediatamente após gerar, deve validar
        assertTrue(totp30s.validateCode(code), "generateCode() deve ser validado por validateCode()");
    }

    @Test
    void validateCode_shouldReturnFalseForWrongCode() {
        // um valor fixo improvável
        assertFalse(totp30s.validateCode("000000"), "Código aleatório incorreto não deve validar");
    }
}
