package com.idet.pos.controller;

import com.idet.pos.service.CustomerService;
import com.idet.pos.dto.CustomerPurchaseStatsDTO;
import com.idet.pos.dto.CustomerDTO;
import com.idet.pos.exception.InvalidRequestException;
import com.idet.pos.projection.CustomerBasicProjection;
import com.idet.pos.projection.CustomerOrderCountProjection;
import com.idet.pos.projection.CustomerPurchaseStatsProjection;
import com.idet.pos.projection.CustomerSummary;
import com.idet.pos.validation.ValidCustomerId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@RestController
@RequestMapping("/customer")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Customer", description = "Customer management and customer analytics APIs")
@Validated
public class CustomerController {

    private static final Logger logger = Logger.getLogger(CustomerController.class.getName());
    @Autowired
    private CustomerService customerService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @GetMapping
    @Operation(summary = "Get customers", description = "Fetch all customers or a single customer by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customers fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid customer id format")
    })
    public ResponseEntity<?> getCustomers(
            @Parameter(example = "C001")
            @RequestParam(name = "id", required = false)
            @ValidCustomerId(message = "Invalid customer ID format. Expected format: C followed by 3 digits (e.g. C001)")
            String id
    ) {
        if (id != null) {
            validateCustomerId(id);
            return ResponseEntity.ok(customerService.findCustomer(id));
        }

        logger.info("Fetching all customers");
        List<CustomerDTO> all = customerService.findAllCustomers();
        return ResponseEntity.ok(all);
    }

    @PostMapping
    @Operation(summary = "Create customer", description = "Create a new customer record")
    @ApiResponse(responseCode = "201", description = "Customer created successfully")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Customer payload",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "CreateCustomer",
                            value = "{\n  \"id\": \"C001\",\n  \"name\": \"Nimal Perera\",\n  \"address\": \"Colombo\",\n  \"email\": \"nimal@example.com\"\n}"
                    )
            )
    )
    public ResponseEntity<?> createCustomer(@Valid @RequestBody CustomerDTO dto) {
        logger.info("Attempting to save new customer");
        CustomerDTO saved = customerService.saveCustomer(dto);
        logger.info("Customer saved successfully with ID: " + saved.getId());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("message", "Customer saved successfully");
        resp.put("data", saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping
    @Operation(summary = "Update customer", description = "Update an existing customer record")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer updated successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Customer payload",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "UpdateCustomer",
                            value = "{\n  \"id\": \"C001\",\n  \"name\": \"Nimal Perera Updated\",\n  \"address\": \"Kandy\",\n  \"email\": \"nimal.updated@example.com\"\n}"
                    )
            )
    )
    public ResponseEntity<?> updateCustomer(@Valid @RequestBody CustomerDTO dto) {
        String id = dto.getId();
        logger.info("Attempting to update customer: " + id);
        CustomerDTO updated = customerService.updateCustomer(dto);
        logger.info("Customer updated successfully: " + id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping
    @Operation(summary = "Delete customer", description = "Delete a customer by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<?> deleteCustomer(
            @Parameter(example = "C001")
            @RequestParam("id")
            @ValidCustomerId(message = "Invalid customer ID format. Expected format: C followed by 3 digits (e.g. C001)")
            String id
    ) {
        logger.info("Attempting to delete customer: " + id);
        validateCustomerId(id);
        customerService.deleteCustomer(id);
        logger.info("Customer deleted successfully: " + id);
        return ResponseEntity.ok(successResponse("Customer deleted successfully"));
    }

    @GetMapping("/purchase-stats")
    @Operation(summary = "Customer purchase stats", description = "Get customer purchase statistics using interface projection")
    @ApiResponse(responseCode = "200", description = "Customer purchase stats fetched")
    public ResponseEntity<List<CustomerPurchaseStatsProjection>> getCustomerPurchaseStats() {
        return ResponseEntity.ok(customerService.findCustomerPurchaseStats());
    }

    @GetMapping("/purchase-stats-dto")
    @Operation(summary = "Customer purchase stats DTO", description = "Get customer purchase statistics mapped to DTO objects")
    @ApiResponse(responseCode = "200", description = "Customer purchase stats DTO fetched")
    public ResponseEntity<List<CustomerPurchaseStatsDTO>> getCustomerPurchaseStatsDto() {
        return ResponseEntity.ok(customerService.findCustomerPurchaseStatsDto());
    }


    @GetMapping("/search")
    @Operation(summary = "Search customers by name", description = "Case-insensitive name search using derived query")
    @ApiResponse(responseCode = "200", description = "Matching customers fetched")
    public ResponseEntity<List<CustomerDTO>> searchCustomers(
            @Parameter(example = "nim")
            @RequestParam("name")
            @NotBlank(message = "Name is required for customer search")
            String name
    ) {
        return ResponseEntity.ok(customerService.searchCustomersByName(name));
    }

    @GetMapping("/search-jpql")
    @Operation(summary = "Search customers by name (JPQL)", description = "Name search using a custom JPQL query")
    @ApiResponse(responseCode = "200", description = "Matching customers fetched")
    public ResponseEntity<List<CustomerDTO>> searchCustomersWithJpql(
            @Parameter(example = "nim")
            @RequestParam("name")
            @NotBlank(message = "Name is required for JPQL customer search")
            String name
    ) {
        return ResponseEntity.ok(customerService.searchCustomersByNameJpql(name));
    }

    @GetMapping("/by-email")
    @Operation(summary = "Get customer by email", description = "Find a customer by email address")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<CustomerDTO> getCustomerByEmail(
            @Parameter(example = "nimal@example.com")
            @RequestParam("email")
            @NotBlank(message = "Email is required")
            String email
    ) {
        return ResponseEntity.ok(customerService.findCustomerByEmail(email));
    }

    @GetMapping("/by-name-exact")
    @Operation(summary = "Get customers by exact name", description = "Find customers with an exact name match")
    @ApiResponse(responseCode = "200", description = "Customers fetched successfully")
    public ResponseEntity<List<CustomerDTO>> getCustomersByExactName(
            @Parameter(example = "Nimal Perera")
            @RequestParam("name")
            @NotBlank(message = "Name is required")
            String name
    ) {
        return ResponseEntity.ok(customerService.findCustomersByExactName(name));
    }

    @GetMapping("/by-email-exact")
    @Operation(summary = "Get customers by exact email", description = "Find customers with an exact email match")
    @ApiResponse(responseCode = "200", description = "Customers fetched successfully")
    public ResponseEntity<List<CustomerDTO>> getCustomersByExactEmail(
            @Parameter(example = "nimal@example.com")
            @RequestParam("email")
            @NotBlank(message = "Email is required")
            String email
    ) {
        return ResponseEntity.ok(customerService.findCustomersByExactEmail(email));
    }

    @GetMapping("/by-address")
    @Operation(summary = "Get customers by address", description = "Find customers by address pattern")
    @ApiResponse(responseCode = "200", description = "Customers fetched successfully")
    public ResponseEntity<List<CustomerDTO>> getCustomersByAddress(
            @Parameter(example = "Colombo")
            @RequestParam("address")
            @NotBlank(message = "Address is required")
            String address
    ) {
        return ResponseEntity.ok(customerService.findCustomersByAddress(address));
    }

    @GetMapping("/search-by-email")
    @Operation(summary = "Search customers by email", description = "Search customers by partial email text")
    @ApiResponse(responseCode = "200", description = "Matching customers fetched")
    public ResponseEntity<List<CustomerDTO>> searchCustomersByEmail(
            @Parameter(example = "example.com")
            @RequestParam("email")
            @NotBlank(message = "Email is required for search")
            String email
    ) {
        return ResponseEntity.ok(customerService.searchCustomersByEmail(email));
    }

    @GetMapping("/search-by-address")
    @Operation(summary = "Search customers by address", description = "Search customers by partial address text")
    @ApiResponse(responseCode = "200", description = "Matching customers fetched")
    public ResponseEntity<List<CustomerDTO>> searchCustomersByAddress(
            @Parameter(example = "Road")
            @RequestParam("address")
            @NotBlank(message = "Address is required for search")
            String address
    ) {
        return ResponseEntity.ok(customerService.searchCustomersByAddress(address));
    }

    @GetMapping("/search-all-fields")
    @Operation(summary = "Search customers by multiple fields", description = "Search by name, email, or address using one keyword")
    @ApiResponse(responseCode = "200", description = "Matching customers fetched")
    public ResponseEntity<List<CustomerDTO>> searchCustomersByNameEmailAddress(
            @Parameter(example = "C00")
            @RequestParam("keyword")
            @NotBlank(message = "keyword is required for search")
            String keyword
    ) {
        return ResponseEntity.ok(customerService.searchCustomersByNameEmailAddress(keyword));
    }

    @GetMapping("/search-all-fields-with-id")
    @Operation(summary = "Search customers by multiple fields including id", description = "Search by id, name, email, or address using one keyword")
    @ApiResponse(responseCode = "200", description = "Matching customers fetched")
    public ResponseEntity<List<CustomerDTO>> searchCustomersByNameEmailAddressId(
            @Parameter(example = "C00")
            @RequestParam("keyword")
            @NotBlank(message = "keyword is required for search")
            String keyword
    ) {
        return ResponseEntity.ok(customerService.searchCustomersByNameEmailAddressId(keyword));
    }

    @GetMapping("/native")
    @Operation(summary = "Get customers (native)", description = "Fetch all customers using a native SQL query")
    @ApiResponse(responseCode = "200", description = "Customers fetched successfully")
    public ResponseEntity<List<CustomerDTO>> getAllCustomersNative() {
        return ResponseEntity.ok(customerService.findAllCustomersNative());
    }

    @GetMapping("/native-by-name")
    @Operation(summary = "Get customers by name (native)", description = "Fetch customers by exact name using native SQL")
    @ApiResponse(responseCode = "200", description = "Customers fetched successfully")
    public ResponseEntity<List<CustomerDTO>> getCustomersByNameNative(
            @Parameter(example = "Nimal Perera")
            @RequestParam("name")
            @NotBlank(message = "Name is required")
            String name
    ) {
        return ResponseEntity.ok(customerService.findCustomersByNameNative(name));
    }

    @GetMapping("/with-orders")
    @Operation(summary = "Get customers with orders", description = "Return customers that have orders, as a lightweight projection")
    @ApiResponse(responseCode = "200", description = "Customers with orders fetched")
    public ResponseEntity<List<CustomerBasicProjection>> getCustomersWithOrders() {
        return ResponseEntity.ok(customerService.findCustomersWithOrders());
    }

    @GetMapping("/with-order-count")
    @Operation(summary = "Get customers with order count", description = "Return each customer with their order count")
    @ApiResponse(responseCode = "200", description = "Customer order counts fetched")
    public ResponseEntity<List<CustomerOrderCountProjection>> getCustomersWithOrderCount() {
        return ResponseEntity.ok(customerService.findCustomersWithOrderCount());
    }

    @GetMapping("/summary")
    @Operation(summary = "Get customer summaries", description = "Return compact customer summary rows")
    @ApiResponse(responseCode = "200", description = "Customer summaries fetched")
    public ResponseEntity<List<CustomerSummary>> getCustomerSummaries() {
        return ResponseEntity.ok(customerService.findCustomerSummaries());
    }

    private Map<String, Object> successResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", message);
        return response;
    }

    private void validateCustomerId(String id) {
        Set<ConstraintViolation<CustomerIdRequest>> violations = validator.validateValue(CustomerIdRequest.class, "id", id);
        if (!violations.isEmpty()) {
            throw new InvalidRequestException(violations.iterator().next().getMessage());
        }
    }

    private static class CustomerIdRequest {
        @ValidCustomerId(message = "Invalid customer ID format. Expected format: C followed by 3 digits (e.g. C001)")
        private String id;
    }

}
