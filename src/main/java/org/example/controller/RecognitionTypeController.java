package org.example.controller;

import org.example.dto.RecognitionTypeResponse;
import org.example.model.RecognitionType;
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

    public RecognitionTypeController(RecognitionTypeRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public ResponseEntity<RecognitionTypeResponse> create(@RequestBody RecognitionType t) {
        RecognitionType saved = repo.save(t);
        return ResponseEntity.status(201).body(EntityMapper.toRecognitionTypeResponse(saved));
    }

    @GetMapping
    public List<RecognitionTypeResponse> list() {
        return repo.findAll().stream().map(EntityMapper::toRecognitionTypeResponse).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecognitionTypeResponse> getById(@PathVariable Long id) {
        Optional<RecognitionType> t = repo.findById(id);
        return t.map(rt -> ResponseEntity.ok(EntityMapper.toRecognitionTypeResponse(rt))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/uuid/{uuid}")
    public ResponseEntity<RecognitionTypeResponse> getByUuid(@PathVariable UUID uuid) {
        Optional<RecognitionType> t = repo.findByUuid(uuid);
        return t.map(rt -> ResponseEntity.ok(EntityMapper.toRecognitionTypeResponse(rt))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecognitionTypeResponse> update(@PathVariable Long id, @RequestBody RecognitionType req) {
        Optional<RecognitionType> opt = repo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        RecognitionType t = opt.get();
        if (req.getTypeName() != null) t.setTypeName(req.getTypeName());
        repo.save(t);
        return ResponseEntity.ok(EntityMapper.toRecognitionTypeResponse(t));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
