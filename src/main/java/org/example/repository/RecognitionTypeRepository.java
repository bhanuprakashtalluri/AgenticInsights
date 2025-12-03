package org.example.repository;

import org.example.model.RecognitionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecognitionTypeRepository extends JpaRepository<RecognitionType, Long> {
    Optional<RecognitionType> findByUuid(java.util.UUID uuid);
    List<RecognitionType> findAllByUuidIn(List<java.util.UUID> uuids);
    List<RecognitionType> findByTypeNameContainingIgnoreCase(String name);
}
