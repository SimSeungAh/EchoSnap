package com.smartrecycle.backend.domain.user.service;

import com.smartrecycle.backend.domain.user.dto.request.UpdateOnboardingRequest;
import com.smartrecycle.backend.domain.user.dto.request.UpdateUserRequest;
import com.smartrecycle.backend.domain.user.dto.request.UpdateUserSettingsRequest;
import com.smartrecycle.backend.domain.user.dto.response.UserResponse;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserResponse  getMyInfo(Long userId){
        User user = getUser(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse  updateMyInfo(Long userId, UpdateUserRequest request){
        User user = getUser(userId);
        user.updateNickname(request.nickname());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse  updateSetting(Long userId, UpdateUserSettingsRequest request){
        User user = getUser(userId);
        user.updateSettings(request.notificationEnabled(), request.locationEnabled());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse  updateOnboarding(Long userId, UpdateOnboardingRequest request){
        User user = getUser(userId);
        user.updateOnboardingCompleted(request.completed());
        return UserResponse.from(user);
    }

    private User getUser(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(
                        ()->new CustomException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );
    }
}
