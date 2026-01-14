package com.gwl.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;

@Configuration

public class FirebaseConfig {
    @Value("${firebase.service-account}")
    String firebaseServiceAccountPath;

    @PostConstruct
    public void init() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        //InputStream serviceAccount = new ClassPathResource(firebaseServiceAccountPath).getInputStream();
        InputStream serviceAccount = new FileInputStream(firebaseServiceAccountPath);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
    }
}
