/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fp.dam.postgresql.persistencia;

/**
 *
 * @author manuel
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

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
            
            //Creamos a táboa que conterá as imaxes
            //NOTA: nón é moi lóxico crear funcións dende código. Só o fago para despois utilizala
            String sqlTableCreation = new String(
                "CREATE TABLE IF NOT EXISTS imaxes (nomeimaxe text, img bytea);");

            //Executamos a sentencia SQL anterior
            CallableStatement createFunction = conn.prepareCall(sqlTableCreation);
            createFunction.execute();
            createFunction.close();
            
            //Collemos o arquivo
            String nomeFicheiro = new String("logo.png");
            File file = new File(nomeFicheiro);
            FileInputStream fis = new FileInputStream(file);
            
            //Creamos a consulta que inserta a imaxe na base de datos
            String sqlInsert = new String(
                "INSERT INTO imaxes VALUES (?, ?);");
            PreparedStatement ps = conn.prepareStatement(sqlInsert);
            
            //Engadimos como primeiro parámetro o nome do arquivo
            ps.setString(1, file.getName());
            
            //Engadimos como segundo parámetro o arquivo e a súa lonxitude
            ps.setBinaryStream(2, fis, (int)file.length());
            
            //Executamos a consulta
            ps.executeUpdate();
            
            //Cerrramos a consulta e o arquivo aberto
            ps.close();
            fis.close();
            
            //Creamos a consulta para recuperar a imaxe anterior
            String sqlGet = new String(
                "SELECT img FROM imaxes WHERE nomeimaxe = ?;");
            PreparedStatement ps2 = conn.prepareStatement(sqlGet); 
            
            //Engadimos o nome da imaxe que queremos recuperar
            ps2.setString(1, nomeFicheiro); 
            
            //Executamos a consulta
            ResultSet rs = ps2.executeQuery();
            
            //Imos recuperando todos os bytes das imaxes
            byte[] imgBytes = null;
            while (rs.next()) 
            { 
                imgBytes = rs.getBytes(1); 
            }
            
            //Cerramos a consulta
            rs.close(); 
            ps2.close();
            
            //Creamos o fluxo de datos para gardar o arquivo recuperado
            String ficheiroSaida = new String("logo2.png");
            File fileOut = new File(ficheiroSaida);
            FileOutputStream fluxoDatos = new FileOutputStream(fileOut);
            
            //Gardamos o arquivo recuperado
            if(imgBytes != null){
                fluxoDatos.write(imgBytes);
            }
            
            //cerramos o fluxo de datos de saida
            fluxoDatos.close();    
                        
            //Cerramos a conexión coa base de datos
            if(conn!=null) conn.close();        
        
        } catch (SQLException ex) {
            System.err.println("Error: " + ex.toString());
        } catch (FileNotFoundException ex) {
            System.err.println("Error: " + ex.toString());
        } catch (IOException ex) {
            System.err.println("Error: " + ex.toString());
        }       
        
    }
    
}
