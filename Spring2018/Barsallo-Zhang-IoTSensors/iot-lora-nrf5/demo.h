#ifndef __DEMO_H__
#define __DEMO_H__


#define MESH_MAX_ID  5
#define MESH_MAX_ID2 9

/*!
 * \brief Setup routing/neighbors tables for a node. Configuration is done statically based
 * on an already predefined topology. For use for demo purpose.
 *
 * \param [IN] id
 * \param [OUT] rt Routing Table
 * \param [OUT] nt Neighbors Table
 */
void demo_SetupNode(MeshInfo_t *mesh, MeshEntry_t rt[MESH_MAX_ROUTING_TABLE_ENTRIES], node_t nt[MESH_MAX_ROUTING_TABLE_ENTRIES]);


#endif // __DEMO_H__
