package com.huilerie.erp.users;

import com.huilerie.erp.users.dto.CreateUserRequest;
import com.huilerie.erp.users.dto.RoleResponse;
import com.huilerie.erp.users.dto.ResetPasswordRequest;
import com.huilerie.erp.users.dto.UpdateUserRequest;
import com.huilerie.erp.users.dto.UpdateUserStatusRequest;
import com.huilerie.erp.users.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Les permissions sont vérifiées dans UserService (@PreAuthorize), pas seulement dans l'interface. */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public List<UserResponse> list() {
        return userService.listUsers();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request, Authentication auth) {
        return userService.createUser(request, auth.getName());
    }

    @PatchMapping("/users/{id}/status")
    public UserResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request,
                                     Authentication auth) {
        return userService.updateStatus(id, request.active(), auth.getName());
    }

    @GetMapping("/roles")
    public List<RoleResponse> roles() {
        return userService.listRoles();
    }

    @GetMapping("/users/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PutMapping("/users/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest r, Authentication auth) {
        return userService.update(id, r, auth.getName());
    }

    @PostMapping("/users/{id}/reset-password")
    public UserResponse resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest r, Authentication auth) {
        return userService.resetPassword(id, r.temporaryPassword(), auth.getName());
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        userService.delete(id, auth.getName());
    }
}
