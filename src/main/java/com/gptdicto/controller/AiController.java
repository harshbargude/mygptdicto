package com.gptdicto.controller;

import com.gptdicto.model.CsvRequest;
import com.gptdicto.service.AiService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class AiController {

    @Autowired
    private AiService aiService;

    @GetMapping("/")
    public String showUploadForm(Model model) {
        model.addAttribute("csvRequest", new CsvRequest());
        return "upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @RequestParam("question") String question,
                                   HttpSession session,
                                   Model model) {
        try {
            String csvContent = new String(file.getBytes());
            session.setAttribute("csvContent", csvContent); // Store full CSV in session
            String limitedCsvContent = limitCsvContent(csvContent, 5); // Limit to 5 lines
            String response = aiService.askAgent(csvContent, question);
            CsvRequest csvRequest = new CsvRequest();
            csvRequest.setCsvContent(limitedCsvContent); // Use limited content for display
            csvRequest.setResponse(response);
            model.addAttribute("csvRequest", csvRequest);

            // Check for graph and extract filename
            boolean hasGraph = response.contains("[Graph generated");
            model.addAttribute("hasGraph", hasGraph);
            if (hasGraph) {
                Pattern pattern = Pattern.compile("\\[Graph generated and available below:(.*?)\\]");
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    String graphFilename = matcher.group(1);
                    model.addAttribute("graphFilename", graphFilename);
                } else {
                    model.addAttribute("graphFilename", "graph.png"); // Fallback
                }
            }

            return "result";
        } catch (Exception e) {
            model.addAttribute("error", "Error processing file: " + e.getMessage());
            return "upload";
        }
    }

    @PostMapping("/ask")
    public String askNewQuestion(@RequestParam("question") String question,
                                 HttpSession session,
                                 Model model) {
        String csvContent = (String) session.getAttribute("csvContent");
        if (csvContent == null) {
            model.addAttribute("error", "No CSV file uploaded yet. Please upload a file first.");
            return "upload";
        }

        try {
            String limitedCsvContent = limitCsvContent(csvContent, 5); // Limit to 5 lines
            String response = aiService.askAgent(csvContent, question);
            CsvRequest csvRequest = new CsvRequest();
            csvRequest.setCsvContent(limitedCsvContent); // Use limited content for display
            csvRequest.setResponse(response);
            model.addAttribute("csvRequest", csvRequest);

            // Check for graph and extract filename
            boolean hasGraph = response.contains("[Graph generated");
            model.addAttribute("hasGraph", hasGraph);
            if (hasGraph) {
                Pattern pattern = Pattern.compile("\\[Graph generated and available below:(.*?)\\]");
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    String graphFilename = matcher.group(1);
                    model.addAttribute("graphFilename", graphFilename);
                } else {
                    model.addAttribute("graphFilename", "graph.png"); // Fallback
                }
            }

            return "result";
        } catch (Exception e) {
            model.addAttribute("error", "Error processing question: " + e.getMessage());
            return "result";
        }
    }

    @GetMapping("/reset")
    public String resetSession(HttpSession session, Model model) {
        session.removeAttribute("csvContent"); // Clear CSV from session
        model.addAttribute("csvRequest", new CsvRequest());
        model.addAttribute("message", "Session cleared. Please upload a new file.");
        return "upload"; // Redirect to upload page
    }

    private String limitCsvContent(String csvContent, int maxLines) {
        String[] lines = csvContent.split("\n");
        if (lines.length <= maxLines) {
            return csvContent; // Return full content if under maxLines
        }
        StringBuilder limitedContent = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            limitedContent.append(lines[i]).append("\n");
        }
        limitedContent.append("... (showing first ").append(maxLines).append(" lines of ").append(lines.length).append(" total)");
        return limitedContent.toString();
    }
}