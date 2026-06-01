package com.spotify.wrapper.repository;

import com.spotify.wrapper.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySpotifyUserId(String spotifyUserId);
    void deleteBySpotifyUserId(String spotifyUserId);
}
