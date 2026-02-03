import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import ReactFlow, {
  Background,
  Controls,
  Position,
  useNodesState,
  useEdgesState,
  MarkerType,
} from 'reactflow';
import type { Node, Edge } from 'reactflow';
import 'reactflow/dist/style.css';
import dagre from '@dagrejs/dagre';
import { profileService } from '../services/profileService';
import { usePhotoMilestone } from '../contexts/PhotoMilestoneContext';
import { useProfile } from '../contexts/ProfileContext';
import type { MilestoneDto } from '../types';

const nodeWidth = 200;
const nodeHeight = 80;

const getLayoutedElements = (nodes: Node[], edges: Edge[], direction = 'TB') => {
  const dagreGraph = new dagre.graphlib.Graph();
  dagreGraph.setDefaultEdgeLabel(() => ({}));

  dagreGraph.setGraph({ rankdir: direction });

  nodes.forEach((node) => {
    dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight });
  });

  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target);
  });

  dagre.layout(dagreGraph);

  const layoutedNodes = nodes.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id);
    node.targetPosition = direction === 'TB' ? Position.Top : Position.Left;
    node.sourcePosition = direction === 'TB' ? Position.Bottom : Position.Right;

    // We are shifting the dagre node position (anchor=center center) to the top left
    // so it matches the React Flow node anchor point (top left).
    node.position = {
      x: nodeWithPosition.x - nodeWidth / 2,
      y: nodeWithPosition.y - nodeHeight / 2,
    };

    return node;
  });

  return { nodes: layoutedNodes, edges };
};

export default function MilestoneTreePage() {
  const [searchParams] = useSearchParams();
  const userIdParam = searchParams.get('userId');
  const userId = userIdParam ? parseInt(userIdParam, 10) : null;
  const navigate = useNavigate();

  const { profile: selfProfile } = useProfile();
  const { milestones: selfMilestones } = usePhotoMilestone();
  
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [loading, setLoading] = useState(true);

  // Helper to calculate color from percentage (0-100)
  const getProgressColor = (percentage: number) => {
    // Red: #ef4444 (239, 68, 68)
    // Yellow: #eab308 (234, 179, 8)
    // Green: #22c55e (34, 197, 94)
    
    // Ensure percentage is between 0 and 100
    const p = Math.max(0, Math.min(100, percentage));
    
    let r, g, b;
    
    if (p < 50) {
      // Red to Yellow
      const ratio = p / 50;
      r = Math.round(239 + (234 - 239) * ratio);
      g = Math.round(68 + (179 - 68) * ratio);
      b = Math.round(68 + (8 - 68) * ratio);
    } else {
      // Yellow to Green
      const ratio = (p - 50) / 50;
      r = Math.round(234 + (34 - 234) * ratio);
      g = Math.round(179 + (197 - 179) * ratio);
      b = Math.round(8 + (94 - 8) * ratio);
    }
    
    return `rgb(${r}, ${g}, ${b})`;
  };

  // Crafting slot style for nodes
  const getNodeStyle = (completionRate: number = 0) => {
    const color = getProgressColor(completionRate);
    return {
      background: '#1e1e1e',
      color: '#fff',
      border: `2px solid ${color}`,
      borderRadius: '8px',
      padding: '10px',
      width: nodeWidth,
      fontSize: '12px',
      textAlign: 'center' as const,
      boxShadow: `0 0 10px ${color}40`, // Add a subtle glow
      fontFamily: '"Minecraftia", monospace, sans-serif',
      display: 'flex',
      flexDirection: 'column' as const,
      justifyContent: 'center',
      alignItems: 'center',
    };
  };

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        let milestonesToMap: MilestoneDto[] = [];
        
        const isSelf = !userId || userId === selfProfile?.id;


        if (isSelf) {
          // Try to get data from profile service to ensure we have the hierarchy
          // The context might return a flat list or updated state, but let's be safe and fetch fresh for the tree
          if (selfMilestones) {
             milestonesToMap = selfMilestones;
          } 
        } else {
             const data = await profileService.getProfile(userId);
             milestonesToMap = data.milestones || [];
        }

        if (milestonesToMap.length === 0) {
            setNodes([]);
            setEdges([]);
            setLoading(false);
            return;
        }

        const newNodes: Node[] = [];
        const newEdges: Edge[] = [];

        // Recursive traversal function
        const traverse = (milestone: MilestoneDto, parentId: string | null = null) => {
             const nodeId = milestone.id.toString();
             // Use completionRate if available, otherwise calculate from completed boolean (100 or 0)
             const rate = milestone.completionRate !== undefined && milestone.completionRate !== null 
                ? milestone.completionRate * 100 
                : (milestone.completed ? 100 : 0);

             newNodes.push({
                id: nodeId,
                data: { label: `${milestone.task} (${Math.round(rate)}%)` },
                position: { x: 0, y: 0 },
                style: getNodeStyle(rate),
                type: 'default',
             });

             if (parentId) {
                newEdges.push({
                    id: `e${parentId}-${nodeId}`,
                    source: parentId,
                    target: nodeId,
                    type: 'smoothstep',
                    animated: rate < 100, // Animate if not fully complete
                    markerEnd: {
                        type: MarkerType.ArrowClosed,
                        color: getProgressColor(rate),
                    },
                    style: { stroke: getProgressColor(rate), strokeWidth: 2 },
                });
             }

             if (milestone.subMilestones && milestone.subMilestones.length > 0) {
                 milestone.subMilestones.forEach(sub => traverse(sub, nodeId));
             }
        };

        // Only iterate over root nodes (those without parents in the list if it's flat, or just all if it's hierarchical from top)
        // If the API returns a flat list of ALL nodes, we need to rebuild the hierarchy or just find roots.
        // Assuming API returns roots or we can filter. 
        // If the API returns a flat list where children are NOT in subMilestones but just have parentId, we need a different approach.
        // However, MilestoneDto has `subMilestones`, implying hierarchy.
        // Let's assume the API returns a list where top-level items are roots, OR we check parentId.
        
        // Filter for actual root nodes to start traversal if the list is mixed
        const roots = milestonesToMap.filter(m => m.parentId === null || m.parentId === undefined);
        
        // If roots is empty but we have milestones, maybe it's just a non-hierarchical list or we missed something.
        // Fallback: if no roots found by parentId, treat all input as roots (if they have subMilestones populated).
        const startNodes = roots.length > 0 ? roots : milestonesToMap;

        startNodes.forEach(root => traverse(root));

        // Compute layout
        const { nodes: layoutedNodes, edges: layoutedEdges } = getLayoutedElements(
            newNodes,
            newEdges
        );

        setNodes(layoutedNodes);
        setEdges(layoutedEdges);

      } catch (error) {
        console.error('Failed to fetch milestones for tree:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [userId, selfProfile, selfMilestones, setNodes, setEdges]);


  if (loading) {
     return (
        <div className="flex justify-center items-center h-screen bg-gray-900">
             <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#b99547]"></div>
        </div>
     );
  }

  return (
    <div className="h-screen w-screen bg-gray-900 flex flex-col">
        <div className="bg-gray-800 p-4 shadow-md flex justify-between items-center z-10">
            <h1 className="text-xl font-bold text-[#b99547]">
                Milestone Tree
            </h1>
            <button 
                onClick={() => navigate(-1)}
                className="text-gray-300 hover:text-white px-3 py-1 rounded border border-gray-600 hover:bg-gray-700"
            >
                Back
            </button>
        </div>
        <div className="flex-1">
            <ReactFlow
                nodes={nodes}
                edges={edges}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                fitView
                attributionPosition="bottom-right"
            >
                <Controls />
                <Background color="#aaa" gap={16} />
            </ReactFlow>
        </div>
    </div>
  );
}
