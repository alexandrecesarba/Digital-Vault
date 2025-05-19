// Alexandre (2010292) e Enrico (2110927)
package view;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import auth.Auth;
import controller.AuthService;
import db.DBManager;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.nio.file.Paths;

public class PassphraseView extends JFrame {
    private final AuthService authService;
    private final DBManager db;
    private JTextField passphraseField;
    private JButton    okButton;
    private JButton    clearButton;

    public PassphraseView(AuthService authService, DBManager db) {
        this.authService = authService;
        this.db = db;

        initComponents();
    }

    private void initComponents(){
        setTitle("Cofre Digital - Frase secreta");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.anchor = GridBagConstraints.WEST;

        // Label e campo
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Frase secreta do administrador:"), gbc);
        passphraseField = new JPasswordField(25);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(passphraseField, gbc);

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
        clearButton.addActionListener(e -> passphraseField.setText(""));

        getContentPane().add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void onOk() {
        String passphrase = passphraseField.getText();
        if (passphrase.isEmpty()) {
            // Exibir mensagem de erro
            JOptionPane.showMessageDialog(this,
              "Por favor, insira a frase secreta do administrador do cofre.",
              "Frase secreta não existente",
              JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            byte[] PkBytes = db.getChavePrivada(1);
            Auth.getPrivateKey(passphrase, PkBytes);
            authService.setAdminPassphrase(passphrase);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
              "Frase secreta incorreta.",
              "Erro",
              JOptionPane.ERROR_MESSAGE);
            try{db.insertRegistro(1002, null, null);}
            catch(Exception ex){ex.printStackTrace();}
            dispose();
            System.exit(1);
        }

        dispose();
        new LoginView(authService, db);

        /*
         * passphrase = passphraseField.getText();
         * bytesPk
         * Fazer a verificação da frase secreta ao tentar criar a pk com os bytes da pk
         */
        
    }
    
}
