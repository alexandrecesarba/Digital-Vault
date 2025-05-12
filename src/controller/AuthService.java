package controller;
import model.User;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import main.java.util.Node;
import java.security.PrivateKey;

import auth.Auth;
import db.DBManager;


public class AuthService {
    private final DBManager db;         
    private final long lockDurationMs = 2 * 60_000;

    public enum Stage { LOGIN, PASSWORD, TOTP }

    // Armazena em memória a chave privada do administrador
    private PrivateKey adminPrivateKey;

    // Estado corrente de cada sessão de login
    private Stage stage = Stage.LOGIN;
    private User  currentUser;
    private String currentPassword;
    private int   pwdErrorCount = 0;
    private int   totpErrorCount = 0;
    private long  blockedUntil = 0;

    public AuthService(DBManager db) {
        this.db = db;
    }

    // ETAPA 1: login name
    public boolean submitLogin(String email) {
        if (System.currentTimeMillis() < blockedUntil)
            throw new RuntimeException("Conta bloqueada até " + new Date(blockedUntil));
        try {
            User u = db.findUserByEmail(email);
            if (u==null || u.isFrozen()) return false;
            this.currentUser = u;
            this.stage = Stage.PASSWORD;
            return true;
        } catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /* Etapa 2: árvore de teclado virtual. */
    public boolean submitPassword(Node root) {
        if (stage != Stage.PASSWORD) throw new IllegalStateException();
        boolean ok = Auth.verificaArvoreSenha(root, currentUser.getPasswordHash());
        if (ok) {
            this.currentPassword = Auth.recoverPassword(root, currentUser.getPasswordHash());
            pwdErrorCount = 0;
            stage = Stage.TOTP;
        } else {
            if (++pwdErrorCount >= 3) {
                blockedUntil = System.currentTimeMillis() + 2*60_000;
                this.stage = Stage.LOGIN;
            }
        }
        return ok;
    }

    /** Etapa 3: agora decripta e valida o TOTP */
    public boolean submitTOTP(String code) {
        if (stage != Stage.TOTP) throw new IllegalStateException();
        String secret;
        try {
            // pega o encryptedTotp que veio do banco (veja DBManager)
            byte[] encTotp = currentUser.getEncryptedTotp();
            secret = Auth.decryptTOTPKey(encTotp, currentPassword);
        } catch (Exception e) {
            return false;
        }
        try {
            boolean ok = Auth.validateTOTP(secret, code);
            if (ok) return true;
            // contagem de erros...
            return false;
        } catch (Exception e) {
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

        /** Define a chave privada do administrador (cadastro inicial). */
        public void setAdminPrivateKey(PrivateKey key) {
            this.adminPrivateKey = key;
        }
    
    
      
}