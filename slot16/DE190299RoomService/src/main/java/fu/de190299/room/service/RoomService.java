package fu.de190299.room.service;

import fu.de190299.room.dto.ApiResponseDTO;
import fu.de190299.room.dto.RoomDTO;

public interface RoomService {

    ApiResponseDTO createRoom(RoomDTO dto);

    ApiResponseDTO updateRoom(Long roomId, RoomDTO dto);

    ApiResponseDTO getRoomById(Long roomId);

    ApiResponseDTO setMaintenance(Long roomId);

    ApiResponseDTO getRooms(int page, int size, String roomType, String status);
}
