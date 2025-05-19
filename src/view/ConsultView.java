// Alexandre (2010292) e Enrico (2110927)
package view;

import auth.Auth;
import controller.AuthService;
import db.DBManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import model.User;

public class ConsultView extends JFrame {
    private final DBManager   db;
    private final AuthService authService;
    private final String      grupo;
    private int               consultas;
    private JLabel            pathLabel;
    private JButton           pathButton;
    private JPasswordField    passphraseField;
    private String            path;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JScrollPane tableScrollPane;

    public ConsultView(AuthService authService, DBManager db, String grupo) {
        this.authService = authService;
        this.db          = db;
        this.grupo       = grupo;

        initComponents();

        // log da tela de consulta (7001)
        try { db.insertRegistro(7001, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
    }

    private void initComponents(){
        setTitle("Cofre Digital - Consultar Pasta de Arquivos Secretos");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);

        // Pega os dados do usuário atual para preencher a tela
        User currentUser = authService.getCurrentUser();
        int uid = currentUser.getUid();
        String email = currentUser.getEmail();
        String nome = currentUser.getNome();

        try {
            int consultas = db.getUserConsultNum(uid);
            this.consultas = consultas;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Cria os componentes da tela
        JPanel consultPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.weightx   = 0;

        int y = 0;

        // Login do usuário
        gbc.gridy=y;
        consultPanel.add(new JLabel("Login: " + email), gbc);
        y++;

        // Grupo do usuário
        gbc.gridy=y;
        consultPanel.add(new JLabel("Grupo: " + grupo), gbc);
        y++;

        // Nome do usuário
        gbc.gridy=y;
        consultPanel.add(new JLabel("Nome: " + nome), gbc);
        y++;

        // Acessos do usuário
        gbc.gridy=y;
        consultPanel.add(new JLabel("Total de acessos do usuário: " + consultas), gbc);
        y++;

        // Diretório
        
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        consultPanel.add(new JLabel("Caminho da pasta::"), gbc);
        pathLabel = new JLabel();
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=1.0;
        consultPanel.add(pathLabel, gbc);
        pathButton = new JButton("Escolher…");
        pathButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Configura para selecionar apenas diretórios
            fc.setDialogTitle("Selecione a pasta de certificados");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                path = fc.getSelectedFile().getAbsolutePath();
                pathLabel.setText(path);
            }
        });
        gbc.gridx=2; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        consultPanel.add(pathButton, gbc);
        y++;

        // Frase secreta
        gbc.gridx=0; gbc.gridy=y; gbc.gridwidth=1; gbc.weightx=0;
        consultPanel.add(new JLabel("Frase secreta:"), gbc);
        passphraseField = new JPasswordField();
        gbc.gridx=1; gbc.gridy=y; gbc.gridwidth=2; gbc.weightx=1.0;
        consultPanel.add(passphraseField, gbc);
        y++;

        // Botões
        // JPanel buttons = new JPanel();

        // buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS)); // Modificar o layout para BoxLayout vertical
        
        JButton listButton = new JButton("Listar");
        gbc.gridx = 0; gbc.gridy = y;
        gbc.gridheight = 1;
        gbc.gridwidth = 1; //gbc.fill = GridBagConstraints.NONE;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Alterado para preencher horizontalmente
        gbc.anchor = GridBagConstraints.NORTH; // Ancorar no topo
        consultPanel.add(listButton, gbc);
        y++;
        // buttons.add(listButton);

        // buttons.add(Box.createRigidArea(new Dimension(0, 10))); // Adiciona 10 pixels de espaço vertical

        // Criação da tabela (inicialmente vazia)
        String[] colunas = {"Nome Código", "Nome Secreto", "Dono", "Grupo"}; // Definindo os nomes das colunas

        // Criando um modelo de tabela personalizado que não permite edição
        tableModel = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Torna todas as células não editáveis
            }
        };

        dataTable = new JTable(tableModel);
        dataTable.setFocusable(true);
        dataTable.setRowSelectionAllowed(true);
        dataTable.setColumnSelectionAllowed(false);
        dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Permite apenas uma linha selecionada por vez

        // dataTable.setEnabled(false); // Desabilita a tabela para edição
        tableScrollPane = new JScrollPane(dataTable);
        tableScrollPane.setPreferredSize(new Dimension(500, 200)); // Define um tamanho preferencial
        tableScrollPane.setVisible(false); // Inicialmente invisível
    
        // Adicionar listener de seleção à tabela
        dataTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                // Ignorar eventos de "ajuste" da seleção
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = dataTable.getSelectedRow();
                    if (selectedRow != -1) {  // Se alguma linha está selecionada
                        String nomeCod = (String) dataTable.getValueAt(selectedRow, 0);  // Pega o valor da coluna "Nome Código"
                        String nomeSec = (String) dataTable.getValueAt(selectedRow, 1);  // Pega o valor da coluna "Nome Secreto"
                        String dono = (String) dataTable.getValueAt(selectedRow, 2);  // Pega o valor da coluna "Dono"
                        
                        onItemSelected(nomeCod, nomeSec, dono);
                    }
                }
            }
        });

        // Adicionar a tabela ao painel de consulta
        gbc.gridx = 0;
        gbc.gridy = y + 1; // Posicionado abaixo dos botões
        gbc.gridwidth = 2; // Ocupa duas colunas para ser mais largo
        gbc.gridheight = 2; // Altura para a tabela
        gbc.fill = GridBagConstraints.BOTH; // Preencher em ambas direções
        gbc.weightx = 1.0;
        gbc.weighty = 1.0; // Dar peso para crescer verticalmente
        consultPanel.add(tableScrollPane, gbc);
        y+= 10;

        JPanel backPanel = new JPanel();
        JButton backButton = new JButton("Voltar de Consultar para o Menu Principal");
        backPanel.add(backButton);
        gbc.gridy = y;
        consultPanel.add(backPanel, gbc);
        // buttons.add(backButton);
        
        // consultPanel.add(buttons, gbc);

        // Eventos
        listButton.addActionListener(e -> onList());
        backButton.addActionListener(e -> onBack());

        // Montagem final
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(consultPanel), BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void onList(){
        // Grava log de Listar (7003)
        try { db.insertRegistro(7003, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
        System.out.println("lista");

        // Limpa a tabela existente
        tableModel.setRowCount(0);
    
        boolean valid;
        try {
            byte[] pkBytes = db.getChavePrivada(1);
            valid = validateInput(pkBytes);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
            "Ocorreu um erro ao se tentar se comunicar com o banco de dados.",
            "Erro de conexão", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Aqui você adicionaria dados reais da sua aplicação
        // Este é apenas um exemplo:
        if (!valid) {
            // Se a validação falhar, não faz nada
            return;
        }

        // Torna a tabela visível se estava oculta
        if (!tableScrollPane.isVisible()) {
            tableScrollPane.setVisible(true);
            pack(); // Ajusta o tamanho da janela para acomodar a tabela
        }
    }
    
    private void onBack() {
        // Grava log de saída (7002)
        try { db.insertRegistro(7002, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }   
        dispose(); // Fecha a tela atual
        new MainView(authService, db); // Abre a tela de menu principal
    }

    private boolean validateInput(byte[] pkBytes) {
        if (path == null || path.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "O caminho para a pasta segura deve ser dado.",
                "Caminho inválido", JOptionPane.ERROR_MESSAGE);
            try { db.insertRegistro(7004, null, null); } catch(SQLException ex){}
            return false;
        }
        if (passphraseField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this,
                "Sua frase secreta deve ser informada.",
                "Frase secreta não informada", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            // PrivateKey pk = Auth.getPrivateKey(passphraseField.getText(), pkBytes);
            PrivateKey pk = Auth.getPrivateKey(authService.getAdminPassphrase(), pkBytes);
            fillTable(tableModel, pk);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "A frase secreta dada não corresponde.",
                "Frase secreta inválida", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }


    private void fillTable(DefaultTableModel tableModel, PrivateKey pk) {
        byte[] indexBytes = validateIndex(pk);
        
        // Check if indexBytes is null or empty
        if (indexBytes == null || indexBytes.length == 0) {
            return;
        }

        String indexContent = new String(indexBytes);
        
        // Check if content is empty
        if (indexContent.trim().isEmpty()) {
            return;
        }

        String[] lines = indexContent.split("\n");

        for (String line : lines) {
            // Skip empty lines
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split(" ");
            if (parts.length == 4) {
                String codeFileName = parts[0];
                String secretFileName = parts[1]; 
                String fileOwner = parts[2];
                String fileGroup = parts[3];

                if (grupo.equalsIgnoreCase(fileGroup) || 
                    (grupo.equals("Usuário") && fileGroup.equals("usuario"))){
                    tableModel.addRow(new Object[]{
                        codeFileName,
                        secretFileName, 
                        fileOwner,
                        fileGroup
                    });
                }
                try { db.insertRegistro(7009, authService.getCurrentUser().getUid(), null); } catch(SQLException e){ e.printStackTrace(); }
            }
        }
    }


    private byte[] validateIndex(PrivateKey pk) {
        byte[] envBytes, encBytes, asdBytes;

        try {
            // Procura os arquivos index
            envBytes = Files.readAllBytes(Paths.get(path + "/index.env"));
            encBytes = Files.readAllBytes(Paths.get(path + "/index.enc"));
            asdBytes = Files.readAllBytes(Paths.get(path + "/index.asd"));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "O arquivo index não foi encontrado.",
                "Arquivo index não encontrado", JOptionPane.ERROR_MESSAGE);
                try { db.insertRegistro(7004, authService.getCurrentUser().getUid(), null); } catch(SQLException ex){}
            return null;
        }


        try{
            // Decripta o envelope para receber a seed
            byte[] indexSeed = Auth.decryptEnvelope(envBytes, pk);

            byte[] certBytes = db.getChaveiroCertificado(1).getBytes();
            ByteArrayInputStream certStream = new ByteArrayInputStream(certBytes);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(certStream);

            // Texto em bytes
            byte[] indexBytes = Auth.decryptFile(indexSeed, encBytes);
            try { db.insertRegistro(7005, authService.getCurrentUser().getUid(), null); } catch(SQLException ex){}

            cert.getPublicKey(); // Public key do chaveiro
            if (!Auth.verifySignature(indexBytes, asdBytes, cert)){
                // Se a assinatura não for válida, retorna null
                try { db.insertRegistro(7008, authService.getCurrentUser().getUid(), null); } catch(SQLException ex){}
                return null;
            }
            try { db.insertRegistro(7006, authService.getCurrentUser().getUid(), null); } catch(SQLException ex){}
            return indexBytes;
        } catch (Exception e){
            try { db.insertRegistro(7007, authService.getCurrentUser().getUid(), null); } catch(SQLException ex){}
            e.printStackTrace();
        }
        return null;
    }

    private void onItemSelected(String nomeCod, String nomeSec, String dono){
        System.out.println("Nome Código: " + nomeCod);

        try { db.insertRegistro(7010, authService.getCurrentUser().getUid(), nomeCod); } catch(SQLException ex){}

        if (authService.getCurrentUser().getEmail().equalsIgnoreCase(dono)){ // Acesso permitido para o arquivo
            try { db.insertRegistro(7011, authService.getCurrentUser().getUid(), nomeCod); } catch(SQLException ex){}
            // Decriptar o arquivo
            byte[] fileBytes = decryptItem(nomeCod);

            // Check if indexBytes is null or empty
            if (fileBytes == null || fileBytes.length == 0) {
                return;
            }

            try (FileOutputStream stream = new FileOutputStream(nomeSec)) {
                stream.write(fileBytes);
            } catch (IOException e) {
                
            }

        } else {
            try { db.insertRegistro(7012, authService.getCurrentUser().getUid(), nomeCod); } catch(SQLException ex){}
            JOptionPane.showMessageDialog(this,
                "Você não tem permissão para acessar esse arquivo.",
                "Sem permissão", JOptionPane.ERROR_MESSAGE);
        }
        
    }

    private byte[] decryptItem(String nomeCod){
        // Procura os arquivos index
        byte[] envBytes, encBytes, asdBytes;
        try{
            envBytes = Files.readAllBytes(Paths.get(path + "/" + nomeCod + ".env"));
            encBytes = Files.readAllBytes(Paths.get(path + "/" + nomeCod + ".enc"));
            asdBytes = Files.readAllBytes(Paths.get(path + "/" + nomeCod + ".asd"));
        } catch (Exception e){
            e.printStackTrace();
            try { db.insertRegistro(7015, authService.getCurrentUser().getUid(), nomeCod); } catch(SQLException ex){}
            return null;
        }
        
        try{
            byte[] pkBytes = db.getChavePrivada(authService.getCurrentUser().getUid());
            PrivateKey pk = Auth.getPrivateKey(passphraseField.getText(), pkBytes);

            // Decripta o envelope para receber a seed
            byte[] fileSeed = Auth.decryptEnvelope(envBytes, pk);

            byte[] certBytes = db.getChaveiroCertificado(db.getKidFromUid(authService.getCurrentUser().getUid())).getBytes();
            ByteArrayInputStream certStream = new ByteArrayInputStream(certBytes);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(certStream);

            // Texto em bytes
            byte[] fileBytes = Auth.decryptFile(fileSeed, encBytes);
            try { db.insertRegistro(7013, authService.getCurrentUser().getUid(), null); } catch(SQLException ex){}

            cert.getPublicKey(); // Public key do chaveiro
            if (!Auth.verifySignature(fileBytes, asdBytes, cert)){
                // Se a assinatura não for válida, retorna null
                try { db.insertRegistro(7016, authService.getCurrentUser().getUid(), null); } catch(SQLException ex){}
                return null;
            }
            try { db.insertRegistro(7014, authService.getCurrentUser().getUid(), null); } catch(SQLException ex){}
            return fileBytes;

        } catch (Exception e){
            try { db.insertRegistro(7015, authService.getCurrentUser().getUid(), null); } catch(SQLException ex){}
            e.printStackTrace();
            return null;
        }
    
        
    

    }

}
