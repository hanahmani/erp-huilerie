package com.huilerie.erp.users;

import com.huilerie.erp.users.dto.PermissionResponse;
import com.huilerie.erp.users.dto.RoleRequest;
import com.huilerie.erp.users.dto.RoleResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RoleController {

    private final RoleService service;

    public RoleController(RoleService service) {
        this.service = service;
    }

    @GetMapping("/permissions")
    public List<PermissionResponse> permissions() {
        return service.listPermissions();
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse create(@Valid @RequestBody RoleRequest r) {
        return service.create(r);
    }

    @PutMapping("/roles/{id}")
    public RoleResponse update(@PathVariable Long id, @Valid @RequestBody RoleRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/roles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
