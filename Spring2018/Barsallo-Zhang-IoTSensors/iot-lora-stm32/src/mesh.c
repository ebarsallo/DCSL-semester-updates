#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <limits.h>
#include <stdbool.h>

#include "mesh.h"

// MeshEntry_t rt[MAX_ROUTING_TABLE_ENTRIES];
// node_t nb[MESH_MAX_ROUTING_TABLE_ENTRIES];

// TODO: It makes more sense to define mesh as an struct with methods, similar to radio.h
//       radio.h initialization is on: sx1272mb2das.c
//       check: https://stackoverflow.com/questions/12642830/possible-to-define-a-function-inside-a-c-structure

//void mesh_join(MeshInfo_t node)
//{
//}

bool mesh_isNeighbor(node_t *neighbors, node_t prev)
{
	int i;
	
	for ( i = 0; i < MESH_MAX_ROUTING_TABLE_ENTRIES; i++ )
	{
		if ( neighbors[i] == prev) return true;
	}
	
	// Not found
	return false;
}

//void mesh_send(node_t dest, uint8_t *buffer)
//{
//}


//void mesh_GetType (const char buffer)
//{
//}

//// TODO: Change name for one more appropiate
//bool mesh_IsData(const char *buffer, node_t *neighbors, MeshInfo_t node)
//{
//	// Verify if the message is type `data`
//	if ( buffer[4] == MESH_TYPE_DATA )		
//	{
//		// TODO: Check DST if it's a multicast, then do by NEIGHBORS
//		// Verify if the message is comes from a neighbor of the node
//		if ( mesh_isNeighbor( neighbors, buffer[2] ) ) 
//		{
//			return true;
//		}
//	}	
//	
//	return false;
//}

TypeMsg_t mesh_getTypeMsg(uint8_t *buffer, node_t *neighbors, MeshInfo_t node)
{
	node_t next = buffer[3];
	node_t prev = buffer[2];
	TypeMsg_t type = (TypeMsg_t) buffer[4];
	
	if ( next == MESH_DST_BROADCAST )
	{
		// BROADCAST:
		// Every node should receive the msg
		return type;
	}
	else if ( next == MESH_DST_MULTICAST )
	{
		// MULTICAST:
		// Check if the source correspond to a neighbor of node
		if ( mesh_isNeighbor( neighbors, prev ) )
		{
			return type;
		}
		
	}
	else
	{
		// P2P:
		// Check if the message is intended (dst) to the node (node.id)
		if ( node.id == next ) 
		{
			return type;
		}
	}
	
	// NULL: if the messge is not inteded to the node
	return MESH_TYPE_NULL;
}

node_t mesh_GetNextHop(MeshEntry_t *routing, node_t dest)
{
	int i;
	
	// BROADCAST:
	// The message should reach all nodes possible
	if ( dest == MESH_DST_BROADCAST )
	{
		return dest;
	}
	
	// MULTICAST: 
	// No need to look into the RT 
	if ( MESH_COMM_MODE == MESH_COMM_MODE_MULTICAST ) 
	{		
		return MESH_DST_MULTICAST;
	}
	
	// P2P:
	// Find the next hop in the RT
	for ( i = 0; i < MESH_MAX_ROUTING_TABLE_ENTRIES; i++ )
	{
		if ( routing[i].dest == dest) 
		{
			return routing[i].next;
		}
	}
	
	// FIXME: Check for implication of the `warning: #69-D: integer conversion resulted in truncation`
	return MESH_NOT_FOUND;
	
}

