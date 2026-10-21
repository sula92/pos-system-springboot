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
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();
        pageableResolver.setMaxPageSize(100);

        mockMvc = MockMvcBuilders.standaloneSetup(customerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(pageableResolver)
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

    @Test
    void getCustomers_withoutId_returnsRequestedPageAsPagedModel() throws Exception {
        when(customerService.findAllCustomers(any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(new CustomerDTO("C006", "Nimal", "Colombo", "nimal@example.com")),
                PageRequest.of(1, 5), 12));

        mockMvc.perform(get("/customer").param("page", "1").param("size", "5").param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("C006"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(12))
                .andExpect(jsonPath("$.page.totalPages").value(3));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(customerService).findAllCustomers(captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "name"), captor.getValue().getSort());
    }

    @Test
    void getCustomers_withoutPagingParams_usesDefaultPageAndSort() throws Exception {
        when(customerService.findAllCustomers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(customerService).findAllCustomers(PageRequest.of(0, 20, Sort.by("id")));
    }

    @Test
    void getCustomers_withSizeAboveMaximum_capsPageSize() throws Exception {
        when(customerService.findAllCustomers(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/customer").param("size", "500"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(customerService).findAllCustomers(captor.capture());
        assertEquals(100, captor.getValue().getPageSize());
    }

    @Test
    void getCustomers_withUnknownSortProperty_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/customer").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported sort property: password. Allowed: address, email, id, name"));

        verifyNoInteractions(customerService);
    }

    @Test
    void searchCustomers_passesKeywordAndPageable() throws Exception {
        when(customerService.searchCustomersByName(eq("nim"), any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(new CustomerDTO("C001", "Nimal", "Colombo", "nimal@example.com")),
                PageRequest.of(0, 2), 1));

        mockMvc.perform(get("/customer/search").param("name", "nim").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Nimal"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        verify(customerService).searchCustomersByName("nim", PageRequest.of(0, 2, Sort.by("id")));
    }
}
