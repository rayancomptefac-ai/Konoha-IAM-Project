package org.sdk.APIREST;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;

public class API_REST {

    // --- VERIFICATION DES PERMISSIONS ---
    public static void VERIFIERPERMISSION(Context ctx, String permissionRequise) {
        String roleUser = ctx.attribute("role");
        // Protection contre le null si le role n'est pas trouvé
        if (roleUser == null) roleUser = "GUEST"; 

        List<String> mesDroits = SecurityPolicy.MATRICE.getOrDefault(roleUser, List.of());
        
        if (!mesDroits.contains(permissionRequise)) {
            // On logue la tentative d'intrusion
            LOG_AUDIT(ctx.attribute("nom_ninja"), "ACCES_REFUSE", "Action interdite : " + permissionRequise);
            throw new ForbiddenResponse("Tu n'as pas le niveau nécessaire pour faire cela");
        }
    }

    // --- JOURNAL D'AUDIT ---
    public static void LOG_AUDIT(String ninja, String action, String details) {
        try {
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            // Utilisation d'un format propre
            String log = String.format("[%s] | NINJA: %s | ACTION: %s | DETAILS: %s\n", 
                                        date, (ninja == null ? "ANONYME" : ninja), action, details);
            Files.write(Paths.get("village.log"), log.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.err.println("Erreur d'écriture de log : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Database.initialiser();
        var app = Javalin.create().start(7001);

        // --- MIDDLEWARE DE SÉCURITÉ ---
        app.before(ctx -> {
            if (ctx.path().equals("/login")) return;

            String idUtilisateur = null;
            String nomUtilisateur = null;
            String roleUtilisateur = null;

            // 1. Vérification du Badge (JWT)
            String header = ctx.header("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    idUtilisateur = JwtUtil.validerlebadge(header.substring(7));
                    if (idUtilisateur != null) {
                        Utilisateurs u = Database.trouverNinjaParNom(idUtilisateur);
                        if (u != null) {
                            nomUtilisateur = u.nom;
                            roleUtilisateur = u.role;
                        }
                    }
                } catch (Exception e) {
                    // Si le badge est périmé ou faux, on ignore et on tente le mot de passe
                    System.out.println("Badge invalide : " + e.getMessage());
                }
            }

            // 2. Vérification du Mot de passe (Basic Auth) si le badge a échoué
            if (idUtilisateur == null) {
                var creds = ctx.basicAuthCredentials();
                if (creds != null) {
                    Utilisateurs trouve = Database.trouverNinjaParNom(creds.getUsername());
                    if (trouve != null && BCrypt.checkpw(creds.getPassword(), trouve.password)) {
                        idUtilisateur = trouve.id;
                        nomUtilisateur = trouve.nom;
                        roleUtilisateur = trouve.role;
                    }
                }
            }

            // Verdict final
            if (idUtilisateur == null) {
                ctx.header("WWW-Authenticate", "Basic");
                throw new UnauthorizedResponse("Identifiants ou Badge requis");
            }

            // Injection dans le contexte
            ctx.attribute("nom_ninja", nomUtilisateur != null ? nomUtilisateur : "Inconnu");
            ctx.attribute("role", roleUtilisateur != null ? roleUtilisateur : "GUEST");
        });

        // --- ROUTES ---
        app.patch("/admin/promote/{id}", ctx -> {
    // 1. VERIFICATION HIERARCHIQUE (Le cœur de l'IAM)
    String roleConnecte = ctx.attribute("role_ninja"); // Récupéré de ton JWT
    
    if (!"HOKAGE".equals(roleConnecte)) {
        ctx.status(403).result("Accès refusé : Seul le Hokage peut promouvoir des ninjas.");
        return;
    }
    String idNinja = ctx.pathParam("id");
    Map<String, Object> body = ctx.bodyAsClass(Map.class);
    String nouveauRole = (String) body.get("role");

    // 3. Appel à une méthode spécifique (ex: updateRole)
    Database.updateRole(idNinja, nouveauRole);
    
    ctx.result("Promotion effectuée avec succès !");
});


        
        app.patch("/profil", ctx -> {
    // 1. Vérif Identité
    String nomConnecte = ctx.attribute("nom_ninja");

    // 2. Extraction sécurisée (On ne prend que ce qu'on autorise)
    Map<String, Object> body = ctx.bodyAsClass(Map.class);
    String nouveauNom = (String) body.get("nom");
    String nouveauDep = (String) body.get("departement");

    Database.updateBasicInfo(nomConnecte, nouveauNom, nouveauDep);
    
    ctx.result("Profil mis à jour");
});

        app.post("/login", ctx -> {
            String body = ctx.body();
            if (body.isEmpty()) {
        ctx.status(400).result("Erreur : Le corps de la requête est vide (JSON attendu)");
        return;
    }
            Utilisateurs logindata = ctx.bodyAsClass(Utilisateurs.class);
            Utilisateurs trouve = Database.trouverNinjaParNom(logindata.nom);

            if (trouve != null && BCrypt.checkpw(logindata.password, trouve.password)) {
                String badge = JwtUtil.genererToken(trouve.nom, trouve.id);
                LOG_AUDIT(trouve.nom, "LOGIN", "Succès");
                ctx.json(badge); // Renvoie le badge en JSON propre
            } else {
                LOG_AUDIT(logindata.nom, "LOGIN_FAIL", "Mauvais mot de passe");
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
            LOG_AUDIT(ctx.attribute("nom_ninja"), "CREATION", "Nouveau ninja : " + nouveau.nom);
            ctx.status(201).result("Ninja ajouté !");
        });

        app.delete("/ninjas/{nom}", ctx -> {
            VERIFIERPERMISSION(ctx, SecurityPolicy.PERM_SUPPRIMER);
            String cible = ctx.pathParam("nom");
            
            if (Database.trouverNinjaParNom(cible) != null) {
                Database.supprimerninja(cible);
                LOG_AUDIT(ctx.attribute("nom_ninja"), "SUPPRESSION", "Cible : " + cible);
                ctx.status(200).result("Ninja supprimé");
            } else {
                ctx.status(404).result("Ninja non trouvé");
            }
        });

        app.patch("/ninjas/{nom}", ctx -> {
            VERIFIERPERMISSION(ctx, SecurityPolicy.PERM_MODIFIER); 
            String nomCible = ctx.pathParam("nom");
            
            if (Database.trouverNinjaParNom(nomCible) != null) {
                Utilisateurs maj = ctx.bodyAsClass(Utilisateurs.class);
                Database.modifierdepartement(maj.departement, nomCible);
                LOG_AUDIT(ctx.attribute("nom_ninja"), "MODIFICATION", "Cible : " + nomCible);
                ctx.status(200).result("Modification réussie");
            } else {
                ctx.status(404).result("Ninja non trouvé");
            }
        });
        app.post("/refresh", ctx -> {
            String secretRecu = ctx.cookie("refreshtoken");
            if (secretRecu == null) {
        throw new UnauthorizedResponse("Pas de refresh token trouvé");
    }
        

        Utilisateurs ninjatrouve = null;
        for(Utilisateurs u : Database.listerninja()){
            if (ninjatrouve.equals(u)) {
                break;
            }
            if (ninjatrouve != null) {
                String generertoken = JwtUtil.genererToken(ninjatrouve.nom, ninjatrouve.id);
                ctx.status(201).result(generertoken);
            }
            else{
                ctx.status(404).result("Token non generé due a une erreur");
            }
        }
        });
    }
}