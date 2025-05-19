// Alexandre (2010292) e Enrico (2110927)

package main.java.util;

import db.DBManager;
import auth.Auth;

public class TesteTOTP {
    public static void main(String[] args) throws Exception {
        DBManager db = new DBManager();

        // 1) busca o blob encriptado
        byte[] encTotp = db.getEncryptedTotpByEmail("admin@inf1416.puc-rio.br");
        if (encTotp == null) {
            System.err.println("Não encontrou totp_key_encrypted no banco!");
            return;
        }

        // 2) decripta com a senha do usuário
        //    (no seu caso: "12345678")
        String senha = "12345678";
        String secret = Auth.decryptTOTPKey(encTotp, senha);
        System.out.println("TOTP Secret (Base32): " + secret);

        // 3) gera o código atual
        String code = Auth.generateTOTP(secret);
        System.out.println("Código TOTP gerado: " + code);

        // 4) valida
        boolean valido = Auth.validateTOTP(secret, code);
        System.out.println("Validação do mesmo código: " + valido);

        // 5) testa um código errado
        String fake = code.equals("000000") ? "123456" : "000000";
        boolean invalido = Auth.validateTOTP(secret, fake);
        System.out.println("Validação de código errado \"" + fake + "\": " + invalido);
    }
}
