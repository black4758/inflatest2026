package com.example.backend.config;

import com.example.backend.domain.Post;
import com.example.backend.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PostRepository postRepository;

    @Override
    public void run(String... args) {
        try {
            if (postRepository.count() == 0) {
                log.info("Seeding initial board data...");
                postRepository.save(Post.builder()
                    .title("인프라 테스트 게시판에 오신 것을 환영합니다!")
                    .content("Spring Boot 4.x + MySQL + React 기반의 인프라 검증용 CRUD 게시판입니다.\n도커 컨테이너, 네트워크 통신, 볼륨 영속성 등을 점검해보세요.")
                    .author("관리자")
                    .build());

                postRepository.save(Post.builder()
                    .title("MySQL 연결 및 데이터 영속성 테스트 안내")
                    .content("새로운 글을 작성하거나 수정/삭제한 후 컨테이너를 재시작해도 데이터가 유지되는지 확인하세요.")
                    .author("데브옵스")
                    .build());

                postRepository.save(Post.builder()
                    .title("헬스 체크 엔드포인트 안내 (/actuator/health)")
                    .content("상단의 헬스 상태 바 또는 /actuator/health 를 통해 DB 연결 상태, 디스크 용량, 헬스 프로브를 실시간 모니터링할 수 있습니다.")
                    .author("인프라팀")
                    .build());

                postRepository.save(Post.builder()
                    .title("GitHub Actions 스마트 CI/CD 파이프라인 연동 완료")
                    .content("경로 기반 필터링(Path Filter)을 통해 백엔드와 프론트엔드의 변경 사항만 선별하여 자동 빌드 및 배포가 수행됩니다.")
                    .author("데브옵스")
                    .build());
                log.info("Initial board data seeded successfully.");
            }
        } catch (Exception e) {
            log.warn("Data seeding skipped or failed (DB might still be initializing): {}", e.getMessage());
        }
    }
}
