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
    Page<Recognition> findAllByRecipientId(Long recipientId, Pageable pageable);
    Page<Recognition> findAllBySenderId(Long senderId, Pageable pageable);

    @Query("SELECT r FROM Recognition r WHERE r.sentAt BETWEEN :from AND :to")
    List<Recognition> findAllBetween(Instant from, Instant to);

    Optional<Recognition> findByUuid(UUID uuid);

    // Eager fetch methods to avoid lazy loading during serialization
    @Query("SELECT DISTINCT r FROM Recognition r LEFT JOIN FETCH r.recipient LEFT JOIN FETCH r.sender LEFT JOIN FETCH r.recognitionType WHERE r.id = :id")
    Optional<Recognition> findByIdWithRelations(Long id);

    @Query("SELECT DISTINCT r FROM Recognition r LEFT JOIN FETCH r.recipient LEFT JOIN FETCH r.sender LEFT JOIN FETCH r.recognitionType WHERE r.uuid = :uuid")
    Optional<Recognition> findByUuidWithRelations(UUID uuid);

    @Query(value = "SELECT DISTINCT r FROM Recognition r LEFT JOIN FETCH r.recipient LEFT JOIN FETCH r.sender LEFT JOIN FETCH r.recognitionType",
           countQuery = "SELECT count(r) FROM Recognition r")
    Page<Recognition> findAllWithRelations(Pageable pageable);

    @Query("SELECT DISTINCT r FROM Recognition r LEFT JOIN FETCH r.recipient LEFT JOIN FETCH r.sender LEFT JOIN FETCH r.recognitionType WHERE r.sentAt BETWEEN :from AND :to")
    List<Recognition> findAllBetweenWithRelations(Instant from, Instant to);

    // Native aggregate queries for leaderboards (sender)
    @Query(value = "SELECT r.sender_id AS id, COUNT(*) AS cnt, COALESCE(SUM(r.award_points),0) AS pts FROM recognitions r WHERE r.sender_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to GROUP BY r.sender_id ORDER BY cnt DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> topSendersNative(@Param("from") Instant from, @Param("to") Instant to, @Param("limit") int limit);

    // Native aggregate queries for leaderboards (recipient)
    @Query(value = "SELECT r.recipient_id AS id, COUNT(*) AS cnt, COALESCE(SUM(r.award_points),0) AS pts FROM recognitions r WHERE r.recipient_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to GROUP BY r.recipient_id ORDER BY cnt DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> topRecipientsNative(@Param("from") Instant from, @Param("to") Instant to, @Param("limit") int limit);

    // Native aggregate queries with optional filters (role, unit_id, manager_id) - sender
    @Query(value = "SELECT s.id AS id, COUNT(*) AS cnt, COALESCE(SUM(r.award_points),0) AS pts FROM recognitions r JOIN employee s ON r.sender_id = s.id JOIN employee rec ON r.recipient_id = rec.id WHERE r.sender_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to " +
            "AND (:role IS NULL OR rec.role = :role) " +
            "AND (:unitId IS NULL OR rec.unit_id = :unitId) " +
            "AND (:managerId IS NULL OR rec.manager_id = :managerId) " +
            "GROUP BY s.id ORDER BY cnt DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> topSendersNativeFiltered(@Param("from") Instant from, @Param("to") Instant to, @Param("role") String role, @Param("unitId") Long unitId, @Param("managerId") Long managerId, @Param("limit") int limit);

    // Native aggregate queries with optional filters - recipient
    @Query(value = "SELECT rec.id AS id, COUNT(*) AS cnt, COALESCE(SUM(r.award_points),0) AS pts FROM recognitions r JOIN employee rec ON r.recipient_id = rec.id WHERE r.recipient_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to " +
            "AND (:role IS NULL OR rec.role = :role) " +
            "AND (:unitId IS NULL OR rec.unit_id = :unitId) " +
            "AND (:managerId IS NULL OR rec.manager_id = :managerId) " +
            "GROUP BY rec.id ORDER BY cnt DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> topRecipientsNativeFiltered(@Param("from") Instant from, @Param("to") Instant to, @Param("role") String role, @Param("unitId") Long unitId, @Param("managerId") Long managerId, @Param("limit") int limit);

    // Paged native queries (use LIMIT :size OFFSET :offset) and corresponding count queries
    @Query(value = "SELECT s.id AS id, COUNT(*) AS cnt, COALESCE(SUM(r.award_points),0) AS pts, s.first_name AS first_name, s.last_name AS last_name, s.uuid AS uuid FROM recognitions r JOIN employee s ON r.sender_id = s.id JOIN employee rec ON r.recipient_id = rec.id WHERE r.sender_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to " +
            "AND (:role IS NULL OR rec.role = :role) " +
            "AND (:unitId IS NULL OR rec.unit_id = :unitId) " +
            "AND (:managerId IS NULL OR rec.manager_id = :managerId) " +
            "GROUP BY s.id, s.first_name, s.last_name, s.uuid ORDER BY cnt DESC LIMIT :size OFFSET :offset", nativeQuery = true)
    List<Object[]> topSendersNativePaged(@Param("from") Instant from, @Param("to") Instant to, @Param("role") String role, @Param("unitId") Long unitId, @Param("managerId") Long managerId, @Param("size") int size, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(DISTINCT s.id) FROM recognitions r JOIN employee s ON r.sender_id = s.id JOIN employee rec ON r.recipient_id = rec.id WHERE r.sender_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to " +
            "AND (:role IS NULL OR rec.role = :role) " +
            "AND (:unitId IS NULL OR rec.unit_id = :unitId) " +
            "AND (:managerId IS NULL OR rec.manager_id = :managerId)", nativeQuery = true)
    int countTopSendersNativeFiltered(@Param("from") Instant from, @Param("to") Instant to, @Param("role") String role, @Param("unitId") Long unitId, @Param("managerId") Long managerId);

    @Query(value = "SELECT rec.id AS id, COUNT(*) AS cnt, COALESCE(SUM(r.award_points),0) AS pts, rec.first_name AS first_name, rec.last_name AS last_name, rec.uuid AS uuid FROM recognitions r JOIN employee rec ON r.recipient_id = rec.id WHERE r.recipient_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to " +
            "AND (:role IS NULL OR rec.role = :role) " +
            "AND (:unitId IS NULL OR rec.unit_id = :unitId) " +
            "AND (:managerId IS NULL OR rec.manager_id = :managerId) " +
            "GROUP BY rec.id, rec.first_name, rec.last_name, rec.uuid ORDER BY cnt DESC LIMIT :size OFFSET :offset", nativeQuery = true)
    List<Object[]> topRecipientsNativePaged(@Param("from") Instant from, @Param("to") Instant to, @Param("role") String role, @Param("unitId") Long unitId, @Param("managerId") Long managerId, @Param("size") int size, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(DISTINCT rec.id) FROM recognitions r JOIN employee rec ON r.recipient_id = rec.id WHERE r.recipient_id IS NOT NULL AND r.sent_at BETWEEN :from AND :to " +
            "AND (:role IS NULL OR rec.role = :role) " +
            "AND (:unitId IS NULL OR rec.unit_id = :unitId) " +
            "AND (:managerId IS NULL OR rec.manager_id = :managerId)", nativeQuery = true)
    int countTopRecipientsNativeFiltered(@Param("from") Instant from, @Param("to") Instant to, @Param("role") String role, @Param("unitId") Long unitId, @Param("managerId") Long managerId);

    // Native aggregate daily count query with optional role and manager filters
    @Query(value = "SELECT to_char(r.sent_at AT TIME ZONE 'UTC','YYYY-MM-DD') AS day, COUNT(*) AS cnt " +
            "FROM recognitions r JOIN employee rec ON r.recipient_id = rec.id " +
            "WHERE r.sent_at BETWEEN :from AND :to " +
            "AND (:role IS NULL OR rec.role = :role) " +
            "AND (:managerId IS NULL OR rec.manager_id = :managerId) " +
            "GROUP BY day ORDER BY day", nativeQuery = true)
    List<Object[]> countByDayFiltered(@Param("from") Instant from,
                                      @Param("to") Instant to,
                                      @Param("role") String role,
                                      @Param("managerId") Long managerId);

    @Query("SELECT r.sentAt AS sentAt, r.awardPoints AS awardPoints, r.recipientId AS recipientId, r.senderId AS senderId, r.approvalStatus AS approvalStatus, r.level AS level FROM Recognition r WHERE r.sentAt BETWEEN :from AND :to")
    List<RecognitionLite> findAllLiteBetween(Instant from, Instant to);
}
