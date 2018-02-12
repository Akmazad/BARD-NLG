package Bard.NLG.Generator;

import java.util.Objects;

public class causalEdge {

    // --- --- --- Fields

    public final String source;
    public final String target;

    // --- --- --- Constructor

    public causalEdge(String source, String target) {
        this.source = source;
        this.target = target;
    }

    // --- --- --- Equals & HashCode & toString

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            causalEdge that = (causalEdge) o;
            return source.equals(that.source) && target.equals(that.target);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }

    @Override
    public String toString() {
        return "causalEdge{" +
                "source='" + source + '\'' +
                ", target='" + target + '\'' +
                '}';
    }
}
