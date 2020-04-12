/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fp.dam.postgresql.notificaciones;

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
        
        //Tempo en minutos que estara a espera
        Integer tempo = 1;
        
        //Tempo que espera para cada consulta en milisegundos
        Integer espera = 1000;
        
        //Conectamos a base de datos
        try {
            Connection conn = DriverManager.getConnection(postgres,props);
            
            //Creamos a táboa que conterá as mensaxes dos usarios
            String sqlTableCreation = new String(
                "CREATE TABLE IF NOT EXISTS mensaxes ("
                        + "id SERIAL PRIMARY KEY, "
                        + "usuario TEXT NOT NULL, "
                        + "mensaxe TEXT NOT NULL);");
            CallableStatement createTable = conn.prepareCall(sqlTableCreation);
            createTable.execute();
            createTable.close();

            //Creamos a función que notificará que se engadiu unha nova mensaxe
            String sqlCreateFunction = new String(
                "CREATE OR REPLACE FUNCTION notificar_mensaxe() "+
                "RETURNS trigger AS $$ "+
                "BEGIN " +
                    "PERFORM pg_notify('novamensaxe',NEW.id::text); "+
                "RETURN NEW; "+
                "END; "+
                "$$ LANGUAGE plpgsql; ");
            CallableStatement createFunction = conn.prepareCall(sqlCreateFunction);
            createFunction.execute();
            createFunction.close();

            //Creamos o triguer que se executa tras unha nova mensaxe
            String sqlCreateTrigger = new String(
                "DROP TRIGGER IF EXISTS not_nova_mensaxe ON mensaxes; "+
                "CREATE TRIGGER not_nova_mensaxe "+
                "AFTER INSERT "+
                "ON mensaxes "+
                "FOR EACH ROW "+
                "EXECUTE PROCEDURE notificar_mensaxe(); ");
            CallableStatement createTrigger = conn.prepareCall(sqlCreateTrigger);
            createTrigger.execute();
            createTrigger.close();
       
            //Configuramos para estar a escoita
            PGConnection pgconn = conn.unwrap(PGConnection.class);
            Statement stmt = conn.createStatement();
            stmt.execute("LISTEN novamensaxe");
            stmt.close();
            System.out.println("Esperando mensaxes...");
            
            
            //Variables para controlar o tempo de espera
            boolean flag=true;
            long finishAt = new Date().getTime() + (tempo * 60000);
            
            //Creamos a consulta que necesitaremos para obter a mensaxe
            PreparedStatement sqlMensaxe = conn.prepareStatement("SELECT usuario,mensaxe FROM mensaxes WHERE id=?;");
            
            //Bucle para ir lendo as mensaxes
            while(flag){
                
                //Collemos todas as notificacions
                PGNotification notifications[] = pgconn.getNotifications();
                
                //Se hai notificacions, recorrémolas e imprimimos os parametros 
                if(notifications != null){
                    for(int i=0;i < notifications.length;i++){
                        
                        //A notificacion danos como parámetro o id da mensaxe
                        int id = Integer.parseInt(notifications[i].getParameter());
                        
                        //Facemos unha consulta a base de datos e imprimimos a mensaxe
                        sqlMensaxe.setInt(1, id);
                        ResultSet rs = sqlMensaxe.executeQuery();
                        rs.next();
                        System.out.println(rs.getString(1) + ":" + rs.getString(2));
                        rs.close();
                    }
                }
                
                //Esperamos un pouco de tempo entre conexións
                Thread.sleep(espera);
                
                //Comprobamos se pasou o tempo de espera
                if(new Date().getTime() > finishAt)flag=false;
                    
            }
            
            //deixamos de escoitar conexións
            Statement stmt2 = conn.createStatement();
            stmt2.execute("UNLISTEN novamensaxe");
            stmt2.close();
            
            if(conn!=null) conn.close();        
        
        } catch (SQLException ex) {
            System.err.println("Error: " + ex.toString());
        } catch (InterruptedException ex) {
            System.err.println("Error: " + ex.toString());
        }     
        
    }
    
}
