package org.example.dto;

import java.util.UUID;

public class EmployeeCreateRequest {
    public String firstName;
    public String lastName;
    public Long unitId;
    public Long managerId;
    public UUID managerUuid;
    public String email;
    public String joiningDate; // ISO date
    public String role; // employee, teamlead, manager
}

