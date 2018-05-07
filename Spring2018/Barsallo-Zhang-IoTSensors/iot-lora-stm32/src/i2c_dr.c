#include "i2c_dr.h"
#include "nau7802.h"
//#include "stm32l0xx_hal_adc.h"


/* I2C handler declaration */
I2C_HandleTypeDef I2cHandle;
uint8_t data24[3] = {0x00,0x00,0x00};

void i2c_init()
{
  I2cHandle.Instance             = I2Cx;
  I2cHandle.Init.Timing          = 0x00B1112E;
  I2cHandle.Init.OwnAddress1     = 0x11;
  I2cHandle.Init.AddressingMode  = I2C_ADDRESSINGMODE_7BIT;
  I2cHandle.Init.DualAddressMode = I2C_DUALADDRESS_DISABLE;
  I2cHandle.Init.OwnAddress2Masks = I2C_OA2_NOMASK;
  I2cHandle.Init.OwnAddress2     = 0xFF;
  I2cHandle.Init.GeneralCallMode = I2C_GENERALCALL_DISABLE;
  I2cHandle.Init.NoStretchMode   = I2C_NOSTRETCH_DISABLE;  
	
	HAL_I2C_Init(&I2cHandle);
	/* Enable the Analog I2C Filter */
  HAL_I2CEx_ConfigAnalogFilter(&I2cHandle,I2C_ANALOGFILTER_ENABLE);
}


/*Basic I2C read and write stuff*/
void write(uint8_t addr,uint8_t reg, uint8_t val)
{
    uint8_t reg1[2] = {reg, val};
    HAL_I2C_Master_Transmit(&I2cHandle, addr << 1, reg1, 2,100);
		
/*		while (HAL_I2C_GetState(&I2cHandle) != HAL_I2C_STATE_READY)
      {
      } */

}


uint8_t read(uint8_t addr,uint8_t reg) {

    uint8_t rxdata;
    HAL_I2C_Master_Transmit(&I2cHandle, addr << 1, &reg, 1, 100);

	/*	while (HAL_I2C_GetState(&I2cHandle) != HAL_I2C_STATE_READY)
      {
      } */

    HAL_I2C_Master_Receive(&I2cHandle, addr << 1, &rxdata, 1,100);

	/*	while (HAL_I2C_GetState(&I2cHandle) != HAL_I2C_STATE_READY)
      {
      } */

    return rxdata;
}

void writeBit(uint8_t reg, uint8_t bit) {
    uint8_t val = read(0x2A,reg) | (1<<bit);
    write(0x2A,reg, val);
}

void clearBit(uint8_t reg, uint8_t bit) {
    uint8_t val = read(0x2A,reg) & ~(1<<bit);
    write(0x2A,reg, val);
}

bool readBit(uint8_t reg, uint8_t bit) {
    //create bitmask
    uint8_t bitmask = 1<<bit;
    if(read(0x2A,reg) & bitmask) {
        return true;
    }
    return false;
}

uint32_t read24(uint8_t reg) {
    uint32_t val;
		//uint8_t	data24[4];
	
	  HAL_I2C_Master_Transmit(&I2cHandle, (0x2A) << 1, &reg, 1, 100);

		/*while (HAL_I2C_GetState(&I2cHandle) != HAL_I2C_STATE_READY)
      {
      } */

    HAL_I2C_Master_Receive(&I2cHandle, (0x2A) << 1, data24, 3, 100);

	/*	while (HAL_I2C_GetState(&I2cHandle) != HAL_I2C_STATE_READY)
      {
      } */

    val = data24[0];    // receive high byte
    val <<= 8;            // shift byte to make room for new byte
    val |= data24[1];   // receive mid byte
    val <<= 8;            // shift both bytes
    val |= data24[2];   // receive low byte
    return val;
}


void readUntilTrue(uint8_t reg, uint8_t bit) {
    //create bitmask
    uint8_t bitmask = 1<<bit;
    uint8_t r1;
    bool readUntil = false;
    //Just keep reading until bit requested is true
    while(readUntil == false) {
        r1 = read(0x2A,reg);
        if(r1 & bitmask) {
            readUntil = true;
        }
    }
}

void readUntilFalse(uint8_t reg, uint8_t bit) {
    //create bitmask
    uint8_t bitmask = 1<<bit;
    bool readUntil = false;
    //Just keep reading until bit requested is false
    while(readUntil == false) {
        if(~read(0x2A,reg) & bitmask) {
            readUntil = true;
        }
    }
}

void calibrate() {
    writeBit(NAU7802_CTRL2, NAU7802_CALS);        //Begin calibration
    readUntilFalse(NAU7802_CTRL2, NAU7802_CALS);  //Wait for calibration to finish
}


void resetSettings() {
    writeBit(NAU7802_PU_CTRL, NAU7802_RR);        //Reset Registers
    clearBit(NAU7802_PU_CTRL, NAU7802_RR);        //Clear Reset Registers
    writeBit(NAU7802_PU_CTRL, NAU7802_PUD);       //Power up digital

    readUntilTrue(NAU7802_PU_CTRL, NAU7802_PUR);  //Wait until power up
    writeBit(NAU7802_PU_CTRL, NAU7802_PUA);       //Power up analog

    writeBit(NAU7802_ADC_REG, 4);                 //Diasble chopper funcition
    writeBit(NAU7802_ADC_REG, 5);                 //Diasble chopper funcition

    writeBit(NAU7802_PGA_REG, 0);                 //Diasble chopper funcition
    writeBit(NAU7802_PGA_REG, 4);                 //Bypass PGA
    //	writeBit(NAU7802_PGA_REG,6);

    writeBit(NAU7802_PU_CTRL, NAU7802_AVDDS);   //Enable LDO

    writeBit(NAU7802_CTRL1, NAU7802_VLDO2);     //Set AVCC to 4.5V
    writeBit(NAU7802_CTRL1, NAU7802_VLDO1);     //Set AVCC to 4.5V
    clearBit(NAU7802_CTRL1, NAU7802_VLDO0);     //Set AVCC to 4.5V

    //	writeBit(NAU7802_CTRL1, NAU7802_GAINS2);
    //	writeBit(NAU7802_CTRL1, NAU7802_GAINS1);
    //	clearBit(NAU7802_CTRL1, NAU7802_GAINS0);

    //	writeBit( 0x1C, 7);


    calibrate();                                  //Calibrate

    writeBit(NAU7802_I2C_CTRL, NAU7802_SPE);      //Enable Strong Pullup

    writeBit(NAU7802_PU_CTRL,   NAU7802_CS);      //Start Conversion
}

uint32_t readADC() {
	
  readUntilTrue(NAU7802_PU_CTRL,NAU7802_CR);
  uint32_t adcVal = read24(NAU7802_ADC_B2);
	
	//NRF_LOG_INFO("RAW: %x \r\n", adcVal);

  writeBit(NAU7802_PU_CTRL, NAU7802_CS);
	
  return adcVal;
}




