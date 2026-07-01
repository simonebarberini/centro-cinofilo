package it.cinofilo.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/catalog")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_APP')")
public class CatalogAdminController {

    private final ModuleService moduleService;

    @GetMapping
    public ResponseEntity<List<ModuleResponse>> listActive() {
        List<ModuleResponse> modules = moduleService.findActive()
                .stream()
                .map(ModuleResponse::from)
                .toList();
        return ResponseEntity.ok(modules);
    }

    @GetMapping("/{moduleKey}")
    public ResponseEntity<ModuleResponse> getByKey(@PathVariable String moduleKey) {
        return ResponseEntity.ok(ModuleResponse.from(moduleService.findByKey(moduleKey)));
    }
}
