import db.DBManager;
import controller.AuthService;
import view.LoginView;
import javax.swing.*;
import java.sql.SQLException;
import java.util.List;
public class App {
    public static void main(String[] args) {
        DBManager db           = new DBManager();
        AuthService authService = new AuthService(db);

        SwingUtilities.invokeLater(() -> {
            new LoginView(
                authService,         
                db,                  
                () -> {             
                    System.out.println("Login successful!");
                    //TODO: abrir a PasswordView, posteriormente
                }
            );
        });
    }
}

