package com.idet.pos.service;

import com.idet.pos.dto.CustomerDTO;
import com.idet.pos.entity.Customer;
import com.idet.pos.exception.ResourceNotFoundException;
import com.idet.pos.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void saveCustomer_returnsSavedDto() {
        CustomerDTO input = new CustomerDTO("C001", "Nimal", "Colombo", "nimal@example.com");
        Customer savedEntity = new Customer("C001", "Nimal", "Colombo", "nimal@example.com");

        when(customerRepository.save(any(Customer.class))).thenReturn(savedEntity);

        CustomerDTO result = customerService.saveCustomer(input);

        assertEquals("C001", result.getId());
        assertEquals("Nimal", result.getName());
        assertEquals("Colombo", result.getAddress());
        assertEquals("nimal@example.com", result.getEmail());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void findCustomer_whenNotFound_throwsResourceNotFoundException() {
        when(customerRepository.findById("C999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.findCustomer("C999"));

        verify(customerRepository).findById("C999");
    }

    @Test
    void findAllCustomers_mapsEntityListToDtoList() {
        List<Customer> entities = List.of(
                new Customer("C001", "Nimal", "Colombo", "nimal@example.com"),
                new Customer("C002", "Kamal", "Galle", "kamal@example.com")
        );
        when(customerRepository.findAll()).thenReturn(entities);

        List<CustomerDTO> result = customerService.findAllCustomers();

        assertEquals(2, result.size());
        assertEquals("C001", result.get(0).getId());
        assertEquals("C002", result.get(1).getId());
        verify(customerRepository).findAll();
    }

    @Test
    void deleteCustomer_whenExists_deletesCustomer() {
        when(customerRepository.existsById("C001")).thenReturn(true);

        customerService.deleteCustomer("C001");

        verify(customerRepository).existsById("C001");
        verify(customerRepository).deleteById("C001");
    }

    @Test
    void deleteCustomer_whenMissing_throwsResourceNotFoundException() {
        when(customerRepository.existsById("C404")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> customerService.deleteCustomer("C404"));

        verify(customerRepository).existsById("C404");
        verifyNoMoreInteractions(customerRepository);
    }
}

