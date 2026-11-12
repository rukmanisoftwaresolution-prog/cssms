package com.cssms.service;

import com.cssms.dto.PageResponse;
import com.cssms.dto.UserDto.Request;
import com.cssms.dto.UserDto.Response;
import com.cssms.entity.User;
import com.cssms.enums.RoleName;
import com.cssms.exception.BadRequestException;
import com.cssms.exception.BusinessException;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.repository.RefreshTokenRepository;
import com.cssms.repository.RoleRepository;
import com.cssms.repository.UserRepository;
import com.cssms.util.CurrentUser;
import com.cssms.util.Pages;
import com.cssms.util.Specs;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Map<String, String> SORTS = Map.of(
            "id", "id", "username", "username", "fullName", "fullName", "email", "email", "role", "role.name",
            "active", "active");

    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<Response> search(String search, RoleName role, Pageable pageable) {
        Specification<User> spec = Specification
                .where(Specs.<User>search(search, "username", "fullName", "email"))
                .and(Specs.eq("role.name", role));
        Pageable safe = Pages.safe(pageable, SORTS, Sort.by(Sort.Direction.ASC, "username"));
        return PageResponse.of(repository.findAll(spec, safe).map(UserService::toResponse));
    }

    @Transactional(readOnly = true)
    public Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public Response create(Request request) {
        if (request.password() == null) {
            throw new BadRequestException("Password is required");
        }
        if (repository.existsByUsernameIgnoreCase(request.username())) {
            throw new BusinessException("Username '" + request.username() + "' is already taken");
        }
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("Email '" + request.email() + "' is already used");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        apply(user, request);
        return toResponse(repository.save(user));
    }

    @Transactional
    public Response update(Long id, Request request) {
        User user = find(id);
        if (repository.existsByUsernameIgnoreCaseAndIdNot(request.username(), id)) {
            throw new BusinessException("Username '" + request.username() + "' is already taken");
        }
        if (repository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new BusinessException("Email '" + request.email() + "' is already used");
        }
        boolean isSelf = user.getUsername().equalsIgnoreCase(CurrentUser.usernameOrSystem());
        if (isSelf && (user.getRole().getName() != request.role() || Boolean.FALSE.equals(request.active()))) {
            throw new BusinessException("You cannot change your own role or deactivate your own account");
        }
        user.setUsername(request.username());
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            refreshTokenRepository.revokeAllForUser(id);
        }
        apply(user, request);
        if (!user.isActive()) {
            refreshTokenRepository.revokeAllForUser(id);
        }
        return toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = find(id);
        if (user.getUsername().equalsIgnoreCase(CurrentUser.usernameOrSystem())) {
            throw new BusinessException("You cannot delete your own account");
        }
        user.setDeleted(true);
        refreshTokenRepository.revokeAllForUser(id);
    }

    private User find(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private void apply(User user, Request request) {
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setMobileNumber(request.mobileNumber());
        user.setRole(roleRepository.findByName(request.role())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.role())));
        user.setActive(request.active() == null || request.active());
    }

    static Response toResponse(User u) {
        return new Response(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getMobileNumber(),
                u.getRole().getName(), u.isActive());
    }
}
