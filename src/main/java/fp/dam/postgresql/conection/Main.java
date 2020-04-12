/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fp.dam.postgresql.conection;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

/**
 *
 * @author manuel
 */
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
            
            //Creamos a sentencia SQL para crear unha función
            //NOTA: nón é moi lóxico crear funcións dende código. Só o fago para despois utilizala
            String sqlCreateFucction = new String(
                "CREATE OR REPLACE FUNCTION inc(val integer) RETURNS integer AS $$ "+
                "BEGIN "+
                "RETURN val + 1; "+
                "END;"+
                "$$ LANGUAGE PLPGSQL;");
        
            //Executamos a sentencia SQL anterior
            CallableStatement createFunction = conn.prepareCall(sqlCreateFucction);
            createFunction.execute();
            createFunction.close();

            //Creamos a chamada a función
            String sqlCallFunction = new String("{? = call inc( ? ) }");
            CallableStatement callFunction = conn.prepareCall(sqlCallFunction);
            
            //O primeiro parámetro indica o tipo de datos que devolve
            callFunction.registerOutParameter(1, Types.INTEGER);
            
            //O segundo parámetro indica o valor que lle pasamos a función, neste exemplo 5
            callFunction.setInt(2,5);
            
            //Executamos a función
            callFunction.execute();
            
            //Obtemos o valor resultante da función
            int valorDevolto = callFunction.getInt(1);
            callFunction.close();
            
            //Mostramos o valor devolto
            System.out.println("Valor devolto da función: " +  valorDevolto);
            
            //Cerramos a conexión coa base de datos
            if(conn!=null) conn.close();
        
        
        } catch (SQLException ex) {
            System.err.println("Error: " + ex.toString());
        }
        

        
        
        
    }
    
}
