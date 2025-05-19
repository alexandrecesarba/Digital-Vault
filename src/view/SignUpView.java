// Alexandre (2010292) e Enrico (2110927)

package view;

import auth.Auth;
import controller.AuthService;
import db.DBManager;
import model.User;
import totp.Base32;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class SignUpView extends JFrame {
    private final AuthService authService;
    private final DBManager   db;

    private JLabel            certLabel;
    private JButton           certButton;
    private JLabel            keyLabel;
    private JButton           keyButton;
    private JPasswordField    passphraseField;
    private JComboBox<String> groupCombo;
    private JPasswordField    pwdField;
    private JPasswordField    pwdConfirmField;

    public SignUpView(AuthService authService, DBManager db) {
        this.authService = authService;
        this.db          = db;
        initComponents();
    }

    private void initComponents() {
        setTitle("Cofre Digital – Cadastro Inicial do Administrador");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets    = new Insets(5,5,5,5);
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.weightx   = 0;

        int y = 0;

        // Certificado (.crt)
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        formPanel.add(new JLabel("Certificado (.crt):"), gbc);
        certLabel = new JLabel();
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=1.0;
        formPanel.add(certLabel, gbc);
        certButton = new JButton("Escolher…");
        certButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Certificado X.509", "crt","pem"));
            if (fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
                certLabel.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        gbc.gridx=2; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        formPanel.add(certButton, gbc);
        y++;

        // Chave privada (.key)
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1;
        formPanel.add(new JLabel("Chave privada (.key):"), gbc);
        keyLabel = new JLabel();
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=1.0;
        formPanel.add(keyLabel, gbc);
        keyButton = new JButton("Escolher…");
        keyButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Chave PKCS#8 criptografada", "key"));
            if (fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
                keyLabel.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        gbc.gridx=2; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        formPanel.add(keyButton, gbc);
        y++;

        // Frase secreta
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        formPanel.add(new JLabel("Frase secreta (p/ chave):"), gbc);
        passphraseField = new JPasswordField();
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=2; gbc.weightx=1.0;
        formPanel.add(passphraseField, gbc);
        y++;

        // Grupo
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        formPanel.add(new JLabel("Grupo:"), gbc);
        groupCombo = new JComboBox<>(new String[]{"Administrador"});
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=2; gbc.weightx=1.0;
        formPanel.add(groupCombo, gbc);
        y++;

        // Senha pessoal
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        formPanel.add(new JLabel("Senha pessoal (8–10 dígitos):"), gbc);
        pwdField = new JPasswordField();
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=2; gbc.weightx=1.0;
        formPanel.add(pwdField, gbc);
        y++;

        // Confirmação de senha
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        formPanel.add(new JLabel("Confirme a senha:"), gbc);
        pwdConfirmField = new JPasswordField();
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=2; gbc.weightx=1.0;
        formPanel.add(pwdConfirmField, gbc);
        y++;

        // Botões
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnCadastrar = new JButton("Cadastrar");
        btnCadastrar.addActionListener(e -> onCadastrar());
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> {
            dispose();
            System.exit(0);
        });
        buttonPanel.add(btnCadastrar);
        buttonPanel.add(btnCancelar);

        // Montagem final
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(formPanel), BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void onCadastrar() {
        String crtPath = certLabel.getText();
        String keyPath = keyLabel.getText();
        String frase   = new String(passphraseField.getPassword());
        String senha   = new String(pwdField.getPassword());
        String confirm = new String(pwdConfirmField.getPassword());
        String grupo   = (String)groupCombo.getSelectedItem();

        // 1) validações básicas
        if (crtPath.isEmpty() || keyPath.isEmpty() || frase.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Certificado, chave privada e frase secreta são obrigatórios.",
                "Dados incompletos", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!senha.matches("\\d{8,10}") || !senha.equals(confirm)
         || senha.chars().distinct().count()==1) {
            JOptionPane.showMessageDialog(this,
                "Senha deve ter 8–10 dígitos, sem todos iguais, e confirmar corretamente.",
                "Senha inválida", JOptionPane.ERROR_MESSAGE);
            try { db.insertRegistro(6003, null, null); } catch(SQLException ex){}
            return;
        }

        // 2) lê e valida certificado
        X509Certificate cert;
        try {
            byte[] certBytes = Files.readAllBytes(Paths.get(crtPath));
            cert = Auth.readCertificate(certBytes);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Certificado inválido: " + ex.getMessage(),
                "Erro no .crt", JOptionPane.ERROR_MESSAGE);
            try { db.insertRegistro(6004, null, null); } catch(SQLException ex2){}
            return;
        }

        // 3) carrega e testa chave privada
        try {
            if (!keyPath.endsWith(".key")) {
                JOptionPane.showMessageDialog(this,
                    "Caminho inválido para a .key.",
                    "Erro no .key", JOptionPane.ERROR_MESSAGE);
                try { db.insertRegistro(6005, null, null); } catch(SQLException ex2){}
                return;
}
            var priv = Auth.loadPrivateKey(frase, Paths.get(keyPath));
            if (!Auth.testPrivateKey(priv, cert)) {
                JOptionPane.showMessageDialog(this,
                    "Chave privada ou frase inválida.",
                    "Erro no .key", JOptionPane.ERROR_MESSAGE);
                db.insertRegistro(6006, null, null);    // registro errado?
                return;
            }
            authService.setAdminPassphrase(frase);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Erro ao carregar/verificar chave: " + ex.getMessage(),
                "Erro no .key", JOptionPane.ERROR_MESSAGE);
            try { db.insertRegistro(6007, null, null); } catch(SQLException ex2){}  // registro errado?
            return;
        }

       // Mostra tela de confirmação de dados:
       confirmData(cert);
    }

    private void confirmData(X509Certificate cert){
        JFrame confirmFrame = new JFrame();
        confirmFrame.setTitle("Cofre Digital - Confirme os dados do certificado");
        confirmFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        confirmFrame.setResizable(false);
        
        // Cria os componentes da tela
        JPanel confirmPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets    = new Insets(5,5,5,5);
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.weightx   = 0;
        
        int y = 0;
        
        // Versão do certificado
        gbc.gridy=y;
        confirmPanel.add(new JLabel("Versão: " + cert.getVersion()), gbc);
        y++;
        
        // Série do certificado
        gbc.gridy=y;
        confirmPanel.add(new JLabel("Série: " + cert.getSerialNumber()), gbc);
        y++;
        
        // Validade do certificado
        gbc.gridy=y;
        confirmPanel.add(new JLabel("Validade de: " + cert.getNotBefore() + " até " + cert.getNotAfter()), gbc);
        y++;
        
        // Tipo de assinatura do certificado
        gbc.gridy=y;
        confirmPanel.add(new JLabel("Tipo de Assinatura: " + cert.getSigAlgName()), gbc);
        y++;
        
        // Emissor do certificado
        gbc.gridy=y;
        confirmPanel.add(new JLabel("Emissor: " + cert.getIssuerX500Principal().toString()), gbc);
        y++;
        
        // Lógica para extrair o e-mail e o nome do sujeito
        String dnString = cert.getSubjectX500Principal().toString();
        
        // Padrões para extrair o email e o nome
        Pattern emailPattern = Pattern.compile("EMAILADDRESS=([^,]+)");
        Pattern namePattern = Pattern.compile("CN=([^,]+)");
        
        // Matchers para encontrar os padrões na string
        Matcher emailMatcher = emailPattern.matcher(dnString);
        Matcher nameMatcher = namePattern.matcher(dnString);
        
        // Extrair os valores
        String emailValue = emailMatcher.find() ? emailMatcher.group(1) : "";
        String nameValue = nameMatcher.find() ? nameMatcher.group(1) : "";
        
        // Sujeito
        gbc.gridy=y;
        confirmPanel.add(new JLabel("Sujeito: " + nameValue), gbc);
        y++;
        
        // E-mail
        gbc.gridy=y;
        confirmPanel.add(new JLabel("E-mail: " + emailValue), gbc);
        y++;
        
        // Botões
        JPanel buttons = new JPanel();
        
        JButton confirmButton    = new JButton("Confirmar dados");
        confirmButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(confirmButton);
        
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(cancelButton);
        
        gbc.gridx = 0; gbc.gridy = y;
        gbc.gridheight = 3;
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        confirmPanel.add(buttons, gbc);
        
        // Eventos
        confirmButton.addActionListener(e -> onConfirm(nameValue, emailValue, confirmFrame));
        cancelButton.addActionListener(e -> onCancel(confirmFrame));
        
        confirmFrame.add(confirmPanel);
        confirmFrame.pack();
        confirmFrame.setLocationRelativeTo(null);
        confirmFrame.setVisible(true);
    }
    
    private void onConfirm(String nome, String email, JFrame confirmFrame) {
        // Grava log de confirmação (6008)
        try {db.insertRegistro(6008, 1, null); } catch(SQLException ex2){} // uid é 1 já que é o admin

        confirmFrame.dispose();

        String crtPath = certLabel.getText();
        String keyPath = keyLabel.getText();
        String senha   = new String(pwdField.getPassword());
        String grupoNovo   = (String)groupCombo.getSelectedItem();

        // 4) gera e criptografa TOTP
        String base32;
        byte[] encTotp;
        try {
            byte[] totpRaw = new byte[20];
            SecureRandom.getInstanceStrong().nextBytes(totpRaw);
            base32 = new Base32(Base32.Alphabet.BASE32, false, false)
                        .toString(totpRaw);

            SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
            prng.setSeed(senha.getBytes(StandardCharsets.UTF_8));
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256, prng);
            SecretKey aesKey = kg.generateKey();

            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, aesKey);
            encTotp = c.doFinal(base32.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Erro ao gerar TOTP: " + ex.getMessage(),
                "Falha", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 5) persiste no banco
        try {
            int gid = db.getOrCreateGroup(grupoNovo);
            String senhaHash = Auth.hashPassword(senha);
            boolean ok = db.addUsuario(nome, email, gid, senhaHash, encTotp, 0);
            if (!ok) throw new SQLException("Falha ao inserir usuário");
            
            User u = db.findUserByEmail(email);
            int uid = u.getUid();
            String pem = Files.readAllLines(Paths.get(crtPath))
            .stream().collect(Collectors.joining("\n"))
            .replace("'", "''");
            byte[] rawKey = Files.readAllBytes(Paths.get(keyPath));
            // byte[] encKey = Auth.encryptPrivateKey(rawKey); Desnecessário a pk já está vindo encriptada
            int kid = db.addChaveiro(uid, pem, rawKey);
            
            try (Connection conn = DBManager.connect();
            PreparedStatement p = conn.prepareStatement(
                "UPDATE Usuarios SET kid = ? WHERE uid = ?")) {
                    p.setInt(1, kid);
                    p.setInt(2, uid);
                    p.executeUpdate();
                }
                
                // 6) mostra QR Code
                showTOTPSetupQR(nome, email, base32);
                
                dispose();
                // Recria a tela principal
                new LoginView(authService, db);
                
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                "Erro ao gravar dados: " + ex.getMessage(),
                "Falha", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        private void onCancel(JFrame confirmFrame) {
            // Grava log de rejeição (6009)
            try { db.insertRegistro(6009, 1, null); } catch(SQLException ex2){} // uid é 1 já que é o admin
            confirmFrame.dispose();
            JOptionPane.showMessageDialog(this,
            "Cadastro cancelado: Dados não conferem.",
            "Aviso", JOptionPane.ERROR_MESSAGE);
        }
        
        private void showTOTPSetupQR(String nome, String email, String secret) {
            String issuer = "CofreDigital";
            String label  = issuer + ":" + email;
            String uri    = String.format(
                "otpauth://totp/%s?secret=%s&issuer=%s",
                label, secret, issuer
            );
    
            try {
                int size = 250;
                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix matrix = writer.encode(uri, BarcodeFormat.QR_CODE, size, size);
                BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                for (int x=0; x<size; x++) {
                    for (int y=0; y<size; y++) {
                        img.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                    }
                }
                JLabel pic = new JLabel(new ImageIcon(img));
                JOptionPane.showMessageDialog(
                    this,
                    new Object[]{
                        "<html>Cadastro realizado com sucesso!<br/>",
                        "Escaneie este QR Code no Google Authenticator:</html>",
                        pic
                    },
                    "Configure seu Authenticator",
                    JOptionPane.PLAIN_MESSAGE
                );
            } catch (WriterException e) {
                JOptionPane.showMessageDialog(
                    this,
                    "Não foi possível gerar o QR Code: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE
                );
            }
        }
}
