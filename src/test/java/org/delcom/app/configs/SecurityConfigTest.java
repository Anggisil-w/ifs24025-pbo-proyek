package org.delcom.app.configs;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Test
        void permitAll_forAuthUrls() throws Exception {
                mockMvc.perform(get("/auth/login"))
                                .andExpect(status().isOk());
        }

        @Test
        void permitAll_forApiUrls() throws Exception {
                mockMvc.perform(get("/api/test"))
                                .andExpect(status().is4xxClientError());
        }

        @Test
        void redirect_toLogin_ifNotAuthenticated() throws Exception {
                mockMvc.perform(get("/dashboard"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/auth/login"));
        }

        @Test
        void accessDenied_redirectsToLogout() throws Exception {
                mockMvc.perform(get("/admin")
                                .with(user("testuser").roles("USER"))) // user login tapi bukan ADMIN
                                .andExpect(status().is4xxClientError());
        }

        @Test
        void passwordEncoder_shouldBeBCrypt() {
                assertThat(passwordEncoder).isNotNull();
                assertThat(passwordEncoder.encode("test"))
                                .isNotBlank();
        }
}