package com.example.comparison.service;

import com.example.comparison.model.Student;
import com.example.comparison.repository.StudentRepository;
import java.util.List;
import org.springframework.stereotype.Service;

// COMMON: constructor injection works the same in both.
@Service
public class StudentService {
    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    public List<Student> getStudents() {
        return repository.findAll();
    }
}
