"""
Readings Collector
"""
import serial
import re
import requests
import http.client
import json
import time
import traceback
import sys
import math

__version__ = '1.0'
__author__ = "ebarsallo"


# Serial Connection Parameters
DEFAULT_PORT = 'COM8'			# Connection Port
SPEED = 115200					# Baud Rate

# Server Connection Parameters
SERVER = '128.46.73.217:5001'	# url connection (web server)
BATCH = 10						# Batch limit used for uploading readings

PATTERN = '{(.*)}: LoRaWAN Transmitting to Gateway {(.*)} {(.*)} read: {(.*)} read: {(.*)}'

def connect(port):
	"""
		Establish a connection to the serial port and start to collect readings
		from the serial port
	"""
	list = []
	reading = {}

	print("Collecting readings from " + port)

	# Birck Nanotechonology Center
	longitude = [-86.922967, -86.922943, -86.923098, -86.922741, -86.922559, -86.925159, -86.925006, -86.924111, -86.924081, -86.924656]
	latitude = [40.423488, 40.423991, 40.423828, 40.423510, 40.424067, 40.422974, 40.422283, 40.422349, 40.422956, 40.423248]

	input = serial.Serial(
		port=port, 
		baudrate=SPEED,
		timeout=3
		)
	
	while input.isOpen():
		data = input.readline()

		if data.__len__() > 0:

			#data = data.rstrip()
			#data = data.lstrip()
			#print(data)

			pattn = re.compile(PATTERN)
			match = pattn.search(data.decode('utf-8'))
			if match:
				node = match.group(2)
				read1 = match.group(4) 
				read2 = match.group(5)

				read2f = float(read2)
				if (read2f > 0):
					read2f = read2f * -1
				nitrate = 2e-5 * math.exp(-0.045 * read2f * 1000)
				
				print(node + ": " + read1 + " * " + str(read2f) + " * " + str(nitrate))
				
				reading = {
				'name': node, 
				'location': [longitude[int(node)-1], latitude[int(node)-1]], 
				'timestamp': time.time(),
				'nitrate': float (nitrate), 
				'temp': int (read1)}

				list.append(reading)

				# send readings to server in batches
				if ( len (list) > BATCH ):
					print("Uploading readings to server ...")

					try:
						send_to_server( list )
					except:
						print("Communication Error ...")
						traceback.print_exc()

					list = [] 

	input.close()


def send_to_server(data):
	"""
		Send the data (readings) to the Web Server

		:param data: list of dictionary (keys: name, temp, nitrate) 
	"""
	js_data = json.dumps(data)
	connection = http.client.HTTPConnection(SERVER)

	headers = {'Content-type': 'application/json'}
	connection.request('POST', '/', js_data, headers)

	response = connection.getresponse()
	print(json.loads(response.read().decode())['reply'])


def main():

	port = DEFAULT_PORT;

	if ( len (sys.argv) > 1 ):
		port = sys.argv[1];

	connect(port)


if __name__ == "__main__":
	main()