package com.idet.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.idet.pos.validation.ValidCustomerId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CustomerDTO {
    @Schema(description = "Customer ID", example = "C001", requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^C\\d{3}$")
    @NotBlank(message = "Customer ID is required")
    @ValidCustomerId
    private String id;

    @Schema(description = "Customer name", example = "Nimal Perera", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
    @NotBlank(message = "Customer name is required")
    @Size(max = 100, message = "Customer name must be at most 100 characters")
    private String name;

    @Schema(description = "Customer address", example = "Colombo", maxLength = 255)
    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;

    @Schema(description = "Customer email", example = "nimal@example.com", format = "email", maxLength = 150)
    @Email(message = "Email format is invalid")
    @Size(max = 150, message = "Email must be at most 150 characters")
    private String email;

    public CustomerDTO() {}

    public CustomerDTO(String id, String name, String address, String email) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.email = email;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

