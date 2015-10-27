/*
 * RESTHeart - the data REST API server
 * Copyright (C) 2014 - 2015 SoftInstigate Srl
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.restheart.examples.security;

import com.google.common.collect.Sets;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
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
