## QGJ-Main

QGJ-Main can fuzz a single component or a group of components registered in a device. QGJ supports fuzz injection of Activities and Services either on a wearable (Android Wear) or a mobile device (Android). The tool, which needs to be installed on two paired devices (mobile phone and wearable) consists of three main components:

 * **QGJ Mobile**, a Android application which runs on the mobile and offers a UI to interact with the fuzzer. The application allows the user to choose the target device: *mobile* or *wearable*.
 * **QGJ Wear**, a Android Wear application, which executes on the wearable. It communicates with the mobile app using the Android Wear *MessageAPI* and *DataApi*. The application receives the selected option on the UI (from QCJ Mobile) and executes the fuzz test using the `Fuzzer` library.
 * **Fuzzer library**, is a Java library, which contains the main functions needed to inject intens on the target device. The library is shared by *QGJ Mobile* and *QGJ Wear*.

### Operation

Once both applications are installed on their respective devices (mobile and wearable). First, set the fuzzer mode (either targetin Android or Android Wear).

```
    // Mode (0: Android Mobile, 1: Android Wear)
    private int mode=1;
```

Then, start the QGJ Mobile app.  

<table cellspacing="0" cellpadding="0">
    <tr>
        <td> <img height="600" src="./imgs/qgj-main.png" align="middle"> </td>
        <td> <img height="300" src="./imgs/qgj-wear.png" align="middle"> </td>
    </tr>
</table>

From the **QGJ Mobile app**, select the target app, the component type to fuzz and fuzz campaign to execute. Finally, either run a single experiment (just one campaign) or all the experiments (all campaigns). After the fuzz test is done, the log file (using `adb logcat`) need to be analyzed to find vulnerabilities.
