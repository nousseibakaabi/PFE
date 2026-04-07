package com.example.back.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ChatAIService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    private String currentModel = "gemini-2.0-flash";

    // Cache pour les requêtes SQL (clé = question, valeur = SQL généré)
    private final Map<String, String> sqlCache = new ConcurrentHashMap<>();

    // Cache pour les réponses complètes (clé = question, valeur = réponse formatée)
    private final Map<String, String> answerCache = new ConcurrentHashMap<>();

    // Cache pour les résultats de requêtes (clé = SQL hash, valeur = résultats)
    private final Map<String, List<Map<String, Object>>> queryResultCache = new ConcurrentHashMap<>();

    private final List<String> STABLE_MODELS = Arrays.asList(
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-flash-latest",
            "gemini-1.5-flash"
    );

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();

        log.info("========== 🤖 GEMINI AI SERVICE WITH CACHE ==========");

        if (apiKey == null || apiKey.isEmpty()) {
            log.error("❌ NO API KEY FOUND!");
            log.error("💡 Get a free key from: https://aistudio.google.com/app/apikey");
            log.warn("⚠️ Running in FALLBACK mode only (pattern-based SQL)");
        } else {
            log.info("✅ API Key configured: {}...", apiKey.substring(0, Math.min(10, apiKey.length())));
            log.info("✅ Using model: {}", currentModel);
            log.info("✅ AI mode ACTIVE - Gemini will generate SQL dynamically");
            log.info("✅ Cache enabled - Questions will be cached for faster responses");
        }
        log.info("====================================================");
    }

    /**
     * Appelle l'API Gemini avec fallback sur d'autres modèles
     */
    private String callGeminiWithFallback(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("No API key, cannot call Gemini");
            return null;
        }

        List<String> modelsToTry = new ArrayList<>();
        modelsToTry.add(currentModel);
        modelsToTry.addAll(STABLE_MODELS);

        Set<String> uniqueModels = new LinkedHashSet<>(modelsToTry);

        for (String model : uniqueModels) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                Map<String, Object> requestBody = new LinkedHashMap<>();
                List<Map<String, Object>> contents = new ArrayList<>();
                Map<String, Object> content = new LinkedHashMap<>();
                List<Map<String, Object>> parts = new ArrayList<>();
                Map<String, Object> part = new LinkedHashMap<>();
                part.put("text", prompt);
                parts.add(part);
                content.put("parts", parts);
                contents.add(content);
                requestBody.put("contents", contents);

                Map<String, Object> generationConfig = new LinkedHashMap<>();
                generationConfig.put("temperature", 0.0);
                generationConfig.put("maxOutputTokens", 500);
                requestBody.put("generationConfig", generationConfig);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                log.info("🤖 Calling Gemini AI with model: {}", model);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    if (!model.equals(currentModel)) {
                        currentModel = model;
                        log.info("🔄 Switched to model: {}", currentModel);
                    }
                    log.info("✅ Gemini API call successful");
                    return response.getBody();
                }

            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg.contains("429")) {
                    log.warn("⏳ Model {} rate limited, trying next...", model);
                } else {
                    log.warn("Model {} failed: {}", model, errorMsg);
                }
            }
        }

        log.warn("⚠️ All Gemini models unavailable");
        return null;
    }

    /**
     * Génère une requête SQL avec l'API Gemini (VRAIE IA)
     */
    /**
     * Génère une requête SQL avec l'API Gemini (VRAIE IA)
     */
    /*
    private String generateSQLWithGemini(String userQuestion) {
        // Vérifier le cache SQL
        String cacheKey = normalizeQuestion(userQuestion);
        if (sqlCache.containsKey(cacheKey)) {
            log.info("📦 [SQL CACHE HIT] Using cached SQL for: {}", userQuestion);
            return sqlCache.get(cacheKey);
        }

        String prompt = String.format("""
            Tu es un expert SQL PostgreSQL. Voici le schéma COMPLET de la base de données:
            
            ===================================================
            TABLE conventions
            ===================================================
            - id BIGINT PRIMARY KEY
            - reference_convention VARCHAR(50) UNIQUE
            - libelle VARCHAR(255) NOT NULL
            - date_debut DATE NOT NULL
            - date_fin DATE
            - montant_ttc DECIMAL(15,2)
            - etat VARCHAR(20) ('PLANIFIE', 'EN COURS', 'TERMINE', 'ARCHIVE')
            - archived BOOLEAN DEFAULT false
            
            TABLE factures
            - numero_facture VARCHAR(50) UNIQUE
            - date_echeance DATE
            - montant_ttc DECIMAL(15,2)
            - statut_paiement VARCHAR(20) ('PAYE', 'NON_PAYE', 'EN_RETARD')
            - archived BOOLEAN DEFAULT false
            
            TABLE applications
            - code VARCHAR(50) UNIQUE
            - name VARCHAR(255)
            - client_name VARCHAR(255)
            - status VARCHAR(20) ('PLANIFIE', 'EN_COURS', 'TERMINE')
            - archived BOOLEAN DEFAULT false
            
            DATE AUJOURD'HUI: %s
            
            QUESTION: "%s"
            
            RÈGLES:
            1. Réponds UNIQUEMENT avec la requête SQL, rien d'autre
            2. Toujours ajouter archived = false
            3. Utilise TO_CHAR(date, 'DD/MM/YYYY') pour les dates
            4. La requête doit être COMPLÈTE et se terminer correctement
            
            REQUÊTE SQL COMPLÈTE:
            """, LocalDate.now(), userQuestion);

        String responseBody = callGeminiWithFallback(prompt);

        if (responseBody == null) {
            log.warn("Gemini API failed, will use fallback pattern");
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String sqlQuery = root
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();

            sqlQuery = sqlQuery.trim()
                    .replaceAll("```sql\\n?", "")
                    .replaceAll("```\\n?", "")
                    .replaceAll("^`|`$", "")
                    .replaceAll("\n", " ")
                    .replaceAll("\\s+", " ");

            // Nettoyer les fonctions TO_CHAR incomplètes
            if (sqlQuery.contains("TO_CHAR(") && !sqlQuery.contains("'DD/MM/YYYY'")) {
                sqlQuery = sqlQuery.replaceAll("TO_CHAR\\(([^,)]+)\\)", "TO_CHAR($1, 'DD/MM/YYYY')");
            }

            log.info("✅ SQL généré par GEMINI (brut): {}", sqlQuery);

            // CORRECTION: Vérifier et corriger les requêtes incomplètes
            sqlQuery = fixIncompleteSQL(sqlQuery, userQuestion);

            if (sqlQuery == null) {
                log.warn("Generated SQL is invalid, using fallback");
                return null;
            }

            log.info("✅ SQL généré par GEMINI (corrigé): {}", sqlQuery);

            if (!sqlQuery.toUpperCase().startsWith("SELECT")) {
                log.warn("Generated non-SELECT query, using fallback");
                return null;
            }

            // Mettre en cache SQL
            sqlCache.put(cacheKey, sqlQuery);
            log.info("💾 SQL cached for question: {}", userQuestion);

            return sqlQuery;

        } catch (Exception e) {
            log.error("❌ Error parsing Gemini response: {}", e.getMessage());
            return null;
        }
    }


     */

    /**
     * Génère une requête SQL avec l'API Gemini (VRAIE IA)
     */
    private String generateSQLWithGemini(String userQuestion) {
        // Vérifier le cache SQL
        String cacheKey = normalizeQuestion(userQuestion);
        if (sqlCache.containsKey(cacheKey)) {
            log.info("📦 [SQL CACHE HIT] Using cached SQL for: {}", userQuestion);
            return sqlCache.get(cacheKey);
        }

        String prompt = String.format("""
            Tu es un expert SQL PostgreSQL. Voici le schéma:
            
            TABLE conventions (
                reference_convention VARCHAR(50),
                libelle VARCHAR(255),
                date_debut DATE,
                date_fin DATE,
                montant_ttc DECIMAL(15,2),
                etat VARCHAR(20),
                archived BOOLEAN DEFAULT false
            );
            
            DATE AUJOURD'HUI: %s
            
            QUESTION: "%s"
            
            RÈGLES:
            1. Réponds UNIQUEMENT avec la requête SQL
            2. Toujours ajouter WHERE archived = false
            3. Pour le minimum, utilise ORDER BY montant_ttc ASC LIMIT 1
            4. Pour le maximum, utilise ORDER BY montant_ttc DESC LIMIT 1
            5. Exemple pour "montant le plus faible": 
               SELECT * FROM conventions WHERE archived = false ORDER BY montant_ttc ASC LIMIT 1
            
            REQUÊTE SQL:
            """, LocalDate.now(), userQuestion);

        String responseBody = callGeminiWithFallback(prompt);

        if (responseBody == null) {
            log.warn("Gemini API failed, will use fallback pattern");
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String sqlQuery = root
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();

            sqlQuery = sqlQuery.trim()
                    .replaceAll("```sql\\n?", "")
                    .replaceAll("```\\n?", "")
                    .replaceAll("^`|`$", "")
                    .replaceAll("\n", " ")
                    .replaceAll("\\s+", " ");

            log.info("✅ SQL généré par GEMINI (brut): {}", sqlQuery);

            // CORRECTION: Vérifier et corriger les requêtes incomplètes
            String fixedQuery = fixIncompleteSQL(sqlQuery, userQuestion);

            if (fixedQuery == null || fixedQuery.trim().isEmpty()) {
                log.warn("Generated SQL is invalid, using fallback pattern");
                String patternSQL = generatePatternSQL(userQuestion);
                if (patternSQL != null) {
                    sqlCache.put(cacheKey, patternSQL);
                    return patternSQL;
                }
                return null;
            }

            log.info("✅ SQL généré par GEMINI (corrigé): {}", fixedQuery);

            if (!fixedQuery.toUpperCase().startsWith("SELECT")) {
                log.warn("Generated non-SELECT query, using fallback");
                return null;
            }

            // Mettre en cache SQL
            sqlCache.put(cacheKey, fixedQuery);
            log.info("💾 SQL cached for question: {}", userQuestion);

            return fixedQuery;

        } catch (Exception e) {
            log.error("❌ Error parsing Gemini response: {}", e.getMessage());
            return null;
        }
    }


    /**
     * Normalise la question pour le cache (minuscules, trim, suppression espaces multiples)
     */
    private String normalizeQuestion(String question) {
        return question.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /**
     * Vérifie si la réponse est déjà dans le cache
     */
    private String getCachedAnswer(String question) {
        String cacheKey = normalizeQuestion(question);
        if (answerCache.containsKey(cacheKey)) {
            log.info("📦 [ANSWER CACHE HIT] Returning cached answer for: {}", question);
            return answerCache.get(cacheKey);
        }
        return null;
    }

    /**
     * Met en cache la réponse
     */
    private void cacheAnswer(String question, String answer) {
        String cacheKey = normalizeQuestion(question);
        answerCache.put(cacheKey, answer);
        log.info("💾 Answer cached for question: {}", question);
    }

    /**
     * Vérifie si le résultat de la requête est déjà en cache
     */
    private List<Map<String, Object>> getCachedQueryResult(String sqlQuery) {
        String cacheKey = String.valueOf(sqlQuery.hashCode());
        if (queryResultCache.containsKey(cacheKey)) {
            log.info("📦 [QUERY RESULT CACHE HIT] Using cached results for SQL: {}", sqlQuery);
            return queryResultCache.get(cacheKey);
        }
        return null;
    }

    /**
     * Met en cache le résultat de la requête
     */
    private void cacheQueryResult(String sqlQuery, List<Map<String, Object>> results) {
        String cacheKey = String.valueOf(sqlQuery.hashCode());
        queryResultCache.put(cacheKey, results);
        log.info("💾 Query results cached for SQL hash: {}", cacheKey);
    }

    /**
     * Génération SQL basée sur des patterns (FALLBACK uniquement)
     */
    private String generatePatternSQL(String userQuestion) {
        String q = userQuestion.toLowerCase();

        log.info("🔍 [FALLBACK] Pattern matching for: {}", q);

        // ============ CONVENTIONS ============
        if (q.contains("convention") || q.contains("conventions")) {

            if ((q.contains("montant") && (q.contains("min") || q.contains("faible") || q.contains("petit"))) ||
                    (q.contains("convention") && (q.contains("moins cher") || q.contains("faible")))) {
                return "SELECT reference_convention, libelle, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, montant_ttc, etat FROM conventions WHERE archived = false ORDER BY montant_ttc ASC LIMIT 1";
            }

            if (q.contains("termin") || q.contains("fini")) {
                return "SELECT reference_convention, libelle, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, montant_ttc, etat FROM conventions WHERE etat = 'TERMINE' AND archived = false ORDER BY date_fin DESC";
            }
            if (q.contains("en cours")) {
                return "SELECT reference_convention, libelle, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, montant_ttc, etat FROM conventions WHERE etat = 'EN COURS' AND archived = false ORDER BY date_debut DESC";
            }
            if (q.contains("expire") || q.contains("bientôt")) {
                return "SELECT reference_convention, libelle, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, montant_ttc FROM conventions WHERE date_fin BETWEEN CURRENT_DATE AND CURRENT_DATE + 30 AND archived = false ORDER BY date_fin ASC";
            }
            Pattern convPattern = Pattern.compile("CONV-\\d{4}-\\d{3}");
            Matcher convMatcher = convPattern.matcher(userQuestion);
            if (convMatcher.find()) {
                String ref = convMatcher.group();
                return String.format("SELECT reference_convention, libelle, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, montant_ttc, periodicite, etat, nb_users FROM conventions WHERE reference_convention = '%s' AND archived = false", ref);
            }
        }

        // ============ FACTURES ============
        if (q.contains("facture") || q.contains("factures")) {
            if (q.contains("impay") || q.contains("non pay")) {
                return "SELECT numero_facture, TO_CHAR(date_echeance, 'DD/MM/YYYY') as date_echeance, montant_ttc, statut_paiement FROM factures WHERE statut_paiement IN ('NON_PAYE', 'EN_RETARD') AND archived = false ORDER BY date_echeance ASC";
            }
            if (q.contains("ca") || q.contains("chiffre") || q.contains("revenu")) {
                return "SELECT COALESCE(SUM(montant_ttc), 0) as chiffre_affaires FROM factures WHERE statut_paiement = 'PAYE'";
            }
            Pattern facturePattern = Pattern.compile("FACT-\\d{4}-CONV-\\d{4}-\\d{3}-\\d{3}");
            Matcher factureMatcher = facturePattern.matcher(userQuestion);
            if (factureMatcher.find()) {
                String numero = factureMatcher.group();
                return String.format("SELECT numero_facture, TO_CHAR(date_facturation, 'DD/MM/YYYY') as date_facturation, TO_CHAR(date_echeance, 'DD/MM/YYYY') as date_echeance, montant_ttc, statut_paiement FROM factures WHERE numero_facture = '%s' AND archived = false", numero);
            }
        }

        // ============ APPLICATIONS ============
        if (q.contains("application") || q.contains("applications")) {
            if (q.contains("en cours")) {
                return "SELECT code, name, client_name, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, status FROM applications WHERE status = 'EN_COURS' AND archived = false ORDER BY created_at DESC";
            }
            if (q.contains("chef")) {
                return "SELECT a.code, a.name, a.client_name, a.status, COALESCE(CONCAT(u.first_name, ' ', u.last_name), 'Non assigné') as chef_de_projet FROM applications a LEFT JOIN users u ON a.chef_de_projet_id = u.id WHERE a.archived = false ORDER BY a.created_at DESC";
            }
        }

        return null;
    }

    /**
     * Message d'aide quand rien ne correspond
     */
    private String getHelpMessage() {
        return """
            🤖 **Assistant IA - Ce que je peux faire :**
            
            📋 **Conventions :**
            • "Montre-moi les conventions terminées"
            • "Quelles sont les conventions en cours ?"
            • "Conventions qui expirent bientôt"
            
            📄 **Factures :**
            • "Factures impayées"
            • "Factures payées"
            • "Quel est le chiffre d'affaires ?"
            
            📱 **Applications :**
            • "Applications en cours"
            • "Applications avec leurs chefs de projet"
            """;
    }

    private List<Map<String, Object>> executeSQL(String sqlQuery) {
        try {
            // Vérifier le cache des résultats de requête
            List<Map<String, Object>> cachedResults = getCachedQueryResult(sqlQuery);
            if (cachedResults != null) {
                return cachedResults;
            }

            log.info("🔍 Exécution SQL: {}", sqlQuery);
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery);
            log.info("📊 Résultats: {} ligne(s)", results.size());

            // Mettre en cache les résultats
            cacheQueryResult(sqlQuery, results);

            return results;
        } catch (Exception e) {
            log.error("❌ SQL Error: {}", e.getMessage());
            throw new RuntimeException("Erreur SQL: " + e.getMessage());
        }
    }

    private String formatResults(List<Map<String, Object>> results, String question) {
        if (results.isEmpty()) {
            return "📭 Aucun résultat trouvé pour votre question.";
        }

        StringBuilder sb = new StringBuilder();

        if (results.size() == 1 && results.get(0).size() == 1) {
            Object value = results.get(0).values().iterator().next();
            sb.append(formatValue(value));
        } else {
            int count = 0;
            for (Map<String, Object> row : results) {
                count++;
                sb.append(count).append(". ");
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    String key = formatKey(entry.getKey());
                    String value = formatValue(entry.getValue());
                    sb.append("**").append(key).append("** : ").append(value).append(" | ");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private String formatKey(String key) {
        Map<String, String> translations = new HashMap<>();
        translations.put("referenceconvention", "📋 Réf");
        translations.put("libelle", "🏷️ Libellé");
        translations.put("datedebut", "📅 Début");
        translations.put("datefin", "📅 Fin");
        translations.put("montantttc", "💰 Montant");
        translations.put("etat", "📌 État");
        translations.put("numerofacture", "📄 N° Facture");
        translations.put("dateecheance", "⚠️ Échéance");
        translations.put("statutpaiement", "✅ Statut");
        translations.put("code", "📱 Code");
        translations.put("name", "📝 Nom");
        translations.put("clientname", "👤 Client");
        translations.put("status", "📌 Statut");
        translations.put("chefdeprojet", "👨‍💼 Chef");
        translations.put("chiffre_affaires", "💵 CA");
        return translations.getOrDefault(key.toLowerCase(), key);
    }

    private String formatValue(Object value) {
        if (value == null) return "❌ N/A";
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            return String.format("%,.2f", d);
        }
        if (value instanceof LocalDate) {
            return ((LocalDate) value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return String.valueOf(value);
    }

    /**
     * Affiche les statistiques du cache
     */
    public Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("sqlCacheSize", sqlCache.size());
        stats.put("answerCacheSize", answerCache.size());
        stats.put("queryResultCacheSize", queryResultCache.size());
        return stats;
    }

    /**
     * Vide tout le cache
     */
    public void clearCache() {
        sqlCache.clear();
        answerCache.clear();
        queryResultCache.clear();
        log.info("🗑️ All caches cleared");
    }

    /**
     * Vide le cache pour une question spécifique
     */
    public void clearCacheForQuestion(String question) {
        String cacheKey = normalizeQuestion(question);
        sqlCache.remove(cacheKey);
        answerCache.remove(cacheKey);
        log.info("🗑️ Cache cleared for question: {}", question);
    }

    /**
     * MÉTHODE PRINCIPALE - Avec cache intelligent
     */
    /**
     * MÉTHODE PRINCIPALE - Avec cache intelligent
     */
    public String askQuestion(String question) {
        log.info("========== 🤖 IA GEMINI WITH CACHE ==========");
        log.info("📝 Question: {}", question);

        // 1. Vérifier si la réponse est déjà dans le cache
        String cachedAnswer = getCachedAnswer(question);
        if (cachedAnswer != null) {
            return cachedAnswer;
        }

        // 2. D'ABORD, essayer le pattern SQL (100% fiable)
        String sqlQuery = generatePatternSQL(question);
        boolean patternUsed = true;

        // 3. Si le pattern n'a pas trouvé, essayer l'API Gemini
        if (sqlQuery == null && apiKey != null && !apiKey.isEmpty()) {
            log.info("🚀 No pattern match, attempting to use Gemini AI...");
            sqlQuery = generateSQLWithGemini(question);
            patternUsed = false;
        }

        // 4. Si toujours pas de requête, afficher l'aide
        if (sqlQuery == null) {
            log.info("📝 No SQL generated, showing help");
            String helpMessage = getHelpMessage();
            cacheAnswer(question, helpMessage);
            return helpMessage;
        }

        try {
            log.info("📝 SQL exécuté: {}", sqlQuery);
            List<Map<String, Object>> results = executeSQL(sqlQuery);
            String answer = formatResults(results, question);

            // Mettre en cache la réponse
            cacheAnswer(question, answer);

            if (!patternUsed) {
                log.info("✅ Réponse générée par GEMINI AI et mise en cache");
            } else {
                log.info("✅ Réponse générée par PATTERN et mise en cache");
            }

            return answer;
        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return "❌ " + e.getMessage();
        }
    }


    public String getCurrentModel() {
        return currentModel;
    }

    public boolean isGeminiAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }


    /**
     * Corrige les requêtes SQL incomplètes (coupées par l'API)
     */
    /*private String fixIncompleteSQL(String sqlQuery, String userQuestion) {
        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            return null;
        }

        String q = userQuestion.toLowerCase();

        // Si la requête est déjà complète et se termine correctement
        if (sqlQuery.toUpperCase().trim().endsWith(";") ||
                sqlQuery.toUpperCase().matches(".*\\bFROM\\b.*\\w+$")) {
            return sqlQuery;
        }

        log.warn("⚠️ SQL query seems incomplete: {}", sqlQuery);

        // Cas 1: Requête coupée après TO_CHAR(
        if (sqlQuery.contains("TO_CHAR(") && !sqlQuery.contains("'DD/MM/YYYY'")) {
            if (q.contains("convention") && (q.contains("termin") || q.contains("fini"))) {
                return "SELECT reference_convention, libelle, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, montant_ttc, etat FROM conventions WHERE etat = 'TERMINE' AND archived = false ORDER BY date_fin DESC";
            }
            if (q.contains("convention") && q.contains("en cours")) {
                return "SELECT reference_convention, libelle, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, montant_ttc, etat FROM conventions WHERE etat = 'EN COURS' AND archived = false ORDER BY date_debut DESC";
            }
            if (q.contains("facture") && (q.contains("impay") || q.contains("non pay"))) {
                return "SELECT numero_facture, TO_CHAR(date_echeance, 'DD/MM/YYYY') as date_echeance, montant_ttc, statut_paiement FROM factures WHERE statut_paiement IN ('NON_PAYE', 'EN_RETARD') AND archived = false ORDER BY date_echeance ASC";
            }
        }

        // Cas 2: Requête très courte ou vide
        if (sqlQuery.length() < 20) {
            if (q.contains("convention") && (q.contains("termin") || q.contains("fini"))) {
                return "SELECT reference_convention, libelle, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, montant_ttc, etat FROM conventions WHERE etat = 'TERMINE' AND archived = false ORDER BY date_fin DESC";
            }
            if (q.contains("convention") && q.contains("en cours")) {
                return "SELECT reference_convention, libelle, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, montant_ttc, etat FROM conventions WHERE etat = 'EN COURS' AND archived = false ORDER BY date_debut DESC";
            }
            if (q.contains("facture") && (q.contains("impay") || q.contains("non pay"))) {
                return "SELECT numero_facture, TO_CHAR(date_echeance, 'DD/MM/YYYY') as date_echeance, montant_ttc, statut_paiement FROM factures WHERE statut_paiement IN ('NON_PAYE', 'EN_RETARD') AND archived = false ORDER BY date_echeance ASC";
            }
            if (q.contains("application") && q.contains("en cours")) {
                return "SELECT code, name, client_name, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, status FROM applications WHERE status = 'EN_COURS' AND archived = false ORDER BY created_at DESC";
            }
        }

        return sqlQuery;
    }

     */


    /**
     * Corrige les requêtes SQL incomplètes (coupées par l'API)
     */
    private String fixIncompleteSQL(String sqlQuery, String userQuestion) {
        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            return null;
        }

        String q = userQuestion.toLowerCase();
        String sqlUpper = sqlQuery.toUpperCase();

        // Si la requête est déjà complète
        if (sqlUpper.contains("LIMIT") ||
                (sqlUpper.contains("ORDER BY") && sqlUpper.contains("ASC") && !sqlUpper.endsWith("_")) ||
                (sqlUpper.contains("ORDER BY") && sqlUpper.contains("DESC") && !sqlUpper.endsWith("_"))) {
            // Vérifier si la requête est vraiment complète
            if (!sqlUpper.endsWith("_") && !sqlUpper.endsWith(",") && sqlUpper.length() > 50) {
                return sqlQuery;
            }
        }

        log.warn("⚠️ SQL query incomplete, fixing: {}", sqlQuery);

        // ============ CORRECTIONS SPÉCIFIQUES ============

        // Cas: ORDER BY montant_ (coupé après montant_)
        if (sqlQuery.contains("ORDER BY montant_") && !sqlQuery.contains("ASC") && !sqlQuery.contains("DESC")) {
            // Déterminer si c'est pour min ou max
            if (q.contains("min") || q.contains("faible") || q.contains("petit")) {
                return sqlQuery + "ASC LIMIT 1";
            } else if (q.contains("max") || q.contains("élevé") || q.contains("grand")) {
                return sqlQuery + "DESC LIMIT 1";
            }
            return sqlQuery + "ASC LIMIT 1";
        }

        // Cas: ORDER BY montant_ttc (coupé après la colonne)
        if (sqlQuery.contains("ORDER BY montant_ttc") && !sqlQuery.contains("ASC") && !sqlQuery.contains("DESC")) {
            if (q.contains("min") || q.contains("faible") || q.contains("petit")) {
                return sqlQuery + " ASC LIMIT 1";
            } else if (q.contains("max") || q.contains("élevé") || q.contains("grand")) {
                return sqlQuery + " DESC LIMIT 1";
            }
            return sqlQuery + " ASC LIMIT 1";
        }

        // Cas: Requête qui devrait avoir LIMIT
        if ((sqlQuery.contains("ORDER BY") && !sqlQuery.contains("LIMIT")) &&
                (q.contains("plus") || q.contains("moins") || q.contains("max") || q.contains("min") ||
                        q.contains("élevé") || q.contains("faible") || q.contains("grand") || q.contains("petit"))) {
            if (q.contains("min") || q.contains("faible") || q.contains("petit")) {
                return sqlQuery + " ASC LIMIT 1";
            } else {
                return sqlQuery + " DESC LIMIT 1";
            }
        }

        // Cas: TO_CHAR sans fermeture
        if (sqlQuery.contains("TO_CHAR(") && !sqlQuery.contains("'DD/MM/YYYY')")) {
            sqlQuery = sqlQuery.replaceAll("TO_CHAR\\(([^,)]+)\\)", "TO_CHAR($1, 'DD/MM/YYYY')");
            // Vérifier à nouveau si complet
            if (!sqlQuery.contains("ORDER BY") || (sqlQuery.contains("ORDER BY") && !sqlQuery.endsWith("_"))) {
                return sqlQuery;
            }
        }

        // ============ FALLBACK: Utiliser le pattern SQL ============
        String fallbackSQL = generatePatternSQL(userQuestion);
        if (fallbackSQL != null) {
            log.info("📝 Using fallback pattern SQL instead of incomplete Gemini SQL");
            return fallbackSQL;
        }

        return sqlQuery;
    }
}