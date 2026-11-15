package com.example.comparison.controller;

import com.example.comparison.model.Student;
import com.example.comparison.service.StudentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// COMMON: Spring MVC handles the request; the response is JSON.
@RestController
public class StudentController {
    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @GetMapping("/students")
    public List<Student> getStudents() {
        return service.getStudents(); //This works because @RestController includes @ResponseBody, telling Spring MVC to write the returned value directly into the HTTP response body.
    }
}
// why not return ResponseEntity here?
// Because ResponseEntity is optional in Spring MVC.
// Spring MVC can automatically convert your return value into an HTTP response.
// with ResponseEntity, we explicitly specify the HTTP status, headers, and/or body.
// Example: empty list means error (by default 200)
// return productService.getProductById(id)
//         .map(ResponseEntity::ok)
//         .orElse(ResponseEntity.notFound().build());