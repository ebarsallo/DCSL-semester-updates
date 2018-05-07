package edu.purdue.android.fuzzer.squibble.common;

/**
 * @author ebarsallo
 */

public class ExperimentConstants {

    static String TARGET_APP = "com.google.android.wearable.app";

    static String TARGET_BUILTIN [] = {
            "com.android.providers.telephony",
            "com.android.externalstorage",
            "com.android.mms.service",
            "com.android.defcontainer",
            "com.android.pacprocessor",
            "com.android.carrierconfig",
            "com.android.providers.settings",
            "com.android.sharedstoragebackup",
            "com.android.sdm.plugins.diagmon",
            "com.android.dreams.basic",
            "com.android.inputdevices",
            "com.android.calllogbackup",
            "com.android.proxyhandler",
            "com.android.sdm.plugins.dcmo",
            "com.android.providers.partnerbookmarks",
            "com.android.bookmarkprovider",
            "com.android.wallpaperbackup",
            "com.android.providers.blockednumber",
            "com.android.providers.userdictionary",
            "com.android.location.fused",
            "com.android.bluetoothmidiservice",
            "com.android.statementservice",
            "com.android.sdm.plugins.connmo",
            "com.android.providers.media",
            "com.android.wallpapercropper",
            "com.android.htmlviewer",
            "com.android.providers.downloads",
            "com.android.providers.downloads.ui",
            "com.android.hotwordenrollment",
            "com.android.retaildemo",
            "com.android.keychain",
            "com.android.cts.ctsshim",
            "com.android.shell",
            "com.android.providers.contacts",
            "com.android.captiveportallogin",
            "com.android.mtp",
            "com.android.backupconfirm"
    };

    static String TOP_APPS_NON_BUILTIN [] = {
            "com.facebook.orca",
            "com.whatsapp",
            "com.instagram.android",
            "com.snapchat.android",
            "com.google.android.apps.books",
            "com.google.android.apps.magazines",
            "com.sec.spp.push",
            "com.google.android.street",
            "com.skype.raider"
    };


    static String PREFIX_HEALTH_APPS [] = {
            "com.dungelin.heartrate",
            "com.fitnesskeeper.runkeeper.pro",
            "com.google.android.apps.fitness",
            "com.jwork.wearable.heartratesync2",
            "com.motorola.omni",
            "com.runtastic.android",
            "com.sonymobile.lifelog",
            "com.strava",
            "se.perigee.android.seven"
    };

    static String PREFIX_ANDROID_WEAR_APPS [] = {
            "ch.publisheria.bring",
            "com.accuweather.android",
            "com.aita",
            "com.augmentra.viewranger.android",
            "com.citymapper.app.release",
            "com.clearchannel.iheartradio.controller",
            "com.glidetalk.glideapp",
            "com.google.android.wearable.app",
            "com.hole19golf.hole19.beta",
            "com.hotellook",
            "com.joelapenna.foursquared",
            "com.mobilefootie.wc2010",
            "com.northpark.drinkwater",
            "com.shazam.android",
            "com.strava",
            "com.weather.Weather",
            "se.perigee.android.seven"
    };

    static String PREFIX_ANDROID_WEAR_2_APPS [] = {
            "ch.publisheria.bring",
            "com.accuweather.android",
            "com.fitnesskeeper.runkeeper.pro",
            "com.glidetalk.glideapp",
            "com.google.android.apps.fitness",
            "com.joelapenna.foursquared",
            "com.strava",
            "com.weather.Weather",
            "se.perigee.android.seven"
    };

}
