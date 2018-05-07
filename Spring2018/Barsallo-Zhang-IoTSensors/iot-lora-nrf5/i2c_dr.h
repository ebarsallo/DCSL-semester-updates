#include "nrf_drv_twi.h"
#include <stdint.h>
//#include "NAU7802.h"
#include "app_util_platform.h"


void write(uint8_t ,uint8_t , uint8_t );
uint8_t read(uint8_t ,uint8_t );
void writeBit(uint8_t , uint8_t );
void clearBit(uint8_t , uint8_t );
bool readBit(uint8_t , uint8_t );
uint32_t read24(uint8_t);
uint32_t read32(uint8_t);
void readUntilTrue(uint8_t, uint8_t);
void readUntilFalse(uint8_t , uint8_t );
void twi_handler(nrf_drv_twi_evt_t const * , void * );
void twi_init (void);
uint32_t readADC();
void resetSettings();
void calibrate();
