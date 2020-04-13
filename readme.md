# Conexión a base de datos PostgreSQL e chamada a unha función
## Conexión a base de datos postgre
O primeiro é engadir as dependencias no arquivo pom.xml para obter o driver JBDC de conexión a unha base de datos PostgreSQL.

```xml
<dependencies>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.2.9</version>
    </dependency>
</dependencies>
```
Para conectarnos a base de datos necesitamos (como na maioría de conexións a base de datos) a **URL**, o **nome da base de datos** e as **credenciais**.

```java
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
    
    //Cerramos a conexión coa base de datos
    if(conn!=null) conn.close();
} catch (SQLException ex) {
    System.err.println("Error: " + ex.toString());
}
```
## Chamada a unha función de PostgreSQL
É posible utilizar unha función de PostgreSQL para que nos realice unha operación. Neste caso imos crear a función en código dende JAVA, inda que isto non é moi lóxico. O normal e que estivera xa creada ou se creara doutro xeito (polo admin da base de datos por exemplo). Só é para poder realizar o exemplo.

```java
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
```

A linguaxe que se utiliza para a creación de funcións é PLPSQL. A función do exemplo anterior recibe un enteiro e devolve o valor seguinte.

Agora vemos o código para utilizar dita función.

```java
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

```

# Persistir un arquivo en PostgreSQL
## Creación da táboa
Imos comezas por crear a táboa que conterá o arquivo binario. O tipo de datos que usaremos é [**bytea**](https://www.postgresql.org/docs/9.0/datatype-binary.html). A táboa usarémola neste caso para gardar imaxes, pero podería ser calquera outro arquivo binario.

```java
//Creamos a táboa que conterá as imaxes
//NOTA: nón é moi lóxico crear funcións dende código. Só o fago para despois utilizala
String sqlTableCreation = new String(
    "CREATE TABLE IF NOT EXISTS imaxes (nomeimaxe text, img bytea);");

//Executamos a sentencia SQL anterior
CallableStatement createFunction = conn.prepareCall(sqlTableCreation);
createFunction.execute();
createFunction.close();
```
## Inserción dunha imaxe
Para facer un insert na táboa necesitaremos ler os bytes dun arquivo (vímolo como se realiza na unidade 2) e posteriormente insertala na base de datos.

```java
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
```
## Recuperación dun arquivo binario
Neste caso realizaremos unha consulta para recuperar o arquivo que intruducimos no paso anterior. Posteriormente gardaremos os bytes que se nos proporciona tal e como vimos na unidade 2. 

```java
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
```

## Imaxes grandes
Se as imaxes que queremos gardar son moi grandes podemos utilizar outro tupi de datos en postgreSQL (imageoid). Tedes a documentación de como se realiza no seguinte enlace: [https://jdbc.postgresql.org/documentation/head/binary-data.html#binary-data-example](https://jdbc.postgresql.org/documentation/head/binary-data.html#binary-data-example)


# Notificacións en PostgreSQL
PostgreSQL permite que o propio xestor de base de datos avise ao cliente de que aconteceu un evento. Esto realizase cas **notificacións**.

Tedes máis información en (https://www.postgresql.org/docs/12/sql-notify.html)[https://www.postgresql.org/docs/12/sql-notify.html].

En JAVA non se lle da quitado todo o partido porque non é unha linguaxe asíncrona. Polo tanto en lugar de que o SXBD nos avise dunha notificación, teremos que cada certo tempo comprobar se hai novas notificacións.

## Exemplo
Vamos a facer un pequeno chat no que só recibimos mensaxes. Para iso teremos unha táboa que garde as mensaxes co nome de usuario correspondente.

```java
String sqlTableCreation = new String(
    "CREATE TABLE IF NOT EXISTS mensaxes ("
            + "id SERIAL PRIMARY KEY, "
            + "usuario TEXT NOT NULL, "
            + "mensaxe TEXT NOT NULL);");
CallableStatement createTable = conn.prepareCall(sqlTableCreation);
createTable.execute();
createTable.close();
```
Imos crear unha función para un trigger que xere unha notificación para cada nova mensaxe. O **channel** que usaremos será neste caso 'novamensaxe' e o **payload** o id desa mensaxe.

```java
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
```

Creamos o trigger para que cada vez que se engada unha nova mensaxe se execute a función anterior.
```java
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
```
Agora subscribimonos ao canal **novamensaxe** para poder recibir notificacións.
```java
PGConnection pgconn = conn.unwrap(PGConnection.class);
Statement stmt = conn.createStatement();
stmt.execute("LISTEN novamensaxe");
stmt.close();
System.out.println("Esperando mensaxes...");
```

Cada certo tempo comprobamos se hai nova notificacións. Se hai novas notificacións realizamos unha consulta a base de datos para coller toda a informacións desa mensaxe. Utilizamos o id que se envía no payload.

```java
PGNotification notifications[] = pgconn.getNotifications();

if(notifications != null){
    for(int i=0;i < notifications.length;i++){

        int id = Integer.parseInt(notifications[i].getParameter());

        sqlMensaxe.setInt(1, id);
        ResultSet rs = sqlMensaxe.executeQuery();
        rs.next();
        System.out.println(rs.getString(1) + ":" + rs.getString(2));
        rs.close();
    }
}
```

# Arrays

PostgreSQL permite o uso de arrays. A información sobre este tipo de datos tédela no seguinte enlace: (https://www.postgresql.org/docs/9.1/arrays.html)[https://www.postgresql.org/docs/9.1/arrays.html].

Vamos ver un exemplo de como insertar arrays nunha táboa. Na documentación do enlace anterior tedes ademais como realizar consultas sobre estes arrays.

## Exemplo

Creamos a táboa:
```java
String sqlTableCreation = new String(
    "CREATE TABLE IF NOT EXISTS probaarrays ("
            + "id SERIAL PRIMARY KEY, "
            + "numeros INTEGER[], "
            + "mensaxes TEXT[]);");
CallableStatement createTable = conn.prepareCall(sqlTableCreation);
createTable.execute();
createTable.close();
```

Para poder engadir un array de obxectos de JAVA hai que transformar ese array co seguinte método **Array createArrayOf(String typeName, Object[] elements) throws SQLException**.

```java
String insertSQL = new String(
    "INSERT INTO probaarrays(numeros,mensaxes) VALUES(?,?)");
PreparedStatement insert = conn.prepareStatement(insertSQL);

Integer[] num = {1,2,3,4,5,6,7};
String[] men = {"Acceso","a","datos"};

insert.setArray(1, conn.createArrayOf("INTEGER", num));
insert.setArray(2, conn.createArrayOf("TEXT", men));

insert.execute();
insert.close();   
```






