package com.idet.pos.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "pos.security.admin.username=test-admin",
        "pos.security.admin.password=admin-pass",
        "pos.security.user.username=test-user",
        "pos.security.user.password=user-pass"
})
class SecurityConfigTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static ResultMatcher allowed() {
        return result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    @Test
    void anonymousRequestIsUnauthorized() throws Exception {
        mvc.perform(get("/item")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(get("/item").with(httpBasic("test-user", "wrong"))).andExpect(status().isUnauthorized());
    }

    @Test
    void userCanReadItems() throws Exception {
        mvc.perform(get("/item").with(httpBasic("test-user", "user-pass"))).andExpect(allowed());
    }

    @Test
    void userCannotCreateItems() throws Exception {
        mvc.perform(post("/item").with(httpBasic("test-user", "user-pass"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotDeleteCustomers() throws Exception {
        mvc.perform(delete("/customer").param("id", "C001").with(httpBasic("test-user", "user-pass")))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotViewReports() throws Exception {
        mvc.perform(get("/order/summary").with(httpBasic("test-user", "user-pass")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanViewReports() throws Exception {
        mvc.perform(get("/order/summary").with(httpBasic("test-admin", "admin-pass"))).andExpect(allowed());
    }

    @Test
    void adminInheritsUserAccess() throws Exception {
        mvc.perform(get("/customer").with(httpBasic("test-admin", "admin-pass"))).andExpect(allowed());
    }

    @Test
    void unlistedEndpointIsDenied() throws Exception {
        mvc.perform(get("/not-mapped").with(httpBasic("test-admin", "admin-pass")))
                .andExpect(status().isForbidden());
    }
}
