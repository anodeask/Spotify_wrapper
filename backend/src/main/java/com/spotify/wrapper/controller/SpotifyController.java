package com.spotify.wrapper.controller;

import com.spotify.wrapper.dto.DevicesDto;
import com.spotify.wrapper.dto.PlaybackDto;
import com.spotify.wrapper.dto.QueueDto;
import com.spotify.wrapper.dto.SearchResultDto;
import com.spotify.wrapper.service.SpotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {
    
    private static final Logger logger = LoggerFactory.getLogger(SpotifyController.class);
    
    @Autowired
    private SpotifyService spotifyService;
    
    @GetMapping("/search")
    public ResponseEntity<SearchResultDto> search(
            @RequestParam String userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "track,playlist") String type,
            @RequestParam(defaultValue = "10") int limit) {
        logger.info("Search request - userId: {}, query: '{}', type: '{}', limit: {}", userId, query, type, limit);
        
        try {
            SearchResultDto result = spotifyService.search(userId, query, type, limit);
            logger.info("Search completed successfully for userId: {}, found {} results", userId, 
                    (result.getTracks() != null && result.getTracks().getItems() != null ? result.getTracks().getItems().size() : 0) + 
                    (result.getPlaylists() != null && result.getPlaylists().getItems() != null ? result.getPlaylists().getItems().size() : 0) + 
                    (result.getAlbums() != null && result.getAlbums().getItems() != null ? result.getAlbums().getItems().size() : 0));
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("Search failed for userId: {}, query: '{}'", userId, query, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/devices")
    public ResponseEntity<DevicesDto> getDevices(@RequestParam String userId) {
        logger.info("Get devices request for userId: {}", userId);
        
        try {
            DevicesDto devices = spotifyService.getDevices(userId);
            logger.info("Devices retrieved successfully for userId: {}, found {} devices", 
                    userId, devices.getDevices() != null ? devices.getDevices().size() : 0);
            return ResponseEntity.ok(devices);
        } catch (IOException e) {
            logger.error("Failed to get devices for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/playback")
    public ResponseEntity<PlaybackDto> getCurrentPlayback(@RequestParam String userId) {
        logger.info("Get current playback request for userId: {}", userId);
        
        try {
            PlaybackDto playback = spotifyService.getCurrentPlayback(userId);
            logger.info("Current playback retrieved successfully for userId: {}", userId);
            return ResponseEntity.ok(playback);
        } catch (IOException e) {
            logger.error("Failed to get current playback for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/current-track")
    public ResponseEntity<PlaybackDto> getCurrentTrack(@RequestParam String userId) {
        logger.info("Get current track request for userId: {}", userId);
        
        try {
            PlaybackDto playback = spotifyService.getCurrentPlayback(userId);
            logger.info("Current track retrieved successfully for userId: {}", userId);
            return ResponseEntity.ok(playback);
        } catch (IOException e) {
            logger.error("Failed to get current track for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/play")
    public ResponseEntity<Void> play(
            @RequestParam String userId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String trackUri) {
        logger.info("Play request - userId: {}, deviceId: {}, trackUri: {}", userId, deviceId, trackUri);
        
        try {
            spotifyService.play(userId, deviceId, trackUri);
            logger.info("Play command executed successfully for userId: {}", userId);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            logger.error("Failed to execute play command for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/play-playlist")
    public ResponseEntity<Void> playPlaylist(
            @RequestParam String userId,
            @RequestParam(required = false) String deviceId,
            @RequestParam String contextUri) {
        logger.info("Play playlist/album request - userId: {}, deviceId: {}, contextUri: {}", userId, deviceId, contextUri);
        
        try {
            spotifyService.playContext(userId, deviceId, contextUri);
            logger.info("Play playlist/album command executed successfully for userId: {}", userId);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            logger.error("Failed to execute play playlist/album command for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/pause")
    public ResponseEntity<Void> pause(
            @RequestParam String userId,
            @RequestParam(required = false) String deviceId) {
        logger.info("Pause request - userId: {}, deviceId: {}", userId, deviceId);
        
        try {
            spotifyService.pause(userId, deviceId);
            logger.info("Pause command executed successfully for userId: {}", userId);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            logger.error("Failed to execute pause command for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/next")
    public ResponseEntity<Void> next(
            @RequestParam String userId,
            @RequestParam(required = false) String deviceId) {
        logger.info("Next track request - userId: {}, deviceId: {}", userId, deviceId);
        
        try {
            spotifyService.next(userId, deviceId);
            logger.info("Next track command executed successfully for userId: {}", userId);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            logger.error("Failed to execute next track command for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/previous")
    public ResponseEntity<Void> previous(
            @RequestParam String userId,
            @RequestParam(required = false) String deviceId) {
        logger.info("Previous track request - userId: {}, deviceId: {}", userId, deviceId);
        
        try {
            spotifyService.previous(userId, deviceId);
            logger.info("Previous track command executed successfully for userId: {}", userId);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            logger.error("Failed to execute previous track command for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/transfer")
    public ResponseEntity<Void> transferPlayback(
            @RequestParam String userId,
            @RequestParam String deviceId) {
        logger.info("Transfer playback request - userId: {}, targetDeviceId: {}", userId, deviceId);
        
        try {
            spotifyService.transferPlayback(userId, deviceId);
            logger.info("Playback transfer executed successfully for userId: {} to deviceId: {}", userId, deviceId);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            logger.error("Failed to transfer playback for userId: {} to deviceId: {}", userId, deviceId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/volume")
    public ResponseEntity<Void> setVolume(
            @RequestParam String userId,
            @RequestParam int volumePercent,
            @RequestParam(required = false) String deviceId) {
        logger.info("Set volume request - userId: {}, volumePercent: {}, deviceId: {}", userId, volumePercent, deviceId);
        
        try {
            spotifyService.setVolume(userId, volumePercent, deviceId);
            logger.info("Volume set successfully for userId: {} to {}%", userId, volumePercent);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            logger.error("Failed to set volume for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/seek")
    public ResponseEntity<Void> seek(
            @RequestParam String userId,
            @RequestParam long positionMs,
            @RequestParam(required = false) String deviceId) {
        logger.info("Seek request - userId: {}, positionMs: {}, deviceId: {}", userId, positionMs, deviceId);
        
        try {
            spotifyService.seek(userId, positionMs, deviceId);
            logger.info("Seek command executed successfully for userId: {} to {}ms", userId, positionMs);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            logger.error("Failed to seek for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/queue")
    public ResponseEntity<QueueDto> getQueue(@RequestParam String userId) {
        logger.info("Get queue request for userId: {}", userId);

        try {
            QueueDto queue = spotifyService.getQueue(userId);
            logger.info("Queue retrieved successfully for userId: {}, queue size: {}",
                    userId, queue.getQueue() != null ? queue.getQueue().size() : 0);
            return ResponseEntity.ok(queue);
        } catch (IOException e) {
            logger.error("Failed to get queue for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/queue/add")
    public ResponseEntity<Void> addToQueue(
            @RequestParam String userId,
            @RequestParam String uri,
            @RequestParam(required = false) String deviceId) {
        logger.info("Add to queue request - userId: {}, uri: {}, deviceId: {}", userId, uri, deviceId);

        try {
            spotifyService.addToQueue(userId, uri, deviceId);
            logger.info("Add to queue command executed successfully for userId: {}", userId);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            logger.error("Failed to add to queue for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/me/playlists")
    public ResponseEntity<SearchResultDto.PlaylistsDto> getMyPlaylists(
            @RequestParam String userId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        logger.info("Get my playlists request - userId: {}, limit: {}, offset: {}", userId, limit, offset);
        
        try {
            SearchResultDto.PlaylistsDto playlists = spotifyService.getMyPlaylists(userId, limit, offset);
            logger.info("My playlists retrieved successfully for userId: {}, found {} playlists", 
                    userId, playlists.getItems() != null ? playlists.getItems().size() : 0);
            return ResponseEntity.ok(playlists);
        } catch (IOException e) {
            logger.error("Failed to get playlists for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/me/tracks")
    public ResponseEntity<SearchResultDto.TracksDto> getLikedSongs(
            @RequestParam String userId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        logger.info("Get liked songs request - userId: {}, limit: {}, offset: {}", userId, limit, offset);
        
        try {
            SearchResultDto.TracksDto tracks = spotifyService.getLikedSongs(userId, limit, offset);
            logger.info("Liked songs retrieved successfully for userId: {}, found {} tracks", 
                    userId, tracks.getItems() != null ? tracks.getItems().size() : 0);
            return ResponseEntity.ok(tracks);
        } catch (IOException e) {
            logger.error("Failed to get liked songs for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/me/recently-played")
    public ResponseEntity<SpotifyService.RecentlyPlayedResponse> getRecentlyPlayed(
            @RequestParam String userId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String before) {
        logger.info("Get recently played request - userId: {}, limit: {}, before: {}", userId, limit, before);
        
        try {
            SpotifyService.RecentlyPlayedResponse recentlyPlayed = spotifyService.getRecentlyPlayed(userId, limit, before);
            logger.info("Recently played retrieved successfully for userId: {}, found {} items", 
                    userId, recentlyPlayed.getItems() != null ? recentlyPlayed.getItems().size() : 0);
            return ResponseEntity.ok(recentlyPlayed);
        } catch (IOException e) {
            logger.error("Failed to get recently played for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
