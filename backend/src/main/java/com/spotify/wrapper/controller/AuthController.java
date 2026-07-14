package com.spotify.wrapper.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.wrapper.entity.User;
import com.spotify.wrapper.repository.UserRepository;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Value("${spotify.client.id}")
    private String clientId;
    
    @Value("${spotify.client.secret}")
    private String clientSecret;
    
    @Value("${spotify.redirect.uri}")
    private String redirectUri;
    
    @Value("${spotify.scope}")
    private String scope;
    
    @Autowired
    private UserRepository userRepository;
    
    private final HttpClient httpClient = HttpClients.createDefault();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> pendingStates = ConcurrentHashMap.newKeySet();
    
    @GetMapping("/login")
    public ResponseEntity<String> login() {
        logger.info("OAuth login initiated - generating authorization URL");
        
        String state = generateState();
        pendingStates.add(state);
        String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8);
        
        String authUrl = "https://accounts.spotify.com/authorize?" +
                "response_type=code" +
                "&client_id=" + clientId +
                "&scope=" + encodedScope +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
            "&state=" + state +
            "&show_dialog=true";
        
        logger.info("Generated authorization URL with state: {} and redirect_uri: {}", state, redirectUri);
        return ResponseEntity.ok(authUrl);
    }
    
    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> callback(@RequestParam String code, @RequestParam String state) {
        logger.info("OAuth callback received with state: {}, code: {}...", state, code.substring(0, 10));

        if (state == null || state.isBlank() || !pendingStates.remove(state)) {
            logger.warn("OAuth callback rejected due to invalid/unknown state");
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid OAuth state"));
        }
        
        try {
            // Exchange code for access token
            logger.info("Exchanging authorization code for access token");
            String tokenUrl = "https://accounts.spotify.com/api/token";
            
            HttpPost request = new HttpPost(tokenUrl);
            request.setHeader("Content-Type", "application/x-www-form-urlencoded");
            
            String auth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
            request.setHeader("Authorization", "Basic " + auth);
            
            String body = "grant_type=authorization_code" +
                    "&code=" + code +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
            request.setEntity(new StringEntity(body));
            
            HttpResponse response = httpClient.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());
                int statusCode = response.getStatusLine().getStatusCode();
            
                logger.info("Token exchange response status: {}", statusCode);
            
                @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = objectMapper.readValue(responseBody, Map.class);

                if (statusCode >= 400) {
                String error = tokenResponse.get("error") != null
                    ? String.valueOf(tokenResponse.get("error"))
                    : "unknown_error";
                String errorDescription = tokenResponse.get("error_description") != null
                    ? String.valueOf(tokenResponse.get("error_description"))
                    : "No details provided by Spotify";

                logger.error("Token exchange failed: {} - {}", error, errorDescription);
                return ResponseEntity.status(statusCode).body(Map.of(
                    "error", "Spotify token exchange failed: " + error + " - " + errorDescription
                ));
                }

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            String grantedScope = (String) tokenResponse.get("scope");
                Number expiresInValue = (Number) tokenResponse.get("expires_in");
            
                if (accessToken == null || expiresInValue == null) {
                logger.error("Failed to obtain access token from Spotify. Response: {}", responseBody);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid token response from Spotify"
                ));
            }

                int expiresIn = expiresInValue.intValue();
            
            logger.info("Successfully obtained access token, expires in: {} seconds", expiresIn);
            logger.info("Granted Spotify scopes: {}", grantedScope);
            
            // Get user profile
            logger.info("Fetching user profile from Spotify");
            Map<String, String> userProfile = getUserProfile(accessToken);
            
            // Save or update user
            logger.info("Saving user data for Spotify user: {}", userProfile.get("id"));
            User user = saveOrUpdateUser(userProfile, accessToken, refreshToken, expiresIn);
            
            logger.info("OAuth flow completed successfully for user: {}", user.getDisplayName());
            
            return ResponseEntity.ok(Map.of(
                    "success", "true",
                    "userId", user.getSpotifyUserId(),
                    "displayName", user.getDisplayName()
            ));
            
        } catch (Exception e) {
            logger.error("OAuth callback failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, String>> getUser(@PathVariable String userId) {
        logger.info("Fetching user data for userId: {}", userId);
        
        Optional<User> userOpt = userRepository.findBySpotifyUserId(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            logger.info("User found: {}", user.getDisplayName());
            return ResponseEntity.ok(Map.of(
                    "userId", user.getSpotifyUserId(),
                    "displayName", user.getDisplayName(),
                    "email", user.getEmail()
            ));
        }
        
        logger.warn("User not found for userId: {}", userId);
        return ResponseEntity.notFound().build();
    }

    @Transactional
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam String userId) {
        logger.info("Logout request received for userId: {}", userId);

        Optional<User> userOpt = userRepository.findBySpotifyUserId(userId);
        if (userOpt.isPresent()) {
            userRepository.deleteBySpotifyUserId(userId);
            logger.info("Deleted stored OAuth tokens for userId: {}", userId);
        } else {
            logger.info("No stored user record found during logout for userId: {}", userId);
        }

        return ResponseEntity.ok().build();
    }
    
    private Map<String, String> getUserProfile(String accessToken) throws IOException {
        String profileUrl = "https://api.spotify.com/v1/me";
        
        HttpGet request = new HttpGet(profileUrl);
        request.setHeader("Authorization", "Bearer " + accessToken);
        
        HttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        
        return objectMapper.readValue(responseBody, Map.class);
    }
    
    private User saveOrUpdateUser(Map<String, String> profile, String accessToken, String refreshToken, int expiresIn) {
        String spotifyUserId = profile.get("id");
        if (spotifyUserId == null || spotifyUserId.isBlank()) {
            throw new IllegalStateException("Spotify profile is missing required user id");
        }

        String displayNameFromProfile = profile.get("display_name");
        String emailFromProfile = profile.get("email");
        
        Optional<User> existingUser = userRepository.findBySpotifyUserId(spotifyUserId);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setAccessToken(accessToken);
            if (refreshToken != null && !refreshToken.isBlank()) {
                user.setRefreshToken(refreshToken);
            }
            user.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));

            String resolvedDisplayName = firstNonBlank(displayNameFromProfile, user.getDisplayName(), spotifyUserId, "Spotify User");
            String resolvedEmail = firstNonBlank(emailFromProfile, user.getEmail(), spotifyUserId + "@spotify.local");

            user.setDisplayName(resolvedDisplayName);
            user.setEmail(resolvedEmail);
            return userRepository.save(user);
        } else {
            String resolvedDisplayName = firstNonBlank(displayNameFromProfile, spotifyUserId, "Spotify User");
            String resolvedEmail = firstNonBlank(emailFromProfile, spotifyUserId + "@spotify.local");

            User newUser = new User(
                    spotifyUserId,
                    resolvedDisplayName,
                    resolvedEmail,
                    accessToken,
                    firstNonBlank(refreshToken, "no-refresh-token"),
                    LocalDateTime.now().plusSeconds(expiresIn)
            );
            return userRepository.save(newUser);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return "";
    }
    
    private String generateState() {
        return java.util.UUID.randomUUID().toString();
    }
}
