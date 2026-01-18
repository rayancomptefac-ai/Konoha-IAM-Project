package org.sdk.APIREST;

import java.util.List;
import java.util.Map;

public class SecurityPolicy {
    public static final String PERM_AJOUTER = "AJOUTER";
    public static final String PERM_SUPPRIMER = "SUPPRIMER";

    public static final Map<String, List<String>> MATRICE = Map.of(
        "ADMIN" , List.of(PERM_AJOUTER, PERM_SUPPRIMER),
        "HOKAGE", List.of(PERM_AJOUTER),
        "ANBU" , List.of(PERM_SUPPRIMER)
    );
}