package com.sky.apigateway.service;

import com.sky.apigateway.model.Flight;
import com.sky.apigateway.model.PriceAlarm;
import com.sky.apigateway.model.User;
import com.sky.apigateway.repository.PriceAlarmRepository;
import com.sky.apigateway.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PriceAlarmRepository priceAlarmRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvailabilityService availabilityService;

    @Autowired
    public UserService(UserRepository userRepository, PriceAlarmRepository priceAlarmRepository, AvailabilityService availabilityService) {
        this.userRepository = userRepository;
        this.priceAlarmRepository = priceAlarmRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.availabilityService = availabilityService;
    }

    public User registerUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Bu e-posta adresi zaten kullanılıyor.");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Optional<User> loginUser(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                user.setPassword(null);
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public PriceAlarm createPriceAlarm(PriceAlarm priceAlarm) {
        userRepository.findById(priceAlarm.getUserId())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        List<Flight> flights = availabilityService.getAvailability(priceAlarm.getAvailabilityRequest());

        // Dönen uçuşlar içindeki en düşük fiyatı bul
        Optional<Double> minPriceOptional = flights.stream()
                .map(Flight::getMinFarePrice)
                .min(Double::compare);

        minPriceOptional.ifPresent(priceAlarm::setLastPrice);

        return priceAlarmRepository.save(priceAlarm);
    }

    public List<PriceAlarm> getPriceAlarmsByUserId(int userId) {
        return priceAlarmRepository.findByUserId(userId);
    }


    public void deleteAlarm(Long id) {
        priceAlarmRepository.deleteById(id);
    }
}
