package com.balaji.kafkaeventlab.identity;

import com.balaji.kafkaeventlab.identity.domain.UserProfileRepository;
import com.balaji.kafkaeventlab.identity.event.IdentityEvent;
import com.balaji.kafkaeventlab.identity.event.IdentityEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.kafka.admin.auto-create=false",
        "spring.kafka.listener.auto-startup=false"
})
@AutoConfigureMockMvc
class IdentityApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @MockitoBean
    private IdentityEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        userProfileRepository.deleteAll();
        when(eventPublisher.publish(any(IdentityEvent.class)))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
    }

    @Test
    void registerUserValidatesRequestPersistsUserAndReturnsEvent() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/identity/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "mira.shah@example.com",
                                  "displayName": "Mira Shah",
                                  "department": "Security",
                                  "jobTitle": "IAM Analyst"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventType").value("USER_REGISTERED"))
                .andExpect(jsonPath("$.data.email").value("mira.shah@example.com"));

        assertThat(userProfileRepository.findByEmail("mira.shah@example.com"))
                .isPresent()
                .get()
                .satisfies(user -> assertThat(user.getDepartment()).isEqualTo("Security"));
    }
}
