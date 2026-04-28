package com.spotify.wrapper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.wrapper.dto.DevicesDto;
import com.spotify.wrapper.dto.PlaybackDto;
import com.spotify.wrapper.dto.QueueDto;
import com.spotify.wrapper.dto.SearchResultDto;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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
            
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            
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
            
            if (result.getError() != null) {
                throw new IOException("Spotify API error: " + result.getError().getMessage() + 
                                   " (Status: " + result.getError().getStatus() + ")");
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
            
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            
            DevicesDto result = objectMapper.readValue(responseBody, DevicesDto.class);
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
            
            PlaybackDto playback = objectMapper.readValue(responseBody, PlaybackDto.class);
            long endTime = System.currentTimeMillis();
            logger.info("=== GET CURRENT PLAYBACK METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
            return playback;
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
                throw new IOException("Spotify API error: " + responseBody);
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
                throw new IOException("Spotify API error: " + responseBody);
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
                throw new IOException("Spotify API error: " + responseBody);
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
                throw new IOException("Spotify API error: " + responseBody);
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
                throw new IOException("Spotify API error: " + responseBody);
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
                throw new IOException("Spotify API error: " + responseBody);
            }

            EntityUtils.consume(response.getEntity());

            long endTime = System.currentTimeMillis();
            logger.info("=== ADD TO QUEUE METHOD COMPLETED in {}ms (API: {}ms) ===", endTime - startTime, apiEndTime - apiStartTime);
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
            
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            
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
            
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            
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
            
            RecentlyPlayedResponse result = objectMapper.readValue(responseBody, RecentlyPlayedResponse.class);
            
            // Filter out null items
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
