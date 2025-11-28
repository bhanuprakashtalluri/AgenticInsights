package org.example.controller;

import org.example.dto.EmployeeCreateRequest;
import org.example.dto.EmployeeResponse;
import org.example.dto.EmployeeUpdateRequest;
import org.example.model.Employee;
import org.example.repository.EmployeeRepository;
import org.example.util.EntityMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;

    public EmployeeController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@RequestBody EmployeeCreateRequest req) {
        Employee e = new Employee();
        e.setFirstName(req.firstName);
        e.setLastName(req.lastName);
        e.setUnitId(req.unitId);
        e.setManagerId(req.managerId);
        e.setEmail(req.email);
        if (req.joiningDate != null) e.setJoiningDate(java.time.LocalDate.parse(req.joiningDate));
        e.setRole(req.role == null ? "employee" : req.role);
        Employee saved = employeeRepository.save(e);
        return ResponseEntity.status(201).body(EntityMapper.toEmployeeResponse(saved));
    }

    @GetMapping
    public Page<EmployeeResponse> list(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String role,
                               @RequestParam(required = false) Long managerId) {
        Pageable p = PageRequest.of(page, size);
        Page<Employee> pageResult;
        if (role != null) pageResult = employeeRepository.findAllByRole(role, p);
        else if (managerId != null) pageResult = employeeRepository.findAllByManagerId(managerId, p);
        else pageResult = employeeRepository.findAll(p);
        return pageResult.map(EntityMapper::toEmployeeResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getById(@PathVariable Long id) {
        Optional<Employee> e = employeeRepository.findById(id);
        return e.map(emp -> ResponseEntity.ok(EntityMapper.toEmployeeResponse(emp))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/uuid/{uuid}")
    public ResponseEntity<EmployeeResponse> getByUuid(@PathVariable UUID uuid) {
        Optional<Employee> e = employeeRepository.findByUuid(uuid);
        return e.map(emp -> ResponseEntity.ok(EntityMapper.toEmployeeResponse(emp))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Long id, @RequestBody EmployeeUpdateRequest req) {
        Optional<Employee> opt = employeeRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Employee e = opt.get();
        if (req.firstName != null) e.setFirstName(req.firstName);
        if (req.lastName != null) e.setLastName(req.lastName);
        if (req.unitId != null) e.setUnitId(req.unitId);
        if (req.managerId != null) e.setManagerId(req.managerId);
        if (req.email != null) e.setEmail(req.email);
        if (req.joiningDate != null) e.setJoiningDate(java.time.LocalDate.parse(req.joiningDate));
        if (req.role != null) e.setRole(req.role);
        Employee saved = employeeRepository.save(e);
        return ResponseEntity.ok(EntityMapper.toEmployeeResponse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!employeeRepository.existsById(id)) return ResponseEntity.notFound().build();
        employeeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
