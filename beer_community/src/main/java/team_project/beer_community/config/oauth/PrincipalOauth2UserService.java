package team_project.beer_community.config.oauth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import team_project.beer_community.config.auth.PrincipalDetails;
import team_project.beer_community.domain.User;
import team_project.beer_community.repository.UserRepository;

import java.util.Map;

@Service
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    // 함수종료시 @AuthenticationPrincipal 어노테이션이 만들어짐
    @Override // 구글소셜로그인 후 구글로 부터 받은 userRequest 데이터에 대한 후처리되는 함수
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("userRequest = " + userRequest);
        // org.springframwork.security.oauth2.client.userinfo.OAuth2UserRequest@4e6edb55
        System.out.println("getClientRegistration() = " + userRequest.getClientRegistration());
        // registrationId로 어떤 OAuth로 로그인 했는지 확인가능(ex. google, naver)
        System.out.println("getAccessToken = " + userRequest.getAccessToken().getTokenValue());
        // 구글로그인 버튼클릭 -> 구글로그인 창 -> 로그인을 완료 -> code를 return(OAuth-Client라이브러리) -> code를 사용해서 AccessToken을 요청해서 받는다.
        // 여기까지가 userRequest정보이다. -> loadUser() 함수호출 -> 구글로부터 회원프로필 얻을 수 있다.(ex. email, family_name 등등)

        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println("oAuth2User.getAttributes() = " + oAuth2User.getAttributes());
        // {sub=101301106118139334837, name=고경환, given_name=경환, family_name=고, picture=https://lh3.googleusercontent.com/a-/AFdZucqfqgcr-H-cRolGyJETVNk, email=gkw1207@likelion.org, email_verified=true, locale=en, hd=likelion.org}
        // **회원가입할때 저장될 정보** => username: "google_101301106118139334837", password: "암호화(get in there)", email: "gkw1207@likelion.org", role: "ROLE_USER"

        String provider = userRequest.getClientRegistration().getClientId(); // google
        String providerId = oAuth2User.getAttribute("sub"); // sub키에 저장된 값은 google에서 사용자에게 부여한 pk이다
        String username = oAuth2User.getAttribute("name");
        String password = bCryptPasswordEncoder.encode("password") ; // 소셜로그인이기 때문에 굳이 저장안해도되지만 임의로 생성해서 저장함
        String email = oAuth2User.getAttribute("email");
        String role = "ROLE_USER";
        System.out.println("PrincipalOauth@UserService.java/username = " + username);
        System.out.println("PrincipalOauth@UserService.java/getAttributes() = " + oAuth2User.getAttributes());
        User userEntity = userRepository.findByUsername(username);
        System.out.println("userEntity = " + userEntity);
        if(userEntity == null){
            // User에 생성자를 통해 새로운 User를 생성시킴(회원가입)
            System.out.println("PrincipalOauth2UserService.loadUser/처음 로그인하는군요 회원가입 진행하겠습니다");
            userEntity = User.builder()
                    .username(username)
                    .password(password)
                    .email(email)
                    .role(role)
                    .provider(provider)
                    .providerId(providerId)
                    .birthday(null)
                    .imageUrl(null)
                    .build();
            userRepository.save(userEntity);
        } else{
            System.out.println("PrincipalOauth2UserService.loadUser/회원가입이 이미 되어있습니다.");
        }
        // 회원가입이 이미 되어있다면 그냥 앞서받은 userEntity사용해도 됨
        return new PrincipalDetails(userEntity, oAuth2User.getAttributes()); // Authentication에 저장된다.
    }
}