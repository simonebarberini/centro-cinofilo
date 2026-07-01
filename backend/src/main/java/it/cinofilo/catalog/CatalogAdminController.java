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
    public ResponseEntity<List<ModuleResponse>> listAll() {
        return ResponseEntity.ok(moduleService.findAll());
    }

    @GetMapping("/{moduleKey}")
    public ResponseEntity<ModuleResponse> getByKey(@PathVariable String moduleKey) {
        return ResponseEntity.ok(moduleService.findByKey(moduleKey));
    }
}
