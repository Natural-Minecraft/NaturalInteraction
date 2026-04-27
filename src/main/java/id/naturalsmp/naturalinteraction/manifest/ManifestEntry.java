package id.naturalsmp.naturalinteraction.manifest;

/**
 * Base interface for all manifest entries.
 * Manifest entries are declarative — they define conditions and
 * what to display WHEN those conditions are met.
 */
public interface ManifestEntry {

    /** Unique identifier for this entry. */
    String getId();

    /** Entry type name (for serialization). */
    String getType();
}
