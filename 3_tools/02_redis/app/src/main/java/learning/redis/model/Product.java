package learning.redis.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Product(Long id, @NotBlank @Size(max = 255) String name) {}
