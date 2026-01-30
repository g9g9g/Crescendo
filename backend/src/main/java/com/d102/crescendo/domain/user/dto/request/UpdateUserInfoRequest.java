package com.d102.crescendo.domain.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserInfoRequest {

    @Size(min = 2, max = 20, message = "닉네임은 2-20자 사이여야 합니다")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9_\\-]*$",
            message = "닉네임은 한글, 영어, 숫자, 언더스코어(_), 하이픈(-)만 사용 가능합니다")
    private String nickname;
    private List<Integer> favoriteGenreIds;
    private String profileUrl;
}
