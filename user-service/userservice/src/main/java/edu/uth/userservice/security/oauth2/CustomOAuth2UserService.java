package edu.uth.userservice.security.oauth2;

import edu.uth.userservice.service.UserService;
import edu.uth.userservice.service.RoleService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserService userService;
    private final RoleService roleService;

    public CustomOAuth2UserService(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        logger.info("OAuth2 loadUser attributes: {}", attributes);

        // process/create local user record (key by email)
        try {
            // Gọi hàm xử lý user cục bộ
            userService.processOAuthPostLogin(attributes, userRequest.getClientRegistration().getRegistrationId());
        } catch (Exception ex) {
            logger.warn("Error during processOAuthPostLogin: {}", ex.getMessage(), ex);
        }

        return oauth2User;
    }
}