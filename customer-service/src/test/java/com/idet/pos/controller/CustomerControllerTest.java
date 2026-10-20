package com.idet.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idet.pos.dto.CustomerDTO;
import com.idet.pos.exception.handler.GlobalExceptionHandler;
import com.idet.pos.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerController customerController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(customerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void getCustomers_withId_returnsSingleCustomer() throws Exception {
        when(customerService.findCustomer("C001"))
                .thenReturn(new CustomerDTO("C001", "Nimal", "Colombo", "nimal@example.com"));

        mockMvc.perform(get("/customer").param("id", "C001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("C001"))
                .andExpect(jsonPath("$.name").value("Nimal"));

        verify(customerService).findCustomer("C001");
    }

    @Test
    void getCustomers_withInvalidId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/customer").param("id", "BAD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid customer ID format. Expected format: C followed by 3 digits (e.g. C001)"));

        verifyNoInteractions(customerService);
    }

    @Test
    void createCustomer_withValidPayload_returnsCreatedResponse() throws Exception {
        CustomerDTO request = new CustomerDTO("C001", "Nimal", "Colombo", "nimal@example.com");
        when(customerService.saveCustomer(any(CustomerDTO.class))).thenReturn(request);

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Customer saved successfully"))
                .andExpect(jsonPath("$.data.id").value("C001"));

        verify(customerService).saveCustomer(any(CustomerDTO.class));
    }
}

