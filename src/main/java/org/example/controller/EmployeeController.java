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

import java.util.List;
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

    // Helper method for 'all' check
    private boolean isAll(Object param) {
        if (param == null) return false;
        if (param instanceof String s) return s.equalsIgnoreCase("all");
        return false;
    }

    @GetMapping
    public Page<EmployeeResponse> list(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String role,
                               @RequestParam(required = false) Long managerId,
                               @RequestParam(required = false) Long unitId) {
        Pageable p = PageRequest.of(page, size);
        Page<Employee> pageResult;
        if (role != null && !isAll(role)) pageResult = employeeRepository.findAllByRole(role, p);
        else if (managerId != null && !isAll(managerId)) pageResult = employeeRepository.findAllByManagerId(managerId, p);
        else if (unitId != null && !isAll(unitId)) pageResult = employeeRepository.findAllByUnitId(unitId, p);
        else pageResult = employeeRepository.findAll(p);
        return pageResult.map(EntityMapper::toEmployeeResponse);
    }

    // Unified get by ID or UUID (as request parameters)
    @GetMapping("/single")
    public ResponseEntity<EmployeeResponse> getByIdOrUuid(@RequestParam(required = false) Long id, @RequestParam(required = false) UUID uuid) {
        Optional<Employee> e = Optional.empty();
        if (id != null) e = employeeRepository.findById(id);
        else if (uuid != null) e = employeeRepository.findByUuid(uuid);
        return e.map(emp -> ResponseEntity.ok(EntityMapper.toEmployeeResponse(emp))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Unified update by ID or UUID (as request parameters)
    @PutMapping("/single")
    public ResponseEntity<EmployeeResponse> update(@RequestParam(required = false) Long id, @RequestParam(required = false) UUID uuid, @RequestBody EmployeeUpdateRequest req) {
        Optional<Employee> opt = Optional.empty();
        if (id != null) opt = employeeRepository.findById(id);
        else if (uuid != null) opt = employeeRepository.findByUuid(uuid);
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

    // Unified delete by ID or UUID (as request parameters)
    @DeleteMapping("/single")
    public ResponseEntity<?> delete(@RequestParam(required = false) Long id, @RequestParam(required = false) UUID uuid) {
        Optional<Employee> opt = Optional.empty();
        if (id != null) opt = employeeRepository.findById(id);
        else if (uuid != null) opt = employeeRepository.findByUuid(uuid);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        employeeRepository.deleteById(opt.get().getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public Page<EmployeeResponse> search(@RequestParam(required = false) Long id,
                                         @RequestParam(required = false) UUID uuid,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(required = false) Long unitId,
                                         @RequestParam(required = false) String role,
                                         @RequestParam(required = false) Long managerId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(page, size);
        if (id != null) {
            return employeeRepository.findById(id)
                    .map(e -> new org.springframework.data.domain.PageImpl<>(List.of(EntityMapper.toEmployeeResponse(e)), p, 1))
                    .orElseGet(() -> new org.springframework.data.domain.PageImpl<>(List.of(), p, 0));
        } else if (uuid != null) {
            return employeeRepository.findByUuid(uuid)
                    .map(e -> new org.springframework.data.domain.PageImpl<>(List.of(EntityMapper.toEmployeeResponse(e)), p, 1))
                    .orElseGet(() -> new org.springframework.data.domain.PageImpl<>(List.of(), p, 0));
        }
        Page<Employee> pageResult;
        if (name != null && !isAll(name) && !name.isBlank()) {
            pageResult = employeeRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(name, name, p);
        } else if (unitId != null && !isAll(unitId)) {
            pageResult = employeeRepository.findAllByUnitId(unitId, p);
        } else if (role != null && !isAll(role)) {
            pageResult = employeeRepository.findAllByRole(role, p);
        } else if (managerId != null && !isAll(managerId)) {
            pageResult = employeeRepository.findAllByManagerId(managerId, p);
        } else {
            pageResult = employeeRepository.findAll(p);
        }
        return pageResult.map(EntityMapper::toEmployeeResponse);
    }
}
