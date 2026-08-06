package com.jacobo.platform.iam.infrastructure.hashing.bcrypt;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.jacobo.platform.iam.application.internal.outboundedservices.hashing.HashingService;

public interface BCryptHashingService extends HashingService, PasswordEncoder {

}
