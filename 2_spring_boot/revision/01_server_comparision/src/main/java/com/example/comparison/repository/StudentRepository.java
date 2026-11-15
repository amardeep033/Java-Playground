package com.example.comparison.repository;

import com.example.comparison.model.Student;
import java.util.List;
import org.springframework.stereotype.Repository;

// COMMON: fixed in-memory data keeps the example free of database configuration.
@Repository
public class StudentRepository {
    public List<Student> findAll() {
        return List.of(new Student(1L, "Amar"), new Student(2L, "Priya"));
    }
}
