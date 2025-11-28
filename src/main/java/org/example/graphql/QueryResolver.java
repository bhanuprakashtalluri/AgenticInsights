package org.example.graphql;

import org.example.model.Employee;
import org.example.model.Recognition;
import org.example.model.RecognitionType;
import org.example.repository.EmployeeRepository;
import org.example.repository.RecognitionRepository;
import org.example.repository.RecognitionTypeRepository;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class QueryResolver {

    private final RecognitionRepository recognitionRepository;
    private final EmployeeRepository employeeRepository;
    private final RecognitionTypeRepository recognitionTypeRepository;

    public QueryResolver(RecognitionRepository recognitionRepository, EmployeeRepository employeeRepository, RecognitionTypeRepository recognitionTypeRepository) {
        this.recognitionRepository = recognitionRepository;
        this.employeeRepository = employeeRepository;
        this.recognitionTypeRepository = recognitionTypeRepository;
    }

    @QueryMapping
    public List<Recognition> recognitions() {
        return recognitionRepository.findAll();
    }

    @QueryMapping
    public List<Employee> employees() {
        return employeeRepository.findAll();
    }

    @QueryMapping
    public List<RecognitionType> recognitionTypes() {
        return recognitionTypeRepository.findAll();
    }
}

