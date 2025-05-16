package db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import model.User;
import auth.Auth;
import java.sql.Types;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.Arrays;
import javax.print.attribute.standard.MediaSize;

public class DBManager {
    private static final String DB_URL = "jdbc:sqlite:cofre.db";

    public static Connection connect() {
        try {
            return DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }



    private static boolean insertIntoDB(String query){
        Connection conn = connect();
        try {
            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(30);  // set timeout to 30 sec.
            stmt.executeUpdate(query);
            stmt.close();
        } 
        catch (SQLException e) {
            System.err.println(e.getMessage());
            endConnection(conn);
            return false;
        }
        endConnection(conn);
        return true;
    }

    private static void executeUpdateToDB(String query){
        Connection conn = connect();
        try {
            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(30);  // set timeout to 30 sec.
            stmt.executeUpdate(query);
            stmt.close();
        }
        catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        endConnection(conn);
    }

    private static boolean endConnection(Connection conn){
        try {
            if (conn != null)
                conn.close();
        }
        catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
        return true;
    }

    private static List<HashMap<String, Object>> selectFromDB(String query){
        Connection conn = connect();
        try {
            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(30);  
            ResultSet rs = stmt.executeQuery(query);
            List<HashMap<String,Object>> list = convertResultSetToList(rs);
            stmt.close();
            endConnection(conn);
            return list; 
        }
        catch (SQLException e) {
            System.err.println(e.getMessage());
            endConnection(conn);
            return null;
        }
    }

    private static List<HashMap<String,Object>> convertResultSetToList(ResultSet rs) throws SQLException{
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<HashMap<String,Object>> list = new ArrayList<HashMap<String,Object>>();
        while(rs.next()){
            HashMap<String,Object> row = new HashMap<String,Object>();
            for(int i=1;i<=columnCount;i++){
                row.put(metaData.getColumnName(i),rs.getObject(i));
            }
            list.add(row);
        }
        return list;
    }

        // ──────────────────────────────────────────────────────────────────────────
    // Métodos públicos
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * Retorna o numero de usuarios cadastrados no sistema.
     */
    public int countUsers() throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM Usuarios";
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            if (rs.next()) return rs.getInt("cnt");
            else           return 0;
        }
    }

    /**
     * Inicializa o banco de dados se for a primeira vez logando.
     */
    public void initIfNeeded() throws SQLException {
        boolean hasUsers;
        try {
            hasUsers = countUsers() > 0;
        } catch (SQLException e) {
            // Se não existe a tabela Usuarios, consideramos que precisamos inicializar
            hasUsers = false;
        }
        if (hasUsers) return;
    
        // ----------------------------------------------------------------------------
        // roda script de criação do banco
        // ----------------------------------------------------------------------------
        try (InputStream in = getClass().getResourceAsStream("/db/initDB.sql");
             Scanner sc = new Scanner(in, StandardCharsets.UTF_8)) {
            sc.useDelimiter(";");
            try (Connection c = connect();
                 Statement stmt = c.createStatement()) {
                while (sc.hasNext()) {
                    String sql = sc.next().trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }
            }
        } catch (IOException e) {
            throw new SQLException("Falha ao ler script de criação do banco", e);
        }
    }
    
    
    
    /**
     * Busca um usuário por e-mail. Retorna null se não existir.
     */
    public User findUserByEmail(String email) throws SQLException {
        String sql = """
          SELECT uid,nome,email,senha_hash,totp_key_encrypted,kid,blocked
            FROM Usuarios WHERE email = ?
        """;
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email);
            try (ResultSet rs = p.executeQuery()) {
                if (!rs.next()) return null;
                int    uid    = rs.getInt("uid");
                String nome   = rs.getString("nome");
                String hash   = rs.getString("senha_hash");
                byte[] encTotp= rs.getBytes("totp_key_encrypted");
                boolean frozen= rs.getInt("blocked")==1;
    
                User u = new User(uid, nome, email, hash, null, frozen);
                u.setEncryptedTotp(encTotp);
                u.setChaveiroId(rs.getInt("kid"));
                return u;
            }
        }
    }
    

    // em db/DBManager.java, dentro da classe DBManager:
    public byte[] getEncryptedTotpByEmail(String email) throws SQLException {
        String sql = "SELECT totp_key_encrypted FROM Usuarios WHERE email = ?";
        try (Connection c = connect();
            PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("totp_key_encrypted");
                } else {
                    return null;
                }
            }
        }
    }

    
    
    
    
    

    /** Retorna o ID de um grupo, criando se não existir */
    public int getOrCreateGroup(String nome) throws SQLException {
        String select = "SELECT gid FROM Grupos WHERE nome = ?";
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(select)) {
            p.setString(1, nome);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return rs.getInt("gid");
            }
        }

        String insert = "INSERT INTO Grupos(nome) VALUES(?)";
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, nome);
            p.executeUpdate();
            try (ResultSet rs = p.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        throw new SQLException("Falha ao criar ou obter grupo");
    }

    /**
     * Insere um par (certificado PEM, chave privada AES-256 encryptada)
     * para o usuário UID e retorna o KID gerado.
     */
    public int addChaveiro(int uid, String certificadoPem, byte[] chavePrivadaEncrypted)
            throws SQLException {
        String sql = """
            INSERT INTO Chaveiro(uid, certificado, chave_privada)
            VALUES (?, ?, ?)
            """;
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, uid);
            p.setString(2, certificadoPem);
            p.setBytes(3, chavePrivadaEncrypted);
            p.executeUpdate();
            try (ResultSet rs = p.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Falha ao inserir Chaveiro");
    }

    /**
     * Cadastra um novo usuário no sistema. Retorna true se inseriu com sucesso.
     * @param nome                nome completo
     * @param email               login name (único)
     * @param gid                 grupo já existente
     * @param senhaHash           hash Bcrypt (2y$...)
     * @param totpKeyEncrypted    segredo TOTP (BASE32) já encryptado em AES
     * @param kid                 ID do Chaveiro correspondente
     */
    public boolean addUsuario(String nome,
                              String email,
                              int gid,
                              String senhaHash,
                              byte[] totpKeyEncrypted,
                              int kid) throws SQLException {
        String sql = """
            INSERT INTO Usuarios(nome, email, gid, senha_hash, totp_key_encrypted, kid)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, nome);
            p.setString(2, email);
            p.setInt(3, gid);
            p.setString(4, senhaHash);
            p.setBytes(5, totpKeyEncrypted);
            p.setInt(6, kid);
            return p.executeUpdate() == 1;
        }
    }

    

    /**
     * Insere um registro de evento no log:
     * @param mid     código da mensagem (em Mensagens.mid)
     * @param uid     usuário envolvido (pode ser null)
     * @param arquivo nome do arquivo (pode ser null)
     */
    public boolean insertRegistro(int mid, Integer uid, String arquivo) throws SQLException {
        String sql = """
            INSERT INTO Registros(mid, uid, arquivo)
            VALUES (?, ?, ?)
            """;
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt   (1, mid);
            if (uid != null) p.setInt   (2, uid);
            else             p.setNull  (2, Types.INTEGER);
            if (arquivo != null) p.setString(3, arquivo);
            else                 p.setNull  (3, Types.VARCHAR);
            return p.executeUpdate() == 1;
        }
    }

    /** 
     * Retorna todo o log de eventos, em ordem cronológica. 
     * Cada linha traz: RID, datahora, texto da mensagem e e-mail do usuário.
     */
    public List<Map<String,Object>> getLog() throws SQLException {
        String sql = """
            SELECT r.rid, r.datahora, m.texto, u.email, r.arquivo
              FROM Registros r
              JOIN Mensagens m ON r.mid = m.mid
         LEFT JOIN Usuarios  u ON r.uid = u.uid
             ORDER BY r.rid
            """;
        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {

            List<Map<String,Object>> lista = new ArrayList<>();
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();

            while (rs.next()) {
                Map<String,Object> row = new HashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(md.getColumnName(i), rs.getObject(i));
                }
                lista.add(row);
            }
            return lista;
        }
    }

    /**
     * Retorna o grupo do usuário com o UID.
     * @param uid     usuário envolvido (pode ser null)
     */
    public String getUserGroup(int uid) throws SQLException {
        String sql = """
            SELECT g.nome 
            FROM Grupos g, Usuarios u
            WHERE g.gid = u.gid AND u.uid = ?
        """;

        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, uid);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) return rs.getString("nome");
            }
        }
        return null;
    }

    /**
     * Retorna o número de acessos do usuário com o UID.
     * @param uid     usuário envolvido (pode ser null)
     */
    public int getUserAcessNum(int uid) throws SQLException {
        String sql = """
            SELECT COUNT(r.rid) AS num_rids
            FROM Registros r
            WHERE r.uid = ? AND r.mid = 4003
        """;

        try (Connection c = connect();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, uid);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("num_rids");
                }
            }
        }

        return 0;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  adicionar outros métodos públicos conforme necessidade:
    //     • atualizar senha (`UPDATE Usuarios SET senha_hash = ? WHERE uid = ?`)
    //     • carregar/atualizar TOTP key, etc.
    // ──────────────────────────────────────────────────────────────────────────
    
}



