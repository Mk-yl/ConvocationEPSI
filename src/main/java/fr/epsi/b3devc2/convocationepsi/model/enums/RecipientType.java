package fr.epsi.b3devc2.convocationepsi.model.enums;

public enum RecipientType {
    CANDIDAT ("Candidat"),
    JURY ("Jury");
    private final String displayName;

    RecipientType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}