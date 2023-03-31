import com.google.android.play.core.appupdate.AppUpdateManager

...

// ATENTIE! 
// Acest lucru merge DOAR daca distribuiti aplicatia pe Google Play Store ca Android App Bundle
// APK-urile nu suporta aceasta facilitate


class MainActivity : AppCompatActivity() {

    private var appUpdateManager: AppUpdateManager? = null

    ...

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateManager = AppUpdateManagerFactory.create(this@MainActivity)
        checkForUpdate(true)

        ...

    }

    ...

    /**
     * Check for update availability. If there will be an update available
     * will start the update process with the selected [Constants.UpdateMode].
     */
    private fun checkForUpdate(startUpdate: Boolean) {

        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager!!.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            inAppUpdateStatus.setAppUpdateInfo(appUpdateInfo)
            if (startUpdate) {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    // Request the update.
                    if (mode == UpdateMode.FLEXIBLE && appUpdateInfo.isUpdateTypeAllowed(
                            AppUpdateType.FLEXIBLE
                        )
                    ) {
                        // Start an update.
                        //Toast.makeText(MainActivity.this, "UpdateMode.FLEXIBLE", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "UpdateMode.FLEXIBLE")
                        startAppUpdateFlexible(appUpdateInfo)
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        // Start an update.
                        //Toast.makeText(MainActivity.this, "UpdateMode.IMMEDIATE", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "UpdateMode.IMMEDIATE")
                        startAppUpdateImmediate(appUpdateInfo)
                    }

                    //Toast.makeText(MainActivity.this, "Update available. Version Code: " + appUpdateInfo.availableVersionCode(), Toast.LENGTH_LONG).show();
                    Log.d(
                        TAG,
                        "Update available. Version Code: " + appUpdateInfo.availableVersionCode()
                    )
                } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE) {
                    //Toast.makeText(MainActivity.this, "No Update available. Code: " + appUpdateInfo.updateAvailability(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "No Update available. Code: " + appUpdateInfo.updateAvailability())
                }
            }
        }
    }

    private fun startAppUpdateImmediate(appUpdateInfo: AppUpdateInfo) {
        try {
            appUpdateManager!!.startUpdateFlowForResult(
                appUpdateInfo,
                AppUpdateType.IMMEDIATE,  // The current activity making the update request.
                this@MainActivity,  // Include a request code to later monitor this update request.
                UPDATE_REQUEST_CODE
            )
        } catch (e: SendIntentException) {
            //Toast.makeText(MainActivity.this, "error in startAppUpdateImmediate\n" + e.getMessage().toString(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "error in startAppUpdateImmediate", e)
        }
    }

    private fun startAppUpdateFlexible(appUpdateInfo: AppUpdateInfo) {
        try {
            appUpdateManager!!.startUpdateFlowForResult(
                appUpdateInfo,
                AppUpdateType.FLEXIBLE,  // The current activity making the update request.
                this@MainActivity,  // Include a request code to later monitor this update request.
                UPDATE_REQUEST_CODE
            )
        } catch (e: SendIntentException) {
            //Toast.makeText(MainActivity.this, "error in startAppUpdateFlexible\n" + e.getMessage().toString(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "error in startAppUpdateFlexible", e)
        }
    }
}

