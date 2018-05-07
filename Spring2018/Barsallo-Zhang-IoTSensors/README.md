## SMART/WHIN IoT Project

### Description

SMART/WHIN IoT sensor project. Prototype of Mesh network implementation used for the DEMO on May, 1st 2018. This folder includes project source code for:
 * nRF52 board
 * STM32 board

Each project includes the following features:
 * Dynamic join of nodes (boundary and router nodes) to the mesh network. 
 * Read measurements from sensors (nitrate & temperature) and forward them to the Gateway.
 * Scripts that collects readings from the boundary nodes and upload them to the Cloud Server.

Specific instruction for setup and run each project are in their respective subfolders.

### Future Work

 * Implement dynamic failures/leaves of nodes.
 * Scale the dynamic mesh network to more nodes (currently has been tested with 4 nodes).
 * Tweaks to the communication protocol.
 * Battery optimization.

### DEMO

The mesh network used for the demo followed an static topology as follows:

<img height="300" src="./imgs/topology.png" align="middle">

Note. The STM boards are represented in orange.

### Colaborators

 * Edgardo Barsallo Yi
 * Heng Zhang
 * Xiaofan Jiang