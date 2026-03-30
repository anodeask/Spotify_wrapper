package com.spotify.wrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResultDto {
    private PlaylistsDto playlists;
    private TracksDto tracks;
    private AlbumsDto albums;
    private ArtistsDto artists;
    private ErrorDto error;
    
    // Getters and Setters
    public PlaylistsDto getPlaylists() {
        return playlists;
    }
    
    public void setPlaylists(PlaylistsDto playlists) {
        this.playlists = playlists;
    }
    
    public TracksDto getTracks() {
        return tracks;
    }
    
    public void setTracks(TracksDto tracks) {
        this.tracks = tracks;
    }
    
    public AlbumsDto getAlbums() {
        return albums;
    }
    
    public void setAlbums(AlbumsDto albums) {
        this.albums = albums;
    }
    
    public ArtistsDto getArtists() {
        return artists;
    }
    
    public void setArtists(ArtistsDto artists) {
        this.artists = artists;
    }
    
    public ErrorDto getError() {
        return error;
    }
    
    public void setError(ErrorDto error) {
        this.error = error;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaylistsDto {
        private List<PlaylistDto> items;
        private String href;
        private int limit;
        private String next;
        private int offset;
        private String previous;
        private int total;
        
        public List<PlaylistDto> getItems() {
            return items;
        }
        
        public void setItems(List<PlaylistDto> items) {
            this.items = items;
        }
        
        public String getHref() {
            return href;
        }
        
        public void setHref(String href) {
            this.href = href;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public void setLimit(int limit) {
            this.limit = limit;
        }
        
        public String getNext() {
            return next;
        }
        
        public void setNext(String next) {
            this.next = next;
        }
        
        public int getOffset() {
            return offset;
        }
        
        public void setOffset(int offset) {
            this.offset = offset;
        }
        
        public String getPrevious() {
            return previous;
        }
        
        public void setPrevious(String previous) {
            this.previous = previous;
        }
        
        public int getTotal() {
            return total;
        }
        
        public void setTotal(int total) {
            this.total = total;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TracksDto {
        private List<TrackDto> items;
        private String href;
        private int limit;
        private String next;
        private int offset;
        private String previous;
        private int total;
        
        public List<TrackDto> getItems() {
            return items;
        }
        
        public void setItems(List<TrackDto> items) {
            this.items = items;
        }
        
        public String getHref() {
            return href;
        }
        
        public void setHref(String href) {
            this.href = href;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public void setLimit(int limit) {
            this.limit = limit;
        }
        
        public String getNext() {
            return next;
        }
        
        public void setNext(String next) {
            this.next = next;
        }
        
        public int getOffset() {
            return offset;
        }
        
        public void setOffset(int offset) {
            this.offset = offset;
        }
        
        public String getPrevious() {
            return previous;
        }
        
        public void setPrevious(String previous) {
            this.previous = previous;
        }
        
        public int getTotal() {
            return total;
        }
        
        public void setTotal(int total) {
            this.total = total;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ArtistsDto {
        private List<FullArtistDto> items;
        private String href;
        private int limit;
        private String next;
        private int offset;
        private String previous;
        private int total;
        
        public List<FullArtistDto> getItems() {
            return items;
        }
        
        public void setItems(List<FullArtistDto> items) {
            this.items = items;
        }
        
        public String getHref() {
            return href;
        }
        
        public void setHref(String href) {
            this.href = href;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public void setLimit(int limit) {
            this.limit = limit;
        }
        
        public String getNext() {
            return next;
        }
        
        public void setNext(String next) {
            this.next = next;
        }
        
        public int getOffset() {
            return offset;
        }
        
        public void setOffset(int offset) {
            this.offset = offset;
        }
        
        public String getPrevious() {
            return previous;
        }
        
        public void setPrevious(String previous) {
            this.previous = previous;
        }
        
        public int getTotal() {
            return total;
        }
        
        public void setTotal(int total) {
            this.total = total;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FullArtistDto {
        private String id;
        private String name;
        private String href;
        private String type;
        private String uri;
        private ImageDto[] images;
        private List<String> genres;
        private int popularity;
        private FollowersDto followers;
        @JsonProperty("external_urls")
        private ExternalUrls externalUrls;
        
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
        
        public String getHref() {
            return href;
        }
        
        public void setHref(String href) {
            this.href = href;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getUri() {
            return uri;
        }
        
        public void setUri(String uri) {
            this.uri = uri;
        }
        
        public ImageDto[] getImages() {
            return images;
        }
        
        public void setImages(ImageDto[] images) {
            this.images = images;
        }
        
        public List<String> getGenres() {
            return genres;
        }
        
        public void setGenres(List<String> genres) {
            this.genres = genres;
        }
        
        public int getPopularity() {
            return popularity;
        }
        
        public void setPopularity(int popularity) {
            this.popularity = popularity;
        }
        
        public FollowersDto getFollowers() {
            return followers;
        }
        
        public void setFollowers(FollowersDto followers) {
            this.followers = followers;
        }
        
        public ExternalUrls getExternalUrls() {
            return externalUrls;
        }
        
        public void setExternalUrls(ExternalUrls externalUrls) {
            this.externalUrls = externalUrls;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FollowersDto {
        private String href;
        private int total;
        
        public String getHref() {
            return href;
        }
        
        public void setHref(String href) {
            this.href = href;
        }
        
        public int getTotal() {
            return total;
        }
        
        public void setTotal(int total) {
            this.total = total;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlbumsDto {
        private List<AlbumDto> items;
        private String href;
        private int limit;
        private String next;
        private int offset;
        private String previous;
        private int total;
        
        public List<AlbumDto> getItems() {
            return items;
        }
        
        public void setItems(List<AlbumDto> items) {
            this.items = items;
        }
        
        public String getHref() {
            return href;
        }
        
        public void setHref(String href) {
            this.href = href;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public void setLimit(int limit) {
            this.limit = limit;
        }
        
        public String getNext() {
            return next;
        }
        
        public void setNext(String next) {
            this.next = next;
        }
        
        public int getOffset() {
            return offset;
        }
        
        public void setOffset(int offset) {
            this.offset = offset;
        }
        
        public String getPrevious() {
            return previous;
        }
        
        public void setPrevious(String previous) {
            this.previous = previous;
        }
        
        public int getTotal() {
            return total;
        }
        
        public void setTotal(int total) {
            this.total = total;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaylistDto {
        private String id;
        private String name;
        private String description;
        private String uri;
        private ImageDto[] images;
        @JsonProperty("external_urls")
        private ExternalUrls externalUrls;
        private OwnerDto owner;
        private TracksInfoDto tracks;
        
        // Spotify API may return track count as "tracks" or "items" depending on the endpoint
        @JsonProperty("items")
        private TracksInfoDto itemsInfo;
        
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
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getUri() {
            return uri;
        }
        
        public void setUri(String uri) {
            this.uri = uri;
        }
        
        public ImageDto[] getImages() {
            return images;
        }
        
        public void setImages(ImageDto[] images) {
            this.images = images;
        }
        
        public ExternalUrls getExternalUrls() {
            return externalUrls;
        }
        
        public void setExternalUrls(ExternalUrls externalUrls) {
            this.externalUrls = externalUrls;
        }
        
        public OwnerDto getOwner() {
            return owner;
        }
        
        public void setOwner(OwnerDto owner) {
            this.owner = owner;
        }
        
        public TracksInfoDto getTracks() {
            // Return tracks if set, otherwise fall back to itemsInfo (Spotify API inconsistency)
            return tracks != null ? tracks : itemsInfo;
        }
        
        public void setTracks(TracksInfoDto tracks) {
            this.tracks = tracks;
        }
        
        public TracksInfoDto getItemsInfo() {
            return itemsInfo;
        }
        
        public void setItemsInfo(TracksInfoDto itemsInfo) {
            this.itemsInfo = itemsInfo;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OwnerDto {
        private String id;
        @JsonProperty("display_name")
        private String displayName;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TracksInfoDto {
        private String href;
        private int total;
        
        public String getHref() {
            return href;
        }
        
        public void setHref(String href) {
            this.href = href;
        }
        
        public int getTotal() {
            return total;
        }
        
        public void setTotal(int total) {
            this.total = total;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackDto {
        private String id;
        private String name;
        private ArtistDto[] artists;
        private AlbumDto album;
        @JsonProperty("duration_ms")
        private int durationMs;
        private String uri;
        @JsonProperty("disc_number")
        private int discNumber;
        @JsonProperty("track_number")
        private int trackNumber;
        private boolean explicit;
        @JsonProperty("is_local")
        private boolean isLocal;
        @JsonProperty("is_playable")
        private Boolean isPlayable;
        private int popularity;
        private String href;
        private String type;
        @JsonProperty("external_urls")
        private ExternalUrls externalUrls;
        @JsonProperty("external_ids")
        private ExternalIds externalIds;
        @JsonProperty("preview_url")
        private String previewUrl;
        
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
        
        public ArtistDto[] getArtists() {
            return artists;
        }
        
        public void setArtists(ArtistDto[] artists) {
            this.artists = artists;
        }
        
        public AlbumDto getAlbum() {
            return album;
        }
        
        public void setAlbum(AlbumDto album) {
            this.album = album;
        }
        
        public int getDurationMs() {
            return durationMs;
        }
        
        public void setDurationMs(int durationMs) {
            this.durationMs = durationMs;
        }
        
        public String getUri() {
            return uri;
        }
        
        public void setUri(String uri) {
            this.uri = uri;
        }
        
        public int getDiscNumber() {
            return discNumber;
        }
        
        public void setDiscNumber(int discNumber) {
            this.discNumber = discNumber;
        }
        
        public int getTrackNumber() {
            return trackNumber;
        }
        
        public void setTrackNumber(int trackNumber) {
            this.trackNumber = trackNumber;
        }
        
        public boolean isExplicit() {
            return explicit;
        }
        
        public void setExplicit(boolean explicit) {
            this.explicit = explicit;
        }
        
        public boolean isLocal() {
            return isLocal;
        }
        
        public void setLocal(boolean isLocal) {
            this.isLocal = isLocal;
        }
        
        public Boolean getIsPlayable() {
            return isPlayable;
        }
        
        public void setIsPlayable(Boolean isPlayable) {
            this.isPlayable = isPlayable;
        }
        
        public int getPopularity() {
            return popularity;
        }
        
        public void setPopularity(int popularity) {
            this.popularity = popularity;
        }
        
        public String getHref() {
            return href;
        }
        
        public void setHref(String href) {
            this.href = href;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public ExternalUrls getExternalUrls() {
            return externalUrls;
        }
        
        public void setExternalUrls(ExternalUrls externalUrls) {
            this.externalUrls = externalUrls;
        }
        
        public ExternalIds getExternalIds() {
            return externalIds;
        }
        
        public void setExternalIds(ExternalIds externalIds) {
            this.externalIds = externalIds;
        }
        
        public String getPreviewUrl() {
            return previewUrl;
        }
        
        public void setPreviewUrl(String previewUrl) {
            this.previewUrl = previewUrl;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ArtistDto {
        private String id;
        private String name;
        private String href;
        private String type;
        private String uri;
        @JsonProperty("external_urls")
        private ExternalUrls externalUrls;
        
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
        
        public String getHref() {
            return href;
        }
        
        public void setHref(String href) {
            this.href = href;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getUri() {
            return uri;
        }
        
        public void setUri(String uri) {
            this.uri = uri;
        }
        
        public ExternalUrls getExternalUrls() {
            return externalUrls;
        }
        
        public void setExternalUrls(ExternalUrls externalUrls) {
            this.externalUrls = externalUrls;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlbumDto {
        private String id;
        private String name;
        private ImageDto[] images;
        @JsonProperty("album_type")
        private String albumType;
        private ArtistDto[] artists;
        private String href;
        @JsonProperty("external_urls")
        private ExternalUrls externalUrls;
        @JsonProperty("release_date")
        private String releaseDate;
        @JsonProperty("total_tracks")
        private int totalTracks;
        @JsonProperty("is_playable")
        private Boolean isPlayable;
        @JsonProperty("release_date_precision")
        private String releaseDatePrecision;
        private String type;
        private String uri;
        
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
        
        public ImageDto[] getImages() {
            return images;
        }
        
        public void setImages(ImageDto[] images) {
            this.images = images;
        }
        
        public String getAlbumType() {
            return albumType;
        }
        
        public void setAlbumType(String albumType) {
            this.albumType = albumType;
        }
        
        public ArtistDto[] getArtists() {
            return artists;
        }
        
        public void setArtists(ArtistDto[] artists) {
            this.artists = artists;
        }
        
        public String getHref() {
            return href;
        }
        
        public void setHref(String href) {
            this.href = href;
        }
        
        public ExternalUrls getExternalUrls() {
            return externalUrls;
        }
        
        public void setExternalUrls(ExternalUrls externalUrls) {
            this.externalUrls = externalUrls;
        }
        
        public String getReleaseDate() {
            return releaseDate;
        }
        
        public void setReleaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
        }
        
        public int getTotalTracks() {
            return totalTracks;
        }
        
        public void setTotalTracks(int totalTracks) {
            this.totalTracks = totalTracks;
        }
        
        public Boolean getIsPlayable() {
            return isPlayable;
        }
        
        public void setIsPlayable(Boolean isPlayable) {
            this.isPlayable = isPlayable;
        }
        
        public String getReleaseDatePrecision() {
            return releaseDatePrecision;
        }
        
        public void setReleaseDatePrecision(String releaseDatePrecision) {
            this.releaseDatePrecision = releaseDatePrecision;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getUri() {
            return uri;
        }
        
        public void setUri(String uri) {
            this.uri = uri;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageDto {
        private String url;
        private int height;
        private int width;
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public int getHeight() {
            return height;
        }
        
        public void setHeight(int height) {
            this.height = height;
        }
        
        public int getWidth() {
            return width;
        }
        
        public void setWidth(int width) {
            this.width = width;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExternalUrls {
        private String spotify;
        
        public String getSpotify() {
            return spotify;
        }
        
        public void setSpotify(String spotify) {
            this.spotify = spotify;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExternalIds {
        private String isrc;
        private String ean;
        private String upc;
        
        public String getIsrc() {
            return isrc;
        }
        
        public void setIsrc(String isrc) {
            this.isrc = isrc;
        }
        
        public String getEan() {
            return ean;
        }
        
        public void setEan(String ean) {
            this.ean = ean;
        }
        
        public String getUpc() {
            return upc;
        }
        
        public void setUpc(String upc) {
            this.upc = upc;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorDto {
        private int status;
        private String message;
        
        public int getStatus() {
            return status;
        }
        
        public void setStatus(int status) {
            this.status = status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
