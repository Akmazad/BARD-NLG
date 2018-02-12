package Bard.NLG.Generator;

import java.util.Objects;

public class Edge {

    public final NodeInfo source;
    public final NodeInfo target;

    public Edge(NodeInfo source, NodeInfo target){
        this.source = source;
        this.target = target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return Objects.equals(source, edge.source) &&
                Objects.equals(target, edge.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }
}
