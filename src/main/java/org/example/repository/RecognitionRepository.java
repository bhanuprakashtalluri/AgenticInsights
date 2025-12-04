package org.example.repository;

import org.example.model.Recognition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecognitionRepository extends JpaRepository<Recognition, Long> {
    // --- Basic CRUD and fetch by id/uuid ---
    Optional<Recognition> findByUuid(UUID uuid);

    @Query("SELECT DISTINCT r FROM Recognition r LEFT JOIN FETCH r.recipient LEFT JOIN FETCH r.sender LEFT JOIN FETCH r.recognitionType WHERE r.id = :id")
    Optional<Recognition> findByIdWithRelations(Long id);

    @Query("SELECT DISTINCT r FROM Recognition r LEFT JOIN FETCH r.recipient LEFT JOIN FETCH r.sender LEFT JOIN FETCH r.recognitionType WHERE r.uuid = :uuid")
    Optional<Recognition> findByUuidWithRelations(UUID uuid);

    boolean existsById(Long id);

    // --- List and paging ---
    @Query(value = "SELECT DISTINCT r FROM Recognition r LEFT JOIN FETCH r.recipient LEFT JOIN FETCH r.sender LEFT JOIN FETCH r.recognitionType",
           countQuery = "SELECT count(r) FROM Recognition r")
    Page<Recognition> findAllWithRelations(Pageable pageable);

    Page<Recognition> findAllByRecipientId(Long recipientId, Pageable pageable);

    Page<Recognition> findAllBySenderId(Long senderId, Pageable pageable);

    // --- Search ---
    Page<Recognition> findByRecipient_UnitId(Long unitId, Pageable pageable);

    Page<Recognition> findByRecognitionType_Id(Long typeId, Pageable pageable);

    Page<Recognition> findByAwardPoints(Integer points, Pageable pageable);

    // --- Export helpers ---
    @Query("SELECT DISTINCT r FROM Recognition r LEFT JOIN FETCH r.recipient LEFT JOIN FETCH r.sender LEFT JOIN FETCH r.recognitionType WHERE r.sentAt BETWEEN :from AND :to")
    List<Recognition> findAllBetweenWithRelations(Instant from, Instant to);

    // --- Lite projection for insights ---
    @Query("SELECT r.sentAt as sentAt, r.awardPoints as awardPoints, r.recipientId as recipientId, r.senderId as senderId, r.approvalStatus as approvalStatus, r.level as level FROM Recognition r WHERE r.sentAt BETWEEN :from AND :to")
    List<org.example.repository.RecognitionLite> findAllLiteBetween(@Param("from") Instant from, @Param("to") Instant to);

    // --- All recognitions in time range ---
    @Query("SELECT r FROM Recognition r WHERE r.sentAt BETWEEN :from AND :to")
    List<Recognition> findAllBetween(@Param("from") Instant from, @Param("to") Instant to);

    // --- Leaderboard native queries ---
    @Query(value = "SELECT r.sender_id AS id, COUNT(*) AS cnt, COALESCE(SUM(r.award_points),0) AS pts FROM recognitions r WHERE r.sender_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to GROUP BY r.sender_id ORDER BY cnt DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> topSendersNative(@Param("from") Instant from, @Param("to") Instant to, @Param("limit") int limit);

    @Query(value = "SELECT r.recipient_id AS id, COUNT(*) AS cnt, COALESCE(SUM(r.award_points),0) AS pts FROM recognitions r WHERE r.recipient_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to GROUP BY r.recipient_id ORDER BY cnt DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> topRecipientsNative(@Param("from") Instant from, @Param("to") Instant to, @Param("limit") int limit);

    @Query(value = "SELECT r.sender_id AS id, COUNT(*) AS cnt, COALESCE(SUM(r.award_points),0) AS pts FROM recognitions r WHERE r.sender_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to AND (:role IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.sender_id AND e.role = :role)) AND (:unitId IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.sender_id AND e.unit_id = :unitId)) AND (:managerId IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.sender_id AND e.manager_id = :managerId)) GROUP BY r.sender_id ORDER BY cnt DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> topSendersNativeFiltered(@Param("from") Instant from, @Param("to") Instant to, @Param("role") String role, @Param("unitId") Long unitId, @Param("managerId") Long managerId, @Param("limit") int limit);

    @Query(value = "SELECT r.recipient_id AS id, COUNT(*) AS cnt, COALESCE(SUM(r.award_points),0) AS pts FROM recognitions r WHERE r.recipient_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to AND (:role IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.recipient_id AND e.role = :role)) AND (:unitId IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.recipient_id AND e.unit_id = :unitId)) AND (:managerId IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.recipient_id AND e.manager_id = :managerId)) GROUP BY r.recipient_id ORDER BY cnt DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> topRecipientsNativeFiltered(@Param("from") Instant from, @Param("to") Instant to, @Param("role") String role, @Param("unitId") Long unitId, @Param("managerId") Long managerId, @Param("limit") int limit);

    @Query(value = "SELECT r.sender_id AS id, COUNT(*) AS cnt, COALESCE(SUM(r.award_points),0) AS pts FROM recognitions r WHERE r.sender_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to AND (:role IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.sender_id AND e.role = :role)) AND (:unitId IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.sender_id AND e.unit_id = :unitId)) AND (:managerId IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.sender_id AND e.manager_id = :managerId)) GROUP BY r.sender_id ORDER BY cnt DESC LIMIT :size OFFSET :offset", nativeQuery = true)
    List<Object[]> topSendersNativePaged(@Param("from") Instant from, @Param("to") Instant to, @Param("role") String role, @Param("unitId") Long unitId, @Param("managerId") Long managerId, @Param("size") int size, @Param("offset") int offset);

    @Query(value = "SELECT r.recipient_id AS id, COUNT(*) AS cnt, COALESCE(SUM(r.award_points),0) AS pts FROM recognitions r WHERE r.recipient_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to AND (:role IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.recipient_id AND e.role = :role)) AND (:unitId IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.recipient_id AND e.unit_id = :unitId)) AND (:managerId IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.recipient_id AND e.manager_id = :managerId)) GROUP BY r.recipient_id ORDER BY cnt DESC LIMIT :size OFFSET :offset", nativeQuery = true)
    List<Object[]> topRecipientsNativePaged(@Param("from") Instant from, @Param("to") Instant to, @Param("role") String role, @Param("unitId") Long unitId, @Param("managerId") Long managerId, @Param("size") int size, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(DISTINCT r.sender_id) FROM recognitions r WHERE r.sender_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to AND (:role IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.sender_id AND e.role = :role)) AND (:unitId IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.sender_id AND e.unit_id = :unitId)) AND (:managerId IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.sender_id AND e.manager_id = :managerId))", nativeQuery = true)
    int countTopSendersNativeFiltered(@Param("from") Instant from, @Param("to") Instant to, @Param("role") String role, @Param("unitId") Long unitId, @Param("managerId") Long managerId);

    @Query(value = "SELECT COUNT(DISTINCT r.recipient_id) FROM recognitions r WHERE r.recipient_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to AND (:role IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.recipient_id AND e.role = :role)) AND (:unitId IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.recipient_id AND e.unit_id = :unitId)) AND (:managerId IS NULL OR EXISTS (SELECT 1 FROM employee e WHERE e.id = r.recipient_id AND e.manager_id = :managerId))", nativeQuery = true)
    int countTopRecipientsNativeFiltered(@Param("from") Instant from, @Param("to") Instant to, @Param("role") String role, @Param("unitId") Long unitId, @Param("managerId") Long managerId);

    @Query("SELECT r FROM Recognition r")
    List<Recognition> findAllRecognitions();
}
