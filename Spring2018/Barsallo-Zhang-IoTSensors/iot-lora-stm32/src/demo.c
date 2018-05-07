#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <limits.h>
#include <stdbool.h>

#include "mesh.h"
#include "demo.h"


void demo_SetupNode(MeshInfo_t *mesh, MeshEntry_t rt[MESH_MAX_ROUTING_TABLE_ENTRIES], node_t nt[MESH_MAX_ROUTING_TABLE_ENTRIES]) 
{

	switch ( mesh->id )
	{
		
		// Mesh #1 (Morgan)
		case 1:
			// Node 1
			mesh->type = MESH_NODE_TYPE_ROUTER;
			mesh->slicerMax   = MESH_MAX_ID + 3;
			mesh->slicerBase  = 1;
			mesh->slicerDelay = 300;
		
			rt[0].dest = MESH_NODE_GATEWAY;
			rt[0].next = 2;
																		
			rt[1].dest = MESH_NODE_GATEWAY;
			rt[1].next = 3;
			
			nt[0] = 4;
			break;
		
		case 2:
			// Node 2
			mesh->type = MESH_NODE_TYPE_BORDER;
			mesh->slicerMax  = MESH_MAX_ID * 2;
			mesh->slicerBase = 1;
			mesh->slicerDelay = 500;
		
			rt[0].dest = MESH_NODE_GATEWAY;
			rt[0].next = MESH_NODE_GATEWAY;
			
			nt[0] = 1;
			nt[1] = 3;
			nt[2] = 5;
			break;
		
		case 3:
			// Node 3
			mesh->type = MESH_NODE_TYPE_ROUTER;
			mesh->slicerMax   = MESH_MAX_ID + 2;
			mesh->slicerBase  = 1;
			mesh->slicerDelay = 300;
		
			rt[0].dest = MESH_NODE_GATEWAY;
			rt[0].next = 2;

			nt[0] = 1;
			nt[1] = 5;
			break;
		
		case 4:
			// Node 3
			mesh->type = MESH_NODE_TYPE_ROUTER;
			mesh->slicerMax   = MESH_MAX_ID;
			mesh->slicerBase  = 3;
			mesh->slicerDelay = 1;
		
			rt[0].dest = MESH_NODE_GATEWAY;
			rt[0].next = 1;

			rt[1].dest = MESH_NODE_GATEWAY;
			rt[1].next = 5;
		
			nt[0] = 0;
			break;
		
		
		case 5:
			// Node 3
			mesh->type = MESH_NODE_TYPE_ROUTER;
			mesh->slicerMax   = MESH_MAX_ID + 1;
			mesh->slicerBase  = 1;
			mesh->slicerDelay = 200;
		
			rt[0].dest = MESH_NODE_GATEWAY;
			rt[0].next = 2;

			rt[1].dest = MESH_NODE_GATEWAY;
			rt[1].next = 3;
		
			nt[0] = 4;
			break;

		// Mesh #2 (Birck)
		case 6:
			// Node 6
			mesh->type = MESH_NODE_TYPE_ROUTER;
			mesh->slicerMax   = MESH_MAX_ID2;
			mesh->slicerBase  = 5;
			mesh->slicerDelay = 100;
		
			rt[0].dest = MESH_NODE_GATEWAY;
			rt[0].next = 7;
																		
			rt[1].dest = MESH_NODE_GATEWAY;
			rt[1].next = 8;
			
			nt[0] = 9;
			break;
		
		case 7:
			// Node 7
			mesh->type = MESH_NODE_TYPE_BORDER;
			mesh->slicerMax  = MESH_MAX_ID2 + 3;
			mesh->slicerBase = 5;
			mesh->slicerDelay = 200;
		
			rt[0].dest = MESH_NODE_GATEWAY;
			rt[0].next = MESH_NODE_GATEWAY;
			
			nt[0] = 6;
			nt[1] = 8;
			break;
		
		case 8:
			// Node 8
			mesh->type = MESH_NODE_TYPE_ROUTER;
			mesh->slicerMax   = MESH_MAX_ID2 + 1;
			mesh->slicerBase  = 5;
			mesh->slicerDelay = 100;
		
			rt[0].dest = MESH_NODE_GATEWAY;
			rt[0].next = 7;

			nt[0] = 9;
			break;
		
		case 9:
			// Node 9
			mesh->type = MESH_NODE_TYPE_ROUTER;
			mesh->slicerMax   = MESH_MAX_ID2;
			mesh->slicerBase  = 6;
			mesh->slicerDelay = 100;
		
			rt[0].dest = MESH_NODE_GATEWAY;
			rt[0].next = 6;

			rt[1].dest = MESH_NODE_GATEWAY;
			rt[1].next = 8;
		
			nt[0] = 0;
			break;
	}

}
