
## Main projects
MDK-ARM\STM32L073RZ-Nucleo

## Hard links or junctions to SDK
Create hard links or juntcions to the SDK using `mklink` command. Replace `%SDK_PATH%` with the appropiate path.

```
mklink /j sdk_drivers "%SDK_PATH%\Drivers"
mklink /j sdk_middlewares "%SDK_PATH%\Middlewares"
```

## Hardware Specification

* STM32 L073RZ-Nucleo-64 board (MB1136 C-04).[[Specs](http://www.st.com/en/evaluation-tools/nucleo-f401re.html)][[User Manual](http://www.st.com/content/ccc/resource/technical/document/user_manual/98/2e/fa/4b/e0/82/43/b7/DM00105823.pdf/files/DM00105823.pdf/jcr:content/translations/en.DM00105823.pdf)][[Databrief](http://www.st.com/content/ccc/resource/technical/document/data_brief/c8/3c/30/f7/d6/08/4a/26/DM00105918.pdf/files/DM00105918.pdf/jcr:content/translations/en.DM00105918.pdf)]
* SEMTECH SX1272/73 - 860 MHz to 1020 MHz Low Power Long Range Transceiver (LoRa transceiver). [[Datasheet](https://www.semtech.com/uploads/documents/sx1272.pdf)]


## Requirements

### Keil uVision

The Keil uVision IDE can be downloaded [here](http://www2.keil.com/mdk5/install/). This is development tool used to modify the source code of this project.

### SDK

The current version of the sdk could be downloaded from: [LoRa software expansion for STM32Cube](http://www.st.com/en/embedded-software/i-cube-lrwan.html). The LoRa software expansion version used for this project is 1.1.2 (9/8/2017) and can be found in the `archives` folder of this project.

### ST-Link Driver
The USB driver (STSW-LINK009) can be downloaded from [here](http://www.st.com/en/development-tools/stsw-link009.html).

## Setup

1. Clone this git repo.
2. Decompress the **SMT32 SDK** located on the `./archives` folder of this repo. Remember this path.
3. Create the junctions or simbolic to the SDK links using the `setup.bat` located on the root of this git repo. The script expects the path/to/SDK as a parameter (previous step).
4. Install Keil uVision IDE. After the installation is complete, follow the instructions from the Pack Installer to add all the supplementary software packs.
5. Install ST-Link Driver.

## Notes

For debugging purpose or convenience, you can use putty to connect to the SMT32 board. The configuration used for putty is:

| Param | Value |
| ----- | ----- |
| **Serial line**     | COM3^   |
| **Speed**           | 115200  |
| **Connection Type** | Serial  |

^ *Check the* `Device Manager` *for the correct port number on your installation*.
