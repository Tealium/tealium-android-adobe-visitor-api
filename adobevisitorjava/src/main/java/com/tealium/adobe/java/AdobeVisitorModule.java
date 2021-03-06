package com.tealium.adobe.java;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tealium.adobe.api.AdobeAuthState;
import com.tealium.adobe.api.AdobeExperienceCloudIdService;
import com.tealium.adobe.api.AdobeVisitor;
import com.tealium.adobe.api.AdobeVisitorAPI;
import com.tealium.adobe.api.ResponseListener;
import com.tealium.internal.data.Dispatch;
import com.tealium.internal.listeners.PopulateDispatchListener;
import com.tealium.library.DataSources;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The {@link AdobeVisitorModule} provides methods to request new, and link existing, Adobe
 * Experience Cloud Id's with the Tealium SDK.
 * <p>
 * The ECID will be added during the Dispatch population phase - population could be delayed on
 * first launch if no known ECID is provided, and requests are required to fetch a new ECID.
 */
public final class AdobeVisitorModule implements PopulateDispatchListener {

    private static AdobeVisitorModule INSTANCE = null;
    private static final String ADOBE_VISITOR_SHARED_PREFS_NAME = "tealium.adobevisitor";
    private static final String ADOBE_VISITOR_KEY = "adobe_ecid";

    private final String mAdobeOrgId;
    private final AdobeExperienceCloudIdService mVisitorApi;
    private final SharedPreferences mSharedPreferences;
    private final int mMaxRetries;
    private final Long mRequestTimeoutMs = 1000L;

    private volatile CountDownLatch mRetryLatch = null;
    private volatile AdobeVisitor mVisitor;

    AdobeVisitorModule(@NonNull String adobeOrgId,
                       @NonNull AdobeExperienceCloudIdService visitorApi,
                       @NonNull SharedPreferences sharedPreferences,
                       int maxRetries,
                       @Nullable String initialEcid,
                       @Nullable String dataProviderId,
                       @Nullable @AdobeAuthState Integer authState,
                       @Nullable String customVisitorId) {
        // private
        mAdobeOrgId = adobeOrgId;
        mMaxRetries = maxRetries;

        // Load current Visitor
        mSharedPreferences = sharedPreferences;
        mVisitorApi = visitorApi;
        mVisitor = AdobeVisitor.fromSharedPreferences(sharedPreferences);
        if (initialEcid != null && !initialEcid.equals("")) {
            setVisitor(new AdobeVisitor(initialEcid, -1, 0, ""));
        }

        if (mVisitor != null) {
            if (dataProviderId != null &&
                    customVisitorId != null) {
                visitorApi.linkEcidToKnownIdentifier(
                        customVisitorId,
                        mVisitor.getExperienceCloudId(),
                        dataProviderId,
                        authState,
                        new AdobeVisitorUpdater(null)
                );
            } else {
                syncVisitor();
            }
        } else {
            fetchInitialVisitor(customVisitorId, dataProviderId, authState);
        }
    }

    /**
     * Sets up the AdobeVisitorModule instance.
     *
     * @param context    Android context
     * @param adobeOrgId Adobe OrgId for your organization
     * @return Singleton instance of AdobeVisitorModule
     */
    public static AdobeVisitorModule setUp(@NonNull Context context,
                                           @NonNull String adobeOrgId
    ) {
        return setUp(context, adobeOrgId, 5, null, null, null, null);
    }

    /**
     * Sets up the AdobeVisitorModule instance.
     *
     * @param context         Android context
     * @param adobeOrgId      Adobe OrgId for your organization
     * @param maxRetries      Maximum number of attempts to make when auto fetching a new Ecid on
     *                        app launch.
     *                        Values of 0 or less will not auto-fetch a new ECID.
     *                        Values of greater than 0 will attempt that many times before releasing
     *                        the events without an ecid (unless the initialEcid is also set)
     * @param existingEcid    Sets the initial (known) ECID - this will be refreshed automatically,
     *                        but subsequent launches should load from disk.
     * @param dataProviderId  (Optional) Sets the visitor's dataProviderId if known.
     * @param authState       (Optional) Sets the visitor's authState if known.
     * @param customVisitorId (Optional) Sets a known Visitor Id if already known e.g. email
     * @return Singleton instance of AdobeVisitorModule
     */
    public static AdobeVisitorModule setUp(@NonNull Context context,
                                           @NonNull String adobeOrgId,
                                           int maxRetries,
                                           @Nullable String existingEcid,
                                           @Nullable String dataProviderId,
                                           @Nullable @AdobeAuthState Integer authState,
                                           @Nullable String customVisitorId) {
        if (INSTANCE == null) {
            synchronized (AdobeVisitorModule.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AdobeVisitorModule(adobeOrgId,
                            new AdobeVisitorAPI(context, adobeOrgId),
                            context.getSharedPreferences(ADOBE_VISITOR_SHARED_PREFS_NAME, Context.MODE_PRIVATE),
                            maxRetries,
                            existingEcid,
                            dataProviderId,
                            authState,
                            customVisitorId);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Returns the singleton instance of the AdobeVisitorModule
     *
     * @return Singleton instance of AdobeVisitorModule, or null; see {@link #setUp(Context, String)}
     */
    @Nullable
    public static AdobeVisitorModule getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the configured Adobe OrgId
     *
     * @return Adobe Org Id
     */
    public String getAdobeOrgId() {
        return mAdobeOrgId;
    }

    @Nullable
    public AdobeVisitor getVisitor() {
        return mVisitor;
    }

    /**
     * Updates the in-memory visitor and saves to disk.
     * Adds the registered Tealium instances' persistent data sources.
     *
     * @param visitor
     */
    void setVisitor(AdobeVisitor visitor) {
        mVisitor = visitor;
        AdobeVisitor.toSharedPreferences(mSharedPreferences, visitor);
    }

    private void fetchInitialVisitor(String customVisitorId, String dataProviderId, Integer authState) {
        mRetryLatch = new CountDownLatch(mMaxRetries);
        try {
            for (int i = 0; i < mMaxRetries; i++) {
                Log.d(BuildConfig.TAG, "Fetching ECID attempt (" + i + ")");
                CountDownLatch requestLatch = new CountDownLatch(1);
                ResponseListener<AdobeVisitor> listener = new AdobeVisitorUpdater(
                        new LatchedVisitorUpdater(requestLatch));

                // use given custom id and data provider if available
                if (customVisitorId != null &&
                        dataProviderId != null) {
                    mVisitorApi.requestNewEcidAndLink(customVisitorId, dataProviderId, authState, listener);
                } else {
                    mVisitorApi.requestNewAdobeEcid(listener);
                }
                requestLatch.await(mRequestTimeoutMs, TimeUnit.MILLISECONDS);

                mRetryLatch.countDown();
                if (getVisitor() != null) {
                    // Successful; abort the latch.
                    while (mRetryLatch.getCount() > 0) {
                        mRetryLatch.countDown();
                    }
                    return;
                }
            }
        } catch (InterruptedException iex) {
            mRetryLatch.countDown();
            Log.d(BuildConfig.TAG, "Fetch Ecid interrupted ");
        }
    }

    private void syncVisitor() {
        if (mVisitor != null && (mVisitor.getNextRefresh().compareTo(new Date()) < 0)) {
            mVisitorApi.refreshExistingAdobeEcid(mVisitor.getExperienceCloudId(), new AdobeVisitorUpdater(null));
        }
    }

    @Override
    public void onPopulateDispatch(Dispatch dispatch) {
        if (dispatch.containsKey(ADOBE_VISITOR_KEY)) return;

        if (mVisitor != null) {
            dispatch.put(ADOBE_VISITOR_KEY, mVisitor.getExperienceCloudId());
            return;
        }

        if (mRetryLatch == null) {
            fetchInitialVisitor(null, null, null);
        }
        try {
            // Wait for retries to be completed - max timeout time + buffer
            mRetryLatch.await(mRequestTimeoutMs * (mMaxRetries + 1), TimeUnit.MILLISECONDS);
        } catch (InterruptedException iex) {
            mRetryLatch.countDown();
        }
        if (mVisitor != null) {
            dispatch.put(ADOBE_VISITOR_KEY, mVisitor.getExperienceCloudId());
        }
    }

    /**
     * Removes all stored data and clears the current available from {@link #getVisitor()}.
     * The "adobe_ecid" key will also be removed from {@link DataSources#getPersistentDataSources()}
     * on each registered instance name.
     */
    public void resetVisitor() {
        mVisitor = null;
        mRetryLatch = null;
        mSharedPreferences.edit().clear().apply();
    }

    public void linkEcidToKnownIdentifier(@NonNull String knownId, @NonNull String adobeDataProviderId, Integer authState, ResponseListener<AdobeVisitor> adobeResponseListener) {
        if (mVisitor != null) {
            mVisitorApi.linkEcidToKnownIdentifier(knownId, mVisitor.getExperienceCloudId(), adobeDataProviderId, authState, new AdobeVisitorUpdater(adobeResponseListener));
        }
    }

    private class AdobeVisitorUpdater implements ResponseListener<AdobeVisitor> {

        private final ResponseListener<AdobeVisitor> mOtherListener;

        AdobeVisitorUpdater(ResponseListener<AdobeVisitor> other) {
            mOtherListener = other;
        }

        @Override
        public void success(AdobeVisitor data) {
            setVisitor(data);

            if (mOtherListener != null) {
                mOtherListener.success(data);
            }
        }

        @Override
        public void failure(int errorCode, Exception ex) {
            if (mOtherListener != null) {
                mOtherListener.failure(errorCode, ex);
            }
        }
    }

    private static class LatchedVisitorUpdater implements ResponseListener<AdobeVisitor> {
        private final CountDownLatch mLatch;

        LatchedVisitorUpdater(CountDownLatch latch) {
            mLatch = latch;
        }

        @Override
        public void success(AdobeVisitor data) {
            while (mLatch.getCount() > 0)
                mLatch.countDown();
        }

        @Override
        public void failure(int errorCode, @Nullable Exception ex) {
            mLatch.countDown();
        }
    }
}
