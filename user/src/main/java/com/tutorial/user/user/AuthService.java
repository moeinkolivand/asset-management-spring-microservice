package com.tutorial.user.user;
import com.tutorial.shared.user.events.UserRegisteredEvent;
import com.tutorial.user.user.jwt.JwtService;
import jakarta.persistence.EntityExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;


    @Autowired
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager, KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate

    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new EntityExistsException("Username already exists");
        }

        User user = new User(
                request.phoneNumber(),
                passwordEncoder.encode(request.password()),
                UserRole.NORMAL
        );

        userRepository.save(user);
        publishUserRegisteredEvent(user);
        String token = jwtService.generateToken(user);
        return new AuthResponseDto(token, user.getPhoneNumber(), user.getUserRole().name());
    }

    public void publishUserRegisteredEvent(User user) {
        kafkaTemplate.send(
                "user-registered-topic",
                user.getId().toString(),
                new UserRegisteredEvent(user.getId(), user.getPhoneNumber(), user.getUserRole().name())
        );
    }

    public LoginResponseDto login(LoginRequestDto request) {
        Authentication authenticateUser = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.phoneNumber(), request.password())
        );

        User userDetails = (User) authenticateUser.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        return new LoginResponseDto(
                token
        );
    }

    public UserProfileDto userProfile(User currentUser) {
        return new UserProfileDto(currentUser.getFirstName(), currentUser.getLastName(), currentUser.getPhoneNumber());
    }

    public UserProfileDto updateUserProfile(User currentUser, UserProfileDto userProfileDto) {
        currentUser.setFirstName(userProfileDto.name());
        currentUser.setLastName(userProfileDto.lastName());
        currentUser.setPhoneNumber(userProfileDto.phoneNumber());
        User updateUser = userRepository.save(currentUser);
        return new UserProfileDto(updateUser.getFirstName(), updateUser.getLastName(), updateUser.getPhoneNumber());
    }
}