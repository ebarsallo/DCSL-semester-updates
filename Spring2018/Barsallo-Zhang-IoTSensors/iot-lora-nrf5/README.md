
## Main project
pca10040\blank\arm5_no_packs\spi_pca10040.uvprojx

## Hard links or junctions
Create hard links or juntcions to the SDK using `mklink` command. Replace `%SDK_PATH%` with the appropiate path.

```
mklink /j sdk_components "%SDK_PATH%\components"
mklink /j sdk_external "%SDK_PATH%\external"
mklink /j sdk_svd "%SDK_PATH%\svd"
```

## Hardware Specification

* nRF52 DK (Nordic nRF52832 SoC). [[Dev Kit](https://www.nordicsemi.com/eng/Products/Bluetooth-low-energy/nRF52-DK)][[Guide](https://www.nordicsemi.com/eng/Products/Getting-started-with-the-nRF52-Development-Kit)]
* SEMTECH SX1272/73 - 860 MHz to 1020 MHz Low Power Long Range Transceiver (LoRa transceiver). [[Datasheet](https://www.semtech.com/uploads/documents/sx1272.pdf)]

## Requirements

### Keil uVision

The Keil uVision IDE can be downloaded [here](http://www2.keil.com/mdk5/install/). This is development tool used to modify the source code of this project. The profesional version is recommended since the project superpass the code limit restriction.

### SDK

The current version of the sdk is 14.1.0.1dda907 and can be downloaded from Nordic website: [nRF5_SDK_14.1.0_1dda907.zip](https://developer.nordicsemi.com/nRF5_SDK/nRF5_SDK_v14.x.x/nRF5_SDK_14.1.0_1dda907.zip). This zip can also be found on the `archives` folder.

Further documentation can be found here:
* [Nordic nRF5 SDK](https://developer.nordicsemi.com/nRF5_SDK/).
* [Nordic nRF5 SDK Documentation](http://infocenter.nordicsemi.com/index.jsp?topic=%2Fcom.nordic.infocenter.sdk5.v14.2.0%2Findex.html).

## Setup

1. Clone this git repo.
2. Decompress the **SDK** and remember the path.
3. Create the junctions or simbolic to the SDK links using the `setup.bat` located on the root of this git repo. The script expects the path/to/SDK as a parameter (previous step).
4. Install Keil uVision IDE. After the installation is complete, follow the instructions from the Pack Installer to add all the supplementary software packs.
5. Install ST-Link Driver.

## Notes

For debugging purpose or convenience, you can use putty to connect to the nRF52 board. The configuration used for putty is:

| Param | Value |
| ----- | ----- |
| **Serial line**     | COM3^   |
| **Speed**           | 115200  |
| **Connection Type** | Serial  |

^ *Check the* `Device Manager` *for the correct port number on your installation*.
