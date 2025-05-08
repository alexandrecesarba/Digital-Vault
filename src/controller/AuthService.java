package controller;
import model.User;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import main.java.util.Node;

import auth.Auth;
import db.DBManager;


public class AuthService {
    private final DBManager db;         
    private final long lockDurationMs = 2 * 60_000;

    public enum Stage { LOGIN, PASSWORD, TOTP }

    // Estado corrente de cada sessão de login
    private Stage stage = Stage.LOGIN;
    private User  currentUser;
    private int   pwdErrorCount = 0;
    private int   totpErrorCount = 0;
    private long  blockedUntil = 0;

    public AuthService(DBManager db) {
        this.db = db;
    }

    // ETAPA 1: login name
    public boolean submitLogin(String email) {
        if (System.currentTimeMillis() < blockedUntil) {
            throw new RuntimeException("Conta bloqueada até " + new Date(blockedUntil));
        }
        
        User u;
        try {
            u = db.findUserByEmail(email);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        
        if (u == null) {
            // avisa “E-mail inválido”
            return false;
        }
        if (u.isFrozen()) {
            // avisa “Usuário bloqueado”
            return false;
        }
        this.currentUser = u;
        this.stage = Stage.PASSWORD;
        return true;
    }

    // ETAPA 2: senha via teclado virtual
    public boolean submitPassword(Node root) {
        if (stage != Stage.PASSWORD) throw new IllegalStateException();
        boolean ok = Auth.verificaArvoreSenha(root, currentUser.getPasswordHash());
        if (ok) {
          pwdErrorCount = 0;
          stage = Stage.TOTP;
        } else {
          pwdErrorCount++;
          if (pwdErrorCount >= 3) {
            blockedUntil = System.currentTimeMillis() + lockDurationMs;
            stage = Stage.LOGIN;
          }
        }
        return ok;
      }
      

    // ETAPA 3: TOTP
    public boolean submitTOTP(String code) {
        if (stage != Stage.TOTP) throw new IllegalStateException();

        boolean ok;
        try {
            ok = Auth.validateTOTP(currentUser.getTotpSecret(), code);
        } catch (Exception e) {
            ok = false;
        }

        if (ok) {
            // autenticação completa!
            return true;
        } else {
            totpErrorCount++;
            if (totpErrorCount >= 3) {
                blockedUntil = System.currentTimeMillis() + lockDurationMs;
                stage = Stage.LOGIN;
            }
            return false;
        }
    }

    public Stage getStage() {
        return stage;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public int getPwdErrorCount() {
        return this.pwdErrorCount;
      }

      public void incrementaPwdError() {
        pwdErrorCount++;
        if (pwdErrorCount >= 3) {
            blockedUntil = System.currentTimeMillis() + lockDurationMs;
            stage = Stage.LOGIN;
        }
    }

    public int getTOTPErrorCount() {
        return this.totpErrorCount;
    }
    
      
}