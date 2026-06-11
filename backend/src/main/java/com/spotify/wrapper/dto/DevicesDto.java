package com.spotify.wrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DevicesDto {
    private List<DeviceDto> devices;
    
    public List<DeviceDto> getDevices() {
        return devices;
    }
    
    public void setDevices(List<DeviceDto> devices) {
        this.devices = devices;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeviceDto {
        private String id;
        private String name;
        private String type;
        @JsonProperty("is_active")
        private boolean isActive;
        @JsonProperty("is_private_session")
        private boolean isPrivateSession;
        @JsonProperty("is_restricted")
        private boolean isRestricted;
        // Nullable: Spotify returns null for devices that don't support or report volume
        @JsonProperty("volume_percent")
        private Integer volumePercent;
        @JsonProperty("supports_volume")
        private boolean supportsVolume;
        
        // Getters and Setters
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
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        @JsonProperty("is_active")
        public boolean isActive() {
            return isActive;
        }
        
        public void setActive(boolean active) {
            isActive = active;
        }
        
        @JsonProperty("is_private_session")
        public boolean isPrivateSession() {
            return isPrivateSession;
        }
        
        public void setPrivateSession(boolean privateSession) {
            isPrivateSession = privateSession;
        }
        
        @JsonProperty("is_restricted")
        public boolean isRestricted() {
            return isRestricted;
        }
        
        public void setRestricted(boolean restricted) {
            isRestricted = restricted;
        }
        
        @JsonProperty("volume_percent")
        public Integer getVolumePercent() {
            return volumePercent;
        }
        
        public void setVolumePercent(Integer volumePercent) {
            this.volumePercent = volumePercent;
        }
        
        @JsonProperty("supports_volume")
        public boolean isSupportsVolume() {
            return supportsVolume;
        }
        
        public void setSupportsVolume(boolean supportsVolume) {
            this.supportsVolume = supportsVolume;
        }
    }
}
