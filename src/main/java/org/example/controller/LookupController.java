package org.example.controller;

import org.example.dto.EmployeeResponse;
import org.example.dto.RecognitionTypeResponse;
import org.example.model.Employee;
import org.example.model.RecognitionType;
import org.example.repository.EmployeeRepository;
import org.example.repository.RecognitionTypeRepository;
import org.example.util.EntityMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/lookup")
public class LookupController {

    private final EmployeeRepository employeeRepository;
    private final RecognitionTypeRepository typeRepository;

    public LookupController(EmployeeRepository employeeRepository, RecognitionTypeRepository typeRepository) {
        this.employeeRepository = employeeRepository;
        this.typeRepository = typeRepository;
    }

    @GetMapping("/employee-by-uuid/{uuid}")
    public ResponseEntity<EmployeeResponse> employeeByUuid(@PathVariable UUID uuid) {
        Optional<Employee> e = employeeRepository.findByUuid(uuid);
        return e.map(emp -> ResponseEntity.ok(EntityMapper.toEmployeeResponse(emp))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/type-by-uuid/{uuid}")
    public ResponseEntity<RecognitionTypeResponse> typeByUuid(@PathVariable UUID uuid) {
        Optional<RecognitionType> t = typeRepository.findByUuid(uuid);
        return t.map(rt -> ResponseEntity.ok(EntityMapper.toRecognitionTypeResponse(rt))).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
