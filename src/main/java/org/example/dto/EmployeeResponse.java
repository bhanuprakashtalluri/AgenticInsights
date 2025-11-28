package org.example.dto;

import java.util.UUID;

public class EmployeeResponse {
    public Long id;
    public UUID uuid;
    public String firstName;
    public String lastName;
    public Long unitId;
    public Long managerId;
    public String email;
    public String joiningDate; // ISO date
    public String role;

    public EmployeeResponse() {}

    public EmployeeResponse(Long id, UUID uuid, String firstName, String lastName, Long unitId, Long managerId, String email, String joiningDate, String role) {
        this.id = id;
        this.uuid = uuid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.unitId = unitId;
        this.managerId = managerId;
        this.email = email;
        this.joiningDate = joiningDate;
        this.role = role;
    }
}

