package org.sdk.APIREST;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Database{
    private static final String URL = "jdbc:h2:./konoha_db";
    private static final String USER = "admin";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

public static void initialiser() {
    try (Connection conn = getConnection();
    Statement stmt = conn.createStatement();) {
        String sql = "CREATE TABLE IF NOT EXISTS NINJAS (" +
                         "id VARCHAR(50) PRIMARY KEY, " +
                         "nom VARCHAR(50) NOT NULL, " +
                         "departement VARCHAR(50) NOT NULL, " +
                         "password VARCHAR(255) NOT NULL, " +
                         "role VARCHAR(20) NOT NULL" +
                         ")";
    stmt.execute(sql);
    System.out.println("La BDD a bien été crée");
    } 
    catch (SQLException e) {
        e.printStackTrace();
    
    }
}
public static void AjouterNinja(Utilisateurs u){
    String SQL = "INSERT INTO NINJAS (id, nom,departement,password,role) VALUES (?,?,?,?,?)";
    try(Connection conn = getConnection();
    PreparedStatement pstmt = conn.prepareStatement(SQL)){
        pstmt.setString(1, u.id);
        pstmt.setString(2, u.nom);
        pstmt.setString(3, u.departement);
        pstmt.setString(4, u.password);
        pstmt.setString(5, u.role);
    
    pstmt.executeUpdate();
    System.out.println("Ninja [ " + u.nom + " ] inséré avec son département.");
    }
     catch (SQLException e) {
        System.out.println("Erreur : " + e.getMessage());
    }
}
public static Utilisateurs trouverNinjaParNom(String nomcherche ) {
    String SQL = "SELECT * FROM NINJAS WHERE NOM = ?";
    try (Connection conn = getConnection();
    PreparedStatement pstmt = conn.prepareStatement(SQL)){
        pstmt.setString(1, nomcherche);

ResultSet rs = pstmt.executeQuery();

if (rs.next()){
    
    return new Utilisateurs(
        rs.getString("id"),
        rs.getString("nom"),
        rs.getString("departement"),
        rs.getString("password"),
        rs.getString("role"));
}
}
 catch (SQLException e) {
        System.out.println("Erreur : " + e.getMessage());
    }
    return null;


}

public static void supprimerninja(String nomsupprime){
     String SQL = "DELETE FROM NINJAS WHERE NOM = ?";
     try (Connection conn = getConnection();
    PreparedStatement pstmt = conn.prepareStatement(SQL)){
         pstmt.setString(1, nomsupprime);
        int resultat = pstmt.executeUpdate();

        if (resultat >0) {
            System.out.println("Ninja Supprimé avec succès");
        }
        else{
            System.out.println("Aucun ninja n'a été trouvé");
        }

    }
    catch (SQLException e) {
        System.out.println("Erreur : " + e.getMessage());
    }
}

public static ArrayList<Utilisateurs> listerninja(){
    ArrayList<Utilisateurs> laListe = new ArrayList<>();
    String SQL = "SELECT * FROM NINJAS ";

 try (Connection conn = getConnection();
    PreparedStatement pstmt = conn.prepareStatement(SQL)){
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
        Utilisateurs n = new Utilisateurs(rs.getString("id"),
        rs.getString("nom"),
        rs.getString("departement"),
        rs.getString("password"),
        rs.getString("role")); 
        laListe.add(n); 
        }

    }
    catch (SQLException e) {
        System.out.println("Erreur : " + e.getMessage());
    }
    return laListe;
    
}
public static void modifierdepartement(String departementmodif, String nomcible){
    String SQL = "UPDATE NINJAS SET departement = ? WHERE NOM = ? ";
    try (Connection conn = getConnection();
    PreparedStatement pstmt = conn.prepareStatement(SQL)){
        pstmt.setString(1, departementmodif);
        pstmt.setString(2, nomcible);
    int resultat = pstmt.executeUpdate();
    if (resultat >0) {
            System.out.println("Departement modifié avec succès");
        }
        else{
            System.out.println("Aucun ninja n'a été trouvé");
        }
    }
    catch (SQLException e) {
        System.out.println("Erreur : " + e.getMessage());
    }
}
}