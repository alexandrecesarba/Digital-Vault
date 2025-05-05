import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


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
}



