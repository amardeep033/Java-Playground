package com.eventrelay.servicea.dto;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
public final class Requests {
    public record EndpointConfiguration(
            @NotNull Boolean enabled,
            @NotNull @Min(1) @Max(100000) Integer rateLimitPerSecond,
            @NotNull @Min(1) @Max(1000) Integer concurrencyLimit,
            @NotNull @Min(100) @Max(60000) Integer timeoutMs,
            @NotNull @Min(1) @Max(20) Integer maxAttempts) {
        public static EndpointConfiguration defaults() { return new EndpointConfiguration(true, 10, 4, 10000, 5); }
    }

    public record EndpointSettings(@Size(min=1,max=2048) String url,
            @Size(min=1,max=100) Set<@NotNull UUID> eventTypeIds,@jakarta.validation.Valid EndpointConfiguration configuration) {
        @AssertTrue public boolean isValidUpdate() { return url!=null||eventTypeIds!=null||configuration!=null; }
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void reject(String field,Object value) { throw new IllegalArgumentException("Only url, eventTypeIds and configuration can be updated"); }
    }
    public record EndpointDeliveryConfiguration(@NotNull @Min(1) @Max(100000) Integer rateLimitPerSecond,
            @NotNull @Min(1) @Max(1000) Integer concurrencyLimit,@NotNull @Min(100) @Max(60000) Integer timeoutMs,
            @NotNull @Min(1) @Max(20) Integer maxAttempts) {
        @com.fasterxml.jackson.annotation.JsonAnySetter public void reject(String field,Object value) {throw new IllegalArgumentException("Only delivery limits are allowed; activation is admin-only");}
    }
    public record AdminEndpointUpdate(Boolean active,@Size(min=1,max=2048) String url,@Size(min=1,max=100) Set<@NotNull UUID> eventTypeIds) {
        @AssertTrue @com.fasterxml.jackson.annotation.JsonIgnore public boolean isValidUpdate() {return (active!=null||url!=null||eventTypeIds!=null)&&(url==null||!url.isBlank());}
        @com.fasterxml.jackson.annotation.JsonAnySetter public void reject(String field,Object value) {throw new IllegalArgumentException("Only active, url and eventTypeIds are allowed");}
    }
    private Requests() {}
    public record Signup(@NotBlank @Size(max=120) String name, @NotBlank @Size(max=120) String companyName,
            @NotBlank @Email @Size(max=320) String email,
            @JsonProperty(access=JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(min=12,max=128) String password) {
        @Override public String toString() { return "Signup[credentials redacted]"; }
    }
    public record ClientUser(@NotBlank @Size(max=120) String name,@NotBlank @Email @Size(max=320) String email,
            @JsonProperty(access=JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(min=12,max=128) String password,
            @NotBlank @Pattern(regexp="USER|ADMIN") String role, @Size(max=100) Set<@NotNull UUID> applicationIds) {
        @Override public String toString() { return "ClientUser[credentials redacted]"; }
    }
    public record AccountUpdate(@Size(min=1,max=120) String name,@Email @Size(max=320) String email,@Pattern(regexp="USER|ADMIN") String role) {
        @AssertTrue public boolean isValidUpdate() { return (name!=null||email!=null||role!=null)&&(name==null||!name.isBlank())&&(email==null||!email.isBlank()); }
        @com.fasterxml.jackson.annotation.JsonAnySetter public void reject(String field,Object value) { throw new IllegalArgumentException("Only name, email and role can be updated"); }
    }
    public record ResetPassword(@JsonProperty(access=JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(min=12,max=128) String newPassword) {
        @Override public String toString() { return "ResetPassword[redacted]"; }
    }
    public record ChangePassword(
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(max = 128) String currentPassword,
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(min = 12, max = 128) String newPassword) {
        @Override public String toString() { return "ChangePassword[credentials redacted]"; }
    }

    public record UpdateName(@NotBlank @Size(max=120) String name) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void reject(String field, Object value) { throw new IllegalArgumentException("Only name can be updated"); }
    }
    /** Optional update fields: omitted values stay unchanged; explicit null clears description. */
    public static class ApplicationUpdateFields {
        @Size(max=500) private String description;
        private Boolean active;
        private boolean descriptionProvided;
        @com.fasterxml.jackson.annotation.JsonSetter
        public void setDescription(String value) {description=value;descriptionProvided=true;}
        @com.fasterxml.jackson.annotation.JsonSetter(nulls=com.fasterxml.jackson.annotation.Nulls.FAIL)
        public void setActive(Boolean value) {active=value;}
        public String description() {return description;}
        public Boolean active() {return active;}
        @com.fasterxml.jackson.annotation.JsonIgnore public boolean hasDescription() {return descriptionProvided;}
        protected boolean hasName() {return false;}
        @AssertTrue @com.fasterxml.jackson.annotation.JsonIgnore
        public boolean isValidUpdate() {return hasName()||descriptionProvided||active!=null;}
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void reject(String field,Object value) {throw new IllegalArgumentException("Unsupported resource update field");}
    }
    public static class UpdateApplication extends ApplicationUpdateFields {
        @Size(min=1,max=120) private String name;
        @com.fasterxml.jackson.annotation.JsonSetter(nulls=com.fasterxml.jackson.annotation.Nulls.FAIL)
        public void setName(String value) {name=value;}
        public String name() {return name;}
        @Override protected boolean hasName() {return name!=null;}
        @AssertTrue @com.fasterxml.jackson.annotation.JsonIgnore
        public boolean isValidName() {return name==null||!name.isBlank();}
    }

    public record CreateApiKey(@NotBlank @Size(max = 120) String name) {}

    public record RegisterApplication(@NotBlank @Size(max = 120) String name,UUID clientId) {
        public RegisterApplication(String name) { this(name,null); }
    }

    public record RegisterEventType(
            @NotBlank @Size(max = 120) @Pattern(regexp = "[a-zA-Z0-9_.-]+") String name,
            @Size(max = 500) String description) {}
            
    public record RegisterEndpoint(
            @Size(max=120) String name,
            @NotBlank @Size(max = 2048) String url,
            @Size(max = 500) String description,
            @NotEmpty @Size(max = 100) Set<@NotNull UUID> eventTypeIds,
            @jakarta.validation.Valid EndpointConfiguration configuration) {
        public RegisterEndpoint(String url,String description,Set<UUID> eventTypeIds,EndpointConfiguration configuration) {
            this(null,url,description,eventTypeIds,configuration);
        }
    }
    public record UpdateDescription(@Size(max=500) String description) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void reject(String field,Object value) {throw new IllegalArgumentException("Only description can be updated");}
    }
    public static class UpdateEndpoint {
        @Size(min=1,max=120) private String name;
        @Size(max=500) private String description;
        @jakarta.validation.Valid private EndpointDeliveryConfiguration configuration;
        private boolean descriptionProvided;
        @com.fasterxml.jackson.annotation.JsonSetter(nulls=com.fasterxml.jackson.annotation.Nulls.FAIL) public void setName(String value){name=value;}
        @com.fasterxml.jackson.annotation.JsonSetter public void setDescription(String value){description=value;descriptionProvided=true;}
        @com.fasterxml.jackson.annotation.JsonSetter(nulls=com.fasterxml.jackson.annotation.Nulls.FAIL) public void setConfiguration(EndpointDeliveryConfiguration value){configuration=value;}
        public String name(){return name;} public String description(){return description;}
        public EndpointDeliveryConfiguration configuration(){return configuration;}
        @com.fasterxml.jackson.annotation.JsonIgnore public boolean hasDescription(){return descriptionProvided;}
        @AssertTrue @com.fasterxml.jackson.annotation.JsonIgnore public boolean isValidUpdate(){return (name!=null||descriptionProvided||configuration!=null)&&(name==null||!name.isBlank());}
        @com.fasterxml.jackson.annotation.JsonAnySetter public void reject(String field,Object value){throw new IllegalArgumentException("Only name, description and delivery configuration are allowed");}
    }
    public record Activation(@NotNull Boolean active) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void reject(String field,Object value) {throw new IllegalArgumentException("Only active can be updated");}
    }
}
