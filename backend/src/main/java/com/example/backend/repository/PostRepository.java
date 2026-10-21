package com.example.backend.repository;

import com.example.backend.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findAllByOrderByViewCountDescCreatedAtDesc();

    @Query("SELECT p FROM Post p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(p.author) LIKE LOWER(CONCAT('%', :kw, '%')) ORDER BY p.createdAt DESC")
    List<Post> searchPosts(@Param("kw") String keyword);

    @Query("SELECT p FROM Post p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(p.author) LIKE LOWER(CONCAT('%', :kw, '%')) ORDER BY p.viewCount DESC, p.createdAt DESC")
    List<Post> searchPostsOrderByViewCountDesc(@Param("kw") String keyword);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
