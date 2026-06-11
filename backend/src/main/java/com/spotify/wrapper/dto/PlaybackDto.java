package com.spotify.wrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaybackDto {
    private DeviceDto device;
    @JsonProperty("repeat_state")
    private String repeatState;
    @JsonProperty("shuffle_state")
    private boolean shuffleState;
    @JsonProperty("smart_shuffle")
    private boolean smartShuffle;
    private ContextDto context;
    private long timestamp;
    @JsonProperty("progress_ms")
    private long progressMs;
    @JsonProperty("is_playing")
    private boolean isPlaying;
    private ItemDto item;
    @JsonProperty("currently_playing_type")
    private String currentlyPlayingType;
    
    // Getters and Setters
    public DeviceDto getDevice() {
        return device;
    }
    
    public void setDevice(DeviceDto device) {
        this.device = device;
    }
    
    public String getRepeatState() {
        return repeatState;
    }
    
    public void setRepeatState(String repeatState) {
        this.repeatState = repeatState;
    }
    
    public boolean isShuffleState() {
        return shuffleState;
    }
    
    public void setShuffleState(boolean shuffleState) {
        this.shuffleState = shuffleState;
    }
    
    public boolean isSmartShuffle() {
        return smartShuffle;
    }
    
    public void setSmartShuffle(boolean smartShuffle) {
        this.smartShuffle = smartShuffle;
    }
    
    public ContextDto getContext() {
        return context;
    }
    
    public void setContext(ContextDto context) {
        this.context = context;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getProgressMs() {
        return progressMs;
    }
    
    public void setProgressMs(long progressMs) {
        this.progressMs = progressMs;
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }
    
    public ItemDto getItem() {
        return item;
    }
    
    public void setItem(ItemDto item) {
        this.item = item;
    }
    
    public String getCurrentlyPlayingType() {
        return currentlyPlayingType;
    }
    
    public void setCurrentlyPlayingType(String currentlyPlayingType) {
        this.currentlyPlayingType = currentlyPlayingType;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeviceDto {
        private String id;
        @JsonProperty("is_active")
        private boolean isActive;
        @JsonProperty("is_private_session")
        private boolean isPrivateSession;
        @JsonProperty("is_restricted")
        private boolean isRestricted;
        private String name;
        @JsonProperty("supports_volume")
        private boolean supportsVolume;
        private String type;
        @JsonProperty("volume_percent")
        private Integer volumePercent;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public boolean isActive() {
            return isActive;
        }
        
        public void setActive(boolean active) {
            isActive = active;
        }
        
        public boolean isPrivateSession() {
            return isPrivateSession;
        }
        
        public void setPrivateSession(boolean privateSession) {
            isPrivateSession = privateSession;
        }
        
        public boolean isRestricted() {
            return isRestricted;
        }
        
        public void setRestricted(boolean restricted) {
            isRestricted = restricted;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public boolean isSupportsVolume() {
            return supportsVolume;
        }
        
        public void setSupportsVolume(boolean supportsVolume) {
            this.supportsVolume = supportsVolume;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Integer getVolumePercent() {
            return volumePercent;
        }
        
        public void setVolumePercent(Integer volumePercent) {
            this.volumePercent = volumePercent;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContextDto {
        private String type;
        private String href;
        @JsonProperty("external_urls")
        private SearchResultDto.ExternalUrls externalUrls;
        private String uri;
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getHref() {
            return href;
        }
        
        public void setHref(String href) {
            this.href = href;
        }
        
        public SearchResultDto.ExternalUrls getExternalUrls() {
            return externalUrls;
        }
        
        public void setExternalUrls(SearchResultDto.ExternalUrls externalUrls) {
            this.externalUrls = externalUrls;
        }
        
        public String getUri() {
            return uri;
        }
        
        public void setUri(String uri) {
            this.uri = uri;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItemDto {
        private String id;
        private String name;
        private String uri;
        @JsonProperty("duration_ms")
        private long durationMs;
        private SearchResultDto.ArtistDto[] artists;
        private SearchResultDto.AlbumDto album;
        private SearchResultDto.ImageDto[] images;
        private SearchResultDto.ShowDto show;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        public SearchResultDto.ArtistDto[] getArtists() {
            return artists;
        }

        public void setArtists(SearchResultDto.ArtistDto[] artists) {
            this.artists = artists;
        }

        public SearchResultDto.AlbumDto getAlbum() {
            return album;
        }

        public void setAlbum(SearchResultDto.AlbumDto album) {
            this.album = album;
        }

        public SearchResultDto.ImageDto[] getImages() {
            return images;
        }

        public void setImages(SearchResultDto.ImageDto[] images) {
            this.images = images;
        }

        public SearchResultDto.ShowDto getShow() {
            return show;
        }

        public void setShow(SearchResultDto.ShowDto show) {
            this.show = show;
        }
    }
}
