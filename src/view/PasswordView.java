package view;

import controller.AuthService;
import db.DBManager;
import main.java.util.Node;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PasswordView extends JFrame {
    private final AuthService authService;
    private final DBManager   db;

    private JTextField            pwdField;
    private List<JButton>         btns;
    private final List<List<Integer>> optionsList = new ArrayList<>();
    private static final int MIN_LEN = 8;
    private static final int MAX_LEN = 10;

    private Node root = new Node("");    // raiz da árvore de dígitos
    private final List<Integer> clicks = new ArrayList<>();

    public PasswordView(AuthService authService, DBManager db) {
        this.authService = authService;
        this.db          = db;
        initUI();
        logStart();
    }

    private void initUI() {
        setTitle("Cofre Digital - Autenticação (Etapa 2)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        // 1) label + campo bloqueado
        gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=5;
        panel.add(new JLabel("Senha pessoal:"), gbc);
        pwdField = new JTextField(20);
        pwdField.setEditable(false);
        gbc.gridy=1;
        panel.add(pwdField, gbc);

        // 2) gera as 5 opções iniciais
        regenerateOptions();

        // 3) botões das opções
        btns = new ArrayList<>(5);
        for(int i=0; i<5; i++){
            JButton b = new JButton(labelFor(optionsList.get(i)));
            final int idx = i;
            b.addActionListener(e -> onOptionClicked(idx));
            btns.add(b);
            gbc.gridy = 2 + i/3;
            gbc.gridx = i%3;
            gbc.gridwidth = 1;
            panel.add(b, gbc);
        }

        // 4) OK e LIMPAR
        JButton ok    = new JButton("OK");
        JButton clear = new JButton("LIMPAR");
        ok.addActionListener(e -> onOk());
        clear.addActionListener(e -> {
            clicks.clear();
            pwdField.setText("");
        });
        gbc.gridy=4; gbc.gridx=3; gbc.gridwidth=1;
        panel.add(ok,    gbc);
        gbc.gridx=4;
        panel.add(clear, gbc);

        getContentPane().add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void regenerateOptions() {
        optionsList.clear();
        String digits = "0123456789";
        Random rnd = new Random();
        for(int i=0; i<5; i++){
            List<Integer> pair = new ArrayList<>(2);
            for(int j=0; j<2; j++){
                int pos = rnd.nextInt(digits.length());
                pair.add(digits.charAt(pos) - '0');
                digits = digits.substring(0,pos) + digits.substring(pos+1);
            }
            optionsList.add(pair);
        }
    }

    private String labelFor(List<Integer> pair) {
        return pair.get(0) + " " + pair.get(1);
    }

    private void insereNosFolhas(Node node, List<Integer> par) {
        if (node.esq==null && node.dir==null) {
            node.esq = new Node(par.get(0).toString());
            node.dir = new Node(par.get(1).toString());
        } else {
            insereNosFolhas(node.esq, par);
            insereNosFolhas(node.dir, par);
        }
    }

    private void onOptionClicked(int idx) {
        // bloqueia se exceder
        if (clicks.size() >= MAX_LEN * 2) return;

        insereNosFolhas(root, optionsList.get(idx));
        clicks.addAll(optionsList.get(idx));
        pwdField.setText(pwdField.getText() + "●");

        regenerateOptions();
        for (int i = 0; i < btns.size(); i++) {
            btns.get(i).setText(labelFor(optionsList.get(i)));
        }
    }

    private void onOk() {
        int uid = authService.getCurrentUser().getUid();
        int len = clicks.size();
        if (len < MIN_LEN * 2 || len > MAX_LEN * 2) {
            JOptionPane.showMessageDialog(
                this,
                "A senha pessoal deve ter entre 8 e 10 números.",
                "Erro de autenticação",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // *** CONSTRUÇÃO DO TEXTO PURO DA SENHA ***
        StringBuilder sb = new StringBuilder();
        for (int d : clicks) sb.append(d);
        String plainPassword = sb.toString();
        System.out.println("[PasswordView] Senha em texto: " + plainPassword);

        // chama o serviço com os dois argumentos antigos → novo:
        boolean ok = authService.submitPassword(root);
        System.out.println("[PasswordView] submitPassword → " + ok
                           + ", stage agora=" + authService.getStage());

        // log fim do passo 2
        try { db.insertRegistro(3002, uid, null); } catch(SQLException ex){}

        if (ok) {
            try{ db.insertRegistro(3003, uid, null); } catch(SQLException ex){}
            dispose();
            new TOTPView(authService, db);
        } else {
            authService.incrementaPwdError();
            int errors = authService.getPwdErrorCount();
            int mid = switch(errors) {
                case 1 -> 3005;
                case 2 -> 3006;
                case 3 -> 3007;
                default -> 3004;
            };
            try { db.insertRegistro(mid, uid, null); } catch(SQLException ex){}

            if (errors >= 3) {
                JOptionPane.showMessageDialog(
                    this,
                    "Número máximo de tentativas atingido. Aguarde 2 minutos.",
                    "Autenticação falhou",
                    JOptionPane.WARNING_MESSAGE
                );
                dispose();
                new LoginView(authService, db);
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Senha incorreta. Tente novamente.",
                    "Erro de autenticação",
                    JOptionPane.ERROR_MESSAGE
                );
                resetInput();
            }
        }
    }

    private void resetInput() {
        clicks.clear();
        pwdField.setText("");
        regenerateOptions();
        for (int i = 0; i < btns.size(); i++) {
            btns.get(i).setText(labelFor(optionsList.get(i)));
        }
    }

    private void logStart() {
        int uid = authService.getCurrentUser().getUid();
        try { db.insertRegistro(3001, uid, null); } catch (SQLException ex) { ex.printStackTrace(); }
    }
}
