package com.basketballbot;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
public class Game {
    @JsonProperty("GameDate")
    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate gameDate;
    @JsonProperty("GameTimeMsk")
    private LocalTime gameTime;
    @JsonProperty("TeamAid")
    private Integer firstTeamId;
    @JsonProperty("TeamBid")
    private Integer secondTeamId;
    @JsonProperty("TeamNameAru")
    private String firstTeamName;
    @JsonProperty("TeamNameBru")
    private String secondTeamName;
    @JsonProperty("ArenaRu")
    private String court;
    @JsonProperty("GameStatus")
    private Integer status;
    @JsonProperty("ScoreA")
    private Integer firstTeamScore;
    @JsonProperty("ScoreB")
    private Integer secondTeamScore;
}