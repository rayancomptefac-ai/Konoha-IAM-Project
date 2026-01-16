package org.sdk.APIREST;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;
import org.mindrot.jbcrypt.BCrypt;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import java.nio.file.*;  // Pour manipuler les fichiers
import java.time.LocalDateTime; // Pour avoir l'heure actuelle
import java.time.format.DateTimeFormatter; // Pour faire une jolie date

public class API_REST {

// --- MÉTHODE DE SÉCURITÉ (HOKAGE UNIQUEMENT) ---
    public static void ACCESADMIN(Context ctx) {
        String roleUser = ctx.attribute("role");
        if (roleUser == null || !"ADMIN".equalsIgnoreCase(roleUser.trim())) {
            throw new ForbiddenResponse("Seul l'Hokage peut modifier le village !");
        }
    }
    // --- MÉTHODE DE LOGS ---


public static void LOGS(String message){
    try{
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String logdate =LocalDateTime.now().format(formatter);
        String msgfinal = "["+ logdate + message +"]";
    Files.write(Paths.get("village.log"), 
            (msgfinal + "\n").getBytes(), 
            StandardOpenOption.CREATE, 
            StandardOpenOption.APPEND);
}
catch(Exception e){
    System.err.println("Le log n'a pas pu être effectué" + e.getMessage());


}}
   

    // --- SAUVEGARDE ET CHARGEMENT ---
    public static void Sauvegarder(ArrayList<Utilisateurs> liste) {
        try {
            new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValue(new File("Mon_village.json"), liste);
        } catch (Exception e) {
            System.err.println("Erreur Sauvegarde: " + e.getMessage());
        }
    }

    public static ArrayList<Utilisateurs> chargerlesninjas() {
        try {
            File f = new File("Mon_village.json");
            if (!f.exists()) return new ArrayList<>();
            return new ObjectMapper().readValue(f, new TypeReference<ArrayList<Utilisateurs>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) {
        ArrayList<Utilisateurs> annuaire = chargerlesninjas();
        var app = Javalin.create().start(7001);

        // --- MIDDLEWARE DE SÉCURITÉ ---
        app.before(ctx -> {
                
            var creds = ctx.basicAuthCredentials();
            if (creds == null) {
                ctx.header("WWW-Authenticate", "Basic");
                throw new UnauthorizedResponse("Identifiants requis");  
            }

            Utilisateurs trouve = annuaire.stream()
                .filter(u -> u.nom.equalsIgnoreCase(creds.getUsername()))
                .findFirst()
                .orElse(null);
                LOGS(("L'utilisateur " + creds.getUsername() + " a essayé de se connecter"));

            if (trouve == null || !BCrypt.checkpw(creds.getPassword(), trouve.password)) {
                ctx.header("WWW-Authenticate", "Basic");    
                throw new UnauthorizedResponse("Accès refusé"); 
            }

            ctx.attribute("role", trouve.role);
            LOGS(("L'utilisateur " + creds.getUsername() + " a réussi à se connecter"));
        });

        // --- ROUTES ---
        app.get("/ninjas", ctx -> ctx.json(annuaire));

        app.post("/ninjas", ctx -> {
            ACCESADMIN(ctx);
            Utilisateurs nouveau = ctx.bodyAsClass(Utilisateurs.class);
            nouveau.id = UUID.randomUUID().toString(); 
            nouveau.password = BCrypt.hashpw(nouveau.password, BCrypt.gensalt()); 
            annuaire.add(nouveau);
            Sauvegarder(annuaire);
            LOGS( "L'utilisateur " + nouveau.nom + " a bien été crée");
            ctx.status(201).result("Ninja ajouté !");
        });

        app.delete("/ninjas/{nom}", ctx -> {
            ACCESADMIN(ctx);
            String cible = ctx.pathParam("nom");
            if(annuaire.removeIf(u -> u.nom.equalsIgnoreCase(cible))){
                Sauvegarder(annuaire);
                ctx.status(200).result("Ninja supprimé");
                LOGS( "L'utilisateur " + cible + " a bien été supprimé");
            } else {
                ctx.status(404).result("Ninja non trouvé");
                
            }
        });

        app.patch("/ninjas/{nom}", ctx -> {
            ACCESADMIN(ctx);
            String nomCible = ctx.pathParam("nom");
            Utilisateurs maj = ctx.bodyAsClass(Utilisateurs.class);
            boolean modifie = false;

            for (Utilisateurs u : annuaire) {
                if (u.nom.equalsIgnoreCase(nomCible)) {
                    u.departement = maj.departement;
                    modifie = true;
                    Sauvegarder(annuaire);
                    ctx.status(200).result("Modification réussie");
                    LOGS( "L'utilisateur " + nomCible + " a bien été modifié");
                    break;
                }
            }
            if (!modifie) ctx.status(404).result("Ninja introuvable");
        });

        System.out.println("=== Serveur Konoha Opérationnel (IAM) ===");
    }
}