package com.spotify.wrapper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.wrapper.entity.User;
import com.spotify.wrapper.exception.SpotifyApiException;
import com.spotify.wrapper.repository.UserRepository;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

/**
 * Service for managing user tokens with proper transaction boundaries.
 * This ensures DB connections are released quickly after token retrieval.
 */
@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;

    @Autowired
    private UserRepository userRepository;

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;
    
    // Self-injection to allow @Transactional to work on internal method calls
    @Autowired
    @Lazy
    private TokenService self;
    
    @PostConstruct
    public void init() {
        // Create a connection pool manager with reasonable limits
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);  // Max total connections
        connectionManager.setDefaultMaxPerRoute(20);  // Max connections per route
        
        // Configure request timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(10000)
                .setConnectionRequestTimeout(5000)
                .build();
        
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        logger.info("TokenService HTTP client initialized with connection pool (max: 50, per route: 20)");
    }
    
    @PreDestroy
    public void cleanup() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
            if (connectionManager != null) {
                connectionManager.close();
            }
            logger.info("TokenService HTTP client closed");
        } catch (IOException e) {
            logger.error("Error closing HTTP client", e);
        }
    }

    /**
     * Get a valid access token for the user.
     * Uses separate transactions for read and write to release connections quickly.
     */
    public String getAccessToken(String userId) throws IOException {
        // First transaction: read user data (connection released after this)
        // Using self-reference to ensure proxy intercepts the call
        UserTokenInfo tokenInfo = self.getUserTokenInfo(userId);
        
        // Check if token is still valid (with 5 minute buffer)
        if (tokenInfo.tokenExpiresAt.isAfter(LocalDateTime.now().plusMinutes(5))) {
            return tokenInfo.accessToken;
        }

        // Token expired - refresh it (HTTP call happens outside transaction)
        logger.info("Refreshing access token for userId: {}", userId);
        TokenRefreshResult refreshResult = callSpotifyTokenRefresh(tokenInfo.refreshToken);
        
        // Second transaction: save the new token (connection released after this)
        // Using self-reference to ensure proxy intercepts the call
        self.saveNewToken(userId, refreshResult.accessToken, refreshResult.expiresIn);
        
        return refreshResult.accessToken;
    }
    
    /**
     * Read user token info in a short transaction.
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public UserTokenInfo getUserTokenInfo(String userId) {
        long dbStartTime = System.currentTimeMillis();
        User user = userRepository.findBySpotifyUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        long dbEndTime = System.currentTimeMillis();
        logger.debug("DB lookup took {}ms for userId: {}", dbEndTime - dbStartTime, userId);
        
        return new UserTokenInfo(
            user.getAccessToken(),
            user.getRefreshToken(),
            user.getTokenExpiresAt()
        );
    }
    
    /**
     * Save new token in a short transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveNewToken(String userId, String accessToken, int expiresIn) {
        User user = userRepository.findBySpotifyUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setAccessToken(accessToken);
        user.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
        userRepository.save(user);
        logger.info("Token saved successfully for userId: {}", userId);
    }
    
    /**
     * Call Spotify API to refresh token - NO database connection held during this call.
     */
    private TokenRefreshResult callSpotifyTokenRefresh(String refreshToken) throws IOException {
        String url = "https://accounts.spotify.com/api/token";

        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");

        String auth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        request.setHeader("Authorization", "Basic " + auth);

        String body = "grant_type=refresh_token&refresh_token=" + refreshToken;
        request.setEntity(new StringEntity(body));

        long apiStartTime = System.currentTimeMillis();
        
        // Use try-with-resources to ensure connection is returned to pool
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.debug("Token refresh API call took {}ms", apiEndTime - apiStartTime);

            String responseBody = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = objectMapper.readValue(responseBody, Map.class);

            if (statusCode >= 400) {
                String error = tokenResponse.get("error") != null
                        ? String.valueOf(tokenResponse.get("error"))
                        : "unknown_error";
                String errorDescription = tokenResponse.get("error_description") != null
                        ? String.valueOf(tokenResponse.get("error_description"))
                        : "No details provided by Spotify";

                throw new SpotifyApiException(
                        statusCode,
                        "Spotify token refresh failed: " + error + " - " + errorDescription,
                        responseBody
                );
            }

            String newAccessToken = (String) tokenResponse.get("access_token");
            Number expiresInValue = (Number) tokenResponse.get("expires_in");

            if (newAccessToken == null || expiresInValue == null) {
                throw new SpotifyApiException(
                        502,
                        "Invalid token refresh response from Spotify",
                        responseBody
                );
            }

            int expiresIn = expiresInValue.intValue();

            return new TokenRefreshResult(newAccessToken, expiresIn);
        }
    }
    
    /**
     * Simple data holder for user token information.
     */
    public static class UserTokenInfo {
        public final String accessToken;
        public final String refreshToken;
        public final LocalDateTime tokenExpiresAt;
        
        public UserTokenInfo(String accessToken, String refreshToken, LocalDateTime tokenExpiresAt) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.tokenExpiresAt = tokenExpiresAt;
        }
    }
    
    /**
     * Simple data holder for token refresh result.
     */
    private static class TokenRefreshResult {
        final String accessToken;
        final int expiresIn;
        
        TokenRefreshResult(String accessToken, int expiresIn) {
            this.accessToken = accessToken;
            this.expiresIn = expiresIn;
        }
    }
}
