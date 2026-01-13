package com.hm.mes_final_260106.service;

import com.hm.mes_final_260106.dto.LoginReqDto;
import com.hm.mes_final_260106.dto.MemberResDto;
import com.hm.mes_final_260106.dto.SignUpReqDto;
import com.hm.mes_final_260106.dto.TokenDto;
import com.hm.mes_final_260106.entity.Member;
import com.hm.mes_final_260106.entity.RefreshToken;
import com.hm.mes_final_260106.repository.MemberRepository;
import com.hm.mes_final_260106.repository.RefreshTokenRepository;
import com.hm.mes_final_260106.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final AuthenticationManagerBuilder managerBuilder;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    // 회원 가입
    public MemberResDto signUp(SignUpReqDto dto) {
        if (memberRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("이미 가입되어 있는 사원입니다.");
        }
        Member member = dto.toEntity(passwordEncoder);
        memberRepository.save(member);
        return MemberResDto.of(member);
    }

    // 로그인
    @Transactional
    public TokenDto login(LoginReqDto dto) {
        UsernamePasswordAuthenticationToken authenticationToken = dto.toAuthenticationToken();
        Authentication authentication = managerBuilder.getObject().authenticate(authenticationToken);

        // 토큰 생성
        TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

        // 1:1 연관 관계를 통한 Refresh Token 관리
        Member member = memberRepository.findById(Long.parseLong(authentication.getName()))
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

        // 기존 토큰이 있으면 업데이트, 없으면 신규 생성
        RefreshToken refreshToken = refreshTokenRepository.findByMember(member)
                .map(token -> {
                    token.updateValue(tokenDto.getRefreshToken());
                    return token;
                })
                .orElse(new RefreshToken(member, tokenDto.getRefreshToken()));

        refreshTokenRepository.save(refreshToken);
        return tokenDto;
    }

    // access token 재발급
    public TokenDto refresh(TokenDto tokenRequestDto) {
        // 1. Refresh Token 유효성 검증
        if (!tokenProvider.validateToken(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("전달된 Refresh Token이 유효하지 않습니다.");
        }

        // 2. Access Token에서 사용자 ID 추출 (만료된 토큰도 parseClaims에서 추출 가능)
        Authentication authentication = tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());
        Long memberId = Long.parseLong(authentication.getName());

        // 3. DB에서 Refresh Token 검증
        RefreshToken refreshToken = refreshTokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("로그아웃 상태이거나 토큰이 존재하지 않습니다."));
        if (!refreshToken.getToken().equals(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("토큰 정보가 일치하지 않습니다. 다시 로그인해주세요.");
        }

        // 4. 새로운 토큰 세트 발급 (Access + Refresh)
        TokenDto newTokenDto = tokenProvider.generateTokenDto(authentication);

        // 5. DB에 Refresh Token 업데이트 (Token Rotation)
        refreshToken.updateValue(newTokenDto.getRefreshToken());

        return newTokenDto;
    }

}
