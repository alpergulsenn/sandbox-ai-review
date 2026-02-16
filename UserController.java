package com.abralabs.api.controller;

import com.abralabs.domain.model.User;
import com.abralabs.infrastructure.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // 💣 TRIGGER: Pillar 2 -> Layer Integrity -> Rule 7 (Layer Violation)
    // Controller sınıfı doğrudan Repository'i çağırıyor. Service katmanı bypass edilmiş.
    // Thread Safety hatası YOK (Repository stateless kabul edilir ve field final değil ama state tutmuyor).
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        // MİMARİ İHLALİ: İş mantığı veya veri erişimi Controller'da olmamalı.
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        // Validasyon logic'i Controller içine sızmış (Logic Leak)
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        
        // Doğrudan DB kaydı
        return userRepository.save(user);
    }
}
