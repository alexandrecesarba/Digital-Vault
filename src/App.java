import db.DBManager;
import controller.AuthService;
import view.LoginView;
import view.PasswordView;
import view.TOTPView;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        DBManager     db          = new DBManager();
        AuthService   authService = new AuthService(db);

        SwingUtilities.invokeLater(() -> {
            new LoginView(authService, db);
        });
    }
}
