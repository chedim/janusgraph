package com.thinkaurelius.titan.graphdb.database.idassigner;


import com.google.common.base.Preconditions;
import com.thinkaurelius.titan.core.TitanLabel;
import com.thinkaurelius.titan.core.TitanType;
import com.thinkaurelius.titan.core.TitanKey;
import com.thinkaurelius.titan.diskstorage.IDAuthority;
import com.thinkaurelius.titan.graphdb.relations.InternalRelation;
import com.thinkaurelius.titan.graphdb.idmanagement.IDManager;
import com.thinkaurelius.titan.graphdb.vertices.InternalTitanVertex;

import java.util.Random;


public class SimpleVertexIDAssigner implements VertexIDAssigner {
    
	private final IDPool node;
	private final IDPool edge;
	private final IDPool edgeType;

	private final IDManager idManager;
	private final long maxPartitionID;
    private final int offsetBits;
    private final int offset;
	
	private final Random randomSource;
	
	
	public SimpleVertexIDAssigner(IDManager idManager, IDAuthority storage, int randomBits, long baseBlockSize) {
        Preconditions.checkNotNull(idManager);
        Preconditions.checkNotNull(storage);
        Preconditions.checkArgument(randomBits>=0 && randomBits<=8,"RandomBits must be in [0,8]");
        
		this.idManager = idManager;
		this.maxPartitionID = idManager.getMaxPartitionID();
        this.offsetBits = randomBits;
        this.randomSource = new Random();
        this.offset = randomSource.nextInt((1<<randomBits));

        storage.setIDBlockSizer(new SimpleVertexIDBlockSizer(baseBlockSize));
        
		node = new StandardIDPool(storage,IDManager.IDType.Node.addPadding(offset));
		edge = new StandardIDPool(storage,IDManager.IDType.Edge.addPadding(offset));
		edgeType = new StandardIDPool(storage,IDManager.IDType.EdgeType.addPadding(offset));

	}

    @Override
    public long getNewID(InternalTitanVertex node) {
        assert !node.hasID();
        if (node instanceof InternalRelation) {
            return nextEdgeID();
        } else if (node instanceof TitanKey) {
            return nextPropertyTypeID(((TitanType)node).getGroup().getID());
        } else if (node instanceof TitanLabel) {
            return nextRelationshipTypeID(((TitanType)node).getGroup().getID());
        } else {
            return nextNodeID();
        }
    }
    
    @Override
    public long getNewID(IDManager.IDType type, long groupid) {
        switch (type) {
            case Edge : return nextEdgeID();
            case PropertyType: return nextPropertyTypeID(groupid);
            case RelationshipType: return nextRelationshipTypeID(groupid);
            case Node: return nextNodeID();
            default: throw new IllegalArgumentException("ID type not supported: " + type);
        }
    }

    @Override
    public IDManager getIDManager() {
        return idManager;
    }

    @Override
	public synchronized void close() {
		node.close();
		edge.close();
		edgeType.close();
	}

    private final long getPartitionID() {
        if (maxPartitionID==0) return 0;
        else return randomSource.nextInt((int)maxPartitionID)+1;
    }

	private long nextEdgeID() {
		return idManager.getEdgeID(padID(edge.nextID()));
	}

	private long nextNodeID() {
		return idManager.getNodeID(padID(node.nextID()), getPartitionID());
	}

	public long nextPropertyTypeID(long groupid) {
		return idManager.getPropertyTypeID(padID(edgeType.nextID()), groupid, getPartitionID());
	}

	private long nextRelationshipTypeID(long groupid) {
		return idManager.getRelationshipTypeID(padID(edgeType.nextID()), groupid, getPartitionID());
	}

    private long padID(long id) {
        return (id<<offsetBits) + offset;
    }
	
	private static class SimpleVertexIDBlockSizer implements IDBlockSizer {

        private static final int AVG_EDGES_PER_VERTEX = 10;
        private static final int DEFAULT_NUM_EDGE_TYPES = 50;

        private final long baseBlockSize;

        SimpleVertexIDBlockSizer(final long size) {
            Preconditions.checkArgument(size>0 && size<Integer.MAX_VALUE);
            this.baseBlockSize=size;
        }
        
        @Override
        public long getBlockSize(int partitionID) {
            if (IDManager.IDType.Node.is(partitionID)) {
                return baseBlockSize;
            } else if (IDManager.IDType.Edge.is(partitionID)) {
                return baseBlockSize*AVG_EDGES_PER_VERTEX;
            } else if (IDManager.IDType.EdgeType.is(partitionID)) {
                return DEFAULT_NUM_EDGE_TYPES;
            } else throw new IllegalArgumentException("Invalid partition id: " + partitionID);
        }
    }
	
}
