package org.sdk.APIREST;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;

public class API_REST {

    public static void VERIFIERPERMISSION(Context ctx, String permissionRequise) {
        String roleUser = ctx.attribute("role");
        List<String> mesDroits = SecurityPolicy.MATRICE.getOrDefault(roleUser, List.of());
        if (!mesDroits.contains(permissionRequise)) 
        {
            throw new ForbiddenResponse("Tu n'as pas le niveau nécessaire pour faire cela");
        
    }
    }

    // --- SYSTÈME DE LOGS ---
    public static void LOGS(String message) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String logdate = LocalDateTime.now().format(formatter);
            String msgfinal = "[" + logdate + " " + message + "]\n";
            Files.write(Paths.get("village.log"), msgfinal.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.err.println("Le log n'a pas pu être effectué : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Database.initialiser();
        var app = Javalin.create().start(7001);

        // --- MIDDLEWARE DE SÉCURITÉ ---
        app.before(ctx -> {
            if (ctx.path().equals("/login")) return;

            String idUtilisateur = null;
            String roleUtilisateur = null;

            // 1. Essayer le Badge (JWT)
            String header = ctx.header("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                idUtilisateur = JwtUtil.validerlebadge(header.substring(7));
            }

            // 2. Essayer le Mot de passe (Basic Auth)
            if (idUtilisateur == null) {
                var creds = ctx.basicAuthCredentials();
                if (creds != null) {
                    Utilisateurs trouve = Database.trouverNinjaParNom(creds.getUsername());

                    if (trouve != null && BCrypt.checkpw(creds.getPassword(), trouve.password)) {
                        idUtilisateur = trouve.id;
                        roleUtilisateur = trouve.role;
                    }
                }
            }

            // Verdict
            if (idUtilisateur == null) {
                ctx.header("WWW-Authenticate", "Basic");
                throw new UnauthorizedResponse("Identifiants ou Badge requis");
            }

            // Récupérer le rôle si on a utilisé un badge
            if (roleUtilisateur == null) {
                roleUtilisateur = "USER";
                String finalId = idUtilisateur;
                roleUtilisateur = "USER";
            }

            ctx.attribute("role", roleUtilisateur);
        });

        // --- ROUTES ---

        app.post("/login", ctx -> { 
            Utilisateurs logindata = ctx.bodyAsClass(Utilisateurs.class);
           Utilisateurs trouve = Database.trouverNinjaParNom(logindata.nom);

            if (trouve != null && BCrypt.checkpw(logindata.password, trouve.password)) {
                String badge = JwtUtil.genererToken(trouve.nom, trouve.id);
                ctx.status(201).result(badge);
            } else {
                ctx.status(401).result("Identifiants incorrects");
            }
        });

        app.get("/ninjas", ctx -> ctx.json(Database.listerninja()));

        app.post("/ninjas", ctx -> {
            VERIFIERPERMISSION(ctx, SecurityPolicy.PERM_AJOUTER);
            Utilisateurs nouveau = ctx.bodyAsClass(Utilisateurs.class);
            nouveau.id = UUID.randomUUID().toString(); 
            nouveau.password = BCrypt.hashpw(nouveau.password, BCrypt.gensalt()); 
            Database.AjouterNinja(nouveau);
            LOGS("Création du ninja : " + nouveau.nom);
            ctx.status(201).result("Ninja ajouté !");
        });

        app.delete("/ninjas/{nom}", ctx -> {
             VERIFIERPERMISSION(ctx, SecurityPolicy.PERM_SUPPRIMER);
            String cible = ctx.pathParam("nom");
            if (Database.trouverNinjaParNom(cible) != null) {
                Database.supprimerninja(cible);
                LOGS("Suppression du ninja : " + cible);
                ctx.status(200).result("Ninja supprimé");
            } else {
                ctx.status(404).result("Ninja non trouvé");
            }
        });

        app.patch("/ninjas/{nom}", ctx -> {
            String nomCible = ctx.pathParam("nom");
            if (Database.trouverNinjaParNom(nomCible) != null) {
                
            
            Utilisateurs maj = ctx.bodyAsClass(Utilisateurs.class);
            Database.modifierdepartement(maj.departement, nomCible);
            LOGS("Modification SQL du ninja : " + nomCible);
    ctx.status(200).result("Modification réussie en base de données");
            }
            else{
                ctx.status(404).result("Aucun ninja n'a été trouvé");
            }
            
        });
    }
}