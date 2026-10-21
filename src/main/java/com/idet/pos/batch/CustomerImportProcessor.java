package com.idet.pos.batch;

import com.idet.pos.dto.CustomerDTO;
import com.idet.pos.entity.Customer;
import jakarta.validation.Validator;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * Validates one customer CSV row and maps it to the entity.
 * Existing customers with the same id are updated by the writer (merge), like PUT /customer.
 */
public class CustomerImportProcessor implements ItemProcessor<CustomerDTO, Customer> {

    private final Validator validator;

    public CustomerImportProcessor(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Customer process(CustomerDTO row) {
        ImportValidation.validate(validator, row, row.getId());
        return new Customer(row.getId(), row.getName(), row.getAddress(), row.getEmail());
    }
}
