package com.example.overall.s8validation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// @Valid on the containing object tells Bean Validation to inspect these field-level constraints.
// Here the DTO itself carries the rules: @NotNull, @NotBlank, @Size, and @Email.
public record OrderRequest(
        @NotNull(message = "notnullitem must not be null")
        Integer notnullitem,

        @NotBlank(message = "notblankitem must not be blank") //cant be applied on integer
        String notblankitem,

        @Size(min = 2, max = 4, message = "sizeitem must be between 2 and 4 characters")
        String sizeitem,

        @Email(message = "emailitem must be a valid email")
        String emailitem,

        @NotBlank(message = "goodemailitem must not be blank")
        @Email(message = "goodemailitem must be a valid email")
        String goodemailitem
) {}

// Important: @Size and @Email do NOT mean "required".
// If null should be rejected, also add @NotNull or @NotBlank.
// @Size is not only for Strings. It can also apply to collections, maps, and arrays.

// | Input        | `@NotNull` | `@NotBlank` |
// | ------------ | ---------: | ----------: |
// | `null`       |         ❌ |          ❌ |
// | `""`         |         ✅ |          ❌ |
// | `"   "`      |         ✅ |          ❌ |
// | `"Amardeep"` |         ✅ |          ✅ |
