package com.jacobo.cinekt.iam.infrastructure.hashing.bcrypt;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.jacobo.cinekt.iam.application.internal.outboundedservices.hashing.HashingService;

public interface BCryptHashingService extends HashingService, PasswordEncoder {

}
