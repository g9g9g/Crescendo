package com.d102.crescendo.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Component
public class FirebaseConfig {

    @PostConstruct
    public void initializeFirebase() throws IOException {

        // ✅ 1️⃣ 우선순위: Kubernetes 환경의 Secret mount 경로
        String k8sPath = "/app/firebase/firebase-key.json";
        File file = new File(k8sPath);

        // ✅ 2️⃣ fallback: 로컬 개발용 리소스 경로
        if (!file.exists()) {
            file = new File("src/main/resources/firebase/serviceAccountKey.json");
        }

        try (FileInputStream serviceAccount = new FileInputStream(file)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            System.out.println("✅ Firebase initialized successfully!");
        }
    }
}
