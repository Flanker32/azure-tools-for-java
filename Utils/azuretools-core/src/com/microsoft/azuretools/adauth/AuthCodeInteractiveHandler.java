/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.UnsupportedEncodingException;

class AuthCodeInteractiveHandler {
    private static final  Logger log = Logger.getLogger(AuthCodeInteractiveHandler.class.getName());
    private static final String LOGIN = "login";
    private static final String ERROR_NOWEBUI = "webUi not set";
    private static final String ERROR_GETCODE = "Service principal sign in is unsuccessful or canceled";

    private final URI redirectUri;
    private final String clientId;
    private final IWebUi webUi;
    private final String resource;
    private final AuthenticationAuthority authenticator;
    private final String userDisplayableId;

    AuthCodeInteractiveHandler(@NotNull final AuthenticationAuthority authenticator, @NotNull String clientId,
            @NotNull IWebUi webUi, @NotNull final String redirectUri, @NotNull final String resource,
            final String userDisplayableId) throws Exception {
        this.authenticator = authenticator;
        this.clientId = clientId;
        try {
            this.redirectUri = new URI(redirectUri);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        if (!StringUtils.isNullOrEmpty(this.redirectUri.getFragment())) {
            throw new IllegalArgumentException("redirectUri: " + AuthErrorMessage.RedirectUriContainsFragment);
        }
        this.webUi = webUi;
        this.resource = resource;
        this.userDisplayableId = userDisplayableId;
        log.log(Level.FINEST, String.format(
                "\n=== AcquireTokenInteractiveHandler params:"
                        + "\n\tresource: %s\n\twebUi: %s\n\tdisplabableId: %s\n\tauthority: %s",
                this.resource,
                this.webUi.getClass().getName(),
                this.userDisplayableId,
                this.authenticator.getAuthority()));
    }

    String acquireAuthCode(UUID correlationId) throws AuthException {
        if (null == webUi) {
            throw new AuthException(ERROR_NOWEBUI);
        }

        String resultUri = null;
        try {
            log.log(Level.FINEST, "acquireAuthorization...");
            URI authorizationUri = this.createAuthorizationUri(correlationId);
            log.log(Level.FINEST, "Starting web ui...");
            resultUri = webUi.authenticate(authorizationUri, redirectUri);
        } catch (UnsupportedEncodingException | URISyntaxException ex) {
            log.log(Level.SEVERE, "acquireAuthorization@AcquireTokenInteractiveHandler: " + ex);
            throw new AuthException(ex.getMessage(), ex);
        }

        if (resultUri == null) {
            log.log(Level.SEVERE, ERROR_GETCODE);
            throw new AuthException(ERROR_GETCODE);
        }
        return resultUri;
    }

    private URI createAuthorizationUri(UUID correlationId) throws UnsupportedEncodingException, URISyntaxException {
        Map<String, String> requestParameters = this.createAuthorizationRequest(correlationId);
        return new URI(this.authenticator.getAuthorizationEndpoint() + "?" + UriUtils.toQueryString(requestParameters));
    }

    private Map<String, String> createAuthorizationRequest(UUID correlationId) {
        Map<String, String> authorizationRequestParameters = new HashMap<>();
        authorizationRequestParameters.put(OAuthParameter.Resource, this.resource);
        authorizationRequestParameters.put(OAuthParameter.ClientId, this.clientId);
        authorizationRequestParameters.put(OAuthParameter.ResponseType, OAuthResponseType.Code);
        authorizationRequestParameters.put(OAuthParameter.RedirectUri, redirectUri.toString());
        if (!StringUtils.isNullOrEmpty(userDisplayableId)) {
            authorizationRequestParameters.put(OAuthParameter.LoginHint, userDisplayableId);
        }

        if (correlationId != null) {
            authorizationRequestParameters.put(OAuthParameter.CorrelationId, correlationId.toString());
            authorizationRequestParameters.put(OAuthHeader.RequestCorrelationIdInResponse, "true");
        }

        authorizationRequestParameters.put(OAuthParameter.Prompt, LOGIN);

        return authorizationRequestParameters;
    }
}
