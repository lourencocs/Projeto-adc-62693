package pt.unl.fct.di.apdc.firstwebapp.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;

import java.util.logging.Logger;

public class AuthUtil {

    private static final Logger LOG = Logger.getLogger(AuthUtil.class.getName());
    public static final String AUTH_TOKEN_KIND = "AuthToken"; // Kind name for storing tokens

    /**
     * Validates a token ID and retrieves the corresponding User entity if valid.
     * Checks for existence and expiration.
     *
     * @param datastore The Datastore service instance.
     * @param tokenID The token ID string to validate.
     * @return The User Entity if the token is valid, null otherwise.
     */
    public static Entity validateToken(Datastore datastore, String tokenID) {
        if (tokenID == null || tokenID.isEmpty()) {
            LOG.fine("Token validation attempt with null or empty tokenID.");
            return null;
        }

        Key tokenKey = datastore.newKeyFactory().setKind(AUTH_TOKEN_KIND).newKey(tokenID);
        Entity tokenEntity = datastore.get(tokenKey); // Non-transactional read is fine here

        if (tokenEntity == null) {
            LOG.warning("Token validation failed: Token not found - " + tokenID);
            return null; // Token doesn't exist
        }

        long expirationDate = tokenEntity.getLong("expiration_date"); // Ensure this field name is used when storing
        if (expirationDate < System.currentTimeMillis()) {
            LOG.warning("Token validation failed: Token expired - " + tokenID);
            return null; // Token expired
        }

        // Token is valid, get the associated username
        String username = tokenEntity.getString("user_username"); // Ensure this field name is used
        if (username == null || username.isEmpty()) {
            LOG.severe("Token validation error: Valid token " + tokenID + " has no associated username.");
            return null; // Data integrity issue
        }

        // Retrieve the User entity
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
        Entity userEntity = datastore.get(userKey); // Non-transactional read

        if (userEntity == null) {
            LOG.severe("Token validation error: User " + username + " associated with valid token " + tokenID + " not found.");
            // Perhaps the user was deleted while the token was active?
            return null; // User not found
        }

        LOG.fine("Token validated successfully for user: " + username);
        // Optionally attach token details to the user entity or return a wrapper object
        // For simplicity, just returning the user entity. The caller knows the role from this entity.
        return userEntity;
    }

    /**
     * Extracts the token ID from an Authorization header (e.g., "Bearer <tokenID>").
     * @param authHeader The content of the Authorization header.
     * @return The token ID string, or null if the header is missing or malformed.
     */
    public static String extractTokenID(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7).trim(); // "Bearer ".length() == 7
    }
}
