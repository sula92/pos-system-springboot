package com.idet.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idet.pos.dto.ItemDTO;
import com.idet.pos.exception.handler.GlobalExceptionHandler;
import com.idet.pos.service.ItemService;
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
class ItemControllerTest {

    @Mock
    private ItemService itemService;

    @InjectMocks
    private ItemController itemController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(itemController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void getItems_withInvalidCode_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/item").param("code", "X1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid item code format. Expected format: I followed by 3 digits (e.g. I001)"));

        verifyNoInteractions(itemService);
    }

    @Test
    void existsByDescription_returnsBooleanPayload() throws Exception {
        when(itemService.existsByDescriptionIgnoreCase("Rice 1kg")).thenReturn(true);

        mockMvc.perform(get("/item/exists-by-description").param("description", "Rice 1kg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        verify(itemService).existsByDescriptionIgnoreCase("Rice 1kg");
    }

    @Test
    void createItem_withInvalidPayload_returnsValidationError() throws Exception {
        ItemDTO invalid = new ItemDTO("BAD", "", -1.0);

        mockMvc.perform(post("/item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.code").exists())
                .andExpect(jsonPath("$.validationErrors.description").exists())
                .andExpect(jsonPath("$.validationErrors.unitPrice").exists());

        verifyNoInteractions(itemService);
    }

    @Test
    void createItem_withValidPayload_returnsCreated() throws Exception {
        ItemDTO dto = new ItemDTO("I001", "Rice 1kg", 250.0);
        when(itemService.saveItem(any(ItemDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Item saved successfully"))
                .andExpect(jsonPath("$.data.code").value("I001"));

        verify(itemService).saveItem(any(ItemDTO.class));
    }
}

