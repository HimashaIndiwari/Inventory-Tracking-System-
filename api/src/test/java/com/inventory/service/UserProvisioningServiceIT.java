package com.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.inventory.security.VerifiedToken;
import com.inventory.support.DataWritingIntegrationSupport;

/** Data-writing (gate G3). */
class UserProvisioningServiceIT extends DataWritingIntegrationSupport {

    @Autowired
    private UserProvisioningService provisioning;

    @Test
    void firstResolveInsertsRowSecondReusesIt() {
        VerifiedToken token = new VerifiedToken("it-uid-1", "it@example.com", true, "IT User");

        Long first = provisioning.resolve(token).userId();
        Long second = provisioning.resolve(token).userId();

        assertThat(second).isEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class)).isEqualTo(1);
    }
}
