package org.example.service;

import org.example.model.AuditLog;
import org.example.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    public String log(String username, String action, String details) {
        AuditLog log = new AuditLog(username, action, details);
        AuditLog saved = auditLogRepository.save(log);
        return "audit-" + saved.getId();
    }
}
