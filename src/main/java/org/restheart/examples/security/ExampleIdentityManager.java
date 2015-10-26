/*
 * Copyright SoftInstigate srl. All Rights Reserved.
 *
 *
 * The copyright to the computer program(s) herein is the property of
 * SoftInstigate srl, Italy. The program(s) may be used and/or copied only
 * with the written permission of SoftInstigate srl or in accordance with the
 * terms and conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied. This copyright notice must not be removed.
 */
package org.restheart.examples.security;

import com.google.common.collect.Sets;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.restheart.security.impl.SimpleAccount;

/**
 * ExampleIdentityManager verifies the password to be the flipped id string,
 * i.e. id="username" => pwd="emanresu"
 *
 * @author Andrea Di Cesare <andrea@softinstigate.com>
 */
public class ExampleIdentityManager implements IdentityManager {
    public enum ROLE {
        ADMIN, USER
    };

    public ExampleIdentityManager(Map<String, Object> arguments) {
        // args are ignored
    }

    public static final String ADMIN_ID = "admin";

    private static final Set<String> NORMAL_USER_ROLES;
    private static final Set<String> ADMIN_ROLES;

    static {
        NORMAL_USER_ROLES = Sets.newHashSet(ROLE.USER.toString());
        ADMIN_ROLES = Sets.newHashSet(ROLE.ADMIN.toString());
    }

    @Override
    public Account verify(String id, Credential credential) {
        if (credential instanceof PasswordCredential) {
            char[] password = ((PasswordCredential) credential).getPassword();

            String flippedPassword = new StringBuilder(new String(password))
                    .reverse()
                    .toString();

            if (id.equals(flippedPassword)) {
                if (id.equals(ADMIN_ID)) {
                    return new SimpleAccount(id, password, ADMIN_ROLES);
                } else {
                    return new SimpleAccount(id, password, NORMAL_USER_ROLES);
                }
            }
        }

        return null;
    }

    @Override
    public Account verify(Account account
    ) {
        return account;
    }

    @Override
    public Account verify(Credential credential
    ) {
        // Auto-generated method stub
        return null;
    }
}
