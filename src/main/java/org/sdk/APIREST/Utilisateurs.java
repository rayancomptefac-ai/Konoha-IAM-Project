package org.sdk.APIREST;

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
    public Utilisateurs(String id, String nom, String departement, String password, String role) {
       this.id = id;
    this.nom = nom;
    this.departement = departement;
    this.password = password;
    this.role = role;
    this.estActif = true;
    }
}