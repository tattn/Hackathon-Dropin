package io.github.axxiss.places.enums;

/**
 * Available request's params.
 *
 * @author Axxiss
 */
public enum Params {

    Key,
    Keyword,
    Input,
    Reference,
    Sensor,
    Query,
    Location,
    Radius,
    Types,
    RankBy,
    Name,
    MinPrice,
    MaxPrice,
    OpenNow,
    ZagatSelected,
    PageToken,
    PhotoReference,
    Language,
    Page,
    Extensions,
    MaxHeight,
    MaxWidth;

    public String getValue() {
        return this.name().toLowerCase();
    }

}
