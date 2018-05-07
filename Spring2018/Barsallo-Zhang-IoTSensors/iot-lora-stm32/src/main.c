/*
 / _____)             _              | |
( (____  _____ ____ _| |_ _____  ____| |__
 \____ \| ___ |    (_   _) ___ |/ ___)  _ \
 _____) ) ____| | | || |_| ____( (___| | | |
(______/|_____)_|_|_| \__)_____)\____)_| |_|
    (C)2013 Semtech

Description: Ping-Pong implementation

License: Revised BSD License, see LICENSE.TXT file include in the project

Maintainer: Miguel Luis and Gregory Cristian
*/
/******************************************************************************
  * @file    main.c
  * @author  MCD Application Team
  * @version V1.1.2
  * @date    08-September-2017
  * @brief   this is the main!
  ******************************************************************************
  * @attention
  *
  * <h2><center>&copy; Copyright (c) 2017 STMicroelectronics International N.V. 
  * All rights reserved.</center></h2>
  *
  * Redistribution and use in source and binary forms, with or without 
  * modification, are permitted, provided that the following conditions are met:
  *
  * 1. Redistribution of source code must retain the above copyright notice, 
  *    this list of conditions and the following disclaimer.
  * 2. Redistributions in binary form must reproduce the above copyright notice,
  *    this list of conditions and the following disclaimer in the documentation
  *    and/or other materials provided with the distribution.
  * 3. Neither the name of STMicroelectronics nor the names of other 
  *    contributors to this software may be used to endorse or promote products 
  *    derived from this software without specific written permission.
  * 4. This software, including modifications and/or derivative works of this 
  *    software, must execute solely and exclusively on microcontroller or
  *    microprocessor devices manufactured by or for STMicroelectronics.
  * 5. Redistribution and use of this software other than as permitted under 
  *    this license is void and will automatically terminate your rights under 
  *    this license. 
  *
  * THIS SOFTWARE IS PROVIDED BY STMICROELECTRONICS AND CONTRIBUTORS "AS IS" 
  * AND ANY EXPRESS, IMPLIED OR STATUTORY WARRANTIES, INCLUDING, BUT NOT 
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
  * PARTICULAR PURPOSE AND NON-INFRINGEMENT OF THIRD PARTY INTELLECTUAL PROPERTY
  * RIGHTS ARE DISCLAIMED TO THE FULLEST EXTENT PERMITTED BY LAW. IN NO EVENT 
  * SHALL STMICROELECTRONICS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
  * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
  * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
  * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
  * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  *
  ******************************************************************************
  */

/* Includes ------------------------------------------------------------------*/
#include <string.h>
#include "i2c_dr.h"
#include "radio.h"
#include "timeServer.h"
#include "delay.h"
#include "low_power.h"
#include "vcom.h"
#include <math.h>

#include "mesh.h"
#include "demo.h"



#if defined( USE_BAND_868 )

#define RF_FREQUENCY                                868000000 // Hz

#elif defined( USE_BAND_915 )

#define RF_FREQUENCY                                919100000 // Hz

#else
    #error "Please define a frequency band in the compiler options."
#endif

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

#define RX_TIMEOUT_VALUE                            1000
#define BUFFER_SIZE                                 64 // Define the payload size here
#define LED_PERIOD_MS               200

#define LEDS_OFF   do{ \
                   LED_Off( LED_BLUE ) ;   \
                   LED_Off( LED_RED ) ;    \
                   LED_Off( LED_GREEN1 ) ; \
                   LED_Off( LED_GREEN2 ) ; \
                   } while(0) ;

const uint8_t PingMsg[] = "PING";
const uint8_t PongMsg[] = "PONG";

uint16_t BufferSize = BUFFER_SIZE;
uint8_t Buffer[BUFFER_SIZE];

States_t State = LOWPOWER;

int8_t RssiValue = 0;
int8_t SnrValue = 0;
									 
/* ADC handler declaration */
ADC_HandleTypeDef    AdcHandle;

/* ADC channel configuration structure declaration */
ADC_ChannelConfTypeDef        sConfig;

double t,r;

uint32_t adc_data = 0;
double temp_data = 0;
__IO uint32_t uwADCxConvertedValue = 0;


 /* Led Timers objects*/
static  TimerEvent_t timerLed;

/* Mesh Network --------------------------------------------------------------*/
/* Global Variables  ---------------------------------------------------------*/
MeshEntry_t mesh_RT[MESH_MAX_ROUTING_TABLE_ENTRIES];     	/** Mesh Routing Table */
node_t      mesh_NT[MESH_MAX_ROUTING_TABLE_ENTRIES];     	/** Node's Neighbors*/
MeshInfo_t  mesh;                                        	/** Mesh info */

MeshInfo_t	mesh_CT[MESH_MAX_ROUTING_TABLE_ENTRIES];			/** Candidate Table */

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

void meshJoin( void );
void meshJoinReply( node_t src );

// TODO: Move to mesh.h
void meshAddRT( node_t n );
void meshAddNT( node_t n );


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

/*!
 * \brief Function executed on when led timer elapses
 */
static void OnledEvent( void );
/**
 * Main application entry point.
 */
int main( void )
{
	long double a1 = 0.9247125400 * pow(10,-3);
  long double b1 = 2.508830995 * pow(10,-4);
  long double c1 = 1.680251474 * pow(10,-7);

  bool isMaster = true;
  uint8_t i;
	
	// mesh (eba)
	uint8_t counter = 0;
	node_t  next;
	uint8_t Rsp[BUFFER_SIZE];

	
  HAL_Init( );
  
  SystemClock_Config( );
  
  DBG_Init( );

  HW_Init( );  
	
	
	i2c_init();
	resetSettings();
	
	//ADC init
	AdcHandle.Instance = ADC1;
  
  AdcHandle.Init.OversamplingMode      = DISABLE;
  
  AdcHandle.Init.ClockPrescaler        = ADC_CLOCK_SYNC_PCLK_DIV1;
  AdcHandle.Init.LowPowerAutoPowerOff  = DISABLE;
  AdcHandle.Init.LowPowerFrequencyMode = ENABLE;
  AdcHandle.Init.LowPowerAutoWait      = DISABLE;
    
  AdcHandle.Init.Resolution            = ADC_RESOLUTION_12B;
  AdcHandle.Init.SamplingTime          = ADC_SAMPLETIME_7CYCLES_5;
  AdcHandle.Init.ScanConvMode          = ADC_SCAN_DIRECTION_FORWARD;
  AdcHandle.Init.DataAlign             = ADC_DATAALIGN_RIGHT;
  AdcHandle.Init.ContinuousConvMode    = ENABLE;
  AdcHandle.Init.DiscontinuousConvMode = DISABLE;
  AdcHandle.Init.ExternalTrigConvEdge  = ADC_EXTERNALTRIGCONVEDGE_NONE;
  AdcHandle.Init.EOCSelection          = ADC_EOC_SINGLE_CONV;
  AdcHandle.Init.DMAContinuousRequests = DISABLE;

	HAL_ADC_Init(&AdcHandle);
	HAL_ADCEx_Calibration_Start(&AdcHandle, ADC_SINGLE_ENDED);
	sConfig.Channel = ADC_CHANNEL_4;    
	HAL_ADC_ConfigChannel(&AdcHandle, &sConfig);
	HAL_ADC_Start(&AdcHandle);
	
	
	
  
  /* Led Timers*/
  TimerInit(&timerLed, OnledEvent);   
  TimerSetValue( &timerLed, LED_PERIOD_MS);

  TimerStart(&timerLed );

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
	
	// Mesh Network
	// INIT MESH
	mesh.id   = 6;
	mesh.type = MESH_NODE_TYPE_ROUTER;
	counter = mesh.slicerBase;
	
	// FIXME Manage behaviour of master/slave in communication
	isMaster = true;

	mesh_delay_offset = (1000 / mesh.id*1.0);
	DelayMs( mesh_delay_offset );	
		
	// Set static tables (for Demo)
	demo_SetupNode(&mesh, mesh_RT, mesh_NT);
	MeshStatus = MESH_NODE_STATUS_CONNECTED;
	
	// Setup info
	PRINTF("id {%d} master {%d}\n\r", mesh.id, isMaster);
	
	for (i=0; i<MESH_MAX_ROUTING_TABLE_ENTRIES; i++)
		if ( mesh_RT[i].next > 0 ) PRINTF("id {%d} RT.next [%d] RT.dest [%d]\n\r", mesh.id, mesh_RT[i].next, mesh_RT[i].dest);
	
	for (i=0; i<MESH_MAX_ROUTING_TABLE_ENTRIES; i++)
		if ( mesh_NT[i] > 0 ) PRINTF("id {%d} NT [%d]\n\r", mesh.id, mesh_NT[i]);
		
  while( 1 )
  {
		
		// PRINTF("{%d} State: %d\n\r", mesh.id, State);
		
    switch( State )
    {
    case RX:
      if( isMaster == true )
      {
        if( BufferSize > 0 )
        {
					
					switch( mesh_getTypeMsg(Buffer, mesh_NT, mesh) ) 
					{
						case MESH_TYPE_DATA:
						
							TimerStop(&timerLed );
							LED_Off( LED_BLUE);
							LED_Off( LED_GREEN ) ; 
							LED_Off( LED_RED1 ) ;;
							// Indicates on a LED that the received frame is a PONG
							LED_Toggle( LED_RED2 ) ;
							
							// Forward message (to next node)
							next = mesh_GetNextHop(mesh_RT, Buffer[1]);
							fwdMsg(Buffer, BufferSize, next);
						
							Radio.Rx( RX_TIMEOUT_VALUE );
							break;
						
						case MESH_TYPE_NULL:
						default:
							isMaster = true;
							Radio.Rx( RX_TIMEOUT_VALUE );
							break;
						
					}
				}
			}
			
      State = LOWPOWER;
      break;
			
    case TX:
      // Indicates on a LED that we have sent a PING [Master]
      // Indicates on a LED that we have sent a PONG [Slave]
      //GpioWrite( &Led2, GpioRead( &Led2 ) ^ 1 );
      Radio.Rx( RX_TIMEOUT_VALUE );
      State = LOWPOWER;
      break;
		
    case RX_TIMEOUT:
    case RX_ERROR:
      if( true )
      {
					
				// Mesh Slicer
				counter = ( counter > mesh.slicerMax ) ? mesh.slicerBase : counter + 1;
				
				// PRINTF("id {%d} counter _%d_", mesh.id, counter);
				
				if ( counter != mesh.id )
				{
					Radio.Rx( RX_TIMEOUT_VALUE );
					State = LOWPOWER;
					break;
				}
				
				//temp
				HAL_ADC_PollForConversion(&AdcHandle, 10);
			
				/* Check if the continous conversion of regular channel is finished */
				if ((HAL_ADC_GetState(&AdcHandle) & HAL_ADC_STATE_REG_EOC) == HAL_ADC_STATE_REG_EOC)
				{
					/*##-6- Get the converted value of regular channel  ########################*/
					  uwADCxConvertedValue = HAL_ADC_GetValue(&AdcHandle); 	
						temp_data = (double)uwADCxConvertedValue/4096 * 3.3;
						r  = (95 * (temp_data/5)) / (1 - temp_data/5);
						t = (1 / (a1 + b1 * (log(r*1000)) + c1 * (long double)pow((log(r*1000)),3)) - 273.15);

				}

				//I2C
				adc_data = readADC();
			
        // Send the next PING frame
        Rsp[0] = (int)(t*10);
				Rsp[1] = adc_data >> 16;
				Rsp[2] = adc_data >> 8;
				Rsp[3] = adc_data;
        for( i = 4; i < BufferSize; i++ )
        {
          Rsp[i] = i - 4;
        }
        DelayMs( mesh.slicerDelay ); 
				sndMsg(MESH_TYPE_DATA, Rsp, 4, MESH_NODE_GATEWAY);
        
      }
      else
      {
        Radio.Rx( RX_TIMEOUT_VALUE );
      }
			
			Radio.Rx( RX_TIMEOUT_VALUE );
      State = LOWPOWER;
      break;
			
    case TX_TIMEOUT:
      Radio.Rx( RX_TIMEOUT_VALUE );
      State = LOWPOWER;
      break;
		
    case LOWPOWER:
		default:
        // Set low power
				// PRINTF(".");
				break;
    }
    
    DISABLE_IRQ( );
    /* if an interupt has occured after __disable_irq, it is kept pending 
     * and cortex will not enter low power anyway  */
    if (State == LOWPOWER)
    {
#ifndef LOW_POWER_DISABLE
       LowPower_Handler( );   // *
#endif
    }
    ENABLE_IRQ( );
       
  }
}

void OnTxDone( void )
{
    Radio.Sleep( );
    State = TX;
    //PRINTF("OnTxDone\n\r");
}

void OnRxDone( uint8_t *payload, uint16_t size, int16_t rssi, int8_t snr )
{
    Radio.Sleep( );
    BufferSize = size;
    memcpy( Buffer, payload, BufferSize );
    RssiValue = rssi;
    SnrValue = snr;
    State = RX;
  
    //PRINTF("OnRxDone\n\r");
		//PRINTF("%c %c %c %c %d || RssiValue=%d dBm, SnrValue=%d\n\r\n\r",payload[0],payload[1],payload[2],payload[3],payload[4], rssi, snr);
}

void OnTxTimeout( void )
{
    Radio.Sleep( );
    State = TX_TIMEOUT;
	
    //PRINTF("OnTxTimeout\n\r");
}

void OnRxTimeout( void )
{
    Radio.Sleep( );
    State = RX_TIMEOUT;

    //PRINTF("OnRxTimeout\n\r");
}

void OnRxError( void )
{
    Radio.Sleep( );    // *
    State = RX_ERROR;  // *
    //PRINTF("OnRxError\n\r");
}

static void OnledEvent( void )
{
  LED_Toggle( LED_BLUE ) ; 
  LED_Toggle( LED_RED1 ) ; 
  LED_Toggle( LED_RED2 ) ; 
  LED_Toggle( LED_GREEN ) ;   

  TimerStart(&timerLed );
}

/* Mesh Network --------------------------------------------------------------*/

void fwdMsg(uint8_t *buffer, uint16_t size, node_t next)
{	
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
			
			if (buffer[4] == MESH_TYPE_DATA) PRINTF("{%d}: Dest [%d] Next [%d] Forwarding >> [%d]\n\r", mesh.id, buffer[1], buffer[3], buffer[6]);	// DEMO
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
	
	if (type == MESH_TYPE_DATA) PRINTF("{%d}: Sending >> _%d_\n\r", mesh.id, payload[0]);	

	// Send message
	txData( msg, BufferSize );  
}

void txData( uint8_t *buffer, uint8_t size )
{
	uint8_t  temp;
	uint32_t adcVal;
	double   result;
	
	//char *dummy;
	//char *buf;
	  
	
	if ( buffer[3] == MESH_NODE_GATEWAY ) 
	{
		
		// TODO: Implement tx using LoRaWAN
		DelayMs ( 1 );
		
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
		
		//buf = (char *) malloc(sizeof(char) * 20);
		//dummy = (char *) malloc(sizeof(char) * 20);
		
		//sprintf( buf, "%f", result );
		//sprintf( dummy, "%f", result );
		
		PRINTF("{%d}: LoRaWAN Transmitting to Gateway {%d} {%d} read: {%d} read: {%lf}\n\r", mesh.id, buffer[0], buffer[2], temp, result);
		
		//free(buf);
		//free(dummy);
	} 
	else 
	{
		
		// Transmit data using LoRa
		DelayMs ( 1 ); 
		Radio.Send( buffer, size );
	}
}

