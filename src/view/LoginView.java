package view;

import db.DBManager;
import controller.AuthService;
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.Date;

public class LoginView extends JFrame {
    private final AuthService authService;
    private final DBManager db;          // para gravar log

    private JTextField emailField;
    private JButton    okButton;
    private JButton    clearButton;

    public LoginView(AuthService authService,
                     DBManager db) {
        this.authService     = authService;
        this.db              = db;
        initComponents();

        // **Antes** de exibir a tela, registra o início da Etapa 1 (2001)
        try {
            db.insertRegistro(2001, null, null);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initComponents() {
        setTitle("Cofre Digital - Autenticação (Etapa 1)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.anchor = GridBagConstraints.WEST;

        // Label e campo
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Login name:"), gbc);
        emailField = new JTextField(25);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(emailField, gbc);

        // Botões
        okButton    = new JButton("OK");
        clearButton = new JButton("LIMPAR");
        JPanel buttons = new JPanel();
        buttons.add(okButton);
        buttons.add(clearButton);
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        panel.add(buttons, gbc);

        // Eventos
        okButton.addActionListener(e -> onOk());
        clearButton.addActionListener(e -> emailField.setText(""));

        getContentPane().add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void onOk() {
        String email = emailField.getText().trim();

        // 1) Valida formato
        if (!email.matches("[\\w\\-\\.]+@[\\w\\-]+\\.[\\w\\-\\.]+")) {
            JOptionPane.showMessageDialog(this,
              "Por favor, insira um e-mail válido.",
              "E-mail inválido",
              JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2) Tenta login
    boolean ok;
    try {
        System.out.println("[LoginView] stage antes do submitLogin: " + authService.getStage());
        ok = authService.submitLogin(email);
        System.out.println("[LoginView] submitLogin retornou: " + ok + ", stage agora: " + authService.getStage());
    } catch (RuntimeException ex) {
        // conta bloqueada → MID=2004
        JOptionPane.showMessageDialog(this,
            ex.getMessage(),
            "Acesso bloqueado",
            JOptionPane.WARNING_MESSAGE);
        try { db.insertRegistro(2004, null, null); } catch(SQLException e){e.printStackTrace();}
        return;
    }

    // 3) Se falhou (usuário inválido)
    if (!ok) {
        JOptionPane.showMessageDialog(this,
            "Login não encontrado ou inválido.",
            "Erro de autenticação",
            JOptionPane.ERROR_MESSAGE);
        try { db.insertRegistro(2005, null, null); } catch(SQLException e){e.printStackTrace();}
        return;
    }

    // 4) Chegou aqui: login OK → agora sim currentUser está preenchido
    int uid = authService.getCurrentUser().getUid();
    try {
        db.insertRegistro(2003, uid, null);
        dispose();
        new PasswordView(authService, db);
    } catch (SQLException e) {
        e.printStackTrace();
    }
    }
}
