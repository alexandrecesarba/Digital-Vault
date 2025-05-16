package view;

import controller.AuthService;
import db.DBManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import model.User;

public class MainView extends  JFrame {
    private final DBManager   db;
    private final AuthService authService;
    private String grupo;
    private int acessos;

    public MainView(AuthService authService, DBManager db) {
        this.authService = authService;
        this.db = db;

        initComponents();

        // log da tela de cadastro (5001)
        try { db.insertRegistro(5001, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
    }

    private void initComponents() {
        
        setTitle("Cofre Digital - Tela Principal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);
        
        // Pega os dados do usuário atual para preencher a tela
        User currentUser = authService.getCurrentUser();
        int uid = currentUser.getUid();
        String email = currentUser.getEmail();
        String nome = currentUser.getNome();
        
        try {
            String grupo = db.getUserGroup(uid);
            this.grupo = grupo;
            int acessos = db.getUserAcessNum(uid);
            this.acessos = acessos;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Cria os componentes da tela
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets    = new Insets(5,5,5,5);
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.weightx   = 0;

        int y = 0;

        // Login do usuário
        gbc.gridy=y;
        mainPanel.add(new JLabel("Login: " + email), gbc);
        y++;

        // Grupo do usuário
        gbc.gridy=y;
        mainPanel.add(new JLabel("Grupo: " + grupo), gbc);
        y++;

        // Nome do usuário
        gbc.gridy=y;
        mainPanel.add(new JLabel("Nome: " + nome), gbc);
        y++;

        // Acessos do usuário
        gbc.gridy=y;
        mainPanel.add(new JLabel("Total de acessos do usuário: " + acessos), gbc);
        y++;

        // Menu Principal
        gbc.gridy=y;
        mainPanel.add(new JLabel("Menu Principal:"), gbc);
        y++;

        // Botões
        JPanel buttons = new JPanel();
        
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS)); // Modificar o layout para BoxLayout vertical

        if ("Administrador".equals(grupo)){
            JButton registerButton    = new JButton("Cadastrar um novo usuário");
            registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            buttons.add(registerButton);
            buttons.add(Box.createRigidArea(new Dimension(0, 10))); // Adiciona 10 pixels de espaço vertical
            // Evento
            registerButton.addActionListener(e -> onRegister());
        }
        JButton consultButton = new JButton("Consultar pasta de arquivos secretos do usuário");
        consultButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(consultButton);
        buttons.add(Box.createRigidArea(new Dimension(0, 10))); // Adiciona 10 pixels de espaço vertical
        JButton exitButton = new JButton("Sair do Sistema");
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(exitButton);
        
        gbc.gridx = 0; gbc.gridy = y;
        gbc.gridheight = 3;
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(buttons, gbc);

        // Eventos
        exitButton.addActionListener(e -> onExit());
        consultButton.addActionListener(e -> onConsult());

        // Montagem final
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(mainPanel), BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

    }

    private void onRegister(){
        // Grava log de cadastro (5002)
        try { db.insertRegistro(5002, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
        dispose();
        // Chama a tela de cadastro
        new UserSignUpView(authService, db, grupo);
    }

    private void onConsult(){
        // Grava log de consulta (5003)
        try { db.insertRegistro(5003, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
        dispose();
        // Chama a tela de consulta
    }

    private void onExit() {
        // Grava log de saída (5004)
        try { db.insertRegistro(5004, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
        dispose();
        System.exit(0);
    }

    
    
}
