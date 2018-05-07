#ifndef __MESH_H__
#define __MESH_H__


// TODO: enable msg sending between nodes (right now, is only possible to send msg to the gateway.

/*!
 * \brief Max number of entries on the routing table
 */
#define MESH_MAX_ROUTING_TABLE_ENTRIES 3

/*!
 * \brief Gateway node id
 */
#define MESH_NODE_GATEWAY 99

/*!
 * \brief Constant used to represent the destination node (multicast, broadcast)
 */
#define MESH_DST_MULTICAST 0
#define MESH_DST_BROADCAST 101

/*!
 * \brief Communication mode (0 - Peer to Peer, 1 - Multicast)
 */
#define MESH_COMM_MODE_P2P 0
#define MESH_COMM_MODE_MULTICAST 1
#define MESH_COMM_MODE 1


#define MESH_NOT_FOUND UINT_MAX

/*!
 * \brief Node
 */
typedef uint8_t node_t;

/*!
 * \brief Types of Message interchanged in the mesh
 */
typedef enum
{
    MESH_TYPE_DATA     = 0,		/** Data transmission */
		MESH_TYPE_ACK      = 1,		/** ACK or Acknowledge packet */
		MESH_TYPE_BEACON   = 2,		/** Join request */
	  MESH_TYPE_JOIN_REQ = 3,	  /** Join Request */
		MESH_TYPE_JOIN_RPL = 4, 	/** Join Reply */
		MESH_TYPE_JOIN_ACT = 5,		/** Join Accept */
		MESH_TYPE_JOIN_ACK = 6,		/** Join Ack */
	  MESH_TYPE_DOWN     = 7,		/** Node is down (report) */
		MESH_TYPE_NULL     = 99		/** Null */
} TypeMsg_t;

/*!
 * \brief Node status (in mesh network)
 */
typedef enum
{
    MESH_NODE_STATUS_NOT_CONNECTED = 0,		/** The node is in a initial status (not connected to the mesh network) */
	  MESH_NODE_STATUS_JOIN          = 1,		/** The node initiated the join process */
		MESH_NODE_STATUS_CONNECTED     = 2,		/** The node is connected to the mesh network */
		MESH_NODE_STATUS_UNKNOWN       = 3		/** The node is in a unknown status (isolated) */
} NodeStatus_t;

/*!
 * \brief Types of Nodes in the mesh
 */
typedef enum
{
    MESH_NODE_TYPE_ROUTER = 0,	/** The node route packet in the mesh */
	  MESH_NODE_TYPE_BORDER = 1		/** The node is connected directly to the Gateway */
} NodeType_t;


/*!
 * \brief Entries on the Routing Table
 */
typedef struct MeshEntry_s {
	node_t dest;
	node_t prev;
	node_t next;	/** NOT USED */
} MeshEntry_t;

/*!
 * \brief Mesh node primary info
 */
typedef struct MeshInfo_s {
	node_t 	   id;		/** Node identifier */
	uint8_t	   hop;  	/** Hops to Gateway (metric) */
	NodeType_t type;	/** Type of the node */
	
	int    slicerMax;		/** demo purpose */
	int    slicerBase;  /** demo purpose */
	int 	 slicerDelay;	/** demo purpose */
	
} MeshInfo_t;

/*!
 * \brief Packet structure (64 bytes)
 * Map between MeshPacket_s and uint8_t*
 *     0: Source (node-id)
 *     1: Destination (node-id)
 *     2: Previous hop (node-id)
 *     3: Next hop (node-id)
 *     4: Type of Message
 *     5: Payload Size
 *  6-63: Payload 
 */
typedef struct MeshPacket_s {
	uint8_t src;
	uint8_t dst;
	uint8_t prv;
	uint8_t nxt;
	uint8_t typ;
	uint8_t siz;
	uint8_t payload [59];
} MeshPacket_t;


//void mesh_join(MeshInfo_t node);

bool mesh_isneighbor(node_t *neighbors, node_t dest);

//void mesh_GetType (const char buffer);

// TODO: Change name
bool mesh_IsData(const char *buffer, node_t *neighbors, MeshInfo_t node);

/*!
 * \brief Get the type of the message. Returns the type of message (TypeMsg_t) if
 * the message is intended to the receiver. Otherwise returns MESTH_TYPE_NULL.
 *
 * \param [IN] buffer Message
 * \param [IN] node Receiver (node) info
 */
TypeMsg_t mesh_getTypeMsg(uint8_t *buffer, node_t *neighborsmesh_getTypeMsg, MeshInfo_t node);

/*!
 * \brief Get the next hop to reach a destination using the routing protocol. Returns
 * the identifier of the node for the next hop. Otherwise returns MESH_NOT_FOUND
 *
 * \param [IN] buffer Message
 * \param [IN] node Receiver (node) info
 */
node_t mesh_GetNextHop(MeshEntry_t *routing, node_t dest);

/*!
 * \brief Send a buffer (msg) to another node (dest) in the mesh.
 *
 * \param [IN] dest Sender
 * \param [IN] buffer Buffer (message) intended to send
 */
void mesh_send(node_t dest, uint8_t *buffer);

#endif // __MESH_H__
