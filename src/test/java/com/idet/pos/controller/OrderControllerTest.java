package com.idet.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.idet.pos.dto.OrderDTO;
import com.idet.pos.dto.OrderDetailDTO;
import com.idet.pos.exception.handler.GlobalExceptionHandler;
import com.idet.pos.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void getOrders_withInvalidId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/order").param("id", "BAD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid order ID format. Expected format: O followed by 3 digits (e.g. O001)"));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_withoutDate_setsDateBeforeServiceCall() throws Exception {
        OrderDTO request = new OrderDTO(null, null, "C001", List.of(new OrderDetailDTO(null, "I001", 2, 250.0)));

        when(orderService.placeOrder(any(OrderDTO.class))).thenAnswer(invocation -> {
            OrderDTO captured = invocation.getArgument(0);
            return new OrderDTO("O001", captured.getDate(), captured.getCustomerId(), captured.getOrderDetails());
        });

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Order placed successfully"))
                .andExpect(jsonPath("$.data.orderId").value("O001"))
                .andExpect(jsonPath("$.data.customerId").value("C001"));

        ArgumentCaptor<OrderDTO> orderCaptor = ArgumentCaptor.forClass(OrderDTO.class);
        verify(orderService).placeOrder(orderCaptor.capture());
        assertNotNull(orderCaptor.getValue().getDate());
    }

    @Test
    void countByCustomerNative_whenCustomerIdBlank_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/order/count-by-customer-native").param("customerId", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("customerId is required"));
    }
}

