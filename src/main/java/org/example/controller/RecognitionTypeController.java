package org.example.controller;

import org.example.dto.RecognitionTypeResponse;
import org.example.model.RecognitionType;
import org.example.repository.EmployeeRepository;
import org.example.repository.RecognitionTypeRepository;
import org.example.util.EntityMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/recognition-types")
public class RecognitionTypeController {

    private final RecognitionTypeRepository repo;
    private final EmployeeRepository employeeRepo;

    public RecognitionTypeController(RecognitionTypeRepository repo, EmployeeRepository employeeRepo) {
        this.repo = repo;
        this.employeeRepo = employeeRepo;
    }

    @PostMapping
    public ResponseEntity<RecognitionTypeResponse> create(@RequestBody RecognitionType t) {
        RecognitionType saved = repo.save(t);
        return ResponseEntity.status(201).body(EntityMapper.toRecognitionTypeResponse(saved, employeeRepo));
    }

    // Helper method for 'all' check
    private boolean isAll(Object param) {
        if (param == null) return false;
        if (param instanceof String s) return s.equalsIgnoreCase("all");
        return false;
    }

    @GetMapping
    public org.springframework.data.domain.Page<RecognitionTypeResponse> list(@RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "20") int size,
                                                                              @RequestParam(required = false) String name) {
        org.springframework.data.domain.Pageable p = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<RecognitionType> pageResult;
        if (name != null && !isAll(name) && !name.isBlank()) {
            pageResult = repo.findByTypeNameContainingIgnoreCase(name, p);
        } else {
            pageResult = repo.findAll(p);
        }
        return pageResult.map(t -> EntityMapper.toRecognitionTypeResponse(t, employeeRepo));
    }

    // Unified get by ID or UUID (as request parameters)
    @GetMapping("/single")
    public ResponseEntity<RecognitionTypeResponse> getByIdOrUuid(@RequestParam(required = false) Long id, @RequestParam(required = false) UUID uuid) {
        Optional<RecognitionType> t = Optional.empty();
        if (id != null) t = repo.findById(id);
        else if (uuid != null) t = repo.findByUuid(uuid);
        return t.map(rt -> ResponseEntity.ok(EntityMapper.toRecognitionTypeResponse(rt, employeeRepo))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Unified update by ID or UUID (as request parameters)
    @PutMapping("/single")
    public ResponseEntity<RecognitionTypeResponse> update(@RequestParam(required = false) Long id, @RequestParam(required = false) UUID uuid, @RequestBody RecognitionType req) {
        Optional<RecognitionType> opt = Optional.empty();
        if (id != null) opt = repo.findById(id);
        else if (uuid != null) opt = repo.findByUuid(uuid);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        RecognitionType t = opt.get();
        if (req.getTypeName() != null) t.setTypeName(req.getTypeName());
        repo.save(t);
        return ResponseEntity.ok(EntityMapper.toRecognitionTypeResponse(t, employeeRepo));
    }

    // Unified delete by ID or UUID (as request parameters)
    @DeleteMapping("/single")
    public ResponseEntity<?> delete(@RequestParam(required = false) Long id, @RequestParam(required = false) UUID uuid) {
        Optional<RecognitionType> opt = Optional.empty();
        if (id != null) opt = repo.findById(id);
        else if (uuid != null) opt = repo.findByUuid(uuid);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        repo.deleteById(opt.get().getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<RecognitionTypeResponse> search(@RequestParam(required = false) Long id,
                                                @RequestParam(required = false) java.util.UUID uuid,
                                                @RequestParam(required = false) String name) {
        if (id != null) {
            return repo.findById(id)
                    .map(t -> List.of(EntityMapper.toRecognitionTypeResponse(t, employeeRepo)))
                    .orElseGet(List::of);
        } else if (uuid != null) {
            return repo.findByUuid(uuid)
                    .map(t -> List.of(EntityMapper.toRecognitionTypeResponse(t, employeeRepo)))
                    .orElseGet(List::of);
        } else if (name != null && !isAll(name) && !name.isBlank()) {
            return repo.findByTypeNameContainingIgnoreCase(name).stream().map(t -> EntityMapper.toRecognitionTypeResponse(t, employeeRepo)).collect(Collectors.toList());
        }
        return repo.findAll().stream().map(t -> EntityMapper.toRecognitionTypeResponse(t, employeeRepo)).collect(Collectors.toList());
    }
}
