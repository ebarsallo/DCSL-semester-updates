 /*
 / _____)             _              | |
( (____  _____ ____ _| |_ _____  ____| |__
 \____ \| ___ |    (_   _) ___ |/ ___)  _ \
 _____) ) ____| | | || |_| ____( (___| | | |
(______/|_____)_|_|_| \__)_____)\____)_| |_|
    (C)2013 Semtech

Description: Generic lora driver implementation

License: Revised BSD License, see LICENSE.TXT file include in the project

Maintainer: Miguel Luis, Gregory Cristian and Wael Guibene
*/
/******************************************************************************
  * @file    timeserver.c
  * @author  MCD Application Team
  * @version V1.1.2
  * @date    08-September-2017
  * @brief   Time server infrastructure
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
#include <time.h>
#include "hw.h"
#include "timeServer.h"

//#include "low_power.h"

const nrf_drv_timer_t TIMER_TX = NRF_DRV_TIMER_INSTANCE(0);
const nrf_drv_timer_t TIMER_RX = NRF_DRV_TIMER_INSTANCE(1);
const nrf_drv_timer_t TIMER_SYNC = NRF_DRV_TIMER_INSTANCE(2);

/*!
 * safely execute call back
 */
#define exec_cb( _callback_ )     \
  do {                          \
      if( _callback_ == NULL )    \
      {                           \
        while(1);                 \
      }                           \
      else                        \
      {                           \
        _callback_( );               \
      }                           \
  } while(0);                   

void TimerInit(uint8_t instance, void ( *callback )( nrf_timer_event_t event_type,void * p_context ) )
{
		nrf_drv_timer_config_t timer_cfg = NRF_DRV_TIMER_DEFAULT_CONFIG;
		if(instance == 0)
		{
			nrf_drv_timer_init(&TIMER_TX, &timer_cfg, callback);
			nrf_drv_timer_enable(&TIMER_TX);
			nrf_drv_timer_pause(&TIMER_TX);
		}
		else if(instance == 1)
		{
			nrf_drv_timer_init(&TIMER_RX, &timer_cfg, callback);
			nrf_drv_timer_enable(&TIMER_RX);
			nrf_drv_timer_pause(&TIMER_RX);
		}
		else if(instance == 2)
		{
			nrf_drv_timer_init(&TIMER_SYNC, &timer_cfg, callback);
			nrf_drv_timer_enable(&TIMER_SYNC);
			nrf_drv_timer_pause(&TIMER_SYNC);
		}
}

void TimerStart( uint8_t instance )
{
		if(instance == 0)
		{
			nrf_drv_timer_clear(&TIMER_RX);
			nrf_drv_timer_resume(&TIMER_RX);
		}
		else if(instance == 1)
		{
			nrf_drv_timer_clear(&TIMER_RX);
			nrf_drv_timer_resume(&TIMER_RX);
		}
		else if(instance == 2)
		{
			nrf_drv_timer_clear(&TIMER_RX);
			nrf_drv_timer_resume(&TIMER_RX);	
		}
}

void TimerStop( uint8_t instance ) 
{
		if(instance == 0)
		{
			//nrf_drv_timer_disable(&TIMER_TX);
			nrf_drv_timer_pause(&TIMER_TX);
		}
		else if(instance == 1)
		{
			//nrf_drv_timer_disable(&TIMER_RX);
			nrf_drv_timer_pause(&TIMER_RX);
		}
		else if(instance == 2)
		{
			//nrf_drv_timer_disable(&TIMER_SYNC);	
			nrf_drv_timer_pause(&TIMER_SYNC);			
		}
}  
  
void TimerReset( uint8_t instance )
{
		if(instance == 0)
		{
			nrf_drv_timer_clear(&TIMER_TX);
		}
		else if(instance == 1)
		{
			nrf_drv_timer_clear(&TIMER_RX);
		}
		else if(instance == 2)
		{
			nrf_drv_timer_clear(&TIMER_SYNC);		
		}
}

void TimerSetValue( uint8_t instance, uint32_t value )
{	
		int time_ticks;
	
		if(instance == 0)
		{
			time_ticks = nrf_drv_timer_ms_to_ticks(&TIMER_TX, value);
			nrf_drv_timer_extended_compare(&TIMER_TX, NRF_TIMER_CC_CHANNEL0, time_ticks,NRF_TIMER_SHORT_COMPARE0_CLEAR_MASK, true);
		}
		else if(instance == 1)
		{
			time_ticks = nrf_drv_timer_ms_to_ticks(&TIMER_RX, value);
			nrf_drv_timer_extended_compare(&TIMER_RX, NRF_TIMER_CC_CHANNEL1, time_ticks,NRF_TIMER_SHORT_COMPARE1_CLEAR_MASK, true);
		}
		else if(instance == 2)
		{
			time_ticks = nrf_drv_timer_ms_to_ticks(&TIMER_SYNC, value);
			nrf_drv_timer_extended_compare(&TIMER_SYNC, NRF_TIMER_CC_CHANNEL2, time_ticks,NRF_TIMER_SHORT_COMPARE2_CLEAR_MASK, true);
		}
}
/************************ (C) COPYRIGHT STMicroelectronics *****END OF FILE****/
