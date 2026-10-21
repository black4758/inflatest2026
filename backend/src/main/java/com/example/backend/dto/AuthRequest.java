package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
    @NotBlank(message = "아이디(닉네임)를 입력해주세요")
    @Size(min = 2, max = 50, message = "아이디는 2~50자 사이여야 합니다")
    String username,

    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 4, max = 100, message = "비밀번호는 최소 4자 이상이어야 합니다")
    String password
) {}
