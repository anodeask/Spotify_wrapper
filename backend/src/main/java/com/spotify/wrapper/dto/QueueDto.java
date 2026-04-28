package com.spotify.wrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueueDto {
    @JsonProperty("currently_playing")
    private SearchResultDto.TrackDto currentlyPlaying;

    private List<SearchResultDto.TrackDto> queue;

    public SearchResultDto.TrackDto getCurrentlyPlaying() {
        return currentlyPlaying;
    }

    public void setCurrentlyPlaying(SearchResultDto.TrackDto currentlyPlaying) {
        this.currentlyPlaying = currentlyPlaying;
    }

    public List<SearchResultDto.TrackDto> getQueue() {
        return queue;
    }

    public void setQueue(List<SearchResultDto.TrackDto> queue) {
        this.queue = queue;
    }
}
