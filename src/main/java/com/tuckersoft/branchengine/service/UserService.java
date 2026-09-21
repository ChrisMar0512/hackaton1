package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.request.RoleUpdateRequest;
import com.tuckersoft.branchengine.dto.response.UserResponse;
import com.tuckersoft.branchengine.exception.BusinessRuleException;
import com.tuckersoft.branchengine.exception.ResourceNotFoundException;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser(User currentUser) {
        return toResponse(currentUser);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse updateRole(Long id, RoleUpdateRequest request, User currentUser) {
        String newRole = request.getRole();
        if (!"ROLE_USER".equals(newRole) && !"ROLE_ADMIN".equals(newRole)) {
            throw new BusinessRuleException("El rol debe ser ROLE_USER o ROLE_ADMIN");
        }

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (currentUser != null && currentUser.getId().equals(targetUser.getId())) {
            throw new BusinessRuleException("Un administrador no puede cambiar su propio rol");
        }

        targetUser.setRole(newRole);
        userRepository.save(targetUser);

        return toResponse(targetUser);
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
