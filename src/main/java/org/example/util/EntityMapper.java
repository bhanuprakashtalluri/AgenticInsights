package org.example.util;

import org.example.dto.EmployeeResponse;
import org.example.dto.LeaderboardEntry;
import org.example.dto.RecognitionResponse;
import org.example.dto.RecognitionTypeResponse;
import org.example.model.Employee;
import org.example.model.Recognition;
import org.example.model.RecognitionType;
import org.example.repository.EmployeeRepository;

public class EntityMapper {

    public static EmployeeResponse toEmployeeResponse(Employee e) {
        if (e == null) return null;
        EmployeeResponse r = new EmployeeResponse();
        r.id = e.getId();
        r.uuid = e.getUuid();
        r.firstName = e.getFirstName();
        r.lastName = e.getLastName();
        r.unitId = e.getUnitId();
        r.managerId = e.getManagerId();
        r.email = e.getEmail();
        r.joiningDate = e.getJoiningDate() == null ? null : e.getJoiningDate().toString();
        r.role = e.getRole();
        return r;
    }

    public static RecognitionTypeResponse toRecognitionTypeResponse(RecognitionType t, EmployeeRepository employeeRepo) {
        if (t == null) return null;
        String createdByName = null;
        if (t.getCreatedBy() != null) {
            var empOpt = employeeRepo.findById(t.getCreatedBy());
            if (empOpt.isPresent()) {
                var emp = empOpt.get();
                createdByName = emp.getFirstName() + " " + emp.getLastName();
            } else {
                createdByName = String.valueOf(t.getCreatedBy());
            }
        }
        return new RecognitionTypeResponse(
            t.getId(),
            t.getUuid(),
            t.getTypeName(),
            createdByName,
            t.getCreatedAt() == null ? null : t.getCreatedAt().toString()
        );
    }

    public static RecognitionResponse toRecognitionResponse(Recognition r) {
        if (r == null) return null;
        RecognitionResponse out = new RecognitionResponse();
        out.id = r.getId();
        out.uuid = r.getUuid();
        out.recognitionTypeId = r.getRecognitionType() == null ? null : r.getRecognitionType().getId();
        out.recognitionTypeName = r.getRecognitionType() == null ? null : r.getRecognitionType().getTypeName();
        out.category = r.getCategory();
        out.level = r.getLevel();
        out.recipientId = r.getRecipientId();
        out.recipientUuid = r.getRecipient() == null ? null : r.getRecipient().getUuid();
        out.recipientName = r.getRecipient() == null ? null : (r.getRecipient().getFirstName() + " " + r.getRecipient().getLastName());
        out.recipientRole = r.getRecipient() == null ? null : r.getRecipient().getRole();
        out.senderId = r.getSenderId();
        out.senderUuid = r.getSender() == null ? null : r.getSender().getUuid();
        out.senderName = r.getSender() == null ? null : (r.getSender().getFirstName() + " " + r.getSender().getLastName());
        out.senderRole = r.getSender() == null ? null : r.getSender().getRole();
        out.sentAt = r.getSentAt();
        out.message = r.getMessage();
        out.awardPoints = r.getAwardPoints();
        out.approvalStatus = r.getApprovalStatus();
        out.rejectionReason = r.getRejectionReason();
        out.createdAt = r.getCreatedAt();
        return out;
    }

    public static LeaderboardEntry toLeaderboardEntry(Long id, String name, Long count, Integer points) {
        return new LeaderboardEntry(id, name, count, points);
    }
}
