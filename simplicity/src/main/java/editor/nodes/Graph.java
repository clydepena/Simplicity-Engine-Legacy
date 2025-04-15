package editor.nodes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Graph {
    public int nextNodeId = 1;
    public int nextPinId = 100;

    public final Map<Integer, GraphNode> nodes = new HashMap<>();

    public Graph() {
        // final GraphNode first = createGraphNode();
        // final GraphNode second = createGraphNode();
        // first.outputNodeId = second.nodeId;
    }

    public GraphNode createGraphNode() {
        final GraphNode node = new GraphNode(this, 5, 3);
        this.nodes.put(node.nodeId, node);
        return node;
    }

    public GraphTuple findByInput(final long inputPinId) {
        for (GraphNode node : nodes.values()) {
            int len = node.getInputPinId().length;
            for (int i = 0; i < len; i++) {
                if (node.getInputPinId()[i] == inputPinId) {
                    int id = node.getInputPinId()[i];
                    return new GraphTuple(node, id, i);
                }
            }
        }
        return null;
    }

    public GraphTuple findByOutput(final long outputPinId) {
        for (GraphNode node : nodes.values()) {
            int len = node.getOutputPinId().length;
            for (int i = 0; i < len; i++) {
                if (node.getOutputPinId()[i] == outputPinId) {
                    int id = node.getOutputPinId()[i];
                    return new GraphTuple(node, id, i);
                }
            }
        }
        return null;
    }

    public static final class GraphTuple {
        public final GraphNode node;
        public final int pinId;
        public final int pinIndex;

        public GraphTuple(GraphNode node, int pinId, int pinIndex) {
            this.node = node;
            this.pinId = pinId;
            this.pinIndex = pinIndex;
        }

        public boolean isNull(){
            if (node == null)
                return true;
            if (pinIndex == -1)
                return true;
            if (pinId == -1)
                return true;
            return false;
        }
    }

    public static final class GraphNode {
        public final int nodeId;
        public final int[] inputPinId;
        public final int[] outputPinId;
        public int[] outputNodeId, outputNodePinId;
        // public String[] inputName;
        // public String[] outputName;

        public GraphNode(Graph graph, int inputs, int outputs) {
            this.nodeId = graph.nextNodeId++;
            this.inputPinId = new int[inputs];
            this.outputPinId = new int[outputs];
            this.outputNodeId = new int[outputs];
            this.outputNodePinId = new int[outputs];

            for (int i = 0; i < inputPinId.length; i++) {
                inputPinId[i] = graph.nextPinId++;
            }

            for (int i = 0; i < outputPinId.length; i++) {
                outputPinId[i] = graph.nextPinId++;
                outputNodeId[i] = -1;
                outputNodePinId[i] = -1;
            }
        }

        public int[] getInputPinId() {
            return inputPinId;
        }

        public int[] getOutputPinId() {
            return outputPinId;
        }

        public String getName() {
            return "Node " + (char) (64 + nodeId) + " (" + nodeId + ")";
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
