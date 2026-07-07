package com.spotify.wrapper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.spotify.wrapper.dto.DevicesDto;
import com.spotify.wrapper.dto.PlaybackDto;
import com.spotify.wrapper.dto.QueueDto;
import com.spotify.wrapper.dto.SearchResultDto;
import com.spotify.wrapper.exception.SpotifyApiException;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class SpotifyService {
    
    private static final Logger logger = LoggerFactory.getLogger(SpotifyService.class);
    private static final String SPOTIFY_API_BASE_URL = "https://api.spotify.com/v1";
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;
    
    @Autowired
    private TokenService tokenService;

    private void throwSpotifyApiError(int statusCode, String responseBody) {
        String message = extractSpotifyErrorMessage(responseBody);
        throw new SpotifyApiException(statusCode, message, responseBody);
    }

    private CloseableHttpResponse executeWithRateLimitRetry(HttpRequestBase request, String operationName) throws IOException {
        final int maxAttempts = 4;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            CloseableHttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 429 || attempt == maxAttempts) {
                return response;
            }

            long delayMs = resolveRetryDelayMs(response, attempt);
            logger.warn("Spotify API rate-limited during {} (attempt {}/{}). Retrying in {} ms", operationName, attempt, maxAttempts, delayMs);

            EntityUtils.consumeQuietly(response.getEntity());
            response.close();

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting to retry after Spotify rate limit", ie);
            }
        }

        throw new IOException("Rate limit retry loop exited unexpectedly");
    }

    private long resolveRetryDelayMs(CloseableHttpResponse response, int attempt) {
        Header retryAfterHeader = response.getFirstHeader("Retry-After");
        if (retryAfterHeader != null) {
            String value = retryAfterHeader.getValue();
            try {
                long retrySeconds = Long.parseLong(value.trim());
                if (retrySeconds > 0) {
                    return retrySeconds * 1000L;
                }
            } catch (NumberFormatException ignored) {
                // Fall through to exponential backoff.
            }
        }

        long baseDelayMs = 500L;
        long exponentialDelayMs = baseDelayMs * (1L << Math.max(0, attempt - 1));
        long jitterMs = (long) (Math.random() * 250L);
        return exponentialDelayMs + jitterMs;
    }

    private String extractSpotifyErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "Spotify API error";
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode messageNode = root.path("error").path("message");
            if (!messageNode.isMissingNode() && !messageNode.asText().isBlank()) {
                return messageNode.asText();
            }
        } catch (Exception ignored) {
            // Fallback to plain-text body when response is not JSON.
        }

        return responseBody;
    }
    
    @PostConstruct
    public void init() {
        // Create a connection pool manager with reasonable limits
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);  // Max total connections
        connectionManager.setDefaultMaxPerRoute(50);  // Max connections per route (Spotify API)
        
        // Configure request timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)      // 5 seconds to establish connection
                .setSocketTimeout(30000)       // 30 seconds for data transfer
                .setConnectionRequestTimeout(5000)  // 5 seconds to get connection from pool
                .build();
        
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        logger.info("SpotifyService HTTP client initialized with connection pool (max: 100, per route: 50)");
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
            logger.info("SpotifyService HTTP client closed");
        } catch (IOException e) {
            logger.error("Error closing HTTP client", e);
        }
    }
    
    public SearchResultDto search(String userId, String query, String type, int limit) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== SEARCH METHOD CALLED ===");
        logger.debug("userId: {}, query: {}, type: {}, limit: {}", userId, query, type, limit);
        
        String accessToken = tokenService.getAccessToken(userId);
        
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        int sanitizedLimit = Math.max(1, Math.min(limit, 50));
        String url = SPOTIFY_API_BASE_URL + "/search?q=" + encodedQuery + "&type=" + type + "&limit=" + sanitizedLimit;
        logger.debug("Request URL: {}", url);
        
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        
        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /search took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Search request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }
            
            logger.info("Raw search response (first 2000 chars): {}", responseBody.length() > 2000 ? responseBody.substring(0, 2000) : responseBody);
            SearchResultDto result = objectMapper.readValue(responseBody, SearchResultDto.class);
            
            // Filter out null items from playlist results (Spotify API can return null for unavailable playlists)
            if (result.getPlaylists() != null && result.getPlaylists().getItems() != null) {
                result.getPlaylists().setItems(
                    result.getPlaylists().getItems().stream()
                        .filter(p -> p != null)
                        .collect(java.util.stream.Collectors.toList())
                );
                // Debug playlist tracks info
                for (SearchResultDto.PlaylistDto playlist : result.getPlaylists().getItems()) {
                    logger.debug("Playlist '{}' - tracks: {}", playlist.getName(), 
                        playlist.getTracks() != null ? playlist.getTracks().getTotal() : "NULL");
                }
            }
            
            // Filter out null items from track results
            if (result.getTracks() != null && result.getTracks().getItems() != null) {
                result.getTracks().setItems(
                    result.getTracks().getItems().stream()
                        .filter(t -> t != null)
                        .collect(java.util.stream.Collectors.toList())
                );
            }
            
            // Filter out null items from album results
            if (result.getAlbums() != null && result.getAlbums().getItems() != null) {
                result.getAlbums().setItems(
                    result.getAlbums().getItems().stream()
                        .filter(a -> a != null)
                        .collect(java.util.stream.Collectors.toList())
                );
            }
            
            // Filter out null items from artist results
            if (result.getArtists() != null && result.getArtists().getItems() != null) {
                result.getArtists().setItems(
                    result.getArtists().getItems().stream()
                        .filter(a -> a != null)
                        .collect(java.util.stream.Collectors.toList())
                );
            }

            // Filter out null items from show results
            if (result.getShows() != null && result.getShows().getItems() != null) {
                result.getShows().setItems(
                    result.getShows().getItems().stream()
                        .filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList())
                );
            }
            
            if (result.getError() != null) {
                int errorStatus = result.getError().getStatus() > 0 ? result.getError().getStatus() : 400;
                throw new SpotifyApiException(errorStatus, result.getError().getMessage(), responseBody);
            }
            
            long endTime = System.currentTimeMillis();
            logger.info("=== SEARCH METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
            return result;
        }
    }
    
    public DevicesDto getDevices(String userId) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== GET DEVICES METHOD CALLED ===");
        logger.debug("userId: {}", userId);
        
        String accessToken = tokenService.getAccessToken(userId);
        
        String url = SPOTIFY_API_BASE_URL + "/me/player/devices";
        logger.debug("Request URL: {}", url);
        
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        
        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/player/devices took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 204) {
                logger.debug("No devices available (204 response)");
                DevicesDto emptyResult = new DevicesDto();
                emptyResult.setDevices(Collections.emptyList());
                long endTime = System.currentTimeMillis();
                logger.info("=== GET DEVICES METHOD COMPLETED in {}ms (API: {}ms) - No devices ===", endTime - startTime, apiEndTime - apiStartTime);
                return emptyResult;
            }
            
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());

            if (responseBody == null || responseBody.isBlank()) {
                logger.debug("Devices response body is empty");
                DevicesDto emptyResult = new DevicesDto();
                emptyResult.setDevices(Collections.emptyList());
                long endTime = System.currentTimeMillis();
                logger.info("=== GET DEVICES METHOD COMPLETED in {}ms (API: {}ms) - Empty response body ===", endTime - startTime, apiEndTime - apiStartTime);
                return emptyResult;
            }

            if (statusCode >= 400) {
                logger.error("Get devices request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }
            
            DevicesDto result = objectMapper.readValue(responseBody, DevicesDto.class);
            if (result.getDevices() == null) {
                result.setDevices(Collections.emptyList());
            }
            long endTime = System.currentTimeMillis();
            logger.info("=== GET DEVICES METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
            return result;
        }
    }
    
    public PlaybackDto getCurrentPlayback(String userId) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== GET CURRENT PLAYBACK METHOD CALLED ===");
        logger.debug("userId: {}", userId);
        
        String accessToken = tokenService.getAccessToken(userId);
        logger.debug("Access token obtained (first 10 chars): {}...", accessToken.substring(0, Math.min(10, accessToken.length())));
        
        String url = SPOTIFY_API_BASE_URL + "/me/player";
        logger.debug("Request URL: {}", url);
        
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        
        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/player took {}ms", apiEndTime - apiStartTime);
            
            int statusCode = response.getStatusLine().getStatusCode();
            logger.debug("Response status code: {}", statusCode);
            
            if (statusCode == 204) {
                logger.debug("No playback currently active (204 response)");
                long endTime = System.currentTimeMillis();
                logger.info("=== GET CURRENT PLAYBACK METHOD COMPLETED in {}ms (API: {}ms) - No active playback ===", endTime - startTime, apiEndTime - apiStartTime);
                return null;
            }
            
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            logger.debug("Response body: {}", responseBody);

            if (statusCode >= 400) {
                logger.error("Get current playback request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }
            
            PlaybackDto playback = objectMapper.readValue(responseBody, PlaybackDto.class);

            if (playback != null
                    && "episode".equalsIgnoreCase(playback.getCurrentlyPlayingType())
                    && playback.getItem() == null) {
                logger.info("/me/player returned item=null for episode playback. Attempting fallback via /me/player/currently-playing.");
                PlaybackDto fallbackPlayback = fetchCurrentlyPlayingEpisode(accessToken);
                if (fallbackPlayback != null && fallbackPlayback.getItem() != null) {
                    playback.setItem(fallbackPlayback.getItem());
                    if (fallbackPlayback.getProgressMs() > 0) {
                        playback.setProgressMs(fallbackPlayback.getProgressMs());
                    }
                    logger.info("Episode metadata recovered via fallback currently-playing endpoint.");
                } else {
                    logger.warn("Fallback currently-playing endpoint also returned null item for episode playback.");
                }
            }

            long endTime = System.currentTimeMillis();
            logger.info("=== GET CURRENT PLAYBACK METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
            return playback;
        }
    }

    private PlaybackDto fetchCurrentlyPlayingEpisode(String accessToken) {
        String url = SPOTIFY_API_BASE_URL + "/me/player/currently-playing?additional_types=episode";
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 204) {
                EntityUtils.consume(response.getEntity());
                return null;
            }

            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.warn("Fallback currently-playing request failed with status {}: {}", statusCode, responseBody);
                return null;
            }

            return objectMapper.readValue(responseBody, PlaybackDto.class);
        } catch (Exception ex) {
            logger.warn("Fallback currently-playing request threw exception", ex);
            return null;
        }
    }
    
    public void play(String userId, String deviceId, String trackUri) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== PLAY METHOD CALLED ===");
        logger.debug("userId: {}, deviceId: {}, trackUri: {}", userId, deviceId, trackUri);
        
        String accessToken = tokenService.getAccessToken(userId);
        logger.debug("Access token obtained (first 10 chars): {}...", accessToken.substring(0, Math.min(10, accessToken.length())));
        
        String url = SPOTIFY_API_BASE_URL + "/me/player/play";
        if (deviceId != null && !deviceId.isEmpty()) {
            url += "?device_id=" + deviceId;
        }
        logger.debug("Request URL: {}", url);
        
        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Content-Type", "application/json");
        
        if (trackUri != null) {
            Map<String, Object> body = new HashMap<>();
            body.put("uris", Arrays.asList(trackUri));
            String jsonBody = objectMapper.writeValueAsString(body);
            logger.debug("Request body: {}", jsonBody);
            request.setEntity(new StringEntity(jsonBody));
        }
        
        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/player/play took {}ms", apiEndTime - apiStartTime);
            
            int statusCode = response.getStatusLine().getStatusCode();
            logger.debug("Response status code: {}", statusCode);
            
            if (statusCode >= 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.error("Play request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }
            EntityUtils.consume(response.getEntity());
            
            long endTime = System.currentTimeMillis();
            logger.info("=== PLAY METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }
    
    public void playContext(String userId, String deviceId, String contextUri) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== PLAY CONTEXT METHOD CALLED ===");
        logger.debug("userId: {}, deviceId: {}, contextUri: {}", userId, deviceId, contextUri);
        
        String accessToken = tokenService.getAccessToken(userId);
        
        String url = SPOTIFY_API_BASE_URL + "/me/player/play";
        if (deviceId != null && !deviceId.isEmpty()) {
            url += "?device_id=" + deviceId;
        }
        logger.debug("Request URL: {}", url);
        
        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Content-Type", "application/json");
        
        Map<String, Object> body = new HashMap<>();
        body.put("context_uri", contextUri);
        String jsonBody = objectMapper.writeValueAsString(body);
        logger.debug("Request body: {}", jsonBody);
        request.setEntity(new StringEntity(jsonBody));
        
        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/player/play (context) took {}ms", apiEndTime - apiStartTime);
            
            int statusCode = response.getStatusLine().getStatusCode();
            logger.debug("Response status code: {}", statusCode);
            
            if (statusCode >= 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.error("PlayContext request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }
            EntityUtils.consume(response.getEntity());
            
            long endTime = System.currentTimeMillis();
            logger.info("=== PLAY CONTEXT METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }
    
    public void pause(String userId, String deviceId) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== PAUSE METHOD CALLED ===");
        
        String accessToken = tokenService.getAccessToken(userId);
        
        String url = SPOTIFY_API_BASE_URL + "/me/player/pause";
        if (deviceId != null && !deviceId.isEmpty()) {
            url += "?device_id=" + deviceId;
        }
        
        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        
        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/player/pause took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.error("Pause request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }
            EntityUtils.consume(response.getEntity());
            
            long endTime = System.currentTimeMillis();
            logger.info("=== PAUSE METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }
    
    public void next(String userId, String deviceId) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== NEXT METHOD CALLED ===");
        
        String accessToken = tokenService.getAccessToken(userId);
        
        String url = SPOTIFY_API_BASE_URL + "/me/player/next";
        if (deviceId != null && !deviceId.isEmpty()) {
            url += "?device_id=" + deviceId;
        }
        
        HttpPost request = new HttpPost(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        
        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/player/next took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.error("Next request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }
            EntityUtils.consume(response.getEntity());
            
            long endTime = System.currentTimeMillis();
            logger.info("=== NEXT METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }
    
    public void previous(String userId, String deviceId) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== PREVIOUS METHOD CALLED ===");
        
        String accessToken = tokenService.getAccessToken(userId);
        
        String url = SPOTIFY_API_BASE_URL + "/me/player/previous";
        if (deviceId != null && !deviceId.isEmpty()) {
            url += "?device_id=" + deviceId;
        }
        
        HttpPost request = new HttpPost(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        
        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/player/previous took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.error("Previous request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }
            EntityUtils.consume(response.getEntity());
            
            long endTime = System.currentTimeMillis();
            logger.info("=== PREVIOUS METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }
    
    public void transferPlayback(String userId, String deviceId) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== TRANSFER PLAYBACK METHOD CALLED ===");
        
        String accessToken = tokenService.getAccessToken(userId);
        
        String url = SPOTIFY_API_BASE_URL + "/me/player";
        
        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Content-Type", "application/json");
        
        Map<String, Object> body = new HashMap<>();
        body.put("device_ids", Arrays.asList(deviceId));
        body.put("play", false);
        
        String jsonBody = objectMapper.writeValueAsString(body);
        request.setEntity(new StringEntity(jsonBody));
        
        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/player (transfer) took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.error("Transfer playback request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }
            EntityUtils.consume(response.getEntity());
            
            long endTime = System.currentTimeMillis();
            logger.info("=== TRANSFER PLAYBACK METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }
    
    public void seek(String userId, long positionMs, String deviceId) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== SEEK METHOD CALLED ===");
        logger.debug("userId: {}, positionMs: {}, deviceId: {}", userId, positionMs, deviceId);
        
        positionMs = Math.max(0, positionMs);
        
        String accessToken = tokenService.getAccessToken(userId);
        
        String url = SPOTIFY_API_BASE_URL + "/me/player/seek?position_ms=" + positionMs;
        if (deviceId != null && !deviceId.isEmpty()) {
            url += "&device_id=" + deviceId;
        }
        
        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        
        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/player/seek took {}ms", apiEndTime - apiStartTime);
            
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.error("Seek request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }
            EntityUtils.consume(response.getEntity());
            
            long endTime = System.currentTimeMillis();
            logger.info("=== SEEK METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }
    
    public void setVolume(String userId, int volumePercent, String deviceId) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== SET VOLUME METHOD CALLED ===");
        logger.debug("userId: {}, volumePercent: {}, deviceId: {}", userId, volumePercent, deviceId);
        
        // Ensure volume is within valid range
        volumePercent = Math.max(0, Math.min(100, volumePercent));
        
        String accessToken = tokenService.getAccessToken(userId);
        
        String url = SPOTIFY_API_BASE_URL + "/me/player/volume?volume_percent=" + volumePercent;
        if (deviceId != null && !deviceId.isEmpty()) {
            url += "&device_id=" + deviceId;
        }
        
        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        
        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/player/volume took {}ms", apiEndTime - apiStartTime);
            
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.error("Set volume request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }
            EntityUtils.consume(response.getEntity());
            
            long endTime = System.currentTimeMillis();
            logger.info("=== SET VOLUME METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }

    public QueueDto getQueue(String userId) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== GET QUEUE METHOD CALLED ===");
        logger.debug("userId: {}", userId);

        String accessToken = tokenService.getAccessToken(userId);

        String url = SPOTIFY_API_BASE_URL + "/me/player/queue";
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/player/queue took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 204) {
                QueueDto emptyQueue = new QueueDto();
                emptyQueue.setQueue(new ArrayList<>());
                return emptyQueue;
            }

            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Get queue request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }

            QueueDto result = objectMapper.readValue(responseBody, QueueDto.class);
            if (result.getQueue() == null) {
                result.setQueue(new ArrayList<>());
            } else {
                result.setQueue(
                        result.getQueue().stream()
                                .filter(Objects::nonNull)
                                .collect(java.util.stream.Collectors.toList())
                );
            }

            long endTime = System.currentTimeMillis();
            logger.info("=== GET QUEUE METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
            return result;
        }
    }

    public void addToQueue(String userId, String uri, String deviceId) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== ADD TO QUEUE METHOD CALLED ===");
        logger.debug("userId: {}, uri: {}, deviceId: {}", userId, uri, deviceId);

        String accessToken = tokenService.getAccessToken(userId);

        addUriToQueue(accessToken, uri, deviceId);

        long endTime = System.currentTimeMillis();
        logger.info("=== ADD TO QUEUE METHOD COMPLETED in {}ms ===", endTime - startTime);
    }

    public void addToQueue(String userId, String uri, String id, String type, String deviceId) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== ADD TO QUEUE (URI/ID-TYPE) METHOD CALLED ===");
        logger.debug("userId: {}, uri: {}, id: {}, type: {}, deviceId: {}", userId, uri, id, type, deviceId);

        String accessToken = tokenService.getAccessToken(userId);

        if (uri != null && !uri.isBlank()) {
            addUriToQueue(accessToken, uri, deviceId);
            long endTime = System.currentTimeMillis();
            logger.info("=== ADD TO QUEUE (URI/ID-TYPE) METHOD COMPLETED in {}ms ===", endTime - startTime);
            return;
        }

        List<String> uris = resolveQueueUrisByIdType(accessToken, id, type);
        if (uris.isEmpty()) {
            throw new IOException("No queueable tracks found for id=" + id + ", type=" + type);
        }

        int queuedCount = 0;

        for (String queueUri : uris) {
            addUriToQueue(accessToken, queueUri, deviceId);
            queuedCount++;
        }

        long endTime = System.currentTimeMillis();
        logger.info("=== ADD TO QUEUE (URI/ID-TYPE) METHOD COMPLETED in {}ms; queued {} item(s) ===", endTime - startTime, queuedCount);
    }

    private void addUriToQueue(String accessToken, String uri, String deviceId) throws IOException {
        addUriToQueue(accessToken, uri, deviceId, true);
    }

    private void addUriToQueue(String accessToken, String uri, String deviceId, boolean allowRetryWithoutDevice) throws IOException {
        if (uri == null || uri.isBlank()) {
            throw new IOException("Queue URI is required");
        }

        String encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8);
        StringBuilder urlBuilder = new StringBuilder(SPOTIFY_API_BASE_URL)
                .append("/me/player/queue?uri=")
                .append(encodedUri);

        if (deviceId != null && !deviceId.isEmpty()) {
            urlBuilder.append("&device_id=")
                    .append(URLEncoder.encode(deviceId, StandardCharsets.UTF_8));
        }

        HttpPost request = new HttpPost(urlBuilder.toString());
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/player/queue took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 400) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.error("Add to queue request failed with status {}: {}", statusCode, responseBody);

                // Some active-player states reject explicit device_id, but succeed without it.
                if (statusCode == 403 && allowRetryWithoutDevice && deviceId != null && !deviceId.isBlank()) {
                    logger.warn("Retrying add-to-queue without device_id after 403 for uri={}", uri);
                    addUriToQueue(accessToken, uri, null, false);
                    return;
                }

                throwSpotifyApiError(statusCode, responseBody);
            }

            EntityUtils.consume(response.getEntity());
        }
    }

    private List<String> resolveQueueUrisByIdType(String accessToken, String id, String type) throws IOException {
        if (id == null || id.isBlank() || type == null || type.isBlank()) {
            return Collections.emptyList();
        }

        String normalizedType = type.trim().toLowerCase(Locale.ROOT);
        String encodedId = URLEncoder.encode(id.trim(), StandardCharsets.UTF_8);

        if ("track".equals(normalizedType)) {
            return Collections.singletonList("spotify:track:" + id.trim());
        }

        if ("album".equals(normalizedType)) {
            return fetchAlbumTrackUris(accessToken, encodedId);
        }

        return Collections.emptyList();
    }

    private List<String> fetchAlbumTrackUris(String accessToken, String encodedAlbumId) throws IOException {
        List<String> uris = new ArrayList<>();
        int offset = 0;
        int limit = 50;

        while (true) {
            String url = SPOTIFY_API_BASE_URL + "/albums/" + encodedAlbumId + "/tracks?limit=" + limit + "&offset=" + offset;
            JsonNode root = getJson(accessToken, url);
            JsonNode items = root.path("items");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String uri = item.path("uri").asText("");
                    if (!uri.isBlank()) {
                        uris.add(uri);
                    }
                }
            }

            String next = root.path("next").asText("");
            if (next.isBlank()) {
                break;
            }
            offset += limit;
        }

        return uris;
    }

    private JsonNode getJson(String accessToken, String url) throws IOException {
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                throwSpotifyApiError(statusCode, responseBody);
            }

            return objectMapper.readTree(responseBody);
        }
    }
    
    public SearchResultDto.PlaylistsDto getMyPlaylists(String userId, int limit, int offset) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== GET MY PLAYLISTS METHOD CALLED ===");
        logger.debug("userId: {}, limit: {}, offset: {}", userId, limit, offset);
        
        String accessToken = tokenService.getAccessToken(userId);
        
        String url = SPOTIFY_API_BASE_URL + "/me/playlists?limit=" + limit + "&offset=" + offset;
        logger.debug("Request URL: {}", url);
        
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        
        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/playlists took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Get playlists request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }
            
            SearchResultDto.PlaylistsDto result = objectMapper.readValue(responseBody, SearchResultDto.PlaylistsDto.class);
            
            // Filter out null items
            if (result.getItems() != null) {
                result.setItems(
                    result.getItems().stream()
                        .filter(p -> p != null)
                        .collect(java.util.stream.Collectors.toList())
                );
            }
            
            long endTime = System.currentTimeMillis();
            logger.info("=== GET MY PLAYLISTS METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
            return result;
        }
    }
    
    public SearchResultDto.TracksDto getLikedSongs(String userId, int limit, int offset) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== GET LIKED SONGS METHOD CALLED ===");
        logger.debug("userId: {}, limit: {}, offset: {}", userId, limit, offset);
        
        String accessToken = tokenService.getAccessToken(userId);
        
        String url = SPOTIFY_API_BASE_URL + "/me/tracks?limit=" + limit + "&offset=" + offset;
        logger.debug("Request URL: {}", url);
        
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        
        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/tracks took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Get liked songs request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }
            
            // Spotify returns saved tracks in a wrapper with "added_at" field
            // We need to parse it and extract the tracks
            SavedTracksResponse savedTracks = objectMapper.readValue(responseBody, SavedTracksResponse.class);
            
            SearchResultDto.TracksDto result = new SearchResultDto.TracksDto();
            result.setHref(savedTracks.getHref());
            result.setLimit(savedTracks.getLimit());
            result.setNext(savedTracks.getNext());
            result.setOffset(savedTracks.getOffset());
            result.setPrevious(savedTracks.getPrevious());
            result.setTotal(savedTracks.getTotal());
            
            if (savedTracks.getItems() != null) {
                List<SearchResultDto.TrackDto> tracks = savedTracks.getItems().stream()
                    .filter(item -> item != null && item.getTrack() != null)
                    .map(SavedTrackItem::getTrack)
                    .collect(java.util.stream.Collectors.toList());
                result.setItems(tracks);
            }
            
            long endTime = System.currentTimeMillis();
            logger.info("=== GET LIKED SONGS METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
            return result;
        }
    }

    public void saveLikedSongs(String userId, String ids) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== SAVE LIKED SONGS METHOD CALLED ===");
        logger.debug("userId: {}, ids: {}", userId, ids);

        String accessToken = tokenService.getAccessToken(userId);
        String normalizedUris = normalizeTrackUrisCsv(ids);
        String url = SPOTIFY_API_BASE_URL + "/me/library?uris=" + URLEncoder.encode(normalizedUris, StandardCharsets.UTF_8);
        logger.debug("Request URL: {}", url);

        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = executeWithRateLimitRetry(request, "save liked songs")) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API PUT /me/library took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Save liked songs request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }

            long endTime = System.currentTimeMillis();
            logger.info("=== SAVE LIKED SONGS METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }

    public void removeLikedSongs(String userId, String ids) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== REMOVE LIKED SONGS METHOD CALLED ===");
        logger.debug("userId: {}, ids: {}", userId, ids);

        String accessToken = tokenService.getAccessToken(userId);
        String normalizedUris = normalizeTrackUrisCsv(ids);
        String url = SPOTIFY_API_BASE_URL + "/me/library?uris=" + URLEncoder.encode(normalizedUris, StandardCharsets.UTF_8);
        logger.debug("Request URL: {}", url);

        HttpDelete request = new HttpDelete(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = executeWithRateLimitRetry(request, "remove liked songs")) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API DELETE /me/library took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Remove liked songs request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }

            long endTime = System.currentTimeMillis();
            logger.info("=== REMOVE LIKED SONGS METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }

    private String normalizeTrackUrisCsv(String ids) throws IOException {
        if (ids == null || ids.isBlank()) {
            throw new IOException("Track ids are required");
        }

        List<String> normalizedUris = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .map(this::toTrackUri)
                .distinct()
                .toList();

        if (normalizedUris.isEmpty()) {
            throw new IOException("Track ids are required");
        }

        if (normalizedUris.size() > 40) {
            throw new IOException("A maximum of 40 track IDs is allowed per request");
        }

        return String.join(",", normalizedUris);
    }

    private String toTrackUri(String token) {
        if (token.startsWith("spotify:track:")) {
            return token;
        }

        String trimmed = token.trim();
        if (trimmed.startsWith("https://open.spotify.com/track/")) {
            String withoutPrefix = trimmed.substring("https://open.spotify.com/track/".length());
            String id = withoutPrefix.split("\\?")[0];
            return "spotify:track:" + id;
        }

        if (trimmed.matches("^[A-Za-z0-9]{22}$")) {
            return "spotify:track:" + trimmed;
        }

        throw new IllegalArgumentException("Invalid track id or URI: " + token);
    }
    
    public RecentlyPlayedResponse getRecentlyPlayed(String userId, int limit, String before) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== GET RECENTLY PLAYED METHOD CALLED ===");
        logger.debug("userId: {}, limit: {}, before: {}", userId, limit, before);

        String accessToken = tokenService.getAccessToken(userId);

        String url = SPOTIFY_API_BASE_URL + "/me/player/recently-played?limit=" + limit;
        if (before != null && !before.isEmpty()) {
            url += "&before=" + before;
        }
        logger.debug("Request URL: {}", url);

        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/player/recently-played took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());

            logger.debug("Recently played response status: {}", statusCode);
            logger.debug("Recently played response body: {}", responseBody);

            if (statusCode >= 400) {
                logger.error("Get recently played request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }

            RecentlyPlayedResponse result = objectMapper.readValue(responseBody, RecentlyPlayedResponse.class);

            if (result.getItems() != null) {
                result.setItems(
                        result.getItems().stream()
                                .filter(item -> item != null && item.getTrack() != null)
                                .collect(java.util.stream.Collectors.toList())
                );
            }

            long endTime = System.currentTimeMillis();
            logger.info("=== GET RECENTLY PLAYED METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
            return result;
        }
    }

    public SearchResultDto.TracksDto getAlbumTracks(String userId, String albumId, int limit, int offset) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== GET ALBUM TRACKS METHOD CALLED ===");
        logger.debug("userId: {}, albumId: {}, limit: {}, offset: {}", userId, albumId, limit, offset);

        String accessToken = tokenService.getAccessToken(userId);
        int sanitizedLimit = Math.max(1, Math.min(limit, 50));
        int sanitizedOffset = Math.max(0, offset);

        String encodedAlbumId = URLEncoder.encode(albumId, StandardCharsets.UTF_8);
        String url = SPOTIFY_API_BASE_URL + "/albums/" + encodedAlbumId + "/tracks?limit=" + sanitizedLimit + "&offset=" + sanitizedOffset;
        logger.debug("Request URL: {}", url);

        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /albums/{id}/tracks took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();

            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Get album tracks request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }

            SearchResultDto.TracksDto result = objectMapper.readValue(responseBody, SearchResultDto.TracksDto.class);
            if (result.getItems() != null) {
                result.setItems(
                        result.getItems().stream()
                                .filter(Objects::nonNull)
                                .collect(java.util.stream.Collectors.toList())
                );
            }

            long endTime = System.currentTimeMillis();
            logger.info("=== GET ALBUM TRACKS METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
            return result;
        }
    }

    public SearchResultDto.ShowsDto getSavedShows(String userId, int limit, int offset) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== GET SAVED SHOWS METHOD CALLED ===");
        logger.debug("userId: {}, limit: {}, offset: {}", userId, limit, offset);

        String accessToken = tokenService.getAccessToken(userId);
        int sanitizedLimit = Math.max(1, Math.min(limit, 50));
        int sanitizedOffset = Math.max(0, offset);

        String url = SPOTIFY_API_BASE_URL + "/me/shows?limit=" + sanitizedLimit + "&offset=" + sanitizedOffset;
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/shows took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Get saved shows request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }

            SavedShowsResponse savedShows = objectMapper.readValue(responseBody, SavedShowsResponse.class);
            SearchResultDto.ShowsDto result = new SearchResultDto.ShowsDto();
            result.setHref(savedShows.getHref());
            result.setLimit(savedShows.getLimit());
            result.setNext(savedShows.getNext());
            result.setOffset(savedShows.getOffset());
            result.setPrevious(savedShows.getPrevious());
            result.setTotal(savedShows.getTotal());

            if (savedShows.getItems() != null) {
                result.setItems(
                    savedShows.getItems().stream()
                        .filter(item -> item != null && item.getShow() != null)
                        .map(SavedShowItem::getShow)
                        .collect(java.util.stream.Collectors.toList())
                );
            } else {
                result.setItems(Collections.emptyList());
            }

            long endTime = System.currentTimeMillis();
            logger.info("=== GET SAVED SHOWS METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
            return result;
        }
    }

    public SearchResultDto.ShowDto getShow(String userId, String showId) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== GET SHOW METHOD CALLED ===");
        logger.debug("userId: {}, showId: {}", userId, showId);

        String accessToken = tokenService.getAccessToken(userId);
        String encodedShowId = URLEncoder.encode(showId, StandardCharsets.UTF_8);
        String url = SPOTIFY_API_BASE_URL + "/shows/" + encodedShowId;

        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /shows/{id} took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Get show request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }

            SearchResultDto.ShowDto result = objectMapper.readValue(responseBody, SearchResultDto.ShowDto.class);
            long endTime = System.currentTimeMillis();
            logger.info("=== GET SHOW METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
            return result;
        }
    }

    public ShowEpisodesResponse getShowEpisodes(String userId, String showId, int limit, int offset) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== GET SHOW EPISODES METHOD CALLED ===");
        logger.debug("userId: {}, showId: {}, limit: {}, offset: {}", userId, showId, limit, offset);

        String accessToken = tokenService.getAccessToken(userId);
        int sanitizedLimit = Math.max(1, Math.min(limit, 50));
        int sanitizedOffset = Math.max(0, offset);
        String encodedShowId = URLEncoder.encode(showId, StandardCharsets.UTF_8);

        String url = SPOTIFY_API_BASE_URL + "/shows/" + encodedShowId + "/episodes?limit=" + sanitizedLimit + "&offset=" + sanitizedOffset;
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /shows/{id}/episodes took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Get show episodes request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }

            ShowEpisodesResponse result = objectMapper.readValue(responseBody, ShowEpisodesResponse.class);
            if (result.getItems() != null) {
                result.setItems(
                    result.getItems().stream()
                        .filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList())
                );
            } else {
                result.setItems(Collections.emptyList());
            }

            long endTime = System.currentTimeMillis();
            logger.info("=== GET SHOW EPISODES METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
            return result;
        }
    }

    public void saveShows(String userId, String ids) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== SAVE SHOWS METHOD CALLED ===");
        logger.debug("userId: {}, ids: {}", userId, ids);

        String accessToken = tokenService.getAccessToken(userId);
        String normalizedUris = normalizeShowUrisCsv(ids);
        String url = SPOTIFY_API_BASE_URL + "/me/library?uris=" + URLEncoder.encode(normalizedUris, StandardCharsets.UTF_8);

        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = executeWithRateLimitRetry(request, "save shows")) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API PUT /me/library (shows) took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Save shows request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }

            long endTime = System.currentTimeMillis();
            logger.info("=== SAVE SHOWS METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }

    public void removeShows(String userId, String ids) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== REMOVE SHOWS METHOD CALLED ===");
        logger.debug("userId: {}, ids: {}", userId, ids);

        String accessToken = tokenService.getAccessToken(userId);
        String normalizedUris = normalizeShowUrisCsv(ids);
        String url = SPOTIFY_API_BASE_URL + "/me/library?uris=" + URLEncoder.encode(normalizedUris, StandardCharsets.UTF_8);

        HttpDelete request = new HttpDelete(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = executeWithRateLimitRetry(request, "remove shows")) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API DELETE /me/library (shows) took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Remove shows request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }

            long endTime = System.currentTimeMillis();
            logger.info("=== REMOVE SHOWS METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }

    public ShowEpisodesResponse getSavedEpisodes(String userId, int limit, int offset) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== GET SAVED EPISODES METHOD CALLED ===");
        logger.debug("userId: {}, limit: {}, offset: {}", userId, limit, offset);

        String accessToken = tokenService.getAccessToken(userId);
        int sanitizedLimit = Math.max(1, Math.min(limit, 50));
        int sanitizedOffset = Math.max(0, offset);

        String url = SPOTIFY_API_BASE_URL + "/me/episodes?limit=" + sanitizedLimit + "&offset=" + sanitizedOffset;
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API /me/episodes took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Get saved episodes request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }

            SavedEpisodesResponse savedEpisodes = objectMapper.readValue(responseBody, SavedEpisodesResponse.class);
            ShowEpisodesResponse result = new ShowEpisodesResponse();
            result.setHref(savedEpisodes.getHref());
            result.setLimit(savedEpisodes.getLimit());
            result.setNext(savedEpisodes.getNext());
            result.setOffset(savedEpisodes.getOffset());
            result.setPrevious(savedEpisodes.getPrevious());
            result.setTotal(savedEpisodes.getTotal());

            if (savedEpisodes.getItems() != null) {
                result.setItems(
                    savedEpisodes.getItems().stream()
                        .filter(item -> item != null && item.getEpisode() != null)
                        .map(item -> {
                            SearchResultDto.EpisodeDto episode = item.getEpisode();
                            episode.setAddedAt(item.getAddedAt());
                            return episode;
                        })
                        .collect(java.util.stream.Collectors.toList())
                );
            } else {
                result.setItems(Collections.emptyList());
            }

            long endTime = System.currentTimeMillis();
            logger.info("=== GET SAVED EPISODES METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
            return result;
        }
    }

    public void saveEpisodes(String userId, String ids) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== SAVE EPISODES METHOD CALLED ===");
        logger.debug("userId: {}, ids: {}", userId, ids);

        String accessToken = tokenService.getAccessToken(userId);
        String normalizedUris = normalizeEpisodeUrisCsv(ids);
        String url = SPOTIFY_API_BASE_URL + "/me/library?uris=" + URLEncoder.encode(normalizedUris, StandardCharsets.UTF_8);

        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = executeWithRateLimitRetry(request, "save episodes")) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API PUT /me/library (episodes) took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Save episodes request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }

            long endTime = System.currentTimeMillis();
            logger.info("=== SAVE EPISODES METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }

    public void removeEpisodes(String userId, String ids) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.debug("=== REMOVE EPISODES METHOD CALLED ===");
        logger.debug("userId: {}, ids: {}", userId, ids);

        String accessToken = tokenService.getAccessToken(userId);
        String normalizedUris = normalizeEpisodeUrisCsv(ids);
        String url = SPOTIFY_API_BASE_URL + "/me/library?uris=" + URLEncoder.encode(normalizedUris, StandardCharsets.UTF_8);

        HttpDelete request = new HttpDelete(url);
        request.setHeader("Authorization", "Bearer " + accessToken);

        long apiStartTime = System.currentTimeMillis();
        try (CloseableHttpResponse response = executeWithRateLimitRetry(request, "remove episodes")) {
            long apiEndTime = System.currentTimeMillis();
            logger.info("Spotify API DELETE /me/library (episodes) took {}ms", apiEndTime - apiStartTime);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
            EntityUtils.consume(response.getEntity());

            if (statusCode >= 400) {
                logger.error("Remove episodes request failed with status {}: {}", statusCode, responseBody);
                throwSpotifyApiError(statusCode, responseBody);
            }

            long endTime = System.currentTimeMillis();
            logger.info("=== REMOVE EPISODES METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
        }
    }

    private String normalizeShowUrisCsv(String ids) throws IOException {
        if (ids == null || ids.isBlank()) {
            throw new IOException("Show ids are required");
        }

        List<String> normalizedUris = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .map(this::toShowUri)
                .distinct()
                .toList();

        if (normalizedUris.isEmpty()) {
            throw new IOException("Show ids are required");
        }

        if (normalizedUris.size() > 50) {
            throw new IOException("A maximum of 50 show IDs is allowed per request");
        }

        return String.join(",", normalizedUris);
    }

    private String toShowUri(String token) {
        String trimmed = token.trim();

        if (trimmed.startsWith("spotify:show:")) {
            return trimmed;
        }

        if (trimmed.matches("^[A-Za-z0-9]{22}$")) {
            return "spotify:show:" + trimmed;
        }

        if (trimmed.startsWith("https://open.spotify.com/show/")) {
            String withoutPrefix = trimmed.substring("https://open.spotify.com/show/".length());
            String id = withoutPrefix.split("\\?")[0];
            return "spotify:show:" + id;
        }

        throw new IllegalArgumentException("Invalid show id or URI: " + token);
    }

    private String normalizeEpisodeUrisCsv(String ids) throws IOException {
        if (ids == null || ids.isBlank()) {
            throw new IOException("Episode ids are required");
        }

        List<String> normalizedUris = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .map(this::toEpisodeUri)
                .distinct()
                .toList();

        if (normalizedUris.isEmpty()) {
            throw new IOException("Episode ids are required");
        }

        if (normalizedUris.size() > 50) {
            throw new IOException("A maximum of 50 episode IDs is allowed per request");
        }

        return String.join(",", normalizedUris);
    }

    private String toEpisodeUri(String token) {
        String trimmed = token.trim();

        if (trimmed.matches("^[A-Za-z0-9]{22}$")) {
            return "spotify:episode:" + trimmed;
        }

        if (trimmed.startsWith("spotify:episode:")) {
            return trimmed;
        }

        if (trimmed.startsWith("https://open.spotify.com/episode/")) {
            String withoutPrefix = trimmed.substring("https://open.spotify.com/episode/".length());
            return "spotify:episode:" + withoutPrefix.split("\\?")[0];
        }

        throw new IllegalArgumentException("Invalid episode id or URI: " + token);
    }
    
    // Inner classes for parsing Spotify responses
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class SavedTracksResponse {
        private String href;
        private int limit;
        private String next;
        private int offset;
        private String previous;
        private int total;
        private List<SavedTrackItem> items;
        
        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }
        public int getOffset() { return offset; }
        public void setOffset(int offset) { this.offset = offset; }
        public String getPrevious() { return previous; }
        public void setPrevious(String previous) { this.previous = previous; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public List<SavedTrackItem> getItems() { return items; }
        public void setItems(List<SavedTrackItem> items) { this.items = items; }
    }
    
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class SavedTrackItem {
        @com.fasterxml.jackson.annotation.JsonProperty("added_at")
        private String addedAt;
        private SearchResultDto.TrackDto track;
        
        public String getAddedAt() { return addedAt; }
        public void setAddedAt(String addedAt) { this.addedAt = addedAt; }
        public SearchResultDto.TrackDto getTrack() { return track; }
        public void setTrack(SearchResultDto.TrackDto track) { this.track = track; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class SavedShowsResponse {
        private String href;
        private int limit;
        private String next;
        private int offset;
        private String previous;
        private int total;
        private List<SavedShowItem> items;

        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }
        public int getOffset() { return offset; }
        public void setOffset(int offset) { this.offset = offset; }
        public String getPrevious() { return previous; }
        public void setPrevious(String previous) { this.previous = previous; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public List<SavedShowItem> getItems() { return items; }
        public void setItems(List<SavedShowItem> items) { this.items = items; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class SavedShowItem {
        @com.fasterxml.jackson.annotation.JsonProperty("added_at")
        private String addedAt;
        private SearchResultDto.ShowDto show;

        public String getAddedAt() { return addedAt; }
        public void setAddedAt(String addedAt) { this.addedAt = addedAt; }
        public SearchResultDto.ShowDto getShow() { return show; }
        public void setShow(SearchResultDto.ShowDto show) { this.show = show; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShowEpisodesResponse {
        private String href;
        private int limit;
        private String next;
        private int offset;
        private String previous;
        private int total;
        private List<SearchResultDto.EpisodeDto> items;

        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }
        public int getOffset() { return offset; }
        public void setOffset(int offset) { this.offset = offset; }
        public String getPrevious() { return previous; }
        public void setPrevious(String previous) { this.previous = previous; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public List<SearchResultDto.EpisodeDto> getItems() { return items; }
        public void setItems(List<SearchResultDto.EpisodeDto> items) { this.items = items; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class SavedEpisodesResponse {
        private String href;
        private int limit;
        private String next;
        private int offset;
        private String previous;
        private int total;
        private List<SavedEpisodeItem> items;

        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }
        public int getOffset() { return offset; }
        public void setOffset(int offset) { this.offset = offset; }
        public String getPrevious() { return previous; }
        public void setPrevious(String previous) { this.previous = previous; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public List<SavedEpisodeItem> getItems() { return items; }
        public void setItems(List<SavedEpisodeItem> items) { this.items = items; }
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class SavedEpisodeItem {
        @com.fasterxml.jackson.annotation.JsonProperty("added_at")
        private String addedAt;
        private SearchResultDto.EpisodeDto episode;

        public String getAddedAt() { return addedAt; }
        public void setAddedAt(String addedAt) { this.addedAt = addedAt; }
        public SearchResultDto.EpisodeDto getEpisode() { return episode; }
        public void setEpisode(SearchResultDto.EpisodeDto episode) { this.episode = episode; }
    }
    
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecentlyPlayedResponse {
        private String href;
        private int limit;
        private String next;
        private CursorsDto cursors;
        private int total;
        private List<PlayHistoryItem> items;
        
        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }
        public CursorsDto getCursors() { return cursors; }
        public void setCursors(CursorsDto cursors) { this.cursors = cursors; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public List<PlayHistoryItem> getItems() { return items; }
        public void setItems(List<PlayHistoryItem> items) { this.items = items; }
    }
    
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class CursorsDto {
        private String after;
        private String before;
        
        public String getAfter() { return after; }
        public void setAfter(String after) { this.after = after; }
        public String getBefore() { return before; }
        public void setBefore(String before) { this.before = before; }
    }
    
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayHistoryItem {
        private SearchResultDto.TrackDto track;
        @com.fasterxml.jackson.annotation.JsonProperty("played_at")
        private String playedAt;
        private ContextDto context;
        
        public SearchResultDto.TrackDto getTrack() { return track; }
        public void setTrack(SearchResultDto.TrackDto track) { this.track = track; }
        public String getPlayedAt() { return playedAt; }
        public void setPlayedAt(String playedAt) { this.playedAt = playedAt; }
        public ContextDto getContext() { return context; }
        public void setContext(ContextDto context) { this.context = context; }
    }
    
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContextDto {
        private String type;
        private String href;
        @com.fasterxml.jackson.annotation.JsonProperty("external_urls")
        private SearchResultDto.ExternalUrls externalUrls;
        private String uri;
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
        public SearchResultDto.ExternalUrls getExternalUrls() { return externalUrls; }
        public void setExternalUrls(SearchResultDto.ExternalUrls externalUrls) { this.externalUrls = externalUrls; }
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
    }
}
