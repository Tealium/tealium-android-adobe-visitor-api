package com.tealium.adobe.api;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef(value = {ErrorCode.ERROR_NOT_CONNECTED, ErrorCode.ERROR_INVALID_RESPONSE, ErrorCode.ERROR_INVALID_VISITOR_JSON, ErrorCode.ERROR_FAILED_REQUEST})
@Retention(RetentionPolicy.SOURCE)
public @interface ErrorCode {
    public static int ERROR_NOT_CONNECTED = 0;
    public static int ERROR_INVALID_RESPONSE = 1;
    public static int ERROR_INVALID_VISITOR_JSON = 2;
    public static int ERROR_FAILED_REQUEST = 3;

}