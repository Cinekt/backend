package com.jacobo.platform.iam.application.internal.outboundedservices.hashing;

public interface HashingService {
    String encode(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String encodedPassword);
}
