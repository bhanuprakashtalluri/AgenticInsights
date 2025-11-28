package org.example.repository;

import org.example.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUuid(UUID uuid);
    Page<Employee> findAllByRole(String role, Pageable pageable);
    Page<Employee> findAllByManagerId(Long managerId, Pageable pageable);
    List<Employee> findAllByUnitId(Long unitId);
    List<Employee> findAllByUuidIn(List<UUID> uuids);
}
