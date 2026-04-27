package id.naturalsmp.naturalinteraction.facts;

/**
 * Type of value stored in a Fact.
 * Facts are always stored as String internally and cast on read.
 */
public enum FactType {
    BOOLEAN,  // "true" / "false"
    INTEGER,  // "42"
    FLOAT,    // "3.14"
    STRING    // Arbitrary string
}
