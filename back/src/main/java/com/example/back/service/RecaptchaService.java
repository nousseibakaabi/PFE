package com.example.back.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class RecaptchaService {

    @Value("${recaptcha.secret}")
    private String recaptchaSecret;

    @Value("${recaptcha.verify-url}")
    private String recaptchaVerifyUrl;

    private final RestTemplate restTemplate;

    public RecaptchaService() {
        this.restTemplate = new RestTemplate();
    }

    public boolean verifyRecaptcha(String recaptchaToken) {
        if (recaptchaToken == null || recaptchaToken.isEmpty()) {
            return false;
        }

        try {
            // Prepare request parameters
            Map<String, String> params = new HashMap<>();
            params.put("secret", recaptchaSecret);
            params.put("response", recaptchaToken);

            // Call Google reCAPTCHA API
            String url = recaptchaVerifyUrl + "?secret={secret}&response={response}";
            RecaptchaResponse response = restTemplate.postForObject(url, null, RecaptchaResponse.class, params);

            return response != null && response.isSuccess();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Inner class to map Google's response
    public static class RecaptchaResponse {
        private boolean success;
        private String challenge_ts;
        private String hostname;
        private String[] errorCodes;

        // Getters and setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getChallenge_ts() {
            return challenge_ts;
        }

        public void setChallenge_ts(String challenge_ts) {
            this.challenge_ts = challenge_ts;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String[] getErrorCodes() {
            return errorCodes;
        }

        public void setErrorCodes(String[] errorCodes) {
            this.errorCodes = errorCodes;
        }
    }
}