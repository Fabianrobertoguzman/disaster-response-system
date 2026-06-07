package edu.cqu.drs.model;

/**
 * The kinds of disaster a Citizen can report.
 *
 * <p>The Assessment&nbsp;2 specification names "a hurricane, a fire, an earthquake, etc."  - 
 * Assessment&nbsp;One referenced a {@code HazardType} enumeration on the Citizen Report Form
 * (Figure&nbsp;7) without listing its members, so the prototype defines the concrete six-value
 * set below: the specification's three examples plus three further distinct hazards for finer
 * triage routing. Adding a new hazard type is a single new constant here.</p>
 *
 * @author Fabian Roberto Guzman (12287570)
 */
public enum HazardType {

    /** Tropical cyclone / hurricane  -  one of the specification's named examples. */
    HURRICANE,

    /** Structural, bush or industrial fire  -  one of the specification's named examples. */
    FIRE,

    /** Seismic event  -  one of the specification's named examples. */
    EARTHQUAKE,

    /** Flooding (riverine, flash or storm-surge). */
    FLOOD,

    /** Severe storm event other than a cyclone (e.g. supercell, hail, wind). */
    STORM,

    /** Hazardous-material release (chemical, biological or radiological). */
    HAZMAT
}
