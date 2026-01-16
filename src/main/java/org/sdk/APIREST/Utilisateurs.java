package org.sdk.APIREST;
import java.util.UUID;

public class Utilisateurs {
    public String id;
    public String nom;
    public String departement;
    public boolean estActif;
    public String password;
    public String role;

    // --- LE CONSTRUCTEUR VIDE (Indispensable pour Jackson / JSON) ---
    public Utilisateurs() {
    }

    // Ton constructeur actuel pour créer tes ninjas manuellement
    public Utilisateurs(String nom, String departement, String role) {
        this.id = UUID.randomUUID().toString();
        this.nom = nom;
        this.departement = departement;
        this.estActif = true; 
        this.role = (role == null || role.isEmpty()) ? "USER" : role; 
    }
}