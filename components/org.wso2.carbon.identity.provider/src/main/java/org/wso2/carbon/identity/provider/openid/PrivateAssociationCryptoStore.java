/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.provider.openid;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.association.Association;
import org.openid4java.association.AssociationException;
import org.openid4java.server.InMemoryServerAssociationStore;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;

/**
 * This is the custom AssociationStore that would read the encrypted association from the openid request
 */
public class PrivateAssociationCryptoStore extends InMemoryServerAssociationStore {

    private int storeId = 0;
    private int counter;
    private int expireIn;

    private String serverKey = "jscascasjcwt3276432yvdqwd";

    private static Log log = LogFactory.getLog(PrivateAssociationCryptoStore.class);

    public PrivateAssociationCryptoStore() {
        storeId = new SecureRandom().nextInt(9999);
        counter = 0;
        String serverKey = IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_PRIVATE_ASSOCIATION_SERVER_KEY);
        if(StringUtils.isNotBlank(serverKey)){
            this.serverKey = serverKey;
        }
    }

    @Override
    public Association load(String handle) {

        if(IdentityUtil.isBlank(handle)){
            throw new IllegalArgumentException("Handle is empty");
        }
        if(log.isDebugEnabled()){
            log.debug("Inside load(); handle : " + handle);
        }
        String timeStamp = handle.substring((Integer.toString(storeId)).length(), handle.indexOf("-"));
        Date expireDate = new Date(Long.parseLong(timeStamp)+ this.expireIn);
        if(log.isDebugEnabled()){
            log.debug("Calculated Expiry Time : " + expireDate.getTime());
        }
//        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
//        PBEKeySpec spec = new PBEKeySpec(serverKey.toCharArray(), handle.getBytes(), 1, 256);
//        SecretKey secretKey = factory.generateSecret(spec);

        return Association.createHmacSha256(handle, (serverKey + handle).getBytes(), expireDate);
    }


    @Override
    public Association generate(String type, int expiryIn) throws AssociationException {

        if(log.isDebugEnabled()){
            log.debug("Inside generate();  type : " + type + " expiryIn  : " + expiryIn);
        }

        long timestamp = new Date().getTime();
        if(log.isDebugEnabled()){
            log.debug("Current Time : " + timestamp);
        }
        // make time in to millisecond before it is set
        if(this.expireIn == 0){
            this.expireIn = expiryIn * 1000;
        }
        if(log.isDebugEnabled()){
            log.debug("Expires In : " + this.expireIn);
        }
        Date expireDate = new Date(timestamp + this.expireIn);
        if(log.isDebugEnabled()){
            log.debug("Expiry Time : " + expireDate.getTime());
        }

        String handle = Integer.toString(storeId) + Long.toString(timestamp) + "-" + Integer.toString(counter++);

        if(log.isDebugEnabled()){
            log.debug("Handle generated by crypto store : " + handle);
        }

//        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
//        PBEKeySpec spec = new PBEKeySpec(serverKey.toCharArray(), handle.getBytes(), 1, 256);
//        SecretKey secretKey = factory.generateSecret(spec);

        Association association = Association.createHmacSha256(handle, (serverKey + handle).getBytes(), expireDate);
        OpenIDServerManager.setThreadLocalAssociation(association);
        return association;
    }
}
