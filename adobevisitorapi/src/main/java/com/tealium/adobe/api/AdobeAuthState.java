package com.tealium.adobe.api;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef(value = {AdobeAuthState.AUTH_STATE_UNKNOWN, AdobeAuthState.AUTH_STATE_AUTHENTICATED, AdobeAuthState.AUTH_STATE_LOGGED_OUT})
@Retention(RetentionPolicy.SOURCE)
public @interface AdobeAuthState {
    public static int AUTH_STATE_UNKNOWN = 0;
    public static int AUTH_STATE_AUTHENTICATED = 1;
    public static int AUTH_STATE_LOGGED_OUT = 2;
}