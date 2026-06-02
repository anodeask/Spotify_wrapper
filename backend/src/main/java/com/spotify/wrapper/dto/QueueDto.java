package com.spotify.wrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueueDto {
    @JsonProperty("currently_playing")
    private PlaybackDto.ItemDto currentlyPlaying;

    private List<PlaybackDto.ItemDto> queue;

    public PlaybackDto.ItemDto getCurrentlyPlaying() {
        return currentlyPlaying;
    }

    public void setCurrentlyPlaying(PlaybackDto.ItemDto currentlyPlaying) {
        this.currentlyPlaying = currentlyPlaying;
    }

    public List<PlaybackDto.ItemDto> getQueue() {
        return queue;
    }

    public void setQueue(List<PlaybackDto.ItemDto> queue) {
        this.queue = queue;
    }
}
