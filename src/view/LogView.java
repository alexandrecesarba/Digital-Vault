package view;

import db.DBManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Programa de apoio para visualização cronológica dos registros de evento.
 */
public class LogView extends JFrame {
    public LogView(DBManager db) {
        super("Log de Registros");
        // colunas: RID, datahora, texto, email, arquivo
        String[] cols = { "RID", "Data/Hora", "Mensagem", "Usuário", "Arquivo" };
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        
        try {
            List<Map<String,Object>> logs = db.getLog();
            for (Map<String,Object> row : logs) {
                Object[] line = {
                    row.get("rid"),
                    row.get("datahora"),
                    row.get("texto"),
                    row.get("email"),
                    row.get("arquivo")
                };
                model.addRow(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Falha ao carregar log: " + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }

        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        setSize(800, 400);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        DBManager db = new DBManager();
        db.initIfNeeded();
        new LogView(db);
    }
}
