#include "i2c_dr.h"
#include "NAU7802.h"

/* TWI instance. */
static const nrf_drv_twi_t m_twi = NRF_DRV_TWI_INSTANCE(1);
/* Indicates if operation on TWI has ended. */
volatile bool m_xfer_done = false;
uint8_t data24[3] = {0x00,0x00,0x00};

/*Basic I2C read and write stuff*/
void write(uint8_t addr,uint8_t reg, uint8_t val)
{
    ret_code_t err_code1;
    m_xfer_done = false;
    uint8_t reg1[2] = {reg, val};
    err_code1 = nrf_drv_twi_tx(&m_twi, addr, reg1, 2, false);

    //APP_ERROR_CHECK(err_code);
    while (m_xfer_done == false);
}


uint8_t read(uint8_t addr,uint8_t reg) {

    uint8_t rxdata;
    ret_code_t err_code;
    m_xfer_done = false;
    err_code = nrf_drv_twi_tx(&m_twi, addr, &reg, 1, true);
    //APP_ERROR_CHECK(err_code);
    while (m_xfer_done == false);
    //nrf_delay_ms(20);
    /* Read 1 byte from the specified address  */
    m_xfer_done = false;

    err_code = nrf_drv_twi_rx(&m_twi, addr, &rxdata, 1);

    while (m_xfer_done == false);

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

    m_xfer_done = false;
    nrf_drv_twi_tx(&m_twi, 0x2A, &reg, sizeof(reg), true);
    while (m_xfer_done == false);

    m_xfer_done = false;

    nrf_drv_twi_rx(&m_twi, 0x2A, data24, 3);
    while (m_xfer_done == false);

    val = data24[0];    // receive high byte
    val <<= 8;            // shift byte to make room for new byte
    val |= data24[1];   // receive mid byte
    val <<= 8;            // shift both bytes
    val |= data24[2];   // receive low byte
    return val;
}

uint32_t read32(uint8_t reg) {
    uint32_t val;

    m_xfer_done = false;
    nrf_drv_twi_tx(&m_twi, 0x2A, &reg, sizeof(reg), true);
    while (m_xfer_done == false);

    uint8_t data32[4] = {0x00,0x00,0x00,0x00};
    nrf_drv_twi_rx(&m_twi, 0x2A, data32, 4);
    val = data32[0];
    val <<= 8;
    val |= data32[1];
    val <<= 8;
    val |= data32[2];
    val <<= 8;
    val |= data32[3];
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

/**
 * @brief TWI events handler.
 */
void twi_handler(nrf_drv_twi_evt_t const * p_event, void * p_context)
{
    switch (p_event->type)
    {
    case NRF_DRV_TWI_EVT_DONE:
        if (p_event->xfer_desc.type == NRF_DRV_TWI_XFER_RX)
        {
            //NRF_LOG_INFO("WOWOWOWOW %x  %x  %x\r\n",data24[2],data24[1],data24[0]);
        }
        m_xfer_done = true;
        break;
    default:
        break;
    }
}

/**
 * @brief UART initialization.
 */
void twi_init (void)
{
    ret_code_t err_code;

    const nrf_drv_twi_config_t twi_lm75b_config = {
        .scl                = 4,
        .sda                = 28,
        .frequency          = NRF_TWI_FREQ_100K,
        .interrupt_priority = APP_IRQ_PRIORITY_HIGH
        // .clear_bus_init     = true
        // .hold_bus_uninit    = true
    };

    err_code = nrf_drv_twi_init(&m_twi, &twi_lm75b_config, twi_handler, NULL);
    //APP_ERROR_CHECK(err_code);

    nrf_drv_twi_enable(&m_twi);
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


/*double readADC() {
	
  readUntilTrue(NAU7802_PU_CTRL,NAU7802_CR);
  uint32_t adcVal = read24(NAU7802_ADC_B2);
	
  writeBit(NAU7802_PU_CTRL, NAU7802_CS);
  if(adcVal & 0x00800000){

    adcVal = ~adcVal+1;
		
    adcVal = (adcVal & 0x00FFFFFF);
		return -1*(_avcc/16777216)*(double)adcVal;
  }
	
  return (_avcc/16777216)*(double)adcVal;
}*/

uint32_t readADC() {
	
  readUntilTrue(NAU7802_PU_CTRL,NAU7802_CR);
  uint32_t adcVal = read24(NAU7802_ADC_B2);
	
  writeBit(NAU7802_PU_CTRL, NAU7802_CS);
	
  return adcVal;
}



