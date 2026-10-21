package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 최대 200자까지 가능합니다")
    String title,

    @NotBlank(message = "내용은 필수입니다")
    String content,

    @Size(max = 50, message = "작성자 이름은 최대 50자까지 가능합니다")
    String author
) {}
