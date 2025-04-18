package editor.nodes;

import java.util.ArrayList;
import java.util.List;

public class Tree {

    public int nextNodeId = 1;

    
    public class TreeNode {
        public final int nodeId;
        public TreeNode parent;
        public List<TreeNode> children;

        public TreeNode() {
            nodeId = nextNodeId++;
            children = new ArrayList<>();
        }
        
    }

}
