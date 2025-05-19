// Alexandre (2010292) e Enrico (2110927)

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

public class ExitView extends JFrame {
    private final DBManager   db;
    private final AuthService authService;
    private String grupo;
    private int acessos;

    public ExitView(AuthService authService, DBManager db) {
        this.authService = authService;
        this.db = db;

        initComponents();

        // log da tela de cadastro (8001)
        try { db.insertRegistro(8001, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
    }
    
    private void initComponents(){
        setTitle("Cofre Digital - Tela de Saída");
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
        JPanel exitPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets    = new Insets(5,5,5,5);
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.weightx   = 0;

        int y = 0;

        // Login do usuário
        gbc.gridy=y;
        exitPanel.add(new JLabel("Login: " + email), gbc);
        y++;

        // Grupo do usuário
        gbc.gridy=y;
        exitPanel.add(new JLabel("Grupo: " + grupo), gbc);
        y++;

        // Nome do usuário
        gbc.gridy=y;
        exitPanel.add(new JLabel("Nome: " + nome), gbc);
        y++;

        // Acessos do usuário
        gbc.gridy=y;
        exitPanel.add(new JLabel("Total de acessos do usuário: " + acessos), gbc);
        y++;

        // Menu Principal
        gbc.gridy=y;
        exitPanel.add(new JLabel("Saída do sistema:"), gbc);
        y++;

        gbc.gridy=y;
        exitPanel.add(new JLabel("Pressione o botão Encerrar Sessão ou o botão Encerrar Sistema para confirmar."), gbc);
        y++;

        // Botões
        JPanel buttons = new JPanel();

        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS)); // Modificar o layout para BoxLayout vertical

        JButton closeSessionButton = new JButton("Encerrar Sessão");
        closeSessionButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(closeSessionButton);
        buttons.add(Box.createRigidArea(new Dimension(0, 10))); // Adiciona 10 pixels de espaço vertical

        JButton closeSystemButton = new JButton("Encerrar Sistema");
        closeSystemButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(closeSystemButton);
        buttons.add(Box.createRigidArea(new Dimension(0, 10))); // Adiciona 10 pixels de espaço vertical
        
        JButton backButton = new JButton("Botão Voltar de Sair para o Menu Principal");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(backButton);

        gbc.gridx = 0; gbc.gridy = y;
        gbc.gridheight = 3;
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        exitPanel.add(buttons, gbc);

        // Eventos
        closeSessionButton.addActionListener(e -> onCloseSession());
        closeSystemButton.addActionListener(e -> onCloseSystem());
        backButton.addActionListener(e -> onBack());

        // Montagem final
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(exitPanel), BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void onCloseSession() {
        try { db.insertRegistro(8002, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
        try { db.insertRegistro(1004, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }

        dispose();
        // Volta para a tela de login    
        new LoginView(authService, db);
    }
    private void onCloseSystem() {
        try { db.insertRegistro(8003, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
        try { db.insertRegistro(1002, null, null); } catch(SQLException e){ e.printStackTrace(); }

        dispose();
        // Encerra o sistema
        System.exit(0);
    }

    private void onBack() {
        try { db.insertRegistro(8004, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
        dispose();
        // Volta para a tela principal
        new MainView(authService, db);
    }

}
