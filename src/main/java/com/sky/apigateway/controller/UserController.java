package com.sky.apigateway.controller;

import com.sky.apigateway.model.PriceAlarm;
import com.sky.apigateway.model.User;
import com.sky.apigateway.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);
            registeredUser.setPassword(null);
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public record LoginRequest(String email, String password) {}

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        Optional<User> userOptional = userService.loginUser(loginRequest.email(), loginRequest.password());
        if (userOptional.isPresent()) {
            return new ResponseEntity<>(userOptional.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Email or password incorrect.", HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/alarms")
    public ResponseEntity<PriceAlarm> createPriceAlarm(@RequestBody PriceAlarm priceAlarm) {
        PriceAlarm createdAlarm = userService.createPriceAlarm(priceAlarm);
        return new ResponseEntity<>(createdAlarm, HttpStatus.CREATED);
    }

    @GetMapping("/alarms/{userId}")
    public ResponseEntity<List<PriceAlarm>> getPriceAlarms(@PathVariable int userId) {
        List<PriceAlarm> alarms = userService.getPriceAlarmsByUserId(userId);
        return ResponseEntity.ok(alarms);
    }

    @DeleteMapping("/alarms/{id}")
    public ResponseEntity<?> deleteAlarm(@PathVariable Long id) {
        userService.deleteAlarm(id);
        return ResponseEntity.ok().build();
    }
}
