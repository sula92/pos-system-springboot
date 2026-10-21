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

import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver();
        pageableResolver.setMaxPageSize(100);

        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(pageableResolver)
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

    @Test
    void getOrders_withoutId_returnsNewestFirstPage() throws Exception {
        OrderDTO order = new OrderDTO();
        order.setOrderId("O003");
        order.setCustomerId("C001");
        when(orderService.findAllOrders(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 1), 3));

        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value("O003"))
                .andExpect(jsonPath("$.page.totalElements").value(3));

        verify(orderService).findAllOrders(PageRequest.of(0, 20,
                Sort.by(Sort.Order.desc("date"), Sort.Order.asc("orderId"))));
    }

    @Test
    void getOrderSummaries_passesPageableToReport() throws Exception {
        when(orderService.findOrderSummariesDto(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/order/summary-dto").param("page", "2").param("size", "10"))
                .andExpect(status().isOk());

        verify(orderService).findOrderSummariesDto(PageRequest.of(2, 10));
    }
}
