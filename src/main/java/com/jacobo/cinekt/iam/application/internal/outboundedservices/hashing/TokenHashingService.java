package com.jacobo.cinekt.iam.application.internal.outboundedservices.hashing;

public interface TokenHashingService {
    String hash(String token);
}
