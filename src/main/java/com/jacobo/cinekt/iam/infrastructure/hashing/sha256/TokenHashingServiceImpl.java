package com.jacobo.cinekt.iam.infrastructure.hashing.sha256;

import com.jacobo.cinekt.iam.application.internal.outboundedservices.hashing.TokenHashingService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class TokenHashingServiceImpl implements TokenHashingService {
    @Override
    public String hash(String token){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hexString = new StringBuilder();

            for(byte b : hash){
                String hex = Integer.toHexString(0xff & b);

                if(hex.length() == 1){
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();
        } catch(NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256 algoritm not available", e);
        }
    }
}
