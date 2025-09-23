package io.beanvortex.bitkip.models;

import io.beanvortex.bitkip.utils.CredentialEncryptor;

import java.util.Base64;

public record Credentials(String username, String password) {

    public boolean isOk() {
        return username != null && password != null && !username.isBlank() && !password.isBlank();
    }

    public String base64Encoded() {
        return Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    public String encrypt() {
        return CredentialEncryptor.encrypt(username + ":" + password);
    }

    public static Credentials decrypt(String encryptedStr) {
        try {
            var decrypt = CredentialEncryptor.decrypt(encryptedStr).split(":");
            return new Credentials(decrypt[0], decrypt[1]);
        } catch (Exception ignore) {
        }
        return new Credentials(null, null);
    }
}
