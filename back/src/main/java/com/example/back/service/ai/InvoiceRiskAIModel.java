package com.example.back.service.ai;

import com.example.back.entity.*;
import com.example.back.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InvoiceRiskAIModel {

    private final FactureRepository factureRepository;
    private final ConventionRepository conventionRepository;
    private final StructureRepository structureRepository;
    private final TrainingDataRepository trainingDataRepository;

    private MultiLayerNetwork neuralNetwork;
    private MultiLayerNetwork riskLevelClassifier;

    private Map<String, Double> featureImportance = new ConcurrentHashMap<>();
    private final Map<Long, RiskPrediction> predictionCache = new ConcurrentHashMap<>();

    // Risk levels for classification
    public enum RiskLevel {
        VERY_LOW(0, "✅ Très Faible", "#10b981", 0),
        LOW(1, "🟢 Faible", "#34d399", 1),
        MEDIUM(2, "🟡 Moyen", "#fbbf24", 2),
        HIGH(3, "🟠 Élevé", "#f97316", 3),
        VERY_HIGH(4, "🔴 Très Élevé", "#ef4444", 4),
        CRITICAL(5, "⚠️ Critique", "#dc2626", 5);

        private final int code;
        private final String label;
        private final String color;
        private final int severity;

        RiskLevel(int code, String label, String color, int severity) {
            this.code = code;
            this.label = label;
            this.color = color;
            this.severity = severity;
        }

        public int getCode() { return code; }
        public String getLabel() { return label; }
        public String getColor() { return color; }
        public int getSeverity() { return severity; }

        public static RiskLevel fromCode(int code) {
            for (RiskLevel level : values()) {
                if (level.code == code) return level;
            }
            return MEDIUM;
        }
    }

    // AI Prediction Result
    public static class RiskPrediction {
        private RiskLevel level;
        private double probability;
        private double confidence;
        private int predictedDelayDays;
        private Map<String, Double> featureContributions;
        private List<String> recommendations;
        private LocalDate predictionDate;
        private String explanation;

        public RiskLevel getLevel() { return level; }
        public void setLevel(RiskLevel level) { this.level = level; }
        public double getProbability() { return probability; }
        public void setProbability(double probability) { this.probability = probability; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public int getPredictedDelayDays() { return predictedDelayDays; }
        public void setPredictedDelayDays(int predictedDelayDays) { this.predictedDelayDays = predictedDelayDays; }
        public Map<String, Double> getFeatureContributions() { return featureContributions; }
        public void setFeatureContributions(Map<String, Double> featureContributions) { this.featureContributions = featureContributions; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        public LocalDate getPredictionDate() { return predictionDate; }
        public void setPredictionDate(LocalDate predictionDate) { this.predictionDate = predictionDate; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }

    public InvoiceRiskAIModel(FactureRepository factureRepository,
                              ConventionRepository conventionRepository,
                              StructureRepository structureRepository,
                              TrainingDataRepository trainingDataRepository) {
        this.factureRepository = factureRepository;
        this.conventionRepository = conventionRepository;
        this.structureRepository = structureRepository;
        this.trainingDataRepository = trainingDataRepository;
    }


    @PostConstruct
    public void initialize() {
        log.info("Initializing AI Risk Model...");
        try {
            buildNeuralNetwork();

            // Generate training data with CORRECT risk scores
            List<HistoricalPaymentData> trainingData = new ArrayList<>();

            // Add real invoices with calculated risk scores
            List<Facture> allFactures = factureRepository.findAll();
            for (Facture facture : allFactures) {
                HistoricalPaymentData data = extractFeaturesFromFacture(facture);
                if (data.riskScore > 0) {  // Only add if we have a valid risk score
                    trainingData.add(data);
                }
            }

            // Add synthetic data for balance
            trainingData.addAll(generateBalancedSyntheticData());

            if (!trainingData.isEmpty()) {
                trainModelWithCalculatedRisks(trainingData);
            } else {
                simulateEnhancedTrainingData();
                trainModel();
            }

            log.info("AI Risk Model initialized successfully!");
        } catch (Exception e) {
            log.error("Failed to initialize AI model: {}", e.getMessage(), e);
            initializeFallbackModel();
        }
    }

    private void trainModelWithCalculatedRisks(List<HistoricalPaymentData> trainingData) {
        log.info("Training model with {} records using CALCULATED risk scores", trainingData.size());

        INDArray features = Nd4j.create(trainingData.size(), 15);
        INDArray labels = Nd4j.create(trainingData.size(), 2);

        for (int i = 0; i < trainingData.size(); i++) {
            HistoricalPaymentData data = trainingData.get(i);
            features.putRow(i, extractFeatureVector(data));
            labels.putRow(i, Nd4j.create(new double[]{
                    data.riskScore / 100.0,
                    Math.min(Math.max(data.delayDays, 0), 90) / 90.0
            }));
        }

        for (int epoch = 0; epoch < 200; epoch++) {
            neuralNetwork.fit(features, labels);
        }

        // Train classifier
        INDArray classifierLabels = Nd4j.create(trainingData.size(), 6);
        for (int i = 0; i < trainingData.size(); i++) {
            int levelCode = trainingData.get(i).riskLevel;
            classifierLabels.putRow(i, oneHotEncode(levelCode, 6));
        }

        for (int epoch = 0; epoch < 150; epoch++) {
            riskLevelClassifier.fit(features, classifierLabels);
        }

        log.info("Model training with calculated risks completed!");
    }

    /**
     * Generate balanced synthetic data with proper risk distribution
     */
    private List<HistoricalPaymentData> generateBalancedSyntheticData() {
        List<HistoricalPaymentData> syntheticData = new ArrayList<>();
        Random random = new Random(42);

        // Generate 1000 balanced samples (200 of each risk level)
        int samplesPerLevel = 200;

        // VERY LOW risk (0-10)
        for (int i = 0; i < samplesPerLevel; i++) {
            HistoricalPaymentData data = new HistoricalPaymentData();
            data.paymentOnTimeRate = 0.85 + random.nextDouble() * 0.14;
            data.latePaymentRate = 0.01 + random.nextDouble() * 0.09;
            data.advancePaymentRate = 0.05 + random.nextDouble() * 0.15;
            data.invoiceAmount = 1000 + random.nextDouble() * 10000;
            data.daysUntilDue = 15 + random.nextInt(30);
            data.isRecurring = true;
            data.clientAge = 730 + random.nextInt(1000);
            data.totalConventions = 10 + random.nextInt(20);
            data.averagePaymentDelay = random.nextInt(5);
            data.contractDuration = 365;
            data.nbUsers = 100 + random.nextInt(400);
            data.previousLateCount = random.nextInt(2);
            data.riskScore = 5 + random.nextDouble() * 5;
            data.riskLevel = classifyRiskLevel(data.riskScore);
            data.delayDays = random.nextInt(5);
            syntheticData.add(data);
        }

        // LOW risk (10-25)
        for (int i = 0; i < samplesPerLevel; i++) {
            HistoricalPaymentData data = new HistoricalPaymentData();
            data.paymentOnTimeRate = 0.65 + random.nextDouble() * 0.19;
            data.latePaymentRate = 0.10 + random.nextDouble() * 0.15;
            data.advancePaymentRate = 0.05 + random.nextDouble() * 0.10;
            data.invoiceAmount = 5000 + random.nextDouble() * 20000;
            data.daysUntilDue = 10 + random.nextInt(25);
            data.isRecurring = random.nextBoolean();
            data.clientAge = 365 + random.nextInt(500);
            data.totalConventions = 5 + random.nextInt(15);
            data.averagePaymentDelay = 5 + random.nextInt(10);
            data.contractDuration = 180 + random.nextInt(180);
            data.nbUsers = 50 + random.nextInt(200);
            data.previousLateCount = 2 + random.nextInt(5);
            data.riskScore = 15 + random.nextDouble() * 10;
            data.riskLevel = classifyRiskLevel(data.riskScore);
            data.delayDays = 5 + random.nextInt(10);
            syntheticData.add(data);
        }

        // MEDIUM risk (25-50)
        for (int i = 0; i < samplesPerLevel; i++) {
            HistoricalPaymentData data = new HistoricalPaymentData();
            data.paymentOnTimeRate = 0.40 + random.nextDouble() * 0.24;
            data.latePaymentRate = 0.25 + random.nextDouble() * 0.25;
            data.advancePaymentRate = 0.02 + random.nextDouble() * 0.08;
            data.invoiceAmount = 15000 + random.nextDouble() * 35000;
            data.daysUntilDue = 5 + random.nextInt(20);
            data.isRecurring = random.nextBoolean();
            data.clientAge = 180 + random.nextInt(300);
            data.totalConventions = 2 + random.nextInt(10);
            data.averagePaymentDelay = 15 + random.nextInt(15);
            data.contractDuration = 90 + random.nextInt(180);
            data.nbUsers = 20 + random.nextInt(150);
            data.previousLateCount = 5 + random.nextInt(10);
            data.riskScore = 35 + random.nextDouble() * 15;
            data.riskLevel = classifyRiskLevel(data.riskScore);
            data.delayDays = 15 + random.nextInt(20);
            syntheticData.add(data);
        }

        // HIGH risk (50-75)
        for (int i = 0; i < samplesPerLevel; i++) {
            HistoricalPaymentData data = new HistoricalPaymentData();
            data.paymentOnTimeRate = 0.15 + random.nextDouble() * 0.24;
            data.latePaymentRate = 0.55 + random.nextDouble() * 0.25;
            data.advancePaymentRate = 0.00 + random.nextDouble() * 0.05;
            data.invoiceAmount = 35000 + random.nextDouble() * 65000;
            data.daysUntilDue = -10 + random.nextInt(15); // May be overdue
            data.isRecurring = random.nextBoolean();
            data.clientAge = 90 + random.nextInt(180);
            data.totalConventions = 1 + random.nextInt(5);
            data.averagePaymentDelay = 30 + random.nextInt(25);
            data.contractDuration = 60 + random.nextInt(120);
            data.nbUsers = 10 + random.nextInt(100);
            data.previousLateCount = 10 + random.nextInt(15);
            data.riskScore = 60 + random.nextDouble() * 15;
            data.riskLevel = classifyRiskLevel(data.riskScore);
            data.delayDays = 30 + random.nextInt(30);
            syntheticData.add(data);
        }

        // VERY HIGH / CRITICAL risk (75-100)
        for (int i = 0; i < samplesPerLevel; i++) {
            HistoricalPaymentData data = new HistoricalPaymentData();
            data.paymentOnTimeRate = 0.00 + random.nextDouble() * 0.14;
            data.latePaymentRate = 0.75 + random.nextDouble() * 0.24;
            data.advancePaymentRate = 0.00;
            data.invoiceAmount = 50000 + random.nextDouble() * 150000;
            data.daysUntilDue = -30 + random.nextInt(30); // Overdue
            data.isRecurring = false;
            data.clientAge = 30 + random.nextInt(90);
            data.totalConventions = random.nextInt(3);
            data.averagePaymentDelay = 50 + random.nextInt(40);
            data.contractDuration = 30 + random.nextInt(60);
            data.nbUsers = 5 + random.nextInt(50);
            data.previousLateCount = 15 + random.nextInt(20);
            data.riskScore = 80 + random.nextDouble() * 20;
            data.riskLevel = classifyRiskLevel(data.riskScore);
            data.delayDays = 60 + random.nextInt(60);
            syntheticData.add(data);
        }

        log.info("Generated {} balanced synthetic records ({} per risk level)",
                syntheticData.size(), samplesPerLevel);
        return syntheticData;
    }

    /**
     * Enhanced training data simulation with 10,000 records
     */
    private void simulateEnhancedTrainingData() {
        log.info("Generating ENHANCED simulated training data for AI model...");

        List<HistoricalPaymentData> syntheticData = new ArrayList<>();
        Random random = new Random(42);

        // Generate 10,000 synthetic records (more than before)
        for (int i = 0; i < 10000; i++) {
            HistoricalPaymentData data = new HistoricalPaymentData();

            // More realistic amount distribution (log-normal)
            double amountBase = 1000 + Math.pow(random.nextDouble() * 100, 2) * 5000;
            data.invoiceAmount = Math.min(500000, amountBase);

            // Days until due: -90 to 90 (negative = overdue, positive = future)
            data.daysUntilDue = -30 + random.nextInt(120);

            data.isRecurring = random.nextDouble() < 0.3;
            data.clientAge = random.nextInt(3651);

            // Total conventions based on client age
            if (data.clientAge < 365) {
                data.totalConventions = random.nextInt(5);
            } else if (data.clientAge < 1095) {
                data.totalConventions = random.nextInt(15);
            } else {
                data.totalConventions = 5 + random.nextInt(45);
            }

            // Determine risk level based on days until due and client age
            double riskScore = calculateRiskScoreFromFeatures(data);
            data.riskScore = riskScore;
            data.riskLevel = classifyRiskLevel(riskScore);

            // Set payment rates based on risk level
            if (data.riskLevel <= 1) { // LOW risk
                data.paymentOnTimeRate = 0.85 + random.nextDouble() * 0.14;
                data.latePaymentRate = 0.01 + random.nextDouble() * 0.09;
                data.advancePaymentRate = 0.05 + random.nextDouble() * 0.15;
                data.averagePaymentDelay = random.nextInt(8);
                data.previousLateCount = random.nextInt(3);
                data.delayDays = random.nextInt(10);
            } else if (data.riskLevel <= 2) { // MEDIUM risk
                data.paymentOnTimeRate = 0.50 + random.nextDouble() * 0.30;
                data.latePaymentRate = 0.20 + random.nextDouble() * 0.20;
                data.advancePaymentRate = 0.02 + random.nextDouble() * 0.08;
                data.averagePaymentDelay = 10 + random.nextInt(20);
                data.previousLateCount = 5 + random.nextInt(10);
                data.delayDays = 15 + random.nextInt(25);
            } else { // HIGH/CRITICAL risk
                data.paymentOnTimeRate = 0.10 + random.nextDouble() * 0.20;
                data.latePaymentRate = 0.60 + random.nextDouble() * 0.30;
                data.advancePaymentRate = 0.00 + random.nextDouble() * 0.05;
                data.averagePaymentDelay = 30 + random.nextInt(40);
                data.previousLateCount = 10 + random.nextInt(20);
                data.delayDays = 30 + random.nextInt(60);
            }

            // Contract details
            data.contractDuration = 30 + random.nextInt(701);
            data.nbUsers = 1 + random.nextInt(5000);

            // Seasonal factors
            LocalDate dueDate = generateRandomDueDate(random);
            data.isEndOfMonth = dueDate.getDayOfMonth() == dueDate.lengthOfMonth();
            data.isEndOfQuarter = (dueDate.getMonthValue() % 3 == 0) &&
                    dueDate.getDayOfMonth() == dueDate.lengthOfMonth();
            data.isEndOfYear = dueDate.getMonthValue() == 12 &&
                    dueDate.getDayOfMonth() == 31;

            syntheticData.add(data);
        }

        addEdgeCases(syntheticData);
        saveSyntheticDataToDatabase(syntheticData);

        log.info("Generated {} enhanced synthetic training records", syntheticData.size());
        printTrainingDataStatistics(syntheticData);
    }

    /**
     * Train model with real data (implementation needed)
     */
    private void trainModelWithRealData(List<HistoricalPaymentData> realData) {
        log.info("Training model with {} real records", realData.size());

        // Combine real data with balanced synthetic data
        List<HistoricalPaymentData> trainingData = new ArrayList<>(realData);
        List<HistoricalPaymentData> balancedData = generateBalancedSyntheticData();
        trainingData.addAll(balancedData);

        log.info("Total training data: {} records ({} real + {} synthetic)",
                trainingData.size(), realData.size(), balancedData.size());

        // Prepare training data
        INDArray features = Nd4j.create(trainingData.size(), 15);
        INDArray labels = Nd4j.create(trainingData.size(), 2);

        for (int i = 0; i < trainingData.size(); i++) {
            HistoricalPaymentData data = trainingData.get(i);
            features.putRow(i, extractFeatureVector(data));
            labels.putRow(i, Nd4j.create(new double[]{
                    data.riskScore / 100.0,
                    Math.min(Math.max(data.delayDays, 0), 90) / 90.0
            }));
        }

        // Train the neural network
        for (int epoch = 0; epoch < 150; epoch++) {
            neuralNetwork.fit(features, labels);
        }

        // Train classifier
        INDArray classifierLabels = Nd4j.create(trainingData.size(), 6);
        for (int i = 0; i < trainingData.size(); i++) {
            int levelCode = trainingData.get(i).riskLevel;
            classifierLabels.putRow(i, oneHotEncode(levelCode, 6));
        }

        for (int epoch = 0; epoch < 100; epoch++) {
            riskLevelClassifier.fit(features, classifierLabels);
        }

        calculateFeatureImportance();
        log.info("Model training with real data completed!");
    }



    private void buildNeuralNetwork() {
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(0.001))
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(0, new DenseLayer.Builder()
                        .nIn(15)
                        .nOut(64)
                        .activation(Activation.RELU)
                        .dropOut(0.2)
                        .build())
                .layer(1, new DenseLayer.Builder()
                        .nIn(64)
                        .nOut(32)
                        .activation(Activation.RELU)
                        .dropOut(0.2)
                        .build())
                .layer(2, new DenseLayer.Builder()
                        .nIn(32)
                        .nOut(16)
                        .activation(Activation.RELU)
                        .build())
                .layer(3, new OutputLayer.Builder()
                        .nIn(16)
                        .nOut(2)
                        .activation(Activation.IDENTITY)
                        .lossFunction(LossFunctions.LossFunction.MSE)
                        .build())
                .backpropType(BackpropType.Standard)
                .build();

        neuralNetwork = new MultiLayerNetwork(config);
        neuralNetwork.init();
        neuralNetwork.setListeners(new ScoreIterationListener(100));

        MultiLayerConfiguration classifierConfig = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Adam(0.001))
                .list()
                .layer(0, new DenseLayer.Builder()
                        .nIn(15)
                        .nOut(48)
                        .activation(Activation.RELU)
                        .build())
                .layer(1, new DenseLayer.Builder()
                        .nIn(48)
                        .nOut(24)
                        .activation(Activation.RELU)
                        .build())
                .layer(2, new OutputLayer.Builder()
                        .nIn(24)
                        .nOut(6)
                        .activation(Activation.SOFTMAX)
                        .lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .build())
                .build();

        riskLevelClassifier = new MultiLayerNetwork(classifierConfig);
        riskLevelClassifier.init();
    }

    @Transactional(readOnly = true)
    public void trainModel() {
        log.info("Training AI model with historical payment data...");

        List<HistoricalPaymentData> trainingData = loadHistoricalPaymentData();

        if (trainingData.size() < 100) {
            log.warn("Insufficient training data ({} records). Using simulated training.", trainingData.size());
            simulateTrainingData();
            trainingData = loadHistoricalPaymentData();
        }

        if (trainingData.isEmpty()) {
            log.warn("No training data available after simulation!");
            return;
        }

        INDArray features = Nd4j.create(trainingData.size(), 15);
        INDArray labels = Nd4j.create(trainingData.size(), 2);

        for (int i = 0; i < trainingData.size(); i++) {
            HistoricalPaymentData data = trainingData.get(i);
            features.putRow(i, extractFeatureVector(data));
            labels.putRow(i, Nd4j.create(new double[]{
                    data.riskScore / 100.0,
                    Math.min(data.delayDays, 90) / 90.0
            }));
        }

        for (int epoch = 0; epoch < 100; epoch++) {
            neuralNetwork.fit(features, labels);
        }

        INDArray classifierLabels = Nd4j.create(trainingData.size(), 6);
        for (int i = 0; i < trainingData.size(); i++) {
            int levelCode = trainingData.get(i).riskLevel;
            classifierLabels.putRow(i, oneHotEncode(levelCode, 6));
        }

        for (int epoch = 0; epoch < 80; epoch++) {
            riskLevelClassifier.fit(features, classifierLabels);
        }

        calculateFeatureImportance();

        log.info("Model training completed!");
    }

    private INDArray extractFeatureVector(HistoricalPaymentData data) {
        double[] features = new double[15];

        features[0] = data.paymentOnTimeRate;
        features[1] = data.latePaymentRate;
        features[2] = data.advancePaymentRate;
        features[3] = normalizeAmount(data.invoiceAmount);

        // FIX: For feature 4 (days_until_due), we want:
        // - For future invoices (positive days): value between 0-1 (more days left = lower value)
        // - For overdue invoices (negative days): value = 0
        // - For paid invoices: value = 1 (already paid)
        double daysUntilDueNorm;
        if (data.daysUntilDue > 0) {
            // Future invoice: more days left = less urgency (lower value)
            daysUntilDueNorm = Math.min(data.daysUntilDue, 90) / 90.0;
        } else if (data.daysUntilDue <= 0) {
            // Overdue or due today: maximum urgency
            daysUntilDueNorm = 0.0;
        } else {
            daysUntilDueNorm = 1.0;
        }
        features[4] = daysUntilDueNorm;

        features[5] = data.isRecurring ? 1.0 : 0.0;
        features[6] = Math.min(data.clientAge, 3650) / 3650.0;
        features[7] = Math.min(data.totalConventions, 50) / 50.0;
        features[8] = Math.min(data.averagePaymentDelay, 90) / 90.0;
        features[9] = Math.min(data.contractDuration, 730) / 730.0;
        features[10] = Math.min(data.nbUsers, 1000) / 1000.0;
        features[11] = data.isEndOfMonth ? 1.0 : 0.0;
        features[12] = data.isEndOfQuarter ? 1.0 : 0.0;
        features[13] = data.isEndOfYear ? 1.0 : 0.0;
        features[14] = Math.min(data.previousLateCount, 20) / 20.0;

        return Nd4j.create(new double[][]{features});
    }

    private void calculateFeatureImportance() {
        INDArray weights = neuralNetwork.getLayer(0).getParam("W");

        String[] featureNames = {
                "payment_on_time_rate", "late_payment_rate", "advance_payment_rate",
                "invoice_amount", "days_until_due", "is_recurring",
                "client_age", "total_conventions", "average_payment_delay",
                "contract_duration", "nb_users", "is_end_of_month",
                "is_end_of_quarter", "is_end_of_year", "previous_late_count"
        };

        for (int i = 0; i < 15 && i < weights.rows(); i++) {
            double importance = Math.abs(weights.getDouble(i, 0));
            featureImportance.put(featureNames[i], importance);
        }

        double sum = featureImportance.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            featureImportance.replaceAll((k, v) -> v / sum);
        }
    }

    @Transactional(readOnly = true)
    public RiskPrediction predictInvoiceRisk(Long factureId) {
        if (predictionCache.containsKey(factureId)) {
            RiskPrediction cached = predictionCache.get(factureId);
            if (cached.getPredictionDate().equals(LocalDate.now())) {
                return cached;
            }
        }

        Optional<Facture> factureOpt = factureRepository.findById(factureId);
        if (factureOpt.isEmpty()) {
            return createDefaultPrediction();
        }

        Facture facture = factureOpt.get();

        // EAGERLY fetch lazy relationships within transaction
        Convention convention = facture.getConvention();
        if (convention != null) {
            // Force initialization
            convention.getStructureBeneficiel();
            convention.getStructureResponsable();
            convention.getApplication();
        }

        HistoricalPaymentData features = extractFeaturesFromFacture(facture);
        INDArray featureVector = extractFeatureVector(features);

        // Log the shape for debugging
        log.debug("Feature vector shape: {}", featureVector.shape());

        try {
            // Make sure the neural network exists
            if (neuralNetwork == null || riskLevelClassifier == null) {
                log.warn("Neural network not initialized, using default prediction");
                return createDefaultPrediction();
            }

            INDArray output = neuralNetwork.output(featureVector);
            double predictedRiskScore = output.getDouble(0) * 100;
            int predictedDelayDays = (int) Math.round(output.getDouble(1) * 90);

            INDArray classOutput = riskLevelClassifier.output(featureVector);
            int predictedLevel = argMax(classOutput);
            double confidence = classOutput.getDouble(predictedLevel);

            RiskPrediction prediction = new RiskPrediction();
            prediction.setLevel(RiskLevel.fromCode(predictedLevel));
            prediction.setProbability(confidence);
            prediction.setConfidence(calculateConfidence(features, predictedLevel));
            prediction.setPredictedDelayDays(Math.max(0, predictedDelayDays));
            prediction.setPredictionDate(LocalDate.now());

            Map<String, Double> contributions = calculateFeatureContributions(featureVector, features);
            prediction.setFeatureContributions(contributions);
            prediction.setRecommendations(generateAIRecommendations(prediction, features));
            prediction.setExplanation(generateExplanation(prediction, contributions));

            predictionCache.put(factureId, prediction);

            log.info("AI Prediction for invoice {}: Level={}, Delay={} days, Confidence={}%",
                    facture.getNumeroFacture(), prediction.getLevel().getLabel(), predictedDelayDays,
                    Math.round(prediction.getConfidence() * 100));

            return prediction;
        } catch (Exception e) {
            log.error("Error during prediction: {}", e.getMessage(), e);
            return createDefaultPrediction();
        }
    }

    @Transactional(readOnly = true)
    public List<RiskPrediction> predictClientRisk(Long clientId) {
        List<Facture> clientFactures = factureRepository.findAll().stream()
                .filter(f -> {
                    if (f.getConvention() == null) return false;
                    Convention conv = f.getConvention();
                    // Force initialization
                    Structure beneficiel = conv.getStructureBeneficiel();
                    return beneficiel != null && beneficiel.getId().equals(clientId);
                })
                .filter(f -> !"PAYE".equals(f.getStatutPaiement()))
                .collect(Collectors.toList());

        return clientFactures.stream()
                .map(f -> predictInvoiceRisk(f.getId()))
                .collect(Collectors.toList());
    }

    public RiskPrediction predictNewInvoiceRisk(BigDecimal amount, LocalDate dueDate,
                                                Long clientId, Long applicationId) {
        HistoricalPaymentData syntheticData = buildSyntheticFeatures(amount, dueDate, clientId, applicationId);
        INDArray featureVector = extractFeatureVector(syntheticData);

        // Log the shape for debugging
        log.debug("New invoice feature vector shape: {}", featureVector.shape());

        try {
            if (neuralNetwork == null || riskLevelClassifier == null) {
                log.warn("Neural network not initialized, using default prediction");
                return createDefaultPrediction();
            }

            INDArray output = neuralNetwork.output(featureVector);
            int predictedDelayDays = (int) Math.round(output.getDouble(1) * 90);

            INDArray classOutput = riskLevelClassifier.output(featureVector);
            int predictedLevel = argMax(classOutput);
            double confidence = classOutput.getDouble(predictedLevel);

            RiskPrediction prediction = new RiskPrediction();
            prediction.setLevel(RiskLevel.fromCode(predictedLevel));
            prediction.setProbability(confidence);
            prediction.setConfidence(calculateConfidence(syntheticData, predictedLevel));
            prediction.setPredictedDelayDays(predictedDelayDays);
            prediction.setPredictionDate(LocalDate.now());
            prediction.setRecommendations(generateNewInvoiceRecommendations(predictedLevel, predictedDelayDays, amount));

            return prediction;
        } catch (Exception e) {
            log.error("Error predicting new invoice risk: {}", e.getMessage(), e);
            return createDefaultPrediction();
        }
    }


    @Scheduled(cron = "0 0 * * * *")
    public void autoRetrainModel() {
        log.info("Starting automatic model retraining with fresh data...");
        try {
            trainModel();
            predictionCache.clear();
            log.info("Auto-retraining completed successfully!");
        } catch (Exception e) {
            log.error("Auto-retraining failed: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    protected List<HistoricalPaymentData> loadHistoricalPaymentData() {
        List<HistoricalPaymentData> data = new ArrayList<>();

        // Use JOIN FETCH to eagerly load all relationships
        List<Facture> allFactures = factureRepository.findAllWithConventionAndStructure();
        LocalDate twoYearsAgo = LocalDate.now().minusYears(2);

        for (Facture facture : allFactures) {
            try {
                // Skip if no payment date
                if (facture.getDatePaiement() == null) continue;
                if (!facture.getDatePaiement().isAfter(twoYearsAgo)) continue;
                if (!"PAYE".equals(facture.getStatutPaiement())) continue;

                Convention conv = facture.getConvention();
                if (conv == null) continue;

                // These are already loaded by JOIN FETCH
                Structure client = conv.getStructureBeneficiel();
                if (client == null) continue;

                data.add(extractFeaturesFromFacture(facture));
            } catch (Exception e) {
                log.warn("Error processing facture {}: {}", facture.getId(), e.getMessage());
            }
        }

        log.info("Loaded {} historical payment records from database", data.size());
        return data;
    }



    public HistoricalPaymentData extractFeaturesFromFacture(Facture facture) {
        HistoricalPaymentData data = new HistoricalPaymentData();

        Convention convention = facture.getConvention();
        if (convention == null) {
            return data;
        }

        Structure client = convention.getStructureBeneficiel();

        data.invoiceAmount = facture.getMontantTTC() != null ?
                facture.getMontantTTC().doubleValue() : 0;

        if (facture.getDateEcheance() != null) {
            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), facture.getDateEcheance());
            data.daysUntilDue = (int) daysUntilDue;
        }

        data.isRecurring = convention.getPeriodicite() != null &&
                !"UNIQUE".equals(convention.getPeriodicite());

        if (client != null && client.getCreatedAt() != null) {
            data.clientAge = ChronoUnit.DAYS.between(client.getCreatedAt().toLocalDate(), LocalDate.now());

            List<Convention> clientConventions = conventionRepository.findByStructureBeneficiel(client);
            data.totalConventions = clientConventions != null ? clientConventions.size() : 0;

            List<Facture> clientFactures = new ArrayList<>();
            if (clientConventions != null) {
                for (Convention conv : clientConventions) {
                    List<Facture> convFactures = factureRepository.findByConventionId(conv.getId());
                    if (convFactures != null) {
                        clientFactures.addAll(convFactures);
                    }
                }
            }

            List<Facture> paidClientFactures = clientFactures.stream()
                    .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                    .collect(Collectors.toList());

            if (!paidClientFactures.isEmpty()) {
                long onTimeCount = paidClientFactures.stream()
                        .filter(f -> f.getDatePaiement() != null && f.getDateEcheance() != null &&
                                !f.getDatePaiement().isAfter(f.getDateEcheance()))
                        .count();
                long lateCount = paidClientFactures.size() - onTimeCount;
                long advanceCount = paidClientFactures.stream()
                        .filter(f -> f.getDatePaiement() != null && f.getDateEcheance() != null &&
                                f.getDatePaiement().isBefore(f.getDateEcheance()))
                        .count();

                data.paymentOnTimeRate = (double) onTimeCount / paidClientFactures.size();
                data.latePaymentRate = (double) lateCount / paidClientFactures.size();
                data.advancePaymentRate = (double) advanceCount / paidClientFactures.size();

                data.averagePaymentDelay = paidClientFactures.stream()
                        .filter(f -> f.getDatePaiement() != null && f.getDateEcheance() != null &&
                                f.getDatePaiement().isAfter(f.getDateEcheance()))
                        .mapToLong(f -> ChronoUnit.DAYS.between(f.getDateEcheance(), f.getDatePaiement()))
                        .average()
                        .orElse(0);

                data.previousLateCount = (int) lateCount;
            }
        }

        if (convention.getDateDebut() != null && convention.getDateFin() != null) {
            data.contractDuration = ChronoUnit.DAYS.between(
                    convention.getDateDebut(), convention.getDateFin());
        }
        data.nbUsers = convention.getNbUsers() != null ? convention.getNbUsers() : 0;

        LocalDate dueDate = facture.getDateEcheance();
        if (dueDate != null) {
            data.isEndOfMonth = dueDate.getDayOfMonth() == dueDate.lengthOfMonth();
            data.isEndOfQuarter = (dueDate.getMonthValue() % 3 == 0) &&
                    dueDate.getDayOfMonth() == dueDate.lengthOfMonth();
            data.isEndOfYear = dueDate.getMonthValue() == 12 &&
                    dueDate.getDayOfMonth() == 31;
        }

        // ============ FIXED RISK CALCULATION ============

        if ("PAYE".equals(facture.getStatutPaiement()) && facture.getDatePaiement() != null) {
            // PAID invoice - low risk
            long delayDays = 0;
            if (facture.getDateEcheance() != null) {
                delayDays = Math.max(0, ChronoUnit.DAYS.between(
                        facture.getDateEcheance(), facture.getDatePaiement()));
            }
            data.delayDays = (int) delayDays;

            // Paid invoices get low risk score
            if (delayDays == 0) {
                data.riskScore = 5; // On-time payment
            } else if (delayDays <= 15) {
                data.riskScore = 15; // Slightly late
            } else {
                data.riskScore = 30; // Very late but paid
            }
            data.riskLevel = classifyRiskLevel(data.riskScore);

        } else {
            // UNPAID invoice - calculate HIGH risk based on overdue days and client history

            long daysOverdue = 0;
            if (facture.getDateEcheance() != null && facture.getDateEcheance().isBefore(LocalDate.now())) {
                daysOverdue = ChronoUnit.DAYS.between(facture.getDateEcheance(), LocalDate.now());
            }
            data.delayDays = (int) daysOverdue;

            // NEW RISK FORMULA:
            // Days overdue: up to 40 points
            double daysScore = Math.min(daysOverdue, 40);

            // Client payment history: up to 60 points
            double historyScore = 0;

            // If client has no paid invoices (like client 64), that's a RED FLAG
            if (data.paymentOnTimeRate == 0 && data.latePaymentRate == 0 && data.totalConventions > 0) {
                // Client has conventions but NEVER paid any invoice
                historyScore = 60;
            } else if (data.latePaymentRate > 0.5) {
                // Client pays late more than 50% of the time
                historyScore = 50;
            } else if (data.latePaymentRate > 0.3) {
                // Client pays late 30-50% of the time
                historyScore = 35;
            } else if (data.latePaymentRate > 0.1) {
                // Client pays late 10-30% of the time
                historyScore = 20;
            } else if (data.paymentOnTimeRate > 0.8) {
                // Client pays on time 80%+ of the time
                historyScore = 5;
            } else {
                // Default: based on on-time rate
                historyScore = (1 - data.paymentOnTimeRate) * 40;
            }

            // Calculate total risk score (0-100)
            double riskScore = daysScore + historyScore;

            // Bonus: If invoice is more than 30 days overdue, add extra 10 points
            if (daysOverdue > 30) {
                riskScore += 10;
            }

            // Bonus: If client has 0% payment history (like client 64), ensure high risk
            if (data.paymentOnTimeRate == 0 && data.totalConventions > 0) {
                riskScore = Math.max(riskScore, 75);
            }

            data.riskScore = Math.min(100, riskScore);
            data.riskLevel = classifyRiskLevel(data.riskScore);
        }

        return data;
    }

    // Replace the old calculateRiskScoreFromDelay with this
    private double calculateRiskScoreFromDelay(int delayDays, double latePaymentRate) {
        // This should only be used for PAID invoices now
        if (delayDays == 0) return 5;
        if (delayDays <= 7) return 15;
        if (delayDays <= 30) return 30;
        if (delayDays <= 60) return 50;
        return 70;
    }

    // Update classifyRiskLevel to have more realistic thresholds
    private int classifyRiskLevel(double riskScore) {
        if (riskScore < 15) return 0;  // VERY_LOW (0-14)
        if (riskScore < 30) return 1;  // LOW (15-29)
        if (riskScore < 50) return 2;  // MEDIUM (30-49)
        if (riskScore < 70) return 3;  // HIGH (50-69)
        if (riskScore < 85) return 4;  // VERY_HIGH (70-84)
        return 5;                       // CRITICAL (85-100)
    }



    private double calculateConfidence(HistoricalPaymentData features, int predictedLevel) {
        double confidence = 0.7;
        if (features.totalConventions > 10) confidence += 0.1;
        if (features.previousLateCount > 0) confidence += 0.05;
        if (features.paymentOnTimeRate > 0) confidence += 0.05;
        if (features.totalConventions == 0) confidence -= 0.2;
        return Math.min(0.95, Math.max(0.5, confidence));
    }

    private INDArray oneHotEncode(int value, int numClasses) {
        INDArray encoding = Nd4j.zeros(numClasses);
        encoding.putScalar(value, 1.0);
        return encoding;
    }

    private int argMax(INDArray array) {
        int maxIndex = 0;
        double maxValue = array.getDouble(0);
        for (int i = 1; i < array.length(); i++) {
            double value = array.getDouble(i);
            if (value > maxValue) {
                maxValue = value;
                maxIndex = i;
            }
        }
        return maxIndex;
    }


    private Map<String, Double> calculateFeatureContributions(INDArray featureVector,
                                                              HistoricalPaymentData data) {
        Map<String, Double> contributions = new LinkedHashMap<>();
        contributions.put("💰 Historique de paiement", data.paymentOnTimeRate * 100);
        contributions.put("⏰ Retards antérieurs", data.latePaymentRate * 100);
        contributions.put("📊 Montant de la facture", normalizeAmount(data.invoiceAmount) * 100);

        // FIX: Calculate days until due contribution correctly
        // For paid invoices: 100% (good)
        // For future invoices: percentage of time remaining
        // For overdue: 0% (bad)
        double daysUntilDueContribution;
        if (data.delayDays == 0 && data.paymentOnTimeRate > 0) {
            daysUntilDueContribution = 100.0; // Paid on time
        } else if (data.daysUntilDue > 0) {
            daysUntilDueContribution = (1 - Math.min(data.daysUntilDue, 90) / 90.0) * 100;
        } else {
            daysUntilDueContribution = 0.0; // Overdue
        }
        contributions.put("📅 Délai avant échéance", daysUntilDueContribution);

        contributions.put("🏢 Ancienneté client", Math.min(data.clientAge, 3650) / 3650.0 * 100);
        contributions.put("📑 Nombre de conventions", Math.min(data.totalConventions, 50) / 50.0 * 100);
        return contributions;
    }

    private List<String> generateAIRecommendations(RiskPrediction prediction,
                                                   HistoricalPaymentData features) {
        List<String> recommendations = new ArrayList<>();

        switch (prediction.getLevel()) {
            case VERY_HIGH:
            case CRITICAL:
                recommendations.add("🚨 ACTION IMMÉDIATE REQUISE - Risque de non-paiement très élevé");
                recommendations.add("📞 Contacter le service financier du client dans les 24h");
                recommendations.add("🔒 Envisager la suspension temporaire des services");
                recommendations.add("⚖️ Préparer le dossier pour recouvrement");
                break;
            case HIGH:
                recommendations.add("⚠️ RISQUE ÉLEVÉ - Plan d'action requis");
                recommendations.add("📧 Envoyer une relance personnalisée");
                recommendations.add("📞 Planifier un appel de suivi avec le client");
                break;
            case MEDIUM:
                recommendations.add("📋 RISQUE MOYEN - Surveillance recommandée");
                recommendations.add("⏰ Programmer un rappel automatique à J-7");
                break;
            case LOW:
                recommendations.add("✅ RISQUE FAIBLE - Suivi normal");
                break;
            case VERY_LOW:
                recommendations.add("✨ TRÈS FAIBLE RISQUE - Client fiable");
                break;
        }

        if (prediction.getPredictedDelayDays() > 15) {
            recommendations.add("📅 Risque de retard de " + prediction.getPredictedDelayDays() + " jours");
        }
        if (features.latePaymentRate > 0.3) {
            recommendations.add("📊 Historique de retards détecté (" +
                    String.format("%.0f", features.latePaymentRate * 100) + "%)");
        }
        if (features.totalConventions == 0) {
            recommendations.add("🆕 Nouveau client - Demander un acompte de 30% minimum");
        }

        return recommendations;
    }

    private List<String> generateNewInvoiceRecommendations(int riskLevel, int predictedDelay,
                                                           BigDecimal amount) {
        List<String> recommendations = new ArrayList<>();

        if (riskLevel >= 4) {
            recommendations.add("⚠️ DÉCONSEILLÉ: Risque de non-paiement trop élevé");
            recommendations.add("💡 Proposer un paiement à 100% à la commande");
        } else if (riskLevel == 3) {
            recommendations.add("⚠️ Prudent: Risque élevé détecté");
            recommendations.add("💡 Recommandation: Paiement 50% à la commande, 50% à la livraison");
        } else if (riskLevel == 2) {
            recommendations.add("⚖️ Standard avec surveillance");
            recommendations.add("💡 Proposition: Paiement 30% à la commande, solde à 30 jours");
        } else {
            recommendations.add("✅ Conditions standards acceptables");
        }

        if (amount != null && amount.compareTo(new BigDecimal("50000")) > 0) {
            recommendations.add("💰 Montant élevé - Renforcer les garanties de paiement");
        }

        return recommendations;
    }

    private String generateExplanation(RiskPrediction prediction,
                                       Map<String, Double> contributions) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("L'IA a analysé plusieurs facteurs de risque.\n\n");
        explanation.append("Principaux facteurs contribuant au risque:\n");

        contributions.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> {
                    explanation.append("• ").append(entry.getKey())
                            .append(": ").append(String.format("%.1f", entry.getValue()))
                            .append("%\n");
                });

        explanation.append("\nPrécision: ").append(String.format("%.0f", prediction.getConfidence() * 100)).append("%\n");
        if (prediction.getPredictedDelayDays() > 0) {
            explanation.append("Risque de retard estimé: ").append(prediction.getPredictedDelayDays()).append(" jours\n");
        }

        return explanation.toString();
    }

    private RiskPrediction createDefaultPrediction() {
        RiskPrediction prediction = new RiskPrediction();
        prediction.setLevel(RiskLevel.MEDIUM);
        prediction.setProbability(0.65);
        prediction.setConfidence(0.5);
        prediction.setPredictedDelayDays(15);
        prediction.setPredictionDate(LocalDate.now());
        prediction.setRecommendations(Arrays.asList(
                "📊 Données insuffisantes pour une prédiction précise",
                "📞 Contacter le client pour confirmer la réception de la facture",
                "⏰ Programmer un rappel à J-7"
        ));
        return prediction;
    }

    private double normalizeAmount(double amount) {
        return Math.min(amount, 100000) / 100000.0;
    }

    private void simulateTrainingData() {
        log.info("Generating simulated training data for AI model...");

        List<HistoricalPaymentData> syntheticData = new ArrayList<>();
        Random random = new Random(42);

        for (int i = 0; i < 5000; i++) {
            HistoricalPaymentData data = new HistoricalPaymentData();

            double amountBase = 1000 + Math.pow(random.nextDouble() * 100, 2) * 5000;
            data.invoiceAmount = Math.min(500000, amountBase);
            data.daysUntilDue = generateDueDays(random);
            data.isRecurring = random.nextDouble() < 0.3;
            data.clientAge = random.nextInt(3651);

            if (data.clientAge < 365) {
                data.totalConventions = random.nextInt(5);
            } else if (data.clientAge < 1095) {
                data.totalConventions = random.nextInt(15);
            } else {
                data.totalConventions = 5 + random.nextInt(45);
            }

            double r = random.nextDouble();
            if (data.clientAge < 180) {
                if (r < 0.3) {
                    data.paymentOnTimeRate = 0.40 + random.nextDouble() * 0.24;
                    data.latePaymentRate = 0.25 + random.nextDouble() * 0.25;
                    data.advancePaymentRate = 0.02 + random.nextDouble() * 0.08;
                } else if (r < 0.6) {
                    data.paymentOnTimeRate = 0.65 + random.nextDouble() * 0.19;
                    data.latePaymentRate = 0.10 + random.nextDouble() * 0.15;
                    data.advancePaymentRate = 0.05 + random.nextDouble() * 0.10;
                } else {
                    data.paymentOnTimeRate = 0.85 + random.nextDouble() * 0.14;
                    data.latePaymentRate = 0.01 + random.nextDouble() * 0.09;
                    data.advancePaymentRate = 0.05 + random.nextDouble() * 0.15;
                }
            } else if (data.totalConventions > 20) {
                if (r < 0.1) {
                    data.paymentOnTimeRate = 0.15 + random.nextDouble() * 0.24;
                    data.latePaymentRate = 0.55 + random.nextDouble() * 0.25;
                } else if (r < 0.25) {
                    data.paymentOnTimeRate = 0.40 + random.nextDouble() * 0.24;
                    data.latePaymentRate = 0.25 + random.nextDouble() * 0.25;
                } else if (r < 0.75) {
                    data.paymentOnTimeRate = 0.65 + random.nextDouble() * 0.19;
                    data.latePaymentRate = 0.10 + random.nextDouble() * 0.15;
                } else {
                    data.paymentOnTimeRate = 0.85 + random.nextDouble() * 0.14;
                    data.latePaymentRate = 0.01 + random.nextDouble() * 0.09;
                }
                data.advancePaymentRate = 0.05 + random.nextDouble() * 0.10;
            } else {
                if (r < 0.05) {
                    data.paymentOnTimeRate = 0.00 + random.nextDouble() * 0.14;
                    data.latePaymentRate = 0.75 + random.nextDouble() * 0.24;
                } else if (r < 0.15) {
                    data.paymentOnTimeRate = 0.15 + random.nextDouble() * 0.24;
                    data.latePaymentRate = 0.55 + random.nextDouble() * 0.25;
                } else if (r < 0.40) {
                    data.paymentOnTimeRate = 0.40 + random.nextDouble() * 0.24;
                    data.latePaymentRate = 0.25 + random.nextDouble() * 0.25;
                } else if (r < 0.70) {
                    data.paymentOnTimeRate = 0.65 + random.nextDouble() * 0.19;
                    data.latePaymentRate = 0.10 + random.nextDouble() * 0.15;
                } else {
                    data.paymentOnTimeRate = 0.85 + random.nextDouble() * 0.14;
                    data.latePaymentRate = 0.01 + random.nextDouble() * 0.09;
                }
                data.advancePaymentRate = 0.05 + random.nextDouble() * 0.10;
            }

            data.averagePaymentDelay = 5 + random.nextInt(35);
            data.previousLateCount = random.nextInt(20);
            data.contractDuration = 30 + random.nextInt(701);
            data.nbUsers = 1 + random.nextInt(5000);

            LocalDate dueDate = generateRandomDueDate(random);
            data.isEndOfMonth = dueDate.getDayOfMonth() == dueDate.lengthOfMonth();
            data.isEndOfQuarter = (dueDate.getMonthValue() % 3 == 0) &&
                    dueDate.getDayOfMonth() == dueDate.lengthOfMonth();
            data.isEndOfYear = dueDate.getMonthValue() == 12 &&
                    dueDate.getDayOfMonth() == 31;

            data.riskScore = calculateRiskScoreFromFeatures(data);
            data.riskLevel = classifyRiskLevel(data.riskScore);
            data.delayDays = calculateActualDelayDays(data, random);

            syntheticData.add(data);
        }

        addEdgeCases(syntheticData);
        saveSyntheticDataToDatabase(syntheticData);

        log.info("Generated {} synthetic training records", syntheticData.size());
        printTrainingDataStatistics(syntheticData);
    }

    private int generateDueDays(Random random) {
        double r = random.nextDouble();
        if (r < 0.3) return 0;
        if (r < 0.5) return 1 + random.nextInt(7);
        if (r < 0.7) return 8 + random.nextInt(22);
        if (r < 0.85) return 31 + random.nextInt(30);
        return 61 + random.nextInt(30);
    }

    private double calculateRiskScoreFromFeatures(HistoricalPaymentData data) {
        double riskScore = 0.0;
        riskScore += data.latePaymentRate * 40;
        riskScore += Math.min(15, (data.invoiceAmount / 500000) * 15);
        riskScore += (1 - Math.min(1, data.clientAge / 1825.0)) * 10;
        riskScore += Math.min(15, data.previousLateCount / 2.0);
        riskScore += (data.contractDuration / 730.0) * 10;
        riskScore += Math.min(10, (data.averagePaymentDelay / 90.0) * 10);
        return Math.min(100, riskScore);
    }

    private int calculateActualDelayDays(HistoricalPaymentData data, Random random) {
        if (data.riskScore < 20) {
            if (random.nextDouble() < 0.8) return 0;
            return random.nextInt(10);
        }
        if (data.riskScore < 50) {
            if (random.nextDouble() < 0.4) return 0;
            return 5 + random.nextInt(25);
        }
        if (data.riskScore < 75) {
            if (random.nextDouble() < 0.2) return 0;
            return 20 + random.nextInt(40);
        }
        if (random.nextDouble() < 0.15) return 0;
        if (random.nextDouble() < 0.7) return 45 + random.nextInt(45);
        return 90 + random.nextInt(60);
    }

    private LocalDate generateRandomDueDate(Random random) {
        int year = 2024 + random.nextInt(2);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        return LocalDate.of(year, month, day);
    }

    private void addEdgeCases(List<HistoricalPaymentData> data) {
        HistoricalPaymentData perfectClient = new HistoricalPaymentData();
        perfectClient.invoiceAmount = 25000;
        perfectClient.daysUntilDue = 30;
        perfectClient.isRecurring = true;
        perfectClient.clientAge = 1095;
        perfectClient.totalConventions = 25;
        perfectClient.paymentOnTimeRate = 0.95;
        perfectClient.latePaymentRate = 0.00;
        perfectClient.advancePaymentRate = 0.05;
        perfectClient.averagePaymentDelay = 0;
        perfectClient.previousLateCount = 0;
        perfectClient.contractDuration = 365;
        perfectClient.nbUsers = 500;
        perfectClient.isEndOfMonth = false;
        perfectClient.isEndOfQuarter = false;
        perfectClient.isEndOfYear = false;
        perfectClient.riskScore = 5;
        perfectClient.riskLevel = 0;
        perfectClient.delayDays = -5;
        data.add(perfectClient);

        HistoricalPaymentData riskyClient = new HistoricalPaymentData();
        riskyClient.invoiceAmount = 450000;
        riskyClient.daysUntilDue = 15;
        riskyClient.isRecurring = false;
        riskyClient.clientAge = 90;
        riskyClient.totalConventions = 1;
        riskyClient.paymentOnTimeRate = 0.00;
        riskyClient.latePaymentRate = 0.95;
        riskyClient.advancePaymentRate = 0.05;
        riskyClient.averagePaymentDelay = 75;
        riskyClient.previousLateCount = 5;
        riskyClient.contractDuration = 90;
        riskyClient.nbUsers = 2000;
        riskyClient.isEndOfMonth = true;
        riskyClient.isEndOfQuarter = true;
        riskyClient.isEndOfYear = false;
        riskyClient.riskScore = 95;
        riskyClient.riskLevel = 5;
        riskyClient.delayDays = 90;
        data.add(riskyClient);

        HistoricalPaymentData newClient = new HistoricalPaymentData();
        newClient.invoiceAmount = 15000;
        newClient.daysUntilDue = 30;
        newClient.isRecurring = false;
        newClient.clientAge = 0;
        newClient.totalConventions = 0;
        newClient.paymentOnTimeRate = 0;
        newClient.latePaymentRate = 0;
        newClient.advancePaymentRate = 0;
        newClient.averagePaymentDelay = 0;
        newClient.previousLateCount = 0;
        newClient.contractDuration = 365;
        newClient.nbUsers = 50;
        newClient.isEndOfMonth = false;
        newClient.isEndOfQuarter = false;
        newClient.isEndOfYear = false;
        newClient.riskScore = 50;
        newClient.riskLevel = 2;
        newClient.delayDays = 0;
        data.add(newClient);

        for (int i = 0; i < 50; i++) {
            HistoricalPaymentData seasonal = new HistoricalPaymentData();
            seasonal.invoiceAmount = 30000 + (i * 1000);
            seasonal.daysUntilDue = 30;
            seasonal.isRecurring = true;
            seasonal.clientAge = 730;
            seasonal.totalConventions = 12;
            seasonal.paymentOnTimeRate = 0.70;
            seasonal.latePaymentRate = 0.25;
            seasonal.advancePaymentRate = 0.05;
            seasonal.averagePaymentDelay = 20;
            seasonal.previousLateCount = 8;
            seasonal.contractDuration = 365;
            seasonal.nbUsers = 200;
            seasonal.isEndOfMonth = true;
            seasonal.isEndOfQuarter = true;
            seasonal.isEndOfYear = true;
            seasonal.riskScore = 65;
            seasonal.riskLevel = 3;
            seasonal.delayDays = 30;
            data.add(seasonal);
        }
    }

    private void saveSyntheticDataToDatabase(List<HistoricalPaymentData> syntheticData) {
        log.info("Saving synthetic training data to database...");
        if (trainingDataRepository != null) {
            try {
                List<TrainingData> trainingDataList = new ArrayList<>();
                for (HistoricalPaymentData hpd : syntheticData) {
                    TrainingData td = convertToTrainingData(hpd);
                    td.setSynthetic(true);
                    trainingDataList.add(td);
                }
                trainingDataRepository.saveAll(trainingDataList);
                log.info("Saved {} synthetic records to database", trainingDataList.size());
            } catch (Exception e) {
                log.warn("Could not save to training data repository: {}", e.getMessage());
            }
        }
    }

    private TrainingData convertToTrainingData(HistoricalPaymentData hpd) {
        TrainingData td = new TrainingData();
        td.setInvoiceAmount(hpd.invoiceAmount);
        td.setDaysUntilDue(hpd.daysUntilDue);
        td.setIsRecurring(hpd.isRecurring);
        td.setClientAge(hpd.clientAge);
        td.setTotalConventions(hpd.totalConventions);
        td.setPaymentOnTimeRate(hpd.paymentOnTimeRate);
        td.setLatePaymentRate(hpd.latePaymentRate);
        td.setAdvancePaymentRate(hpd.advancePaymentRate);
        td.setAveragePaymentDelay(hpd.averagePaymentDelay);
        td.setPreviousLateCount(hpd.previousLateCount);
        td.setContractDuration(hpd.contractDuration);
        td.setNbUsers(hpd.nbUsers);
        td.setIsEndOfMonth(hpd.isEndOfMonth);
        td.setIsEndOfQuarter(hpd.isEndOfQuarter);
        td.setIsEndOfYear(hpd.isEndOfYear);
        td.setRiskScore(hpd.riskScore);
        td.setRiskLevel(hpd.riskLevel);
        td.setActualDelayDays(hpd.delayDays);
        td.setSynthetic(true);
        return td;
    }


    private void saveFeatureBaselines(List<HistoricalPaymentData> syntheticData) {
        Map<String, Double> averages = new HashMap<>();
        averages.put("avg_invoice_amount", syntheticData.stream()
                .mapToDouble(d -> d.invoiceAmount).average().orElse(0));
        averages.put("avg_ontime_rate", syntheticData.stream()
                .mapToDouble(d -> d.paymentOnTimeRate).average().orElse(0));
        averages.put("avg_late_rate", syntheticData.stream()
                .mapToDouble(d -> d.latePaymentRate).average().orElse(0));
        averages.put("avg_risk_score", syntheticData.stream()
                .mapToDouble(d -> d.riskScore).average().orElse(0));
        log.info("Feature baselines saved: {}", averages);
    }

    private void printTrainingDataStatistics(List<HistoricalPaymentData> data) {
        log.info("========== TRAINING DATA STATISTICS ==========");
        log.info("Total records: {}", data.size());

        Map<Integer, Long> riskDistribution = data.stream()
                .collect(Collectors.groupingBy(d -> d.riskLevel, Collectors.counting()));

        log.info("Risk Level Distribution:");
        for (Map.Entry<Integer, Long> entry : riskDistribution.entrySet()) {
            String levelName = RiskLevel.fromCode(entry.getKey()).getLabel();
            log.info("  {}: {} records ({:.1f}%)",
                    levelName, entry.getValue(),
                    (entry.getValue() * 100.0 / data.size()));
        }

        long onTimeCount = data.stream().filter(d -> d.delayDays == 0).count();
        long earlyCount = data.stream().filter(d -> d.delayDays < 0).count();
        long lateCount = data.stream().filter(d -> d.delayDays > 0).count();

        log.info("Payment Behavior:");
        log.info("  On-time: {} ({:.1f}%)", onTimeCount, onTimeCount * 100.0 / data.size());
        log.info("  Early: {} ({:.1f}%)", earlyCount, earlyCount * 100.0 / data.size());
        log.info("  Late: {} ({:.1f}%)", lateCount, lateCount * 100.0 / data.size());

        log.info("===============================================");
    }


    private void initializeFallbackModel() {
        log.warn("Using fallback rule-based risk assessment");
    }

    private HistoricalPaymentData buildSyntheticFeatures(BigDecimal amount, LocalDate dueDate,
                                                         Long clientId, Long applicationId) {
        HistoricalPaymentData data = new HistoricalPaymentData();
        data.invoiceAmount = amount != null ? amount.doubleValue() : 0;

        if (dueDate != null) {
            data.daysUntilDue = (int) ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
            data.isEndOfMonth = dueDate.getDayOfMonth() == dueDate.lengthOfMonth();
            data.isEndOfQuarter = (dueDate.getMonthValue() % 3 == 0) &&
                    dueDate.getDayOfMonth() == dueDate.lengthOfMonth();
            data.isEndOfYear = dueDate.getMonthValue() == 12 &&
                    dueDate.getDayOfMonth() == 31;
        }

        if (clientId != null) {
            Optional<Structure> clientOpt = structureRepository.findById(clientId);
            if (clientOpt.isPresent()) {
                Structure client = clientOpt.get();
                if (client.getCreatedAt() != null) {
                    data.clientAge = ChronoUnit.DAYS.between(client.getCreatedAt().toLocalDate(), LocalDate.now());
                }

                List<Convention> clientConventions = conventionRepository.findByStructureBeneficielId(clientId);
                data.totalConventions = clientConventions != null ? clientConventions.size() : 0;

                List<Facture> clientFactures = new ArrayList<>();
                if (clientConventions != null) {
                    for (Convention conv : clientConventions) {
                        List<Facture> convFactures = factureRepository.findByConventionId(conv.getId());
                        if (convFactures != null) {
                            clientFactures.addAll(convFactures);
                        }
                    }
                }

                List<Facture> paidFactures = clientFactures.stream()
                        .filter(f -> "PAYE".equals(f.getStatutPaiement()))
                        .collect(Collectors.toList());

                if (!paidFactures.isEmpty()) {
                    long lateCount = paidFactures.stream()
                            .filter(f -> f.getDatePaiement() != null && f.getDateEcheance() != null &&
                                    f.getDatePaiement().isAfter(f.getDateEcheance()))
                            .count();
                    data.latePaymentRate = (double) lateCount / paidFactures.size();
                    data.previousLateCount = (int) lateCount;

                    data.averagePaymentDelay = paidFactures.stream()
                            .filter(f -> f.getDatePaiement() != null && f.getDateEcheance() != null &&
                                    f.getDatePaiement().isAfter(f.getDateEcheance()))
                            .mapToLong(f -> ChronoUnit.DAYS.between(f.getDateEcheance(), f.getDatePaiement()))
                            .average()
                            .orElse(0);
                }
            }
        }

        return data;
    }

    private HistoricalPaymentData convertFromTrainingData(TrainingData td) {
        HistoricalPaymentData hpd = new HistoricalPaymentData();
        hpd.invoiceAmount = td.getInvoiceAmount();
        hpd.daysUntilDue = td.getDaysUntilDue();
        hpd.isRecurring = td.getIsRecurring();
        hpd.clientAge = td.getClientAge();
        hpd.totalConventions = td.getTotalConventions();
        hpd.paymentOnTimeRate = td.getPaymentOnTimeRate();
        hpd.latePaymentRate = td.getLatePaymentRate();
        hpd.advancePaymentRate = td.getAdvancePaymentRate();
        hpd.averagePaymentDelay = td.getAveragePaymentDelay();
        hpd.previousLateCount = td.getPreviousLateCount();
        hpd.contractDuration = td.getContractDuration();
        hpd.nbUsers = td.getNbUsers();
        hpd.isEndOfMonth = td.getIsEndOfMonth();
        hpd.isEndOfQuarter = td.getIsEndOfQuarter();
        hpd.isEndOfYear = td.getIsEndOfYear();
        hpd.riskScore = td.getRiskScore();
        hpd.riskLevel = td.getRiskLevel();
        hpd.delayDays = td.getActualDelayDays();
        return hpd;
    }

    // Internal class for training data
    public static class HistoricalPaymentData {
        double invoiceAmount;
        public int daysUntilDue;
        boolean isRecurring;
        public long clientAge;
        public int totalConventions;
        public double paymentOnTimeRate;
        public double latePaymentRate;
        public double advancePaymentRate;
        double averagePaymentDelay;
        long contractDuration;
        long nbUsers;
        boolean isEndOfMonth;
        boolean isEndOfQuarter;
        boolean isEndOfYear;
        int previousLateCount;
        public int delayDays;
        public double riskScore;
        public int riskLevel;
    }
}