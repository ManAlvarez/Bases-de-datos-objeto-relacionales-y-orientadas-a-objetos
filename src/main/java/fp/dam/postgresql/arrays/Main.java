/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fp.dam.postgresql.arrays;

/**
 *
 * @author manuel
 */
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

public class Main {

    public static void main(String[] args) {
        
        
        //URL e base de datos a cal nos conectamos
        String url = new String("192.168.56.102");
        String db = new String("test");
        
        //Indicamos as propiedades da conexión
        Properties props = new Properties();
        props.setProperty("user", "accesodatos");
        props.setProperty("password", "abc123.");
        
        //Dirección de conexión a base de datos
        String postgres = "jdbc:postgresql://"+url+"/"+db;
        
        //Conectamos a base de datos
        try {
            Connection conn = DriverManager.getConnection(postgres,props);
            
            //Creamos a táboa que un array de enteiros e outro de cades de texto
            String sqlTableCreation = new String(
                "CREATE TABLE IF NOT EXISTS probaarrays ("
                        + "id SERIAL PRIMARY KEY, "
                        + "numeros INTEGER[], "
                        + "mensaxes TEXT[]);");
            CallableStatement createTable = conn.prepareCall(sqlTableCreation);
            createTable.execute();
            createTable.close();
            
            //Creamos a consulta SQL
            String insertSQL = new String(
                "INSERT INTO probaarrays(numeros,mensaxes) VALUES(?,?)");
            PreparedStatement insert = conn.prepareStatement(insertSQL);
            
            //Creamos os dous arrays que imos engadir
            Integer[] num = {1,2,3,4,5,6,7};
            String[] men = {"Acceso","a","datos"};
            
            //Engadimos os arrays, pero primeiro hai que convertilos
            insert.setArray(1, conn.createArrayOf("INTEGER", num));
            insert.setArray(2, conn.createArrayOf("TEXT", men));
            
            //Executamos a consulta
            insert.execute();
            insert.close();            
            
            if(conn!=null) conn.close();        
        
        } catch (SQLException ex) {
            System.err.println("Error: " + ex.toString());
        }   
        
    }
    
}
