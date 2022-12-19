package uk.co.gresearch.siembol.response.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.reinert.jjschema.Attributes;

@Attributes(title = "matcher", description = "Matcher for matching fields in response rules")
public class MatcherDto {
    @JsonProperty("matcher_type")
    @Attributes(required = true,
            description = "Type of matcher, either Regex match or list of strings (newline delimited)")
    private MatcherTypeDto type;

    @JsonProperty("is_negated")
    @Attributes(description = "The matcher is negated")
    private Boolean negated = false;

    @Attributes(required = true, description = "Field on which the matcher will be evaluated")
    private String field;

    @JsonProperty("case_insensitive")
    @Attributes(description = "Use case insensitive string compare")
    private Boolean caseInsensitiveCompare = false;

    @Attributes(required = true, description = "Matcher expression as defined by matcher type")
    private String data;

    public Boolean getNegated() {
        return negated;
    }

    public void setNegated(Boolean negated) {
        this.negated = negated;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Boolean getCaseInsensitiveCompare() {
        return caseInsensitiveCompare;
    }

    public void setCaseInsensitiveCompare(Boolean caseInsensitiveCompare) {
        this.caseInsensitiveCompare = caseInsensitiveCompare;
    }

    public MatcherTypeDto getType() {
        return type;
    }

    public void setType(MatcherTypeDto type) {
        this.type = type;
    }
}