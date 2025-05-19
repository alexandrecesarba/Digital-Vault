// Alexandre (2010292) e Enrico (2110927)
import db.DBManager;
import controller.AuthService;
import view.PassphraseView;
import view.SignUpView;

import javax.swing.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import java.sql.SQLException;
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
                try { db.insertRegistro(1001, null, null); } catch(SQLException e){ e.printStackTrace(); }
                // Primeira entrada? Cadastra usuário admin
                if (db.countUsers() == 0) {
                    db.insertRegistro(1005, null, null);
                    new SignUpView(authService, db);
                }

                else{
                    // Fluxo normal de auth.
                    db.insertRegistro(1006, null, null);
                    // new LoginView(authService, db);
                    new PassphraseView(authService, db);
                }
            }
            catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }
            
        });
    }
}
