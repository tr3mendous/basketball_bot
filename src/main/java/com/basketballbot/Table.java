package com.basketballbot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
public class Table {
    @JsonProperty("Place")
    private Integer place;
    @JsonProperty("CompTeamName")
    private TeamName teamName;
    @JsonProperty("Games")
    private Integer games;
    @JsonProperty("Won")
    private Integer won;
    @JsonProperty("Lost")
    private Integer lost;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    static class TeamName {
        @JsonProperty("CompTeamNameRu")
        private String teamName;
    }
}
