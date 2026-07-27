package com.example.xapp.auth;

import com.example.xapp.auth.dto.LoginRequest;
import com.example.xapp.auth.dto.RegisterRequest;
import com.example.xapp.auth.dto.TokenResponse;
import com.example.xapp.user.dto.UserResponse;
import org.springframework.stereotype.Service;

/**
 * AuthService の未実装スタブ。フェーズ3 で本実装に置き換える。
 *
 * <p>テストが「仕様どおりに落ちる」ことを確認できるよう、コンテキストは起動させたうえで
 * 呼び出し時に UnsupportedOperationException を投げる。
 */
@Service
public class AuthServiceStub implements AuthService {

    @Override
    public UserResponse register(RegisterRequest request) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public AuthResult login(LoginRequest request) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public AuthResult refresh(String refreshTokenValue) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

    @Override
    public void logout(String refreshTokenValue) {
        throw new UnsupportedOperationException("フェーズ3で実装する");
    }

}
