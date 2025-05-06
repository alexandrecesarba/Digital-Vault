package main.java.util;
import model.User;
import db.DBManager;
import auth.Auth;

public class TesteSenhaAdmin {
    public static void main(String[] args) throws Exception {
        DBManager db = new DBManager();
        // 1) busca o usuário admin
        User admin = db.findUserByEmail("admin@inf1416.puc-rio.br");
        if (admin == null) {
            System.err.println("Administrador não cadastrado!");
            return;
        }

        // 2) recupera o hash
        String hash = admin.getPasswordHash();  

        // 3) tenta uma senha qualquer (a que você configurou em SetupAdmin, ex "89234012")
        String tentativa = "89234012";

        // 4) chama o método de verificação de bcrypt
        boolean ok = Auth.authenticatePassword(tentativa, hash);

        System.out.println("Senha correta? " + ok);
    }
}
