import UIKit
import FirebaseCore
import FirebaseCrashlytics
import FirebaseAnalytics

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Configure Firebase
        FirebaseApp.configure()

        // Enable Crashlytics debug logging in debug builds
        // Note: Set to true temporarily if you need to test analytics in debug builds
        #if DEBUG
        let enableInDebug = false  // Change to true to test analytics locally
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(enableInDebug)
        Analytics.setAnalyticsCollectionEnabled(enableInDebug)
        #else
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
        Analytics.setAnalyticsCollectionEnabled(true)
        #endif

        return true
    }
}
