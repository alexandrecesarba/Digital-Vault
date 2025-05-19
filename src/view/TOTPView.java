// Alexandre (2010292) e Enrico (2110927)

package view;

import controller.AuthService;
import db.DBManager;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;

public class TOTPView extends JFrame {
    private final AuthService authService;
    private final DBManager   db;


    private JTextField totpField;

    public TOTPView(AuthService authService, DBManager db) {
        this.authService = authService;
        this.db          = db;
        initComponents();
        logStart();
    }

    private void initComponents() {
        setTitle("Cofre Digital - Autenticação (Etapa 3)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Label e campo de entrada
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Código TOTP:"), gbc);

        totpField = new JTextField(6);
        gbc.gridx = 1;
        panel.add(totpField, gbc);

        // Botões OK / LIMPAR
        JButton ok    = new JButton("OK");
        JButton clear = new JButton("LIMPAR");
        clear.addActionListener(e -> totpField.setText(""));
        ok.addActionListener(e -> onOk());

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(ok, gbc);
        gbc.gridx = 1;
        panel.add(clear, gbc);

        getContentPane().add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void logStart() {
        int uid = authService.getCurrentUser().getUid();
        try {
            db.insertRegistro(4001, uid, null);  // “Autenticação etapa 3 iniciada…”
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void onOk() {
        String code = totpField.getText().trim();
        int uid = authService.getCurrentUser().getUid();

        // 1) valida formato (6 dígitos)
        if (!code.matches("\\d{6}")) {
            JOptionPane.showMessageDialog(
                this,
                "Por favor, insira um token de 6 dígitos.",
                "Token inválido",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try {
            System.out.println("[TOTPView] stage antes do submitTOTP: " + authService.getStage());
            boolean ok = authService.submitTOTP(code);
            System.out.println("[TOTPView] submitTOTP retornou: " + ok + ", stage agora: " + authService.getStage());
            // 2) log fim da etapa 3
            db.insertRegistro(4002, uid, null);

            if (ok) {
                // 3a) sucesso
                db.insertRegistro(4003, uid, null);
                dispose();
                JOptionPane.showMessageDialog(
                    null, "Autenticação completa!", 
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                // inicia tela principal do Cofre
                try { db.insertRegistro(1003, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
                new MainView(authService, db);
            } else {
                // 3b) erro: contabiliza e verifica bloqueio
                int errs = authService.getTOTPErrorCount();
                int mid;
                switch (errs) {
                    case 1: mid = 4004; break;  // primeiro erro
                    case 2: mid = 4005; break;  // segundo erro
                    case 3: mid = 4006; break;  // terceiro erro
                    default: mid = 4007;        // acesso bloqueado
                }
                db.insertRegistro(mid, uid, null);

                if (errs >= 3) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Número máximo de tentativas atingido. Aguarde 2 minutos.",
                        "Autenticação falhou",
                        JOptionPane.WARNING_MESSAGE
                    );
                    // volta ao login
                    dispose();
                    new LoginView(authService, db);
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Token incorreto. Tente novamente.",
                        "Erro de autenticação",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        } catch (IllegalStateException ex) {
            // se o AuthService mudou de etapa sem você querer…
            JOptionPane.showMessageDialog(
                this,
                "Sessão expirada. Retornando ao login.",
                "Erro de sessão",
                JOptionPane.ERROR_MESSAGE
            );
            dispose();
            new LoginView(authService, db);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
