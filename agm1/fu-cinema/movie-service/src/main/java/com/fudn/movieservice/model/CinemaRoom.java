package com.fudn.movieservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "cinema_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CinemaRoom {

    @Id
    private String roomId;

    private String roomName;

    private RoomType roomType;
    private Integer seatRows;
    private Integer seatsPerRow;
    private RoomStatus roomStatus;
}
