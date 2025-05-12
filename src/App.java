import db.DBManager;
import controller.AuthService;
import view.LoginView;
import view.PasswordView;
import view.SignUpView;
import view.TOTPView;

import javax.swing.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
public class App {
    static {
        Security.addProvider(new BouncyCastleProvider());
      }
    public static void main(String[] args) {
        DBManager     db          = new DBManager();
        AuthService   authService = new AuthService(db);

        SwingUtilities.invokeLater(() -> {
            try{
                db.initIfNeeded();
                // Primeira entrada? Cadastra usuário admin
                if (db.countUsers() == 0) {
                    db.insertRegistro(1005, null, null);
                    new SignUpView(authService, db);
                }

                else{
                    // Fluxo normal de auth.
                    db.insertRegistro(1006, null, null);
                    new LoginView(authService, db);
                }
            }
            catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }
            
        });
    }
}
