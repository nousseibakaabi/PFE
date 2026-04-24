package com.example.back.service;

import com.example.back.entity.User;
import com.example.back.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;


@Service
@Slf4j
public class ChatAIService {

    @Value("${gemini.api.key:}")
    private String apiKey;


    private final UserRepository userRepository;
    

    private final JdbcTemplate jdbcTemplate;

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    private String currentModel = "gemini-2.0-flash";


    private final Semaphore rateLimiter = new Semaphore(3);


    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();


    // Cache pour les requêtes SQL (clé = question, valeur = SQL généré)
    private final Map<String, String> sqlCache = new ConcurrentHashMap<>();

    // Cache pour les réponses complètes (clé = question, valeur = réponse formatée)
    private final Map<String, String> answerCache = new ConcurrentHashMap<>();

    // Cache pour les résultats de requêtes (clé = SQL hash, valeur = résultats)
    private final Map<String, List<Map<String, Object>>> queryResultCache = new ConcurrentHashMap<>();

    private final List<String> models = Arrays.asList(
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-flash-latest",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
    );

    public ChatAIService(UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

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


    private boolean canMakeRequest(String model) {
        long now = System.currentTimeMillis();
        Long lastTime = lastRequestTime.get(model);
        // Minimum 500ms between requests
        long MIN_INTERVAL_MS = 500;
        if (lastTime == null || (now - lastTime) >= MIN_INTERVAL_MS) {
            lastRequestTime.put(model, now);
            return true;
        }
        return false;
    }

    private String callGeminiWithFallback(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("No API key, cannot call Gemini");
            return null;
        }

        List<String> modelsToTry = new ArrayList<>();
        modelsToTry.add(currentModel);
        modelsToTry.addAll(models);

        Set<String> uniqueModels = new LinkedHashSet<>(modelsToTry);

        for (String model : uniqueModels) {
            // Check rate limit
            if (!canMakeRequest(model)) {
                log.warn("⏳ Rate limited for model: {}", model);
                continue;
            }

            try {

                rateLimiter.acquire();
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
                generationConfig.put("maxOutputTokens", 1500);
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
         } finally {
            rateLimiter.release();
        }
        }

        log.warn("⚠️ All Gemini models unavailable");
        return null;
    }

    private String generateSQLWithGemini(String userQuestion) {
        String cacheKey = normalizeQuestion(userQuestion);
        if (sqlCache.containsKey(cacheKey)) {
            log.info("📦 [SQL CACHE HIT] Using cached SQL for: {}", userQuestion);
            return sqlCache.get(cacheKey);
        }

        String prompt = String.format("""
                Tu es un expert SQL PostgreSQL. Voici le schéma COMPLET:
                
                TABLE conventions (
                    id BIGINT PRIMARY KEY,
                    reference_convention VARCHAR(50) UNIQUE,
                    libelle VARCHAR(255) NOT NULL,
                    date_debut DATE NOT NULL,
                    date_fin DATE,
                    montant_ttc DECIMAL(15,2),
                    etat VARCHAR(20) CHECK (etat IN ('PLANIFIE', 'EN_COURS', 'TERMINE', 'ARCHIVE')),
                    archived BOOLEAN DEFAULT false
                );
                
                TABLE factures (
                    id BIGINT PRIMARY KEY,
                    numero_facture VARCHAR(50) UNIQUE,
                    date_echeance DATE,
                    montant_ttc DECIMAL(15,2),
                    statut_paiement VARCHAR(20) CHECK (statut_paiement IN ('PAYE', 'NON_PAYE', 'EN_RETARD')),
                    archived BOOLEAN DEFAULT false
                );
                
                TABLE applications (
                    id BIGINT PRIMARY KEY,
                    code VARCHAR(50) UNIQUE,
                    name VARCHAR(255),
                    client_name VARCHAR(255),
                    date_debut DATE,
                    date_fin DATE,
                    status VARCHAR(20) CHECK (status IN ('PLANIFIE', 'EN_COURS', 'TERMINE')),
                    chef_de_projet_id BIGINT,
                    archived BOOLEAN DEFAULT false
                );
                
                TABLE users (
                    id BIGINT PRIMARY KEY,
                    username VARCHAR(50) UNIQUE,
                    email VARCHAR(100) UNIQUE,
                    first_name VARCHAR(50),
                    last_name VARCHAR(50)
                );
                
                DATE AUJOURD'HUI: %s
                
                QUESTION: "%s"
                
                RÈGLES IMPORTANTES:
                1. Réponds UNIQUEMENT avec la requête SQL complète
                2. TOUJOURS ajouter WHERE archived = false
                3. Utiliser TO_CHAR(date, 'DD/MM/YYYY') pour les dates
                4. Pour le minimum, utiliser ORDER BY montant_ttc ASC LIMIT 1
                5. Pour le maximum, utiliser ORDER BY montant_ttc DESC LIMIT 1
                6. La requête doit être complète et se terminer par un point-virgule
                
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


    private String normalizeQuestion(String question) {
        return question.toLowerCase().trim().replaceAll("\\s+", " ");
    }


    private String getCachedAnswer(String question) {
        String cacheKey = normalizeQuestion(question);
        if (answerCache.containsKey(cacheKey)) {
            log.info("📦 [ANSWER CACHE HIT] Returning cached answer for: {}", question);
            return answerCache.get(cacheKey);
        }
        return null;
    }

    private void cacheAnswer(String question, String answer) {
        String cacheKey = normalizeQuestion(question);
        answerCache.put(cacheKey, answer);
        log.info("💾 Answer cached for question: {}", question);
    }


    private List<Map<String, Object>> getCachedQueryResult(String sqlQuery) {
        String cacheKey = String.valueOf(sqlQuery.hashCode());
        if (queryResultCache.containsKey(cacheKey)) {
            log.info("📦 [QUERY RESULT CACHE HIT] Using cached results for SQL: {}", sqlQuery);
            return queryResultCache.get(cacheKey);
        }
        return Collections.emptyList();
    }


    private void cacheQueryResult(String sqlQuery, List<Map<String, Object>> results) {
        String cacheKey = String.valueOf(sqlQuery.hashCode());
        queryResultCache.put(cacheKey, results);
        log.info("💾 Query results cached for SQL hash: {}", cacheKey);
    }

    private String generatePatternSQL(String userQuestion) {
        String q = userQuestion.toLowerCase();

        // Expanded pattern matching for common questions
        Map<String, String> patterns = new HashMap<>();

        if ((q.contains("facture") || q.contains("factures")) &&
                (q.contains("plus faible") || q.contains("minimum") || q.contains("min"))) {
            return "SELECT numero_facture, TO_CHAR(date_echeance, 'DD/MM/YYYY') as date_echeance, montant_ttc, statut_paiement FROM factures WHERE archived = false ORDER BY montant_ttc ASC LIMIT 1";
        }
        // Conventions
        patterns.put(".*(convention|conventions).*(termin|fini).*",
                "SELECT reference_convention, libelle, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, montant_ttc, etat FROM conventions WHERE etat = 'TERMINE' AND archived = false ORDER BY date_fin DESC");

        patterns.put(".*(convention|conventions).*(en cours|actif).*",
                "SELECT reference_convention, libelle, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, montant_ttc, etat FROM conventions WHERE etat = 'EN_COURS' AND archived = false ORDER BY date_debut DESC");

        patterns.put(".*(convention|conventions).*(montant|prix|coût).*(min|faible|petit).*",
                "SELECT reference_convention, libelle, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, montant_ttc, etat FROM conventions WHERE archived = false ORDER BY montant_ttc ASC LIMIT 1");

        patterns.put(".*(convention|conventions).*(montant|prix|coût).*(max|élevé|grand).*",
                "SELECT reference_convention, libelle, TO_CHAR(date_debut, 'DD/MM/YYYY') as date_debut, TO_CHAR(date_fin, 'DD/MM/YYYY') as date_fin, montant_ttc, etat FROM conventions WHERE archived = false ORDER BY montant_ttc DESC LIMIT 1");

        // Factures
        patterns.put(".*(facture|factures).*(impay|non pay).*",
                "SELECT numero_facture, TO_CHAR(date_echeance, 'DD/MM/YYYY') as date_echeance, montant_ttc, statut_paiement FROM factures WHERE statut_paiement IN ('NON_PAYE', 'EN_RETARD') AND archived = false ORDER BY date_echeance ASC");

        patterns.put(".*(ca|chiffre|revenu).*",
                "SELECT COALESCE(SUM(montant_ttc), 0) as chiffre_affaires FROM factures WHERE statut_paiement = 'PAYE' AND archived = false");

        // Match pattern
        for (Map.Entry<String, String> entry : patterns.entrySet()) {
            if (q.matches(entry.getKey())) {
                log.info("✅ Pattern matched: {}", entry.getKey());
                return entry.getValue();
            }
        }

        return null;
    }

    private String getHelpMessage() {
        return """
        🤖 Assistant IA - Ce que je peux faire :
        
        📋 Conventions :
        • "Montre-moi les conventions terminées"
        • "Quelles sont les conventions en cours ?"
        • "Conventions qui expirent bientôt"
        • "Quelle est la convention avec le montant le plus faible ?"
        
        📄 Factures :
        • "Factures impayées"
        • "Factures payées"
        • "Quel est le chiffre d'affaires ?"
        • "Quelle est la facture avec le montant le plus faible ?"
        • "Quelle est la facture avec le montant le plus élevé ?"
        
        📱 Applications :
        • "Applications en cours"
        • "Applications avec leurs chefs de projet"
        
        💡 Si ma réponse ne correspond pas à votre question, essayez de reformuler.
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

        String q = question.toLowerCase();

        // ============ CONVENTIONS QUI EXPIREENT ============
        if ((q.contains("convention") || q.contains("conventions")) &&
                (q.contains("expire") || q.contains("bientôt") || q.contains("ce mois"))) {

            int count = results.size();
            if (count == 1) {
                Map<String, Object> row = results.get(0);
                String ref = getStringValue(row, "reference_convention");
                String libelle = getStringValue(row, "libelle");
                String dateFin = getStringValue(row, "date_fin");
                String montant = getFormattedMontant(row, "montant_ttc");

                return String.format(
                        "📋 Il y a une convention qui expire ce mois-ci :\n" +
                                "La convention %s (%s) expire le %s avec un montant de %s TND.",
                        ref, libelle, dateFin, montant
                );
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("📋 Il y a %d conventions qui expirent ce mois-ci :\n\n", count));
                for (int i = 0; i < results.size(); i++) {
                    Map<String, Object> row = results.get(i);
                    String ref = getStringValue(row, "reference_convention");
                    String libelle = getStringValue(row, "libelle");
                    String dateFin = getStringValue(row, "date_fin");
                    String montant = getFormattedMontant(row, "montant_ttc");
                    sb.append(String.format("%d. La convention %s (%s) expire le %s (Montant: %s TND)\n",
                            i + 1, ref, libelle, dateFin, montant));
                }
                return sb.toString();
            }
        }

        // ============ CONVENTIONS TERMINÉES ============
        if ((q.contains("convention") || q.contains("conventions")) &&
                (q.contains("termin") || q.contains("fini"))) {

            int count = results.size();
            if (count == 1) {
                Map<String, Object> row = results.get(0);
                String ref = getStringValue(row, "reference_convention");
                String libelle = getStringValue(row, "libelle");
                String dateFin = getStringValue(row, "date_fin");
                String montant = getFormattedMontant(row, "montant_ttc");

                return String.format(
                        "✅ Une convention terminée :\n\nLa convention %s (%s) s'est terminée le %s avec un montant de %s TND.",
                        ref, libelle, dateFin, montant
                );
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("✅ %d conventions terminées :\n\n", count));
                for (int i = 0; i < results.size(); i++) {
                    Map<String, Object> row = results.get(i);
                    String ref = getStringValue(row, "reference_convention");
                    String libelle = getStringValue(row, "libelle");
                    String dateFin = getStringValue(row, "date_fin");
                    String montant = getFormattedMontant(row, "montant_ttc");
                    sb.append(String.format("%d. %s - %s (Terminée le %s, %s TND)\n",
                            i + 1, ref, libelle, dateFin, montant));
                }
                return sb.toString();
            }
        }

        // ============ FACTURES IMPAYÉES ============
        if ((q.contains("facture") || q.contains("factures")) &&
                (q.contains("impay") || q.contains("non pay"))) {

            int count = results.size();

            BigDecimal totalMontant = BigDecimal.ZERO;
            for (Map<String, Object> row : results) {
                Object montant = row.get("montant_ttc");
                if (montant instanceof Number) {
                    totalMontant = totalMontant.add(BigDecimal.valueOf(((Number) montant).doubleValue()));
                }
            }

            if (count == 1) {
                Map<String, Object> row = results.get(0);
                String numero = getStringValue(row, "numero_facture");
                String dateEcheance = getStringValue(row, "date_echeance");
                String montant = getFormattedMontant(row, "montant_ttc");

                return String.format(
                        "⚠️ Une facture impayée :\n\nLa facture %s est due le %s pour un montant de %s TND.",
                        numero, dateEcheance, montant
                );
            } else {
                return String.format(
                        "⚠️ %d factures impayées pour un montant total de %.2f TND.\n" +
                                "💡 Pensez à les régler avant la date d'échéance.",
                        count, totalMontant
                );
            }
        }

        // ============ CHIFFRE D'AFFAIRES ============
        if (q.contains("ca") || q.contains("chiffre") || q.contains("revenu")) {
            Map<String, Object> row = results.get(0);
            Object ca = row.get("chiffre_affaires");
            double valeur = ca instanceof Number ? ((Number) ca).doubleValue() : 0;
            return String.format("💵 Le chiffre d'affaires total est de %.2f TND.", valeur);
        }

        // ============ CONVENTIONS EN COURS ============
        if ((q.contains("convention") || q.contains("conventions")) && q.contains("en cours")) {
            int count = results.size();
            return String.format("📋 Il y a actuellement %d convention(s) en cours.", count);
        }

        // ============ APPLICATIONS EN COURS ============
        if ((q.contains("application") || q.contains("applications")) && q.contains("en cours")) {
            int count = results.size();
            return String.format("📱 Il y a actuellement %d application(s) en cours.", count);
        }

        // ============ FORMAT PAR DÉFAUT POUR LES AUTRES QUESTIONS ============
        if (results.size() == 1) {
            Map<String, Object> row = results.get(0);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = formatKey(entry.getKey());
                String value = formatValue(entry.getValue());
                sb.append(key).append(" : ").append(value).append("\n");
            }
            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("📊 Résultats (%d élément(s)) :\n\n", results.size()));
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> row = results.get(i);
                sb.append(i + 1).append(". ");
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    sb.append(entry.getValue()).append(" ");
                }
                sb.append("\n");
            }
            return sb.toString();
        }
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


    public Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("sqlCacheSize", sqlCache.size());
        stats.put("answerCacheSize", answerCache.size());
        stats.put("queryResultCacheSize", queryResultCache.size());
        return stats;
    }


    public void clearCache() {
        sqlCache.clear();
        answerCache.clear();
        queryResultCache.clear();
        log.info("🗑️ All caches cleared");
    }


    public void clearCacheForQuestion(String question) {
        String cacheKey = normalizeQuestion(question);
        sqlCache.remove(cacheKey);
        answerCache.remove(cacheKey);
        log.info("🗑️ Cache cleared for question: {}", question);
    }


    public String askQuestion(String question) {
        long startTime = System.currentTimeMillis();
        log.info("========== 🤖 IA GEMINI WITH CACHE ==========");
        log.info("📝 Question: {}", question);

        // 1. Vérifier si c'est une salutation
        if (isGreeting(question)) {
            String userName = getCurrentUserName();
            String greetingResponse = getGreetingResponse(userName);
            log.info("✅ Réponse de salutation générée");

            // Add delay before returning
            addDelay(startTime);
            return greetingResponse;
        }

        // 2. Vérifier si la réponse est déjà dans le cache
        String cachedAnswer = getCachedAnswer(question);
        if (cachedAnswer != null) {
            log.info("📦 [ANSWER CACHE HIT] Returning cached answer");
            addDelay(startTime);
            return cachedAnswer;
        }

        // 3. D'ABORD, essayer le pattern SQL (100% fiable)
        String sqlQuery = generatePatternSQL(question);
        boolean patternUsed = true;

        // 4. Si le pattern n'a pas trouvé, essayer l'API Gemini
        if (sqlQuery == null && apiKey != null && !apiKey.isEmpty()) {
            log.info("🚀 No pattern match, attempting to use Gemini AI...");
            sqlQuery = generateSQLWithGemini(question);
            patternUsed = false;
        }

        // 5. Si toujours pas de requête, afficher l'aide
        if (sqlQuery == null) {
            log.info("📝 No SQL generated, showing help");
            String helpMessage = getHelpMessage();
            cacheAnswer(question, helpMessage);
            addDelay(startTime);
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

            // Add delay before returning
            addDelay(startTime);
            return answer;

        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            addDelay(startTime);
            return "❌ " + e.getMessage();
        }
    }

    // Helper method to add delay
    private void addDelay(long startTime) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        long delayNeeded = Math.max(0, 3000 - elapsedTime);

        if (delayNeeded > 0) {
            log.info("⏱️ Adding {}ms delay to simulate thinking time", delayNeeded);
            try {
                Thread.sleep(delayNeeded);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("✅ Total response time: {}ms", System.currentTimeMillis() - startTime);
    }

    private boolean isGreeting(String question) {
        String q = question.toLowerCase().trim();

        List<String> greetings = Arrays.asList(
                "bonjour", "salut", "coucou", "hello", "bonsoir",
                "hey", "yo", "bienvenue", "bonjour à tous", "bon matin"
        );

        // Supprimer les ponctuations
        q = q.replaceAll("[?,.!;:]", "");

        for (String greeting : greetings) {
            if (q.equals(greeting) || q.startsWith(greeting) || q.contains(greeting)) {
                return true;
            }
        }
        return false;
    }


    public boolean isGeminiAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    private String getGreetingResponse(String userName) {
        String name = (userName != null && !userName.isEmpty()) ? userName : "cher utilisateur";
        return "Bonjour " + name + " ! 👋 Comment puis-je vous aider aujourd'hui ?";
    }

    private String getCurrentUserName() {
        try {
            // Méthode 1: Via le contexte Spring Security
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (user.getFirstName() != null && user.getLastName() != null) {
                        return user.getFirstName() + " " + user.getLastName();
                    } else if (user.getFirstName() != null) {
                        return user.getFirstName();
                    } else if (user.getUsername() != null) {
                        return user.getUsername();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not get current user name: {}", e.getMessage());
        }
        return "cher utilisateur";
    }



    private String getStringValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return "N/A";
        return String.valueOf(value);
    }

    private String getFormattedMontant(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return "0,00";
        if (value instanceof Number) {
            return String.format("%,.2f", ((Number) value).doubleValue());
        }
        return String.valueOf(value);
    }

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

        if ((sqlQuery.contains("ORDER BY") && !sqlQuery.contains("LIMIT")) &&
                (q.contains("plus") || q.contains("moins") || q.contains("max") || q.contains("min") ||
                        q.contains("élevé") || q.contains("faible") || q.contains("grand") || q.contains("petit"))) {
            if (q.contains("min") || q.contains("faible") || q.contains("petit")) {
                return sqlQuery + " ASC LIMIT 1";
            } else {
                return sqlQuery + " DESC LIMIT 1";
            }
        }

        if (sqlQuery.contains("TO_CHAR(") && !sqlQuery.contains("'DD/MM/YYYY')")) {
            sqlQuery = sqlQuery.replaceAll("TO_CHAR\\(([^,)]+)\\)", "TO_CHAR($1, 'DD/MM/YYYY')");
            // Vérifier à nouveau si complet
            if (!sqlQuery.contains("ORDER BY") || (sqlQuery.contains("ORDER BY") && !sqlQuery.endsWith("_"))) {
                return sqlQuery;
            }
        }

        String fallbackSQL = generatePatternSQL(userQuestion);
        if (fallbackSQL != null) {
            log.info("📝 Using fallback pattern SQL instead of incomplete Gemini SQL");
            return fallbackSQL;
        }

        return sqlQuery;
    }


    @Scheduled(fixedRate = 3600000) // Clear cache every hour
    public void cleanExpiredCache() {
        int sqlSize = sqlCache.size();
        int answerSize = answerCache.size();
        int querySize = queryResultCache.size();

        sqlCache.clear();
        answerCache.clear();
        queryResultCache.clear();

        log.info("🧹 Cache cleared - SQL: {}, Answers: {}, Query Results: {}",
                sqlSize, answerSize, querySize);
    }

}