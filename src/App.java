import db.DBManager;
import controller.AuthService;
import view.LoginView;
import javax.swing.*;
import java.sql.SQLException;
import java.util.List;
import view.PasswordView;
public class App {
    public static void main(String[] args) {
        DBManager     db          = new DBManager();
        AuthService   authService = new AuthService(db);

        SwingUtilities.invokeLater(() -> {
            // 1) LoginView: quando o login for OK, abre PasswordView
            new LoginView(authService, db, () -> {
                new PasswordView(authService, db, () -> {
                    System.out.println("Senha validada!");
                    // 2) Aqui, quando a senha for validada, você pode abrir a TOTPView…
                    // new TOTPView(authService, db, /* callback final */ () -> {
                    //     System.out.println("Autenticação completa!");
                    // });
                });
            });
        });
    }
}


