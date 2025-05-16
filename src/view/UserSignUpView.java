package view;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Dimension;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import auth.Auth;
import controller.AuthService;
import db.DBManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;

import model.User;
import totp.Base32;

public class UserSignUpView extends JFrame {
    private final AuthService authService;
    private final DBManager   db;
    private final String grupo;
    private int userCount;

    // Componentes do formulário de cadastro
    private JLabel            certLabel;
    private JButton           certButton;
    private JLabel            keyLabel;
    private JButton           keyButton;
    private JPasswordField    passphraseField;
    private JComboBox<String> groupCombo;
    private JPasswordField    pwdField;
    private JPasswordField    pwdConfirmField;

    public UserSignUpView(AuthService authService, DBManager db, String grupo) {
        this.authService = authService;
        this.db          = db;
        this.grupo       = grupo;

        initComponents();
        
        // log da tela de cadastro (6001)
        try { db.insertRegistro(6001, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
    }

    private void initComponents(){
        setTitle("Cofre Digital - Tela de Cadastro");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);
        
        // Pega os dados do usuário atual para preencher a tela
        User currentUser = authService.getCurrentUser();
        String email = currentUser.getEmail();
        String nome = currentUser.getNome();
        try {
            int count = db.countUsers();
            this.userCount = count;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Cria os componentes da tela
        JPanel mainSignPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets    = new Insets(5,5,5,5);
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.weightx   = 0;

        int y = 0;

        // Login do usuário
        gbc.gridy=y;
        mainSignPanel.add(new JLabel("Login: " + email), gbc);
        y++;

        // Grupo do usuário
        gbc.gridy=y;
        mainSignPanel.add(new JLabel("Grupo: " + grupo), gbc);
        y++;

        // Nome do usuário
        gbc.gridy=y;
        mainSignPanel.add(new JLabel("Nome: " + nome), gbc);
        y++;

        // Número de usuários
        gbc.gridy=y;
        mainSignPanel.add(new JLabel("Total de usuários do sistema: " + userCount), gbc);
        y++;

        // Formulário de cadastro
        gbc.gridy=y;
        mainSignPanel.add(new JLabel("Formulário de cadastro:"), gbc);
        y++;

        y = createSignUpForm(mainSignPanel, gbc, y); // Cria o formulário de cadastro

        // Botões
        JPanel buttons = new JPanel();

        JButton registerButton    = new JButton("Cadastrar");
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(registerButton);
        // buttons.add(Box.createRigidArea(new Dimension(0, 10))); // Adiciona 10 pixels de espaço vertical
        JButton backButton = new JButton("Voltar de Cadastrar para o Menu Principal");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(backButton);
        
        gbc.gridx = 0; gbc.gridy = y;
        gbc.gridheight = 3;
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        mainSignPanel.add(buttons, gbc);

        // Eventos
        registerButton.addActionListener(e -> onRegister());
        backButton.addActionListener(e -> onBack());

        // Montagem final
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(mainSignPanel), BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private int createSignUpForm(JPanel mainSignPanel, GridBagConstraints gbc, int y) {
        // Certificado (.crt)
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        mainSignPanel.add(new JLabel("Certificado (.crt):"), gbc);
        certLabel = new JLabel();
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=1.0;
        mainSignPanel.add(certLabel, gbc);
        certButton = new JButton("Escolher…");
        certButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Certificado X.509", "crt","pem"));
            if (fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
                certLabel.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        gbc.gridx=2; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        mainSignPanel.add(certButton, gbc);
        y++;

        // Chave privada (.key)
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1;
        mainSignPanel.add(new JLabel("Chave privada (.key):"), gbc);
        keyLabel = new JLabel();
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=1.0;
        mainSignPanel.add(keyLabel, gbc);
        keyButton = new JButton("Escolher…");
        keyButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Chave PKCS#8 criptografada", "key"));
            if (fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
                keyLabel.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        gbc.gridx=2; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        mainSignPanel.add(keyButton, gbc);
        y++;

        // Frase secreta
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        mainSignPanel.add(new JLabel("Frase secreta (p/ chave):"), gbc);
        passphraseField = new JPasswordField();
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=2; gbc.weightx=1.0;
        mainSignPanel.add(passphraseField, gbc);
        y++;

        // Grupo
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        mainSignPanel.add(new JLabel("Grupo:"), gbc);
        groupCombo = new JComboBox<>(new String[]{"Administrador", "Usuário"});
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=2; gbc.weightx=1.0;
        mainSignPanel.add(groupCombo, gbc);
        y++;

        // Senha pessoal
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        mainSignPanel.add(new JLabel("Senha pessoal (8–10 dígitos):"), gbc);
        pwdField = new JPasswordField();
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=2; gbc.weightx=1.0;
        mainSignPanel.add(pwdField, gbc);
        y++;

        // Confirmação de senha
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        mainSignPanel.add(new JLabel("Confirme a senha:"), gbc);
        pwdConfirmField = new JPasswordField();
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=2; gbc.weightx=1.0;
        mainSignPanel.add(pwdConfirmField, gbc);
        y++;
     
        return y;
    }

    private void onRegister() {
        // Grava log de saída (6002)
        try { db.insertRegistro(6002, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
        // Lógica para cadastrar um novo usuário
        onCadastrar();
    }

    private void onBack() {
        // Grava log de saída (6010)
        try { db.insertRegistro(6010, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }   
        dispose(); // Fecha a tela atual
        new MainView(authService, db); // Abre a tela de menu principal
    }

    private void onCadastrar() {
        String crtPath = certLabel.getText();
        String keyPath = keyLabel.getText();
        String frase   = new String(passphraseField.getPassword());
        String senha   = new String(pwdField.getPassword());
        String confirm = new String(pwdConfirmField.getPassword());

        // 1) validações básicas
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
            var priv = Auth.loadPrivateKey(frase, Paths.get(keyPath));
            if (!Auth.testPrivateKey(priv, cert)) {
                JOptionPane.showMessageDialog(this,
                    "Chave privada ou frase inválida.",
                    "Erro no .key", JOptionPane.ERROR_MESSAGE);
                db.insertRegistro(6007, null, null);
                return;
            }
            authService.setAdminPrivateKey(priv);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Erro ao carregar/verificar chave: " + ex.getMessage(),
                "Erro no .key", JOptionPane.ERROR_MESSAGE);
            try { db.insertRegistro(6006, null, null); } catch(SQLException ex2){}
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
        try {db.insertRegistro(6008, authService.getCurrentUser().getUid(), null); } catch(SQLException ex2){}

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
            byte[] encKey = Auth.encryptPrivateKey(rawKey);
            int kid = db.addChaveiro(uid, pem, encKey);
            
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
                // Recria a tela de cadastro vazia
                new UserSignUpView(authService, db, grupo);
                
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                "Erro ao gravar dados: " + ex.getMessage(),
                "Falha", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        private void onCancel(JFrame confirmFrame) {
            // Grava log de rejeição (6009)
            try { db.insertRegistro(6009, authService.getCurrentUser().getUid(), null); } catch(SQLException ex2){}
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
    