package com.gptdicto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {

    @Value("${google.api.key}")
    private String googleApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AiService() {
        this.webClient = WebClient.create("https://generativelanguage.googleapis.com");
        this.objectMapper = new ObjectMapper();
    }

    public String askAgent(String csvContent, String question) {
        StringBuilder parsedData = new StringBuilder();
        try (CSVReader csvReader = new CSVReader(new StringReader(csvContent))) {
            List<String[]> rows = csvReader.readAll();
            for (String[] row : rows) {
                parsedData.append(String.join(", ", row)).append("\n");
            }
        } catch (Exception e) {
            return "Error parsing CSV: " + e.getMessage();
        }

        String prompt = "You are an AI agent analyzing CSV data. Here is the data:\n" +
                parsedData.toString() + "\nQuestion: " + question ;

        String requestBody = String.format(
                "{\"contents\": [{\"parts\": [{\"text\": \"%s\"}]}]}",
                prompt.replace("\"", "\\\"")
        );

        try {
            Mono<String> responseMono = webClient.post()
                    .uri("/v1beta/models/gemini-2.0-flash:generateContent?key={apiKey}", googleApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class);

            String responseJson = responseMono.block();
            JsonNode rootNode = objectMapper.readTree(responseJson);
            String responseText = rootNode.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText();

            boolean isGraphRequest = question.toLowerCase().contains("graph") ||
                    question.toLowerCase().contains("plot") ||
                    question.toLowerCase().contains("chart");
            System.out.println("Graph request detected: " + isGraphRequest);

            if (isGraphRequest && responseText != null) {
                Pattern pattern = Pattern.compile("\\[?DATA: labels=\\[(.*?)\\], values=\\[(.*?)\\]\\]?");
                Matcher matcher = pattern.matcher(responseText);
                if (matcher.find()) {
                    System.out.println("Labels: " + matcher.group(1));
                    System.out.println("Values: " + matcher.group(2));

                    String[] labels = matcher.group(1).split(",");
                    String[] values = matcher.group(2).split(",");

                    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

                    // Extract dynamic titles from question
                    String graphTitle = extractGraphTitle(question);
                    String yAxisLabel = extractYAxisLabel(question);
                    String xAxisLabel = extractXAxisLabel(question);

                    for (int i = 0; i < labels.length; i++) {
                        dataset.addValue(Double.parseDouble(values[i].trim()), yAxisLabel, labels[i].trim());
                    }

                    JFreeChart chart = ChartFactory.createBarChart(
                            graphTitle,      // Dynamic title
                            xAxisLabel,      // Dynamic X-axis
                            yAxisLabel,      // Dynamic Y-axis
                            dataset
                    );

                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String graphFilename = "graph-" + timestamp + ".png";
                    Path tempImagePath = Files.createTempFile("graph-", ".png");
                    Path finalImagePath = Paths.get("target/classes/static/" + graphFilename);

                    try {
                        ChartUtils.saveChartAsPNG(tempImagePath.toFile(), chart, 800, 400);
                        System.out.println("Graph saved to temp: " + tempImagePath.toAbsolutePath());

                        Files.createDirectories(finalImagePath.getParent());
                        Files.move(tempImagePath, finalImagePath);
                        System.out.println("Graph moved to: " + finalImagePath.toAbsolutePath());

                        if (finalImagePath.toFile().exists()) {
                            System.out.println("Graph file confirmed to exist");
                        } else {
                            System.out.println("Graph file missing after move");
                        }
                    } catch (Exception e) {
                        System.err.println("Error saving/moving graph: " + e.getMessage());
                        return responseText + "\n[Graph generation failed: " + e.getMessage() + "]";
                    }

                    return responseText + "\n[Graph generated and available below:" + graphFilename + "]";
                } else {
                    System.out.println("No graph data found in: " + responseText);
                    return responseText;
                }
            }

            return responseText != null ? responseText : "No response from Gemini API";
        } catch (Exception e) {
            return "Error calling Gemini API or generating graph: " + e.getMessage();
        }
    }

    private String extractGraphTitle(String question) {
        // Simple extraction: look for "of X by Y" pattern
        Pattern pattern = Pattern.compile("of\\s+(.+?)\\s+by\\s+(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(question);
        if (matcher.find()) {
            return capitalize(matcher.group(1)) + " by " + capitalize(matcher.group(2));
        }
        return "Data Visualization"; // Fallback
    }

    private String extractYAxisLabel(String question) {
        Pattern pattern = Pattern.compile("of\\s+(.+?)\\s+by", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(question);
        if (matcher.find()) {
            return capitalize(matcher.group(1));
        }
        return "Value"; // Fallback
    }

    private String extractXAxisLabel(String question) {
        Pattern pattern = Pattern.compile("by\\s+(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(question);
        if (matcher.find()) {
            String label = matcher.group(1).trim();
            // Remove trailing words like "graph" or "plot"
            label = label.replaceAll("\\s*(graph|plot|chart).*", "");
            return capitalize(label);
        }
        return "Category"; // Fallback
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}