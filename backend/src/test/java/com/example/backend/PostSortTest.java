package com.example.backend;

import com.example.backend.domain.Post;
import com.example.backend.dto.PostResponse;
import com.example.backend.repository.PostRepository;
import com.example.backend.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PostSortTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostService postService;

    @Test
    @DisplayName("조회수 순(views)으로 정렬 시 조회수가 높은 게시글부터 반환되어야 한다")
    void testSortByViewCountDesc() {
        postRepository.deleteAll();

        postRepository.save(Post.builder().title("Post A").content("Content A").author("Author 1").viewCount(10).build());
        postRepository.save(Post.builder().title("Post B").content("Content B").author("Author 2").viewCount(50).build());
        postRepository.save(Post.builder().title("Post C").content("Content C").author("Author 3").viewCount(30).build());

        List<PostResponse> posts = postService.getAllPosts(null, "views");

        assertThat(posts).hasSize(3);
        assertThat(posts.get(0).viewCount()).isEqualTo(50);
        assertThat(posts.get(0).title()).isEqualTo("Post B");

        assertThat(posts.get(1).viewCount()).isEqualTo(30);
        assertThat(posts.get(1).title()).isEqualTo("Post C");

        assertThat(posts.get(2).viewCount()).isEqualTo(10);
        assertThat(posts.get(2).title()).isEqualTo("Post A");
    }

    @Test
    @DisplayName("인기글 전용 엔드포인트(/popular)도 조회수 내림차순으로 동작해야 한다")
    void testPopularSortWithKeyword() {
        postRepository.deleteAll();

        postRepository.save(Post.builder().title("Spring Boot Guide").content("Content 1").author("Dev 1").viewCount(15).build());
        postRepository.save(Post.builder().title("Spring Security").content("Content 2").author("Dev 2").viewCount(100).build());
        postRepository.save(Post.builder().title("Docker Tutorial").content("Content 3").author("Dev 3").viewCount(200).build());

        List<PostResponse> searchPosts = postService.getAllPosts("Spring", "views");

        assertThat(searchPosts).hasSize(2);
        assertThat(searchPosts.get(0).title()).isEqualTo("Spring Security");
        assertThat(searchPosts.get(0).viewCount()).isEqualTo(100);
        assertThat(searchPosts.get(1).title()).isEqualTo("Spring Boot Guide");
        assertThat(searchPosts.get(1).viewCount()).isEqualTo(15);
    }
}
