/**
 * Copyright (c) 2015 - 2017, Nordic Semiconductor ASA
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form, except as embedded into a Nordic
 *    Semiconductor ASA integrated circuit in a product or a software update for
 *    such product, must reproduce the above copyright notice, this list of
 *    conditions and the following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 * 
 * 3. Neither the name of Nordic Semiconductor ASA nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 * 
 * 4. This software, with or without modification, must only be used with a
 *    Nordic Semiconductor ASA integrated circuit.
 * 
 * 5. Any software provided in binary form under this license must not be reverse
 *    engineered, decompiled, modified and/or disassembled.
 * 
 * THIS SOFTWARE IS PROVIDED BY NORDIC SEMICONDUCTOR ASA "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, NONINFRINGEMENT, AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NORDIC SEMICONDUCTOR ASA OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
#include "nrf_drv_spi.h"
#include "app_util_platform.h"
#include "nrf_gpio.h"
#include "nrf_delay.h"
#include "boards.h"
#include "app_error.h"
#include <string.h>
#include "nrf_log.h"
#include "nrf_log_ctrl.h"
#include "nrf_log_default_backends.h"
#include "sx1272.h"
#include "radio.h"
#include "nrf_temp.h"
#include "i2c_dr.h"

#include <time.h>

#include "mesh.h"
#include "demo.h"

/**
 * Mesh Network
 * 
 */

// TODO: Assumption, if a link is down the sensor is down
// TODO: Report to LoRa Gateway when a sensor is down.
// TODO: Clean up main.c. Move communication methods outside and clean the code because is starting
//   to become really massive (4/5/18).

// Default Severity Level: 0 (Off) 1 (Error) 2 (Warning) 3 (Info) 4 (Debug)
#define NRF_LOG_DEFAULT_LEVEL 3

#if defined( USE_BAND_868 )

#define RF_FREQUENCY                                868000000 // Hz

#elif defined( USE_BAND_915 )

#define RF_FREQUENCY                                919100000 // Hz (old: 915000000)

#else
    #error "Please define a frequency band in the compiler options."
#endif

// 915000000

#define TX_OUTPUT_POWER                             20        // dBm

#if defined( USE_MODEM_LORA )

#define LORA_BANDWIDTH                              0         // [0: 125 kHz,
                                                              //  1: 250 kHz,
                                                              //  2: 500 kHz,
                                                              //  3: Reserved]
#define LORA_SPREADING_FACTOR                       7         // [SF7..SF12]
#define LORA_CODINGRATE                             1         // [1: 4/5,
                                                              //  2: 4/6,
                                                              //  3: 4/7,
                                                              //  4: 4/8]
#define LORA_PREAMBLE_LENGTH                        8         // Same for Tx and Rx
#define LORA_SYMBOL_TIMEOUT                         5         // Symbols
#define LORA_FIX_LENGTH_PAYLOAD_ON                  false
#define LORA_IQ_INVERSION_ON                        false

#elif defined( USE_MODEM_FSK )

#define FSK_FDEV                                    25e3      // Hz
#define FSK_DATARATE                                50e3      // bps
#define FSK_BANDWIDTH                               50e3      // Hz
#define FSK_AFC_BANDWIDTH                           83.333e3  // Hz
#define FSK_PREAMBLE_LENGTH                         5         // Same for Tx and Rx
#define FSK_FIX_LENGTH_PAYLOAD_ON                   false

#else
    #error "Please define a modem in the compiler options."
#endif

typedef enum
{
    LOWPOWER,
    RX,
    RX_TIMEOUT,
    RX_ERROR,
    TX,
    TX_TIMEOUT,
}States_t;

#define RX_TIMEOUT_VALUE                            1000	// Original: 1000
#define BUFFER_SIZE                                 64 		// Define the payload size here

#define RX_MESH_JOIN_TIMEOUT_VALUE                  5000

const uint8_t PingMsg[] = "PING";
const uint8_t PongMsg[] = "PONG";

uint16_t BufferSize = BUFFER_SIZE;
uint8_t Buffer[BUFFER_SIZE];

States_t State = LOWPOWER;

int8_t RssiValue = 0;
int8_t SnrValue = 0;

static RadioEvents_t RadioEvents;

//#define SPI_INSTANCE  0 /**< SPI instance index. */
//static const nrf_drv_spi_t spi = NRF_DRV_SPI_INSTANCE(SPI_INSTANCE);  /**< SPI instance. */
static volatile bool spi_xfer_done;  /**< Flag used to indicate that SPI instance completed the transfer. */

#define TEST_STRING "Nordic"
static uint8_t       m_tx_buf[] = TEST_STRING;           /**< TX buffer. */
static uint8_t       m_rx_buf[sizeof(TEST_STRING) + 1];  /**< RX buffer. */
static const uint8_t m_length = sizeof(m_tx_buf);        /**< Transfer length. */

#define SPI_INSTANCE  0 /**< SPI instance index. */
static const nrf_drv_spi_t spi = NRF_DRV_SPI_INSTANCE(SPI_INSTANCE);  /**< SPI instance. */

// Readings
int32_t  volatile temp;
uint32_t adc_data = 0; 
 
/* Mesh Network --------------------------------------------------------------*/
/* Global Variables  ---------------------------------------------------------*/
MeshEntry_t mesh_RT[MESH_MAX_ROUTING_TABLE_ENTRIES];     	/** Mesh Routing Table */
node_t      mesh_NT[MESH_MAX_ROUTING_TABLE_ENTRIES];     	/** Node's Neighbors*/
MeshInfo_t  mesh;                                        	/** Mesh info */

MeshInfo_t	mesh_CT[MESH_MAX_ROUTING_TABLE_ENTRIES];			/** Candidate Table */

static bool mesh_waiting_ack  = false;

uint8_t     mesh_delay_offset = 0;

/* indexes */
// TODO: Move to mesh.h, create extern global variables, and macros to add/remove
//    items from RT and NT.
uint8_t			i_rt = 0;
uint8_t  		i_nt = 0;
uint8_t 		ct_i = 0;


/*!
 * \brief Node status
 */
NodeStatus_t MeshStatus = MESH_NODE_STATUS_NOT_CONNECTED;

/*!
 * \brief Forward a message to another node in the mesh, using LoRa
 */
void fwdMsg(uint8_t *buffer, uint16_t size, node_t next);

/*!
 * \brief Send a message to another node in the mesh, using LoRa
 */
void sndMsg(TypeMsg_t type, uint8_t *payload, uint16_t size, node_t dest);

/*!
 * \brief Transmit data using either LoRa or LoRaWAN specification
 */
void txData( uint8_t *buffer, uint8_t size );

void meshJoin( );
void meshJoinReply( node_t src );

// TODO: Move to mesh.h
void meshAddRT( node_t n );
void meshAddNT( node_t n );

void getRead1( uint8_t *value, uint8_t *size );
void getRead2( uint8_t *value, uint8_t *size );

/* Private function prototypes -----------------------------------------------*/
/*!
 * Radio events function pointer
 */
static RadioEvents_t RadioEvents;

/*!
 * \brief Function to be executed on Radio Tx Done event
 */
void OnTxDone( void );

/*!
 * \brief Function to be executed on Radio Rx Done event
 */
void OnRxDone( uint8_t *payload, uint16_t size, int16_t rssi, int8_t snr );

/*!
 * \brief Function executed on Radio Tx Timeout event
 */
void OnTxTimeout( void );

/*!
 * \brief Function executed on Radio Rx Timeout event
 */
void OnRxTimeout( void );

/*!
 * \brief Function executed on Radio Rx Error event
 */
void OnRxError( void );

/**
 * Main application entry point.
 */
int main(void)
{
		bool isMaster = true;
		uint8_t adcdata = 0;
		uint8_t i;
	
		// mesh (eba)
		uint8_t counter = 0;
		node_t next;
		// struct tm time_struct;
	
	  // eba: Response Msg
	  uint8_t Rsp[BUFFER_SIZE];
	
	
		nrf_temp_init();
	 
    //bsp_board_leds_init();
	
		nrf_drv_gpiote_init();
	
    APP_ERROR_CHECK(NRF_LOG_INIT(NULL));
    NRF_LOG_DEFAULT_BACKENDS_INIT();
    
    NRF_LOG_INFO("LoRa mesh network init...");
	  Radio.IoInit( );

	  /*HW_SPI_Init();
	
		temp = SX1272Read(0x0C);

    while (1)
    {

        NRF_LOG_FLUSH();

        bsp_board_led_invert(BSP_BOARD_LED_0);
        nrf_delay_ms(200);
    }*/
	
		nrf_drv_spi_config_t spi_config = NRF_DRV_SPI_DEFAULT_CONFIG;
    spi_config.ss_pin   = 22;
    spi_config.miso_pin = 24;
    spi_config.mosi_pin = 23;
    spi_config.sck_pin  = 25;
		spi_config.frequency =  NRF_SPI_FREQ_1M;
    nrf_drv_spi_init(&spi, &spi_config, NULL, NULL);

		// I2C initialization
		twi_init();
    resetSettings();
	
		// Radio initialization
		RadioEvents.TxDone = OnTxDone;
		RadioEvents.RxDone = OnRxDone;
		RadioEvents.TxTimeout = OnTxTimeout;
		RadioEvents.RxTimeout = OnRxTimeout;
		RadioEvents.RxError = OnRxError;

		Radio.Init( &RadioEvents );

		Radio.SetChannel( RF_FREQUENCY );

	#if defined( USE_MODEM_LORA )

		Radio.SetTxConfig( MODEM_LORA, TX_OUTPUT_POWER, 0, LORA_BANDWIDTH,
																	 LORA_SPREADING_FACTOR, LORA_CODINGRATE,
																		 LORA_PREAMBLE_LENGTH, LORA_FIX_LENGTH_PAYLOAD_ON,
																		 true, 0, 0, LORA_IQ_INVERSION_ON, 3000000 );
			
		Radio.SetRxConfig( MODEM_LORA, LORA_BANDWIDTH, LORA_SPREADING_FACTOR,
																		 LORA_CODINGRATE, 0, LORA_PREAMBLE_LENGTH,
																		 LORA_SYMBOL_TIMEOUT, LORA_FIX_LENGTH_PAYLOAD_ON,
																		 0, true, 0, 0, LORA_IQ_INVERSION_ON, true );

	#elif defined( USE_MODEM_FSK )

		Radio.SetTxConfig( MODEM_FSK, TX_OUTPUT_POWER, FSK_FDEV, 0,
																		FSK_DATARATE, 0,
																		FSK_PREAMBLE_LENGTH, FSK_FIX_LENGTH_PAYLOAD_ON,
																		true, 0, 0, 0, 3000000 );
			
		Radio.SetRxConfig( MODEM_FSK, FSK_BANDWIDTH, FSK_DATARATE,
																		0, FSK_AFC_BANDWIDTH, FSK_PREAMBLE_LENGTH,
																		0, FSK_FIX_LENGTH_PAYLOAD_ON, 0, true,
																		0, 0,false, true );

	#else
			#error "Please define a frequency band in the compiler options."
	#endif
																		
		Radio.Rx( RX_TIMEOUT_VALUE );												

		isMaster = true;
	
		// TODO: Initial configuration should not be hardcoded, but instead using config files
		// TODO: Join method
																		
		// Mesh Network
		// INIT MESH
		mesh.id   = 5;
		mesh.type = MESH_NODE_TYPE_ROUTER;
		counter = mesh.slicerBase;
		
		mesh_delay_offset = (1000 / mesh.id*1.0);
		nrf_delay_ms( mesh_delay_offset );		
		
		// Set static tables (for Demo)
		demo_SetupNode(&mesh, mesh_RT, mesh_NT);
		MeshStatus = MESH_NODE_STATUS_CONNECTED;
		
		// Setup info
		NRF_LOG_INFO("id {%d} master {%d}", mesh.id, isMaster);
		
		for (i=0; i<MESH_MAX_ROUTING_TABLE_ENTRIES; i++)
			if ( mesh_RT[i].next > 0 ) NRF_LOG_INFO("id {%d} RT.next [%d] RT.dest [%d]", mesh.id, mesh_RT[i].next, mesh_RT[i].dest);
		
		for (i=0; i<MESH_MAX_ROUTING_TABLE_ENTRIES; i++)
			if ( mesh_NT[i] > 0 ) NRF_LOG_INFO("id {%d} NT [%d]", mesh.id, mesh_NT[i]);
			
		// Main loop
		while( 1 )
		{
			NRF_LOG_FLUSH();
			//printState();
			switch( State )
			{
				case RX:
					
					NRF_LOG_DEBUG("{%d}: Status [%d] Dest [%d] Next [%d] Type [%d] RX => done.", mesh.id, MeshStatus, Buffer[1], Buffer[3], Buffer[4]);
				
					switch ( MeshStatus ) 
					{
					
						case MESH_NODE_STATUS_NOT_CONNECTED:
							
							Radio.Rx( RX_TIMEOUT_VALUE );
							break;
						
						case MESH_NODE_STATUS_JOIN:
						
							// Start join process
							switch ( mesh_getTypeMsg(Buffer, mesh_NT, mesh) )
							{
							
								case MESH_TYPE_JOIN_RPL:
									// JOIN Process
									// (3) Add nodes that respond the request to a candidacy list
									mesh_CT[ct_i].id 	= Buffer[0]; 
									mesh_CT[ct_i].hop = Buffer[5];
									ct_i++; 
								
									NRF_LOG_DEBUG("{%d}: Status [%d] #STEP3 Add node {%d} to candidacy list: {%d}", mesh.id, MeshStatus, Buffer[0], ct_i);
									
									Radio.Rx( RX_TIMEOUT_VALUE );
									break;
								
								case MESH_TYPE_JOIN_ACK:
									// JOIN Process
									// (9) Finish `join` process and change status
									MeshStatus = MESH_NODE_STATUS_CONNECTED;
									//NRF_LOG_DEBUG("{%d}: Status [%d] #STEP9 The node is finally connected", mesh.id, MeshStatus, Buffer[0]);
								
									//for (i=0; i<i_rt; i++)
									//	NRF_LOG_DEBUG("{%d}: Status [%d] RT dest [%d] next [%d]", mesh.id, MeshStatus, mesh_RT[i].dest, mesh_RT[i].next);
								
									NRF_LOG_INFO("{%d}: Status [%d] Node joined mesh network", mesh.id, MeshStatus, Buffer[0]);
									for (i=0; i<i_rt; i++)
											NRF_LOG_INFO("{%d}: Status [%d] RT dest [%d] next [%d]", mesh.id, MeshStatus, mesh_RT[i].dest, mesh_RT[i].next);
								
									Radio.Rx( RX_TIMEOUT_VALUE );
									break;
								
								default:
									//NRF_LOG_DEBUG("{%d}: Status [%d] #STEP?? Something went wrong!", mesh.id, MeshStatus);
									Radio.Rx( RX_TIMEOUT_VALUE );
									break;
								
							}
							
							// Join process
							// mktime(&time_struct);
							break;
						
						case MESH_NODE_STATUS_CONNECTED:
							
							// FIXME: Master and Slave mode are not working correctly (right now is eternally on MASTER)
							// master mode
							if( isMaster == true )
							{
								// Sanity check
								if( BufferSize > 0 )
								{
									
									NRF_LOG_DEBUG("{%d}: Status [%d] Type [%d] Dest [%d] RX => Received msg {%s}", mesh.id, MeshStatus, Buffer[4], Buffer[3], Buffer+6);
									
									switch( mesh_getTypeMsg(Buffer, mesh_NT, mesh) ) {
										
										case MESH_TYPE_ACK:
											break;
										
										case MESH_TYPE_BEACON:
											break;

										case MESH_TYPE_JOIN_REQ:
											// JOIN Process
											// (2) Reply to Join Request
											// Send info needed to requester, such that the requester can make 
											// the selection (e.g. hops to router, etc)
											
											// TODO: Create helper function to acccess Buffer struct (packet)
											// Just reply the msg if the node is connected to the Gateway
											if ( mesh.hop > 0 ) 
											{
												meshJoinReply( Buffer[0] );
											}
											
											Radio.Rx( RX_TIMEOUT_VALUE );
											break;
										
										case MESH_TYPE_JOIN_ACT:
											// JOIN Process
											// (7) Update neighbors table
											meshAddNT( Buffer[0] );
											NRF_LOG_DEBUG("{%d}: Status [%d] #STEP7 Updates neighbor table {%d}", mesh.id, MeshStatus, Buffer[0]);
										
											// (8) Send ack to ACCEPT message
											NRF_LOG_DEBUG("{%d}: Status [%d] #STEP8 Send ACK to {%d}", mesh.id, MeshStatus, Buffer[0]);
											
											NRF_LOG_INFO("{%d}: Status [%d] Sending ACK to node {%d}", mesh.id, MeshStatus, Buffer[0]);
										
											for (i=0; i<i_rt; i++)
												NRF_LOG_INFO("{%d}: Status [%d] NT [%d]", mesh.id, MeshStatus, mesh_NT[i]);
											for (i=0; i<i_rt; i++)
												NRF_LOG_INFO("{%d}: Status [%d] RT dest [%d] next [%d]", mesh.id, MeshStatus, mesh_RT[i].dest, mesh_RT[i].next);
										
											sndMsg( MESH_TYPE_JOIN_ACK, "zzz", 3, Buffer[0] );
											
											break;
										
										case MESH_TYPE_DATA:

											// DATA: 
											// Receive data and foward payload to LoRa Gateway
										
											NRF_LOG_DEBUG("{%d}: RX => Received msg {%s}", mesh.id, Buffer+6);
											
											// Forward message (to next node)
											next = mesh_GetNextHop(mesh_RT, Buffer[1]);
											fwdMsg(Buffer, BufferSize, next);
											
											// TODO: Send ACK
											// isMaster = false;
											// sndMsg(MESH_TYPE_ACK, "ACK", BufferSize, Buffer[2]);
											
											Radio.Rx( RX_TIMEOUT_VALUE );
											break;
										
										case MESH_TYPE_NULL:
										default:
											
											NRF_LOG_DEBUG("{%d}: Status [%d] Message not address to this Node {%s}", mesh.id, MeshStatus, Buffer+6);
											NRF_LOG_DEBUG("{%d}: Buffer {%d/%d/%d/%d/%d}", mesh.id, Buffer[0], Buffer[1], Buffer[2], Buffer[3], Buffer[4]);
											
											isMaster = true;
											//NRF_LOG_DEBUG("{%d}: Status [%d] RX => Not PING nor PONG {%s} Buffer+4 {%s}", mesh.id, MeshStatus, Buffer, Buffer+4)
											Radio.Rx( RX_TIMEOUT_VALUE );
											break;
										
									}
									
								}
							}
							
							// Not using the slave mode
							// slave mode
							//else
							//{
							//		
							//	NRF_LOG_DEBUG("{%d}: SLAVE MODE", mesh.id);
							//	
							//	if( BufferSize > 0 )
							//	{
							//	
							//		NRF_LOG_DEBUG("{%d}: SLAVE MODE {%s}", mesh.id, Buffer+6);							
							//		
							//		// TODO: Replace
							//		if ( ( strncmp( ( const char* )Buffer, ( const char* )PongMsg, 4 ) == 0 ) /*&& 
							//				 ( Buffer[4] == pre )*/ 
							//		
							//		)
							//		{
							//			// Send the reply to the PONG string
							//			Buffer[0] = 'P';
							//			Buffer[1] = 'O';
							//			Buffer[2] = 'N';
							//			Buffer[3] = 'G';
							//			// We fill the buffer with numbers for the payload 
							//			for( i = 4; i < BufferSize; i++ )
							//			{
							//				Buffer[i] = i - 4;
							//			}
							//			nrf_delay_ms( 1 );
							//
							//			// Set device as master again
							//			isMaster = true;
							//			
							//			// Do nothing (no need to send a msg)
							//			// Radio.Send( Buffer, BufferSize );
							//			
							//			Buffer[5] = 0; 	// Temporal EOL for printing purpose
							//			NRF_LOG_INFO("{%d}: RX => Receiving ACK {%s}", mesh.id, Buffer);
							//
							//		}
							//		else // Valid reception but not a PING as expected
							//		{    // Set device as master and start again
							//			isMaster = true;
							//			Radio.Rx( RX_TIMEOUT_VALUE );
							//		}
							//	}
							//}
							break;
						
						default:
							break;
						
					}
					
				State = LOWPOWER;
				break;
					
				case TX:
					// Indicates on a LED that we have sent a PING [Master]
					// Indicates on a LED that we have sent a PONG [Slave]
					//GpioWrite( &Led2, GpioRead( &Led2 ) ^ 1 );
				
					NRF_LOG_DEBUG("{%d}: Status [%d] TX => done.", mesh.id, MeshStatus);
				
					Radio.Rx( RX_TIMEOUT_VALUE );
					State = LOWPOWER;
					break;
				
				case RX_TIMEOUT:
				case RX_ERROR:
					
					if ( State == RX_ERROR )
					{
						NRF_LOG_DEBUG("{%d}: Status [%d] RX_ERROR => done.", mesh.id, MeshStatus);
					}
					else
					{
						NRF_LOG_DEBUG("{%d}: Status [%d] RX_TIMEOUT => done.", mesh.id, MeshStatus);
					}
					
				
					switch ( MeshStatus ) {
					
						case MESH_NODE_STATUS_NOT_CONNECTED:
							
							// JOIN Process
							// (1) Send `JOIN` request
							meshJoin();
							MeshStatus = MESH_NODE_STATUS_JOIN;
							Radio.Rx( RX_MESH_JOIN_TIMEOUT_VALUE );
							
							break;
						
						case MESH_NODE_STATUS_JOIN:
							
							// JOIN Process
							if ( ct_i > 0 )
							{
							
								if ( !mesh_waiting_ack ) 
								{
									// (4) Select the best candidate to connect to the mesh network
									// mesh_CT[ct_i].id;
									NRF_LOG_DEBUG("{%d}: Status [%d] #STEP4 Selecting candidates {%d}", mesh.id, MeshStatus, ct_i);
									
									// TODO: insert algorithm here
									// TODO: For simplification we are choosing the last one.
									// Update hop to Gateway distance
									mesh.hop = mesh_CT[ct_i-1].hop + 1;
									
									// (5) Update RT
									meshAddRT(mesh_CT[ct_i-1].id);
									NRF_LOG_DEBUG("{%d}: Status [%d] #STEP5 Update Routing Table {%d}", mesh.id, MeshStatus, ct_i);
								
									NRF_LOG_INFO("{%d}: Status [%d] Selecting candidates ...", mesh.id, MeshStatus);
									NRF_LOG_INFO("{%d}: Status [%d] Updating Routing Table ... ", mesh.id, MeshStatus);
									
									// Don't update internal structure, once the candidates has been choosen
									mesh_waiting_ack = true;									
								} 

								// (6) Send ACCEPT to chosen node
								NRF_LOG_DEBUG("{%d}: Status [%d] #STEP6 Send ACT to {%d} for update NT {%d}", mesh.id, MeshStatus, mesh_CT[ct_i-1].id , ct_i);
								NRF_LOG_INFO("{%d}: Status [%d] Communicate with node added", mesh.id, MeshStatus);
								
								sndMsg( MESH_TYPE_JOIN_ACT, "yyy", 3, mesh_CT[ct_i-1].id );

								// Waiting for ACK
								Radio.Rx ( RX_TIMEOUT_VALUE );								
								
							} 
							else
							{
								// If nobody respond, try to connect to the LoRa Gateway
								// FIXME: Harcoded until LoRaWAN is working
								if ( mesh.id == 3 ) 
								{
									mesh.hop  = 1; 
									mesh.type = MESH_NODE_TYPE_BORDER;
									
									meshAddRT( MESH_NODE_GATEWAY );
									MeshStatus = MESH_NODE_STATUS_CONNECTED;
									
									NRF_LOG_DEBUG("{%d}: Status [%d] #STEP3 Connect to Gateway", mesh.id, MeshStatus);
									NRF_LOG_INFO("{%d}: Status [%d] Connected to Gateway", mesh.id, MeshStatus);
								} 
								else 
								{
								
									// Resend `JOIN` Request
									// TODO: If the connection to the LoRa Gateway is not possible, send another `JOIN` request
									// TODO: To avoid starvation limit the periodicity of `JOIN` request. Maybe, one each 5s.
									meshJoin();
									MeshStatus = MESH_NODE_STATUS_JOIN;
									Radio.Rx( RX_MESH_JOIN_TIMEOUT_VALUE );
								}
								
							}
				
							break;
						
						case MESH_NODE_STATUS_CONNECTED:
							
							// TODO: RX_TIMEOUT & RX_ERROR should not share same logic
							// TODO: Change such that all nodes can TX data to the Gateway 
							// TODO: Set a TX schedule for all nodes (to avoid collisions)
							if( isMaster )
							{
								
								counter = ( counter > mesh.slicerMax ) ? mesh.slicerBase : counter + 1;
								// Slicer (workaround)
								if ( counter != mesh.id )
								{
									NRF_LOG_DEBUG("{%d}: OUT RX_TIMEOUT => done.", mesh.id);
									State = LOWPOWER;
									break;
								}
								
								// Send the next PING frame
								// adcdata = HW_GetBatteryLevel();
								NRF_TEMP->TASKS_START = 1;
								while (NRF_TEMP->EVENTS_DATARDY == 0)
								{
										// Do nothing.
								}
								
								// Readings
								
								// Temporature sensor
								NRF_TEMP->EVENTS_DATARDY = 0;
								temp = (nrf_temp_read() / 4);
								NRF_TEMP->TASKS_STOP = 1;
								
								// Nitrate sensor
								adc_data = readADC();
					
								Rsp[0] = temp*10;
								Rsp[1] = adc_data >> 16;
								Rsp[2] = adc_data >> 8;
								Rsp[3] = adc_data;
								
								nrf_delay_ms( mesh.slicerDelay );
								sndMsg(MESH_TYPE_DATA, Rsp, 4, MESH_NODE_GATEWAY);
								
								// Radio.Rx( RX_TIMEOUT_VALUE );
								
								// NRF_LOG_INFO("{%c}: Sending {%s} hop{1}", mesh.id, Buffer);
								// nrf_delay_ms( 1 ); 
								// Radio.Send( Buffer, BufferSize );
							}
							else
							{
								NRF_LOG_DEBUG("{%d}: Not MASTER RX_TIMEOUT => done.", mesh.id);
								Radio.Rx( RX_TIMEOUT_VALUE );
							}
							State = LOWPOWER;
							break;
						
						default:
							break;
					
					}
				
				case TX_TIMEOUT:
					NRF_LOG_DEBUG("{%d}: Status [%d] TX_TIMEOUT => done.", mesh.id, MeshStatus);
				
					Radio.Rx( RX_TIMEOUT_VALUE );
					State = LOWPOWER;
					break;
				
				case LOWPOWER:
				default:
					// Set low power
					// NRF_LOG_INFO("{%c}: LOWPOWER => done.", mesh.id);
					break;
			}
			
			DISABLE_IRQ( );
			/* if an interupt has occured after __disable_irq, it is kept pending 
			 * and cortex will not enter low power anyway  */
			if (State == LOWPOWER)
			{
	#ifndef LOW_POWER_DISABLE
				//LowPower_Handler( );
	#endif
			}
			ENABLE_IRQ( );
				 
		}

	
}

void OnTxDone( void )
{
    Radio.Sleep( );
    State = TX;
    NRF_LOG_DEBUG("OnTxDone");
}

void OnRxDone( uint8_t *payload, uint16_t size, int16_t rssi, int8_t snr )
{
    Radio.Sleep( );
    BufferSize = size;
    memcpy( Buffer, payload, BufferSize );
    RssiValue = rssi;
    SnrValue = snr;
    State = RX;
	
	  //payload[5] = 0; //temp
		
    NRF_LOG_DEBUG("OnRxDone");
    //NRF_LOG_DEBUG("{%d}: RssiValue=%d dBm, SnrValue=%d Type=%d payload=%s", mesh.id, rssi, snr, payload[4], payload+6);
	
    NRF_LOG_DEBUG("{%d}: {%d.%d.%d.%d}", mesh.id, payload[0], payload[1], payload[2], payload[3]);
		//NRF_LOG_INFO("Outside temperature: %d oC || RssiValue=%d dBm, SnrValue=%d",payload[0], rssi, snr);
		//if (payload[4] == MESH_TYPE_DATA) NRF_LOG_INFO("{%d}: Message received {%s} from {%d}", mesh.id, payload+6, payload[2]); // DEMO
}

void OnTxTimeout( void )
{
    Radio.Sleep( );
    State = TX_TIMEOUT;
  
		NRF_LOG_DEBUG("OnTxTimeout");
}

void OnRxTimeout( void )
{
    Radio.Sleep( );
    State = RX_TIMEOUT;
    NRF_LOG_DEBUG("OnRxTimeout");
}

void OnRxError( void )
{
    Radio.Sleep( );
    State = RX_ERROR;
    NRF_LOG_DEBUG("OnRxError");
}

/* Mesh Network --------------------------------------------------------------*/

void fwdMsg(uint8_t *buffer, uint16_t size, node_t next)
{

	NRF_LOG_DEBUG("{%d}: FWD >> {%s} len {%d}", mesh.id , buffer+6, size);
	NRF_LOG_DEBUG("{%d}: FWD >> next {%d} dest {%d}", mesh.id , buffer[2], buffer[1]);
	
	// `data` communication
	// Forward from a BORDER node, with destination to the GATEWAY just have one possible destination:
	// GATEWAY
	if ( buffer[4] == MESH_TYPE_DATA )
	{
		if ( next == MESH_DST_MULTICAST && buffer[1] == MESH_NODE_GATEWAY && mesh.type == MESH_NODE_TYPE_BORDER )
		{
			// No Forward, send packet to Gateway
			buffer[3] = buffer[1];		// next <- dest
		}
		else 
		{
			// Forward to next hop
			buffer[2] = mesh.id;			// prev <- this
			buffer[3] = next;					// next <- next-hop()
			
			if (buffer[4] == MESH_TYPE_DATA) NRF_LOG_INFO("{%d}: Dest [%d] Next [%d] Forwarding >> [%d]", mesh.id, buffer[1], buffer[3], buffer[6]);	// DEMO
		}
	}
	
	// Forward message
	txData( buffer, size );
	
}

void sndMsg(TypeMsg_t type, uint8_t *payload, uint16_t size, node_t dest)
{
	// TODO: Create a matching struct w/fields for the buffer (so, intead of using array position,
	// we can use a dot operator like a struct)
	uint8_t msg[BUFFER_SIZE];
	node_t next;
	
	// Get next hop
	if ( type == MESH_TYPE_DATA )
	{
		// Only `data` communications are routed, all other are p2p
		// TODO: Check if this indeed true
	
		// TOFIX: This is not working as it was supposed to be
		if ( mesh.type == MESH_NODE_TYPE_BORDER && dest == MESH_NODE_GATEWAY )
		{
			next = dest;
		}
		else
		{
			next = mesh_GetNextHop(mesh_RT, dest);
		}
		
	}
	else
	{
		// p2p
		next = dest;
	}
	
	msg[0] = mesh.id;		/* source */
	msg[1] = dest;			/* destination */
	msg[2] = mesh.id;		/* previous */
	msg[3] = next;			/* next */
	msg[4] = type;			/* type */
	msg[5] = size;			/* size */

	// TODO: Fill the payload w/random chars

	memcpy( msg+6, payload, size );
	
	NRF_LOG_DEBUG("{%d}: SND >> {%d.%d.%d.%d}", mesh.id, msg[0], msg[1], msg[2], msg[3]);
	NRF_LOG_DEBUG("{%d}: SND >> type {%d} next {%d} dest {%d} payload {%d}", mesh.id, type, next, dest, payload[0]);
	
	if (type == MESH_TYPE_DATA) NRF_LOG_INFO("{%d}: Sending >> _%d_", mesh.id, payload[0]);	// DEMO

	// Send message
	txData( msg, BufferSize );  
}

void txData( uint8_t *buffer, uint8_t size )
{
	uint8_t  temp;
	uint32_t adcVal;
	double   result;
	
	char *dummy;
	char *buf;
	  
	
	if ( buffer[3] == MESH_NODE_GATEWAY ) 
	{
		
		// TODO: Implement tx using LoRaWAN
		nrf_delay_ms( 1 );
		
		// Transmit data using LoRaWAN
		
		// Decoding sensors readings
		temp = buffer[6]/10;
		
		adcVal = (buffer[7] << 16) | (buffer[8] << 8) | (buffer[9]);
	
		if ( adcVal & 0x00800000 )
		{
			adcVal = ~adcVal+1;
			
			adcVal = (adcVal & 0x00FFFFFF);
			result =  -1*(2.7/16777216)*(double)adcVal;
		}
	
    result = (2.7/16777216)*(double)adcVal;
		
		buf = (char *) malloc(sizeof(char) * 20);
		dummy = (char *) malloc(sizeof(char) * 20);
		
		sprintf( buf, "%f", result );
		sprintf( dummy, "%f", result );
		
		NRF_LOG_INFO("{%d}: LoRaWAN Transmitting to Gateway {%d} {%d} read: {%d} read: {%s}", mesh.id, buffer[0], buffer[2], temp, dummy);
		
		free(buf);
		free(dummy);
	} 
	else 
	{
		
		// Transmit data using LoRa
		nrf_delay_ms( 1 ); 
		Radio.Send( buffer, size );
	}
}

void meshJoin( )
{
	NRF_LOG_DEBUG("{%d}: Status [%d] #STEP1 Sending JOIN REQUEST", mesh.id, MeshStatus);
	NRF_LOG_INFO("{%d}: Status [%d] Send JOIN REQUEST to mesh network", mesh.id, MeshStatus);
	
	sndMsg( MESH_TYPE_JOIN_REQ, "join", 4, MESH_DST_BROADCAST );
}

void meshJoinReply( node_t requester )
{
	uint8_t msg[10];
	
	msg[0] = mesh.hop;
	NRF_LOG_DEBUG("{%d}: Status [%d] #STEP2 Sending JOIN REPLY {hop: %d}", mesh.id, MeshStatus, mesh.hop);
	NRF_LOG_INFO("{%d}: Status [%d] Reply JOIN REQUEST {hops: %d}", mesh.id, MeshStatus, mesh.hop);
	
	sndMsg( MESH_TYPE_JOIN_RPL, msg, 1, requester );
}

void meshAddRT( node_t n )
{
	mesh_RT[i_rt].dest = MESH_NODE_GATEWAY;
	mesh_RT[i_rt].next = n;
	i_rt++;
}

void meshAddNT( node_t n )
{
	mesh_NT[i_nt] = n;
	i_nt++;
}
