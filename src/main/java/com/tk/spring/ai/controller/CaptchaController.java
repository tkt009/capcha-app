package com.tk.spring.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Controller
@RequestMapping("/capcha")
public class CaptchaController {

    private static final String CAPTCHA_SESSION_KEY = "captcha_text";
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int CAPTCHA_LENGTH = 6;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(CaptchaController.class);

    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;

    @GetMapping
    public String showCaptchaPage() {
        // forward to the static HTML file so the page works without a template engine
        return "forward:/captcha.html";
    }

    @PostMapping("/generate")
    @ResponseBody
    public Map<String, String> generateCaptcha(HttpSession session) {
        String captchaText = null;

        // Try generating via GenAI (OpenAI) when API key is available
        if (openAiApiKey != null && !openAiApiKey.isBlank()) {
            try {
                captchaText = generateCaptchaWithLLM();
            } catch (Exception e) {
                // fallback to local generation on error
                logger.warn("LLM generation failed, falling back to local generation", e);
                captchaText = null;
            }
        }

        if (captchaText == null || captchaText.isBlank()) {
            captchaText = generateRandomCaptcha();
        }
        session.setAttribute(CAPTCHA_SESSION_KEY, captchaText);
        
        Map<String, String> response = new HashMap<>();
        response.put("captcha", captchaText);
        return response;
    }

    @PostMapping("/verify")
    @ResponseBody
    public Map<String, Object> verifyCaptcha(String userInput, HttpSession session) {
        String storedCaptcha = (String) session.getAttribute(CAPTCHA_SESSION_KEY);
        
        Map<String, Object> response = new HashMap<>();
        if (storedCaptcha != null && storedCaptcha.equals(userInput)) {
            response.put("success", true);
            response.put("message", "CAPTCHA verified successfully!");
        } else {
            response.put("success", false);
            response.put("message", "CAPTCHA verification failed. Please try again.");
        }
        return response;
    }

    private String generateRandomCaptcha() {
        Random random = new Random();
        StringBuilder captcha = new StringBuilder();
        boolean hasDigit = false;
        boolean hasLetter = false;

        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            int index = random.nextInt(ALPHANUMERIC.length());
            char c = ALPHANUMERIC.charAt(index);
            captcha.append(c);
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isLetter(c)) {
                hasLetter = true;
            }
        }

        // Ensure at least one digit and one letter are present
        if (!hasDigit) {
            int pos = random.nextInt(CAPTCHA_LENGTH);
            char digit = (char) ('0' + random.nextInt(10));
            captcha.setCharAt(pos, digit);
        }
        if (!hasLetter) {
            int pos = random.nextInt(CAPTCHA_LENGTH);
            // choose a random letter (upper or lower)
            char letter = (char) ('A' + random.nextInt(26));
            if (random.nextBoolean()) {
                letter = Character.toLowerCase(letter);
            }
            captcha.setCharAt(pos, letter);
        }

        return captcha.toString();
    }

    private String generateCaptchaWithLLM() throws IOException, InterruptedException {
        // Build a concise prompt asking the model to return only a 6-char alphanumeric string
        String prompt = "Generate a single random 6-character alphanumeric string (letters and digits). " +
                "Ensure there is at least one letter and one digit. Return only the string with no extra text.";

        HttpClient client = HttpClient.newHttpClient();

        // Prepare chat payload for OpenAI chat completions
        Map<String, Object> message = Map.of(
                "role", "user",
                "content", prompt
        );
        Map<String, Object> payload = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", new Object[]{message},
                "max_tokens", 10,
                "temperature", 1.0
        );

        String body = objectMapper.writeValueAsString(payload);

        // Log the message payload just before the request is sent
        logger.info("LLM request payload: {}", body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openAiApiKey.replaceAll("\"", ""))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("LLM generation failed: status=" + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        // OpenAI chat response: choices[0].message.content
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode messageNode = choices.get(0).path("message").path("content");
            if (messageNode.isTextual()) {
                String text = messageNode.asText().trim();
                // sanitize: extract first 6 alphanumeric chars
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < text.length() && sb.length() < CAPTCHA_LENGTH; i++) {
                    char c = text.charAt(i);
                    if (Character.isLetterOrDigit(c)) sb.append(c);
                }
                if (sb.length() == CAPTCHA_LENGTH) {
                    // ensure at least one digit and one letter
                    boolean hasDigit = sb.chars().anyMatch(Character::isDigit);
                    boolean hasLetter = sb.chars().anyMatch(Character::isLetter);
                    if (hasDigit && hasLetter) {
                        return sb.toString();
                    }
                }
            }
        }

        throw new IOException("Invalid LLM response");
    }
}
